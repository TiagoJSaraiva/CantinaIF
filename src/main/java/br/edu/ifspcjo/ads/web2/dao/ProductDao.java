// AVISO: Ainda não testado por prolemas com a página home.

package Dao;

import model.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;

public class ProductDao {
    private Connection connection;

    public ProductDao(Connection connection) {
        this.connection = connection;
    }

    //para criação de produtos
    public void create(Produto produto) throws SQLException {
        String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
        }
    }

    //para os filtros
    public List<Produto> listarTodos() {
        List<Produto> lista = new ArrayList<>();
        String sql = "SELECT * FROM produto";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto p = new Produto(
                    rs.getString("nome"),
                    rs.getString("descricao"),
                    rs.getFloat("preco"),
                    rs.getInt("estoque")
                );
                p.setId(rs.getInt("id"));
                p.setCategoria(Produto.Categoria.valueOf(rs.getString("categoria")));
                lista.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return lista;
    }

    
    //para o carrinho
    public void reservarEstoque(int produtoId, int quantidade) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque - ? WHERE id = ? AND estoque >= ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            stmt.executeUpdate();
        }
    }

    private Connection getConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	public Produto buscarPorId(int id) {
        Produto produto = null;
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                produto = new Produto(
                    rs.getString("nome"),
                    rs.getString("descricao"),
                    rs.getFloat("preco"),
                    rs.getInt("estoque")
                );
                produto.setId(rs.getInt("id"));
                produto.setCategoria(Produto.Categoria.valueOf(rs.getString("categoria")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return produto;
    }

}
