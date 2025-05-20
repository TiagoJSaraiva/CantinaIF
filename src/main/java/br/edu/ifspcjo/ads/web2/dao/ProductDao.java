package dao;

import model.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    private Connection connection;

    public ProductDao(Connection connection) {
        this.connection = connection;
    }

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

    public List<Produto> listarProdutos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM produtos";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Produto produto = new Produto(
                        rs.getString("nome"),
                        rs.getString("descricao"),
                        rs.getFloat("preco"),
                        rs.getInt("estoque")
                );
                produto.setId(rs.getInt("id"));
                produto.setCategoria(Produto.Categoria.valueOf(rs.getString("categoria")));
                produtos.add(produto);
            }
        }
        return produtos;
    }
}
