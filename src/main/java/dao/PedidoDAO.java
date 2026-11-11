package dao;

import model.Comprador;
import model.ItemCarrinho;
import model.Pedido;
import model.Produto;
import model.StatusPedido;
import utils.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Pedidos e estoque. Toda operacao que mexe em estoque ocorre na mesma transacao que grava o pedido
 * (ou muda seu status): ou tudo e gravado, ou nada.
 */
public class PedidoDAO {

    private static final String SELECT_PEDIDOS =
            "SELECT p.id AS pedido_id, p.status, p.criado_em, p.total, "
            + "c.prontuario, c.nome AS comprador_nome, c.email, "
            + "pp.produto_id, prod.nome AS produto_nome, pp.quantidade, pp.preco_unitario "
            + "FROM Pedido p "
            + "JOIN Comprador c ON p.comprador_prontuario = c.prontuario "
            + "JOIN Pedido_Produto pp ON p.id = pp.pedido_id "
            + "JOIN Produto prod ON pp.produto_id = prod.id ";

    private static final String ORDEM = " ORDER BY p.criado_em DESC, p.id DESC, prod.nome ASC";

    public PedidoDAO() {
    }

    // ------------------------------------------------------------------ criacao

    public Pedido criarPedido(String prontuario, List<ItemCarrinho> itens) throws SQLException, RegraNegocioException {
        return criarPedido(prontuario, itens, LocalDateTime.now());
    }

    /**
     * Como {@link #criarPedido(String, List)}, mas confere que o preco no banco e o mesmo que o comprador viu
     * (o preco do item no carrinho); se mudou, lanca {@link PrecoAlteradoException} e nada e gravado.
     */
    public Pedido criarPedidoConfirmandoPrecos(String prontuario, List<ItemCarrinho> itens)
            throws SQLException, RegraNegocioException {
        return criarPedido(prontuario, itens, LocalDateTime.now(), true);
    }

    /**
     * Cria um pedido baixando o estoque na mesma transacao. O preco unitario e o total sao lidos/calculados
     * no servidor a partir do banco (o carrinho da sessao nao e confiavel para preco).
     *
     * @throws EstoqueInsuficienteException se algum item nao tiver estoque suficiente (nada e gravado)
     */
    public Pedido criarPedido(String prontuario, List<ItemCarrinho> itens, LocalDateTime criadoEm)
            throws SQLException, RegraNegocioException {
        return criarPedido(prontuario, itens, criadoEm, false);
    }

    private Pedido criarPedido(String prontuario, List<ItemCarrinho> itens, LocalDateTime criadoEm, boolean confirmarPrecos)
            throws SQLException, RegraNegocioException {
        if (itens == null || itens.isEmpty()) {
            throw new RegraNegocioException("O carrinho está vazio.");
        }
        // Junta itens repetidos e ordena por id para reduzir risco de deadlock
        Map<Integer, Integer> quantidades = new TreeMap<>();
        Map<Integer, BigDecimal> precosVistos = new java.util.HashMap<>();
        for (ItemCarrinho item : itens) {
            precosVistos.put(item.getProduto().getId(), item.getProduto().getPreco());
            if (item.getQuantidade() < 1) {
                throw new RegraNegocioException("A quantidade de cada item deve ser de pelo menos 1.");
            }
            quantidades.merge(item.getProduto().getId(), item.getQuantidade(), Integer::sum);
        }

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Pedido pedido = new Pedido();
                Comprador comprador = new Comprador();
                comprador.setProntuario(prontuario);
                pedido.setComprador(comprador);
                pedido.setCriadoEm(criadoEm);
                pedido.setStatus(StatusPedido.PENDENTE);

                BigDecimal total = BigDecimal.ZERO;
                for (Map.Entry<Integer, Integer> e : quantidades.entrySet()) {
                    int produtoId = e.getKey();
                    int quantidade = e.getValue();
                    baixarEstoque(conn, produtoId, quantidade);
                    Produto produto = lerProduto(conn, produtoId);
                    BigDecimal visto = precosVistos.get(produtoId);
                    if (confirmarPrecos && visto != null && visto.compareTo(produto.getPreco()) != 0) {
                        throw new PrecoAlteradoException(produto.getNome(), visto, produto.getPreco());
                    }
                    ItemCarrinho item = new ItemCarrinho(produto, quantidade);
                    pedido.addItem(item);
                    total = total.add(item.getSubtotal());
                }
                pedido.setTotal(total);

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Pedido (comprador_prontuario, status, criado_em, total) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, prontuario);
                    ps.setString(2, StatusPedido.PENDENTE.name());
                    ps.setTimestamp(3, Timestamp.valueOf(criadoEm));
                    ps.setBigDecimal(4, total);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Não foi possível obter o ID gerado para o pedido.");
                        }
                        pedido.setId(rs.getInt(1));
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Pedido_Produto (pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)")) {
                    for (ItemCarrinho item : pedido.getItens()) {
                        ps.setInt(1, pedido.getId());
                        ps.setInt(2, item.getProduto().getId());
                        ps.setInt(3, item.getQuantidade());
                        ps.setBigDecimal(4, item.getProduto().getPreco());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return pedido;
            } catch (SQLException | RegraNegocioException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    /** UPDATE atomico: so baixa se houver estoque suficiente. */
    private void baixarEstoque(Connection conn, int produtoId, int quantidade)
            throws SQLException, RegraNegocioException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE Produto SET estoque = estoque - ? WHERE id = ? AND estoque >= ?")) {
            ps.setInt(1, quantidade);
            ps.setInt(2, produtoId);
            ps.setInt(3, quantidade);
            if (ps.executeUpdate() == 1) {
                return;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT nome, estoque FROM Produto WHERE id = ?")) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new EstoqueInsuficienteException(rs.getString("nome"), rs.getInt("estoque"));
                }
            }
        }
        throw new RegraNegocioException("Um dos produtos do carrinho não existe mais.");
    }

    private Produto lerProduto(Connection conn, int produtoId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, nome, preco FROM Produto WHERE id = ?")) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                Produto p = new Produto();
                p.setId(rs.getInt("id"));
                p.setNome(rs.getString("nome"));
                p.setPreco(rs.getBigDecimal("preco").setScale(2, java.math.RoundingMode.HALF_UP));
                return p;
            }
        }
    }

    // ------------------------------------------------------------------ status

    /** Muda o status (acao do vendedor). Cancelar devolve o estoque. */
    public void atualizarStatus(int pedidoId, StatusPedido novo) throws SQLException, RegraNegocioException {
        mudarStatus(pedidoId, novo, null);
    }

    /** O comprador cancela o proprio pedido, somente enquanto estiver PENDENTE. */
    public void cancelarComoComprador(int pedidoId, String prontuario) throws SQLException, RegraNegocioException {
        mudarStatus(pedidoId, StatusPedido.CANCELADO, prontuario);
    }

    private void mudarStatus(int pedidoId, StatusPedido novo, String prontuarioDono)
            throws SQLException, RegraNegocioException {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                StatusPedido atual;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status, comprador_prontuario FROM Pedido WHERE id = ? FOR UPDATE")) {
                    ps.setInt(1, pedidoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()
                                || (prontuarioDono != null && !prontuarioDono.equals(rs.getString("comprador_prontuario")))) {
                            throw new RegraNegocioException("Pedido não encontrado.");
                        }
                        atual = StatusPedido.valueOf(rs.getString("status"));
                    }
                }
                if (!atual.podeIrPara(novo)) {
                    throw new TransicaoInvalidaException(atual, novo);
                }
                if (prontuarioDono != null && atual != StatusPedido.PENDENTE) {
                    throw new TransicaoInvalidaException(
                            "Só é possível cancelar pedidos pendentes. Procure a cantina para este pedido.");
                }
                if (novo == StatusPedido.CANCELADO) {
                    devolverEstoque(conn, pedidoId);
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE Pedido SET status = ? WHERE id = ?")) {
                    ps.setString(1, novo.name());
                    ps.setInt(2, pedidoId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException | RegraNegocioException | RuntimeException ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private void devolverEstoque(Connection conn, int pedidoId) throws SQLException {
        List<int[]> itens = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT produto_id, quantidade FROM Pedido_Produto WHERE pedido_id = ?")) {
            ps.setInt(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    itens.add(new int[] { rs.getInt(1), rs.getInt(2) });
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement("UPDATE Produto SET estoque = estoque + ? WHERE id = ?")) {
            for (int[] item : itens) {
                ps.setInt(1, item[1]);
                ps.setInt(2, item[0]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ------------------------------------------------------------------ consultas

    /** Todos os pedidos (mais recentes primeiro), opcionalmente filtrados por status. */
    public List<Pedido> listarPedidos(StatusPedido filtro) throws SQLException {
        if (filtro == null) {
            return consultar("", null);
        }
        return consultar("WHERE p.status = ? ", filtro.name());
    }

    /** Pedidos de um comprador (mais recentes primeiro). */
    public List<Pedido> listarPorComprador(String prontuario) throws SQLException {
        return consultar("WHERE p.comprador_prontuario = ? ", prontuario);
    }

    public Pedido buscarPorId(int id) throws SQLException {
        List<Pedido> r = consultar("WHERE p.id = ? ", id);
        return r.isEmpty() ? null : r.get(0);
    }

    private List<Pedido> consultar(String where, Object parametro) throws SQLException {
        Map<Integer, Pedido> pedidos = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_PEDIDOS + where + ORDEM)) {
            if (parametro != null) {
                ps.setObject(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("pedido_id");
                    Pedido pedido = pedidos.get(id);
                    if (pedido == null) {
                        pedido = new Pedido();
                        pedido.setId(id);
                        pedido.setStatus(StatusPedido.valueOf(rs.getString("status")));
                        pedido.setCriadoEm(rs.getTimestamp("criado_em").toLocalDateTime());
                        pedido.setTotal(rs.getBigDecimal("total"));
                        Comprador c = new Comprador();
                        c.setProntuario(rs.getString("prontuario"));
                        c.setNome(rs.getString("comprador_nome"));
                        c.setEmail(rs.getString("email"));
                        pedido.setComprador(c);
                        pedidos.put(id, pedido);
                    }
                    Produto p = new Produto();
                    p.setId(rs.getInt("produto_id"));
                    p.setNome(rs.getString("produto_nome"));
                    p.setPreco(rs.getBigDecimal("preco_unitario"));
                    pedido.addItem(new ItemCarrinho(p, rs.getInt("quantidade")));
                }
            }
        }
        return new ArrayList<>(pedidos.values());
    }
}
