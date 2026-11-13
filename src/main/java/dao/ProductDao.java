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

/**
 * Acesso a dados de produtos. Precos em BigDecimal (coluna DECIMAL). Cada produto pertence a um vendedor: listagens e
 * alteracoes sempre recebem o vendedor e nunca enxergam ou alteram produtos de outro.
 */
public class ProductDao {

    /** Colunas das listagens: nao carrega o BLOB da imagem, so se ela existe. */
    private static final String COLUNAS_LISTA =
            "id, vendedor_prontuario, nome, descricao, preco, estoque, categoria, (imagem IS NOT NULL) AS tem_imagem";

    public ProductDao() {
    }

    /** Cria o produto do vendedor informado em {@code produto.getVendedorProntuario()} (obrigatorio). */
    public void create(Produto produto) throws SQLException {
        if (produto.getVendedorProntuario() == null) {
            throw new IllegalArgumentException("Produto sem vendedor.");
        }
        String sql = "INSERT INTO Produto (vendedor_prontuario, nome, descricao, preco, estoque, categoria, imagem) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getVendedorProntuario());
            stmt.setString(2, produto.getNome());
            stmt.setString(3, produto.getDescricao());
            stmt.setBigDecimal(4, produto.getPreco());
            stmt.setInt(5, produto.getEstoque());
            if (produto.getCategoria() != null) {
                stmt.setString(6, produto.getCategoria().name().toLowerCase());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }
            if (produto.getImagem() != null) {
                stmt.setBytes(7, produto.getImagem());
            } else {
                stmt.setNull(7, Types.BLOB);
            }
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Produtos de um vendedor (inclusive esgotados), por nome; categoria nula, vazia ou "todos" = sem filtro.
     * Vendedor nulo = lista vazia (comprador sem vinculo nao ve produto nenhum).
     */
    public List<Produto> listarProdutos(String vendedor, String categoria) throws SQLException {
        List<Produto> produtos = new ArrayList<>();
        if (vendedor == null) {
            return produtos;
        }
        boolean filtra = categoria != null && !categoria.isBlank() && !categoria.equalsIgnoreCase("todos");
        String sql = "SELECT " + COLUNAS_LISTA + " FROM Produto WHERE vendedor_prontuario = ?"
                + (filtra ? " AND categoria = ?" : "") + " ORDER BY nome";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, vendedor);
            if (filtra) {
                stmt.setString(2, categoria.trim().toLowerCase());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs, false));
                }
            }
        }
        return produtos;
    }

    /** Produto completo, com os bytes da imagem (de qualquer vendedor: usado para servir a imagem). */
    public Produto buscarPorId(int id) throws SQLException {
        return buscar("SELECT id, vendedor_prontuario, nome, descricao, preco, estoque, categoria, imagem FROM Produto WHERE id = ?",
                id, true);
    }

    /** Produto sem os bytes da imagem (carrinho, validacoes); traz o dono em {@code getVendedorProntuario()}. */
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

    /**
     * Atualiza os dados de um produto do vendedor {@code produto.getVendedorProntuario()}; a imagem so e trocada
     * quando informada. Produto de outro vendedor = nao encontrado (false).
     */
    public boolean atualizar(Produto produto) throws SQLException {
        return atualizar(produto, null) == ResultadoEstoque.OK;
    }

    /** Resultado de uma atualizacao condicionada ao estoque que o vendedor viu. */
    public enum ResultadoEstoque { OK, NAO_ENCONTRADO, CONFLITO }

    /**
     * Ajuste de estoque com comparacao-e-troca atomica no SQL: so grava se o estoque ainda for o que o vendedor
     * viu ({@code esperado}); assim nao sobrescreve baixas (checkouts) ou devolucoes concorrentes.
     */
    public ResultadoEstoque atualizarEstoque(String vendedor, int produtoId, int esperado, int novo) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Produto SET estoque = ? WHERE id = ? AND vendedor_prontuario = ? AND estoque = ?")) {
            stmt.setInt(1, novo);
            stmt.setInt(2, produtoId);
            stmt.setString(3, vendedor);
            stmt.setInt(4, esperado);
            if (stmt.executeUpdate() == 1) {
                return ResultadoEstoque.OK;
            }
        }
        return existe(vendedor, produtoId) ? ResultadoEstoque.CONFLITO : ResultadoEstoque.NAO_ENCONTRADO;
    }

    /** Estoque atual de um produto, ou -1 se nao existir. */
    public int estoqueAtual(int produtoId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT estoque FROM Produto WHERE id = ?")) {
            stmt.setInt(1, produtoId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private boolean existe(String vendedor, int produtoId) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM Produto WHERE id = ? AND vendedor_prontuario = ?")) {
            stmt.setInt(1, produtoId);
            stmt.setString(2, vendedor);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Como {@link #atualizar(Produto)}, mas so grava se o estoque ainda for {@code estoqueEsperado}
     * (null = sem conferir o estoque).
     */
    public ResultadoEstoque atualizar(Produto produto, Integer estoqueEsperado) throws SQLException {
        boolean trocaImagem = produto.getImagem() != null;
        String sql = "UPDATE Produto SET nome = ?, descricao = ?, preco = ?, estoque = ?, categoria = ?"
                + (trocaImagem ? ", imagem = ?" : "") + " WHERE id = ? AND vendedor_prontuario = ?"
                + (estoqueEsperado != null ? " AND estoque = ?" : "");
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
            stmt.setInt(i++, produto.getId());
            stmt.setString(i++, produto.getVendedorProntuario());
            if (estoqueEsperado != null) {
                stmt.setInt(i, estoqueEsperado);
            }
            if (stmt.executeUpdate() == 1) {
                return ResultadoEstoque.OK;
            }
        }
        return estoqueEsperado != null && existe(produto.getVendedorProntuario(), produto.getId())
                ? ResultadoEstoque.CONFLITO : ResultadoEstoque.NAO_ENCONTRADO;
    }

    /** Ajuste rapido de estoque (valor absoluto, sem conferir o valor anterior; preferir a versao com "esperado"). */
    public boolean atualizarEstoque(String vendedor, int produtoId, int estoque) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Produto SET estoque = ? WHERE id = ? AND vendedor_prontuario = ?")) {
            stmt.setInt(1, estoque);
            stmt.setInt(2, produtoId);
            stmt.setString(3, vendedor);
            return stmt.executeUpdate() > 0;
        }
    }

    private Produto mapear(ResultSet rs, boolean comImagem) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setVendedorProntuario(rs.getString("vendedor_prontuario"));
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
