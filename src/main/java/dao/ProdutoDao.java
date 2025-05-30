package dao;

import model.Produto;
import utils.ConnectionFactory; // Certifique-se que o pacote ConnectionFactory está correto

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDao {

    public ProdutoDao() {
        // Construtor padrão, não precisa fazer nada aqui se ConnectionFactory gerencia a conexão
    }

    // Método auxiliar para mapear um ResultSet para um objeto Produto
    private Produto mapearProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        produto.setId(rs.getInt("id"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setPreco(rs.getFloat("preco"));
        produto.setEstoque(rs.getInt("estoque"));

        String categoriaString = rs.getString("categoria");
        try {
            if (categoriaString != null && !categoriaString.trim().isEmpty()) {
                produto.setCategoria(Produto.Categoria.valueOf(categoriaString.toUpperCase()));
            } else {
                produto.setCategoria(null); // Ou um valor padrão do ENUM, como Produto.Categoria.OUTROS
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: Categoria '" + categoriaString + "' do banco de dados inválida para o produto ID: " + produto.getId());
            produto.setCategoria(null); // Define como null se a string não corresponder a um ENUM
        }
        return produto;
    }

    public List<Produto> listarTodosProdutos() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM produtos";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }
        }
        return produtos;
    }

    public List<Produto> listarProdutosDisponiveis() throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto WHERE estoque > 0";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }
        }
        return produtos;
    }

    public List<Produto> listarProdutosPorCategoria(String categoriaNome, boolean incluirEstoqueZero) throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        String sql;
        if (incluirEstoqueZero) {
            sql = "SELECT * FROM Produto WHERE categoria = ?";
        } else {
            sql = "SELECT * FROM Produto WHERE categoria = ? AND estoque > 0";
        }

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoriaNome.toUpperCase()); // Categoria no BD deve ser armazenada em maiúsculas

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapearProduto(rs));
                }
            }
        }
        return produtos;
    }

    // Exemplo de outros métodos de DAO (adicionar, atualizar, deletar)
    public void adicionarProduto(Produto produto) throws SQLException {
        String sql = "INSERT INTO Produto (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setDouble(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria() != null ? produto.getCategoria().name() : null); // Armazena o nome do ENUM
            stmt.executeUpdate();
        }
    }

    public void atualizarProduto(Produto produto) throws SQLException {
        String sql = "UPDATE produtos SET nome = ?, descricao = ?, preco = ?, estoque = ?, categoria = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setDouble(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria() != null ? produto.getCategoria().name() : null);
            stmt.setInt(6, produto.getId());
            stmt.executeUpdate();
        }
    }

    public void deletarProduto(int id) throws SQLException {
        String sql = "DELETE FROM Produto WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    public Produto buscarProdutoPorId(int id) throws SQLException {
        String sql = "SELECT * FROM Produto WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }
        }
        return null; // Retorna null se não encontrar
    }
}