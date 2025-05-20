package dao;

import model.Produto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HomeDao {

    private Connection connection;

    public HomeDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Retorna todos os produtos com estoque maior que zero.
     * Ideal para exibir na página principal (home.jsp).
     */
    public List<Produto> listarProdutosDisponiveis() throws SQLException {
        List<Produto> produtos = new ArrayList<>();

        String sql = "SELECT * FROM produtos WHERE estoque > 0";

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

                // Converte a string do banco para o enum Categoria
                produto.setCategoria(Produto.Categoria.valueOf(rs.getString("categoria")));

                // Se você tiver o campo de imagemURL no banco:
                produto.setImagemURL(rs.getString("imagemURL"));

                produtos.add(produto);
            }
        }

        return produtos;
    }
}
