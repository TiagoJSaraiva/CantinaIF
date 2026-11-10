package dao;

import model.Comprador;
import utils.Database;
import utils.Passwords;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/** Acesso a dados de compradores. Senhas sao sempre guardadas como hash BCrypt. */
public class UserDao {

    /** Hash de uma senha qualquer, usado para igualar o tempo de resposta quando o usuario nao existe. */
    private static final String HASH_FICTICIO = Passwords.hash("senha-ficticia-para-tempo-constante");

    /**
     * Cadastra um comprador; a senha em {@code comprador.getSenha()} e texto puro e e convertida em hash aqui.
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
     * @return o comprador (sem a senha) ou null se prontuario/senha estiverem incorretos
     */
    public Comprador autenticar(String prontuario, String senha) throws SQLException {
        String sql = "SELECT prontuario, email, senha, nome FROM Comprador WHERE prontuario = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, prontuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (Passwords.verify(senha, rs.getString("senha"))) {
                        return new Comprador(rs.getString("prontuario"), rs.getString("email"), null, rs.getString("nome"));
                    }
                    return null;
                }
            }
        }
        Passwords.verify(senha, HASH_FICTICIO);
        return null;
    }
}
