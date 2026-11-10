package dao;

import model.Vendedor;
import utils.Database;
import utils.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acesso a dados de vendedores. Senhas sao guardadas como hash BCrypt. */
public class VendedorDao {

    private static final String HASH_FICTICIO = Passwords.hash("senha-ficticia-para-tempo-constante");

    /**
     * Autentica um vendedor pelo prontuario.
     *
     * @return o vendedor (sem a senha) ou null se prontuario/senha estiverem incorretos
     */
    public Vendedor autenticar(String prontuario, String senha) throws SQLException {
        String sql = "SELECT prontuario, email, senha FROM Vendedor WHERE prontuario = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prontuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (Passwords.verify(senha, rs.getString("senha"))) {
                        Vendedor v = new Vendedor();
                        v.setProntuario(rs.getString("prontuario"));
                        v.setEmail(rs.getString("email"));
                        return v;
                    }
                    return null;
                }
            }
        }
        Passwords.verify(senha, HASH_FICTICIO);
        return null;
    }

    /** Cadastra um vendedor; a senha em texto puro e convertida em hash aqui. Retorna false se ja existir. */
    public boolean create(String prontuario, String email, String senha) throws SQLException {
        String sql = "INSERT INTO Vendedor (prontuario, email, senha) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prontuario);
            stmt.setString(2, email);
            stmt.setString(3, Passwords.hash(senha));
            stmt.executeUpdate();
            return true;
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return false;
        }
    }
}
