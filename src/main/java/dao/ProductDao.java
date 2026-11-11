package dao;

import model.Produto;
import utils.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acesso a dados de produtos. Precos em BigDecimal (coluna DECIMAL). */
public class ProductDao {

    /** Colunas das listagens: nao carrega o BLOB da imagem, so se ela existe. */
    private static final String COLUNAS_LISTA =
            "id, nome, descricao, preco, estoque, categoria, (imagem IS NOT NULL) AS tem_imagem";

    public ProductDao() {
    }

    public void create(Produto produto) throws SQLException {
        String sql = "INSERT INTO Produto (nome, descricao, preco, estoque, categoria, imagem) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setBigDecimal(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            if (produto.getCategoria() != null) {
                stmt.setString(5, produto.getCategoria().name().toLowerCase());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (produto.getImagem() != null) {
                stmt.setBytes(6, produto.getImagem());
            } else {
                stmt.setNull(6, Types.BLOB);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
        }
    }

    /** Lista todos os produtos (inclusive esgotados), por nome; categoria nula, vazia ou "todos" = sem filtro. */
    public List<Produto> listarProdutos(String categoria) throws SQLException {
        boolean filtra = categoria != null && !categoria.isBlank() && !categoria.equalsIgnoreCase("todos");
        String sql = "SELECT " + COLUNAS_LISTA + " FROM Produto" + (filtra ? " WHERE categoria = ?" : "") + " ORDER BY nome";
        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (filtra) {
                stmt.setString(1, categoria.trim().toLowerCase());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs, false));
                }
            }
        }
        return produtos;
    }

    /** Produto completo, com os bytes da imagem. */
    public Produto buscarPorId(int id) throws SQLException {
        return buscar("SELECT id, nome, descricao, preco, estoque, categoria, imagem FROM Produto WHERE id = ?", id, true);
    }

    /** Produto sem os bytes da imagem (carrinho, validacoes). */
    public Produto buscarResumo(int id) throws SQLException {
        return buscar("SELECT " + COLUNAS_LISTA + " FROM Produto WHERE id = ?", id, false);
    }

    private Produto buscar(String sql, int id, boolean comImagem) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs, comImagem) : null;
            }
        }
    }

    /** Atualiza os dados de um produto; a imagem so e trocada quando informada. */
    public boolean atualizar(Produto produto) throws SQLException {
        boolean trocaImagem = produto.getImagem() != null;
        String sql = "UPDATE Produto SET nome = ?, descricao = ?, preco = ?, estoque = ?, categoria = ?"
                + (trocaImagem ? ", imagem = ?" : "") + " WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 1;
            stmt.setString(i++, produto.getNome());
            stmt.setString(i++, produto.getDescricao());
            stmt.setBigDecimal(i++, produto.getPreco());
            stmt.setInt(i++, produto.getEstoque());
            if (produto.getCategoria() != null) {
                stmt.setString(i++, produto.getCategoria().name().toLowerCase());
            } else {
                stmt.setNull(i++, Types.VARCHAR);
            }
            if (trocaImagem) {
                stmt.setBytes(i++, produto.getImagem());
            }
            stmt.setInt(i, produto.getId());
            return stmt.executeUpdate() > 0;
        }
    }

    /** Ajuste rapido de estoque (valor absoluto). */
    public boolean atualizarEstoque(int produtoId, int estoque) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE Produto SET estoque = ? WHERE id = ?")) {
            stmt.setInt(1, estoque);
            stmt.setInt(2, produtoId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Produto mapear(ResultSet rs, boolean comImagem) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setNome(rs.getString("nome"));
        p.setDescricao(rs.getString("descricao"));
        p.setPreco(rs.getBigDecimal("preco"));
        p.setEstoque(rs.getInt("estoque"));
        if (comImagem) {
            p.setImagem(rs.getBytes("imagem"));
        } else {
            p.setTemImagem(rs.getBoolean("tem_imagem"));
        }
        String categoria = rs.getString("categoria");
        if (categoria != null && !categoria.isEmpty()) {
            try {
                p.setCategoria(Produto.Categoria.valueOf(categoria.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.err.println("Categoria invalida no banco para o produto " + p.getId() + ": " + categoria);
            }
        }
        return p;
    }
}
