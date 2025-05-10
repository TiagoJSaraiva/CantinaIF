package dao;

import model.Comprador;
import utils.DataSourceManager;

import java.sql.*;

public class UserDao {

    public void create(Comprador comprador) {
        String sql = "INSERT INTO usuarios (prontuario, email, senha) VALUES (?, ?, ?)";

        try (Connection conn = DataSourceManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, comprador.getProntuario());
            stmt.setString(2, comprador.getEmail());
            stmt.setString(3, comprador.getSenha());
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Comprador encontrarPorProntuarioESenha(String prontuario, String senha) {
        String sql = "SELECT * FROM usuarios WHERE prontuario = ? AND senha = ?";

        try (Connection conn = DataSourceManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, prontuario);
            stmt.setString(2, senha);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Comprador(
                    rs.getString("prontuario"),
                    rs.getString("email"),
                    rs.getString("senha")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
