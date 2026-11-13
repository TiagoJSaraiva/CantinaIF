package dao;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

import utils.Database;

/**
 * Codigos de vinculo comprador-vendedor. O vendedor gera um codigo curto que vale {@link #VALIDADE} e UMA vez; o
 * comprador que o digitar passa a ver os produtos desse vendedor (trocando de vendedor, se ja tinha outro).
 */
public class VinculoDao {

    /** Tempo de validade de um codigo. */
    public static final Duration VALIDADE = Duration.ofSeconds(60);
    /** Tamanho do codigo. Com 31 simbolos: ~887 milhoes de combinacoes, contra tentativas limitadas por conta e por IP. */
    public static final int TAMANHO = 6;
    /** Sem caracteres ambiguos (0/O, 1/I/L) para ditar ou copiar da tela sem erro. */
    static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Codigo gerado e o instante em que expira. */
    public record Codigo(String codigo, LocalDateTime expiraEm) {
    }

    /** Resultado de um vinculo: o vendedor agora vinculado e se o comprador trocou de vendedor. */
    public record Vinculo(String vendedorProntuario, String vendedorNome, boolean trocou) {
    }

    /**
     * Gera um codigo novo para o vendedor. Os codigos ainda nao usados desse vendedor deixam de valer, e os expirados de
     * todos os vendedores sao apagados.
     */
    public Codigo gerarCodigo(String vendedor, LocalDateTime agora) throws SQLException {
        LocalDateTime expira = agora.plus(VALIDADE);
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CodigoVinculo WHERE expira_em <= ? "
                    + "OR (vendedor_prontuario = ? AND usado_por IS NULL)")) {
                ps.setTimestamp(1, Timestamp.valueOf(agora));
                ps.setString(2, vendedor);
                ps.executeUpdate();
            }
            for (int tentativa = 0; ; tentativa++) {
                String codigo = sortear();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO CodigoVinculo (codigo, vendedor_prontuario, criado_em, expira_em) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, codigo);
                    ps.setString(2, vendedor);
                    ps.setTimestamp(3, Timestamp.valueOf(agora));
                    ps.setTimestamp(4, Timestamp.valueOf(expira));
                    ps.executeUpdate();
                    return new Codigo(codigo, expira);
                } catch (SQLIntegrityConstraintViolationException colisao) {
                    if (tentativa >= 5) {
                        throw colisao;
                    }
                }
            }
        }
    }

    /**
     * Usa um codigo: vincula o comprador ao vendedor que o gerou. Atomico e de uso unico (dois compradores com o mesmo
     * codigo: so o primeiro consegue).
     *
     * @throws RegraNegocioException codigo invalido, expirado ou ja usado; ou comprador desativado/inexistente
     */
    public Vinculo resgatar(String codigoDigitado, String comprador, LocalDateTime agora)
            throws SQLException, RegraNegocioException {
        String codigo = normalizar(codigoDigitado);
        if (codigo == null) {
            throw new CodigoInvalidoException();
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String anterior;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT ativo, vendedor_prontuario FROM Comprador WHERE prontuario = ? FOR UPDATE")) {
                    ps.setString(1, comprador);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new RegraNegocioException("Comprador não encontrado.");
                        }
                        if (!rs.getBoolean("ativo")) {
                            throw new RegraNegocioException("Sua conta está desativada; não é possível trocar de vendedor.");
                        }
                        anterior = rs.getString("vendedor_prontuario");
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE CodigoVinculo SET usado_por = ?, usado_em = ? "
                        + "WHERE codigo = ? AND usado_por IS NULL AND expira_em > ?")) {
                    ps.setString(1, comprador);
                    ps.setTimestamp(2, Timestamp.valueOf(agora));
                    ps.setString(3, codigo);
                    ps.setTimestamp(4, Timestamp.valueOf(agora));
                    if (ps.executeUpdate() != 1) {
                        throw new CodigoInvalidoException();
                    }
                }
                String vendedor;
                String nome;
                try (PreparedStatement ps = conn.prepareStatement("SELECT v.prontuario, v.nome FROM CodigoVinculo c "
                        + "JOIN Vendedor v ON v.prontuario = c.vendedor_prontuario WHERE c.codigo = ?")) {
                    ps.setString(1, codigo);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        vendedor = rs.getString(1);
                        nome = rs.getString(2);
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE Comprador SET vendedor_prontuario = ? WHERE prontuario = ?")) {
                    ps.setString(1, vendedor);
                    ps.setString(2, comprador);
                    ps.executeUpdate();
                }
                conn.commit();
                return new Vinculo(vendedor, nome, anterior != null && !anterior.equals(vendedor));
            } catch (SQLException | RegraNegocioException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /** Maiusculas, sem espacos/hifens; null se o formato nao puder ser um codigo. */
    static String normalizar(String digitado) {
        if (digitado == null) {
            return null;
        }
        String c = digitado.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (c.length() != TAMANHO) {
            return null;
        }
        for (int i = 0; i < c.length(); i++) {
            if (ALFABETO.indexOf(c.charAt(i)) < 0) {
                return null;
            }
        }
        return c;
    }

    private static String sortear() {
        StringBuilder sb = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            sb.append(ALFABETO.charAt(ALEATORIO.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    /** Codigo inexistente, expirado ou ja usado (a mensagem nao diz qual, de proposito). */
    public static class CodigoInvalidoException extends RegraNegocioException {
        private static final long serialVersionUID = 1L;

        public CodigoInvalidoException() {
            super("Código inválido ou expirado. Peça um novo código ao vendedor.");
        }
    }
}
