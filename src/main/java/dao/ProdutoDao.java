package dao;

import model.Produto;
import utils.ConnectionFactory; // Certifique-se de que ConnectionFactory.java está funcionando corretamente

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDao {

    public ProdutoDao() {
       
    }

    // Para criação de produtos
    public void create(Produto produto) throws SQLException {
        String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection(); // Obtém a conexão aqui
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            // Certifique-se de que Categoria é um ENUM e tem o método .name()
            if (produto.getCategoria() != null) {
                stmt.setString(5, produto.getCategoria().name());
            } else {
                stmt.setNull(5, Types.VARCHAR); // Ou trate como preferir se a categoria puder ser nula
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
        // Note que o nome da tabela aqui é "produtos" (no plural) para corresponder ao seu CREATE
        String sql = "SELECT * FROM produtos";

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
        // O nome da tabela é "Produto"
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
        // Note o nome da tabela "produtos"
        String sql = "SELECT * FROM produtos WHERE categoria = ?";
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
            // Considere relançar uma RuntimeException aqui
        }
        return produtos;
    }

    // Método auxiliar para mapear os dados do ResultSet para o objeto Produto
    private Produto mapearProduto(ResultSet rs) throws SQLException {
        
        Produto produto = new Produto(null, null, 0, 0);

        produto.setId(rs.getInt("id"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setPreco(rs.getFloat("preco"));
        produto.setEstoque(rs.getInt("estoque"));

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
        // Usando try-with-resources para garantir o fechamento da conexão
        String sql = "UPDATE produtos SET estoque = estoque - ? WHERE id = ? AND estoque >= ?";
        try (Connection conn = ConnectionFactory.getConnection(); // Obtém a conexão aqui
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                // Se nenhuma linha foi afetada, significa que o estoque era insuficiente ou o produto não existia
                throw new SQLException("Não foi possível reservar estoque. Estoque insuficiente ou produto não encontrado.");
            }
        }
    }

    // Removendo o método getConnection() que retornava null
    // e usando ConnectionFactory.getConnection() diretamente.

    public Produto buscarPorId(int id) {
        Produto produto = null;
        String sql = "SELECT * FROM produtos WHERE id = ?"; // Note o nome da tabela "produtos"
        try (Connection conn = ConnectionFactory.getConnection(); // Obtém a conexão aqui
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Chame o método mapearProduto para preencher o objeto
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