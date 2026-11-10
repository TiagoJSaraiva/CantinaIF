package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Ponto unico de acesso ao banco de dados (pool HikariCP).
 *
 * Configuracao por variaveis de ambiente (ou propriedades de sistema com o mesmo nome):
 * <ul>
 *   <li>CANTINA_DB_URL - URL JDBC (ex.: jdbc:mysql://localhost:3306/cantinaif). Se ausente,
 *       usa H2 embutido em memoria, em modo MySQL, com schema e vendedor de demonstracao
 *       criados no boot.</li>
 *   <li>CANTINA_DB_USER - usuario (opcional para H2).</li>
 *   <li>CANTINA_DB_PASSWORD - senha (opcional para H2).</li>
 * </ul>
 */
public final class Database {

    public static final String DEFAULT_H2_URL =
            "jdbc:h2:mem:cantinaif;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private static HikariDataSource dataSource;

    private Database() {
    }

    /** Inicializa o pool a partir das variaveis de ambiente. Idempotente. */
    public static synchronized void init() {
        if (dataSource != null) {
            return;
        }
        String url = Config.get("CANTINA_DB_URL");
        if (url == null) {
            url = DEFAULT_H2_URL;
        }
        init(url, Config.get("CANTINA_DB_USER"), Config.get("CANTINA_DB_PASSWORD"));
    }

    /** Inicializa o pool com parametros explicitos (usado pelos testes). */
    public static synchronized void init(String url, String user, String password) {
        if (dataSource != null) {
            return;
        }
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        if (user != null) {
            cfg.setUsername(user);
        }
        if (password != null) {
            cfg.setPassword(password);
        }
        cfg.setPoolName("cantinaif");
        cfg.setMaximumPoolSize(10);
        if (isH2(url)) {
            cfg.setDriverClassName("org.h2.Driver");
        } else if (url.startsWith("jdbc:mysql:")) {
            cfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
        }
        dataSource = new HikariDataSource(cfg);
        if (isH2(url)) {
            try (Connection conn = dataSource.getConnection()) {
                applySchema(conn);
            } catch (SQLException | IOException e) {
                dataSource.close();
                dataSource = null;
                throw new IllegalStateException("Falha ao preparar o banco H2 embutido", e);
            }
        }
    }

    /** Conexao do pool; deve ser fechada pelo chamador (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            init();
        }
        return dataSource.getConnection();
    }

    /** Encerra o pool (fim da aplicacao ou dos testes). */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private static boolean isH2(String url) {
        return url.startsWith("jdbc:h2:");
    }

    private static void applySchema(Connection conn) throws SQLException, IOException {
        String script;
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IOException("Recurso nao encontrado no classpath: " + SCHEMA_RESOURCE);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder sem = new StringBuilder();
        for (String linha : script.split("\\R")) {
            if (!linha.trim().startsWith("--")) {
                sem.append(linha).append('\n');
            }
        }
        try (Statement st = conn.createStatement()) {
            for (String comando : sem.toString().split(";")) {
                if (!comando.isBlank()) {
                    st.execute(comando);
                }
            }
        }
    }

}
