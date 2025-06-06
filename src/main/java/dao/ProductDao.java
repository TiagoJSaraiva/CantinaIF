package dao;

import model.Produto;
import utils.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {
    public ProductDao() {
        
    }

    public void create(Produto produto) throws SQLException {
        String sql = "INSERT INTO Produto (nome, descricao, preco, estoque, categoria, imagem) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            
            if (produto.getCategoria() != null) {
                stmt.setString(5, produto.getCategoria().name());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            
            if (produto.getImagem() != null) {
                stmt.setBytes(6, produto.getImagem());
            } else {
                stmt.setNull(6, java.sql.Types.BLOB);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
        }
    }

    // Listar todos os produtos (para o administrador, inclusive estoque zero)
    public List<Produto> listarTodosProdutos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar todos os produtos: " + e.getMessage());
            e.printStackTrace();
        }
        return produtos;
    }

    // Listar produtos com quantidade disponível > 0 (para o usuário)
    public List<Produto> listarProdutosDisponiveis() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto WHERE estoque > 0";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos disponíveis: " + e.getMessage());
            e.printStackTrace();
        }
        return produtos;
    }

    // Listar produtos por categoria (com opção de incluir/excluir estoque zero)
    public List<Produto> listarProdutosPorCategoria(String categoriaNome, boolean incluirEstoqueZero) {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto WHERE categoria = ?";
        if (!incluirEstoqueZero) {
            sql += " AND estoque > 0";
        }

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, categoriaNome);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapearProduto(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos por categoria: " + e.getMessage());
            e.printStackTrace();
        }
        return produtos;
    }

    private Produto mapearProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto(null, null, 0, 0);

        produto.setId(rs.getInt("id"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setPreco(rs.getFloat("preco"));
        produto.setEstoque(rs.getInt("estoque"));
        produto.setImagem(rs.getBytes("imagem"));
        String categoriaString = rs.getString("categoria");
        if (categoriaString != null && !categoriaString.isEmpty()) {
            try {
                produto.setCategoria(Produto.Categoria.valueOf(categoriaString));
            } catch (IllegalArgumentException e) {
                System.err.println("Erro: Categoria '" + categoriaString + "' inválida no banco de dados para o produto ID: " + produto.getId());
            }
        }
        return produto;
    }

    // Para o carrinho (reservar estoque)
    public void reservarEstoque(int produtoId, int quantidade) throws SQLException {
        String sql = "UPDATE Produto SET estoque = estoque - ? WHERE id = ? AND estoque >= ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Não foi possível reservar estoque. Estoque insuficiente ou produto não encontrado.");
            }
        }
    }

    public Produto buscarPorId(int id) {
        Produto produto = null;
        String sql = "SELECT * FROM Produto WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    produto = mapearProduto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar produto por ID: " + e.getMessage());
            e.printStackTrace();
        }
        return produto;
    }
}
