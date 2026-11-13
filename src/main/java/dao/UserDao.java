package dao;

import model.Comprador;
import model.CompradorResumo;
import model.EstadoComprador;
import utils.Database;
import utils.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Acesso a dados de compradores. Senhas sao sempre guardadas como hash BCrypt. */
public class UserDao {

    /** Hash de uma senha qualquer, usado para igualar o tempo de resposta quando o usuario nao existe. */
    private static final String HASH_FICTICIO = Passwords.hash("senha-ficticia-para-tempo-constante");

    /**
     * Cadastra um comprador (sem vinculo com vendedor); a senha em {@code comprador.getSenha()} e texto puro e e
     * convertida em hash aqui.
     *
     * @return true se cadastrou; false se o prontuario ou o e-mail ja existem
     */
    public boolean create(Comprador comprador) throws SQLException {
        String sql = "INSERT INTO Comprador (prontuario, email, senha, nome) VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, comprador.getProntuario());
            stmt.setString(2, comprador.getEmail());
            stmt.setString(3, Passwords.hash(comprador.getSenha()));
            stmt.setString(4, comprador.getNome());
            stmt.executeUpdate();
            return true;
        } catch (SQLIntegrityConstraintViolationException e) {
            return false;
        }
    }

    /**
     * Autentica um comprador.
     *
     * @return o comprador (sem a senha, com vinculo e situacao) ou null se prontuario/senha estiverem incorretos
     */
    public Comprador autenticar(String prontuario, String senha) throws SQLException {
        String sql = "SELECT c.prontuario, c.email, c.senha, c.nome, c.ativo, c.vendedor_prontuario, v.nome AS vendedor_nome "
                + "FROM Comprador c LEFT JOIN Vendedor v ON v.prontuario = c.vendedor_prontuario WHERE c.prontuario = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prontuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (Passwords.verify(senha, rs.getString("senha"))) {
                        Comprador c = new Comprador(rs.getString("prontuario"), rs.getString("email"), null, rs.getString("nome"));
                        c.setAtivo(rs.getBoolean("ativo"));
                        c.setVendedorProntuario(rs.getString("vendedor_prontuario"));
                        c.setVendedorNome(rs.getString("vendedor_nome"));
                        return c;
                    }
                    return null;
                }
            }
        }
        Passwords.verify(senha, HASH_FICTICIO);
        return null;
    }

    /** Estado atual (situacao, vinculo, advertencias nao lidas) ou null se o comprador nao existe mais. */
    public EstadoComprador estado(String prontuario) throws SQLException {
        String sql = "SELECT c.ativo, c.vendedor_prontuario, v.nome AS vendedor_nome, "
                + "(SELECT COUNT(*) FROM Advertencia a WHERE a.comprador_prontuario = c.prontuario AND a.lida_em IS NULL) AS nao_lidas "
                + "FROM Comprador c LEFT JOIN Vendedor v ON v.prontuario = c.vendedor_prontuario WHERE c.prontuario = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prontuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new EstadoComprador(rs.getBoolean("ativo"), rs.getString("vendedor_prontuario"),
                        rs.getString("vendedor_nome"), rs.getInt("nao_lidas"));
            }
        }
    }

    /** Compradores vinculados ao vendedor, por nome, com os numeros do relacionamento com ele. */
    public List<CompradorResumo> listarDoVendedor(String vendedor) throws SQLException {
        String doVendedor = "p.comprador_prontuario = c.prontuario AND p.vendedor_prontuario = ?";
        String sql = "SELECT c.prontuario, c.nome, c.email, c.ativo, "
                + "(SELECT COUNT(*) FROM Pedido p WHERE " + doVendedor + ") AS pedidos, "
                + "(SELECT COUNT(*) FROM Pedido p WHERE " + doVendedor + " AND p.cancelado_por = 'COMPRADOR') AS canc_comprador, "
                + "(SELECT COUNT(*) FROM Pedido p WHERE " + doVendedor + " AND p.cancelado_por = 'VENDEDOR') AS canc_vendedor, "
                + "(SELECT MAX(p.criado_em) FROM Pedido p WHERE " + doVendedor + ") AS ultimo, "
                + "(SELECT COUNT(*) FROM Advertencia a WHERE a.comprador_prontuario = c.prontuario "
                + "AND a.vendedor_prontuario = ?) AS advertencias "
                + "FROM Comprador c WHERE c.vendedor_prontuario = ? ORDER BY c.nome, c.prontuario";
        List<CompradorResumo> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i++) {
                stmt.setString(i, vendedor);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Timestamp ultimo = rs.getTimestamp("ultimo");
                    lista.add(new CompradorResumo(rs.getString("prontuario"), rs.getString("nome"), rs.getString("email"),
                            rs.getBoolean("ativo"), rs.getInt("pedidos"), rs.getInt("canc_comprador"),
                            rs.getInt("canc_vendedor"), ultimo == null ? null : ultimo.toLocalDateTime(),
                            rs.getInt("advertencias")));
                }
            }
        }
        return lista;
    }

    /**
     * Ativa ou desativa um comprador; so vale para compradores vinculados a {@code vendedor}.
     *
     * @return false se o comprador nao existe ou nao e desse vendedor
     */
    public boolean definirAtivo(String prontuario, String vendedor, boolean ativo) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Comprador SET ativo = ? WHERE prontuario = ? AND vendedor_prontuario = ?")) {
            stmt.setBoolean(1, ativo);
            stmt.setString(2, prontuario);
            stmt.setString(3, vendedor);
            return stmt.executeUpdate() == 1;
        }
    }

    /**
     * True se o prontuario ja existe como comprador OU como vendedor. O login procura nas duas tabelas, entao o mesmo
     * prontuario nao pode existir nas duas.
     */
    public static boolean prontuarioEmUso(String prontuario) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT (SELECT COUNT(*) FROM Comprador WHERE prontuario = ?) + (SELECT COUNT(*) FROM Vendedor WHERE prontuario = ?)")) {
            stmt.setString(1, prontuario);
            stmt.setString(2, prontuario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
