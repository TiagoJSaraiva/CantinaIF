package utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import seed.DemoDataSeeder;

/**
 * Ponto unico de acesso ao banco de dados (pool HikariCP).
 *
 * Configuracao por variaveis de ambiente (ou propriedades de sistema com o mesmo nome):
 * <ul>
 *   <li>CANTINA_DB_URL - URL JDBC (ex.: jdbc:mysql://localhost:3306/cantinaif). Se ausente,
 *       usa H2 embutido em memoria, em modo MySQL: o schema e os dados de demonstracao sao
 *       criados no boot ("modo demonstracao").</li>
 *   <li>CANTINA_DB_USER - usuario (opcional para H2).</li>
 *   <li>CANTINA_DB_PASSWORD - senha (opcional para H2).</li>
 *   <li>CANTINA_SEED_IF_EMPTY=true - em qualquer banco, semeia os dados de demonstracao no boot
 *       somente se nao houver nenhum vendedor (nao apaga nada).</li>
 * </ul>
 */
public final class Database {

    public static final String DEFAULT_H2_URL =
            "jdbc:h2:mem:cantinaif;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private static HikariDataSource dataSource;
    private static boolean demoMode;
    /** Identifica a "geracao" da base: muda a cada restauracao e a cada inicio (sessoes antigas deixam de valer). */
    private static final java.util.concurrent.atomic.AtomicLong GERACAO =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());

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
        demoMode = isH2(url);
        boolean semear = demoMode || "true".equalsIgnoreCase(Config.get("CANTINA_SEED_IF_EMPTY"));
        if (semear) {
            try {
                DemoDataSeeder.rodarSeVazio(LocalDateTime.now());
            } catch (SQLException e) {
                shutdown();
                throw new IllegalStateException("Falha ao gravar os dados de demonstracao", e);
            }
        }
    }

    /** Inicializa o pool com parametros explicitos (usado pelos testes); em H2 aplica o schema, sem dados. */
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
        demoMode = false;
        if (isH2(url)) {
            try (Connection conn = dataSource.getConnection()) {
                aplicarSchema(conn);
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

    public static long geracao() {
        return GERACAO.get();
    }

    /** Chamado apos restaurar a base: invalida, na pratica, as sessoes de usuarios que deixaram de existir. */
    public static void novaGeracao() {
        GERACAO.updateAndGet(g -> Math.max(g + 1, System.currentTimeMillis()));
    }

    /** True quando o banco e o H2 embutido: ha contas de demonstracao e a base e restaurada periodicamente. */
    public static synchronized boolean isDemoMode() {
        return demoMode;
    }

    /** Encerra o pool (fim da aplicacao ou dos testes). */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        demoMode = false;
    }

    private static boolean isH2(String url) {
        return url.startsWith("jdbc:h2:");
    }

    /** Aplica db/schema.sql (idempotente: CREATE TABLE IF NOT EXISTS). */
    public static void aplicarSchema(Connection conn) throws SQLException, IOException {
        String script;
        try (InputStream in = Database.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IOException("Recurso nao encontrado no classpath: " + SCHEMA_RESOURCE);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder sem = new StringBuilder();
        for (String linha : script.split("\\R")) {
            int comentario = linha.indexOf("--");
            sem.append(comentario >= 0 ? linha.substring(0, comentario) : linha).append('\n');
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
