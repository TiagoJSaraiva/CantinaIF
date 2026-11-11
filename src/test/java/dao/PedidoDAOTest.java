package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Comprador;
import model.ItemCarrinho;
import model.Pedido;
import model.Produto;
import model.Produto.Categoria;
import model.StatusPedido;

class PedidoDAOTest {

    private static final ProductDao produtos = new ProductDao();
    private static final PedidoDAO dao = new PedidoDAO();
    private static int seq = 0;

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(PedidoDAOTest.class);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    private static String novoComprador() throws Exception {
        String prontuario = "CJ" + (++seq);
        assertTrue(new UserDao().create(new Comprador(prontuario, prontuario + "@x.test", "senha1234", "Comprador " + seq)));
        return prontuario;
    }

    private static Produto novoProduto(String nome, String preco, int estoque) throws Exception {
        Produto p = new Produto(nome, "d", new BigDecimal(preco), estoque, Categoria.SALGADO, null);
        produtos.create(p);
        return p;
    }

    private static int estoque(Produto p) throws Exception {
        return produtos.buscarResumo(p.getId()).getEstoque();
    }

    @Test
    void totalCalculadoNoServidorComPrecoDoBancoEBaixaDeEstoque() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Coxinha", "6.50", 10);
        Produto b = novoProduto("Suco", "5.10", 4);

        // preco "forjado" no carrinho da sessao deve ser ignorado
        Produto forjado = new Produto();
        forjado.setId(a.getId());
        forjado.setPreco(new BigDecimal("0.01"));

        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(forjado, 3), new ItemCarrinho(b, 2)));

        assertEquals(StatusPedido.PENDENTE, pedido.getStatus());
        assertEquals(0, new BigDecimal("29.70").compareTo(pedido.getTotal())); // 3 x 6,50 + 2 x 5,10
        assertEquals(7, estoque(a));
        assertEquals(2, estoque(b));

        Pedido lido = dao.buscarPorId(pedido.getId());
        assertEquals(0, new BigDecimal("29.70").compareTo(lido.getTotal()));
        assertEquals(2, lido.getItens().size());
        assertNotNull(lido.getCriadoEm());
        assertEquals(StatusPedido.PENDENTE, lido.getStatus());
    }

    @Test
    void itemGuardaPrecoUnitarioDoMomentoDaCompra() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Pastel", "7.00", 5);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));

        Produto reajustado = new Produto("Pastel", "d", new BigDecimal("9.99"), 4, Categoria.SALGADO, null);
        reajustado.setId(a.getId());
        produtos.atualizar(reajustado);

        Pedido lido = dao.buscarPorId(pedido.getId());
        assertEquals(0, new BigDecimal("7.00").compareTo(lido.getItens().get(0).getProduto().getPreco()));
        assertEquals(0, new BigDecimal("7.00").compareTo(lido.getTotal()));
    }

    @Test
    void estoqueInsuficienteFalhaSemGravarNada() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Quibe", "6.00", 5);
        Produto b = novoProduto("Raro", "3.00", 1);
        int pedidosAntes = dao.listarPorComprador(comprador).size();

        EstoqueInsuficienteException ex = assertThrows(EstoqueInsuficienteException.class,
                () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 2), new ItemCarrinho(b, 2))));

        assertEquals("Raro", ex.getProduto());
        assertEquals(1, ex.getDisponivel());
        assertEquals(5, estoque(a), "o item anterior nao pode ter baixado estoque");
        assertEquals(1, estoque(b));
        assertEquals(pedidosAntes, dao.listarPorComprador(comprador).size());
    }

    @Test
    void produtoEsgotadoNaoPodeSerComprado() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Esgotado", "4.00", 0);
        assertThrows(EstoqueInsuficienteException.class, () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1))));
        assertEquals(0, estoque(a));
    }

    @Test
    void compradorInexistenteFazRollbackDoEstoque() throws Exception {
        Produto a = novoProduto("Com rollback", "4.00", 5);
        assertThrows(java.sql.SQLException.class, () -> dao.criarPedido("NAOEXISTE", List.of(new ItemCarrinho(a, 2))));
        assertEquals(5, estoque(a));
    }

    @Test
    void carrinhoVazioOuQuantidadeInvalidaSaoRejeitados() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Qualquer", "1.00", 5);
        assertThrows(RegraNegocioException.class, () -> dao.criarPedido(comprador, List.of()));
        assertThrows(RegraNegocioException.class, () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 0))));
    }

    @Test
    void cancelarDevolveEstoqueEUmaVezSo() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Cancelavel", "2.00", 6);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 4)));
        assertEquals(2, estoque(a));

        dao.atualizarStatus(pedido.getId(), StatusPedido.CANCELADO);
        assertEquals(6, estoque(a));
        assertEquals(StatusPedido.CANCELADO, dao.buscarPorId(pedido.getId()).getStatus());

        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.CANCELADO));
        assertEquals(6, estoque(a), "cancelar de novo nao devolve estoque em dobro");
    }

    @Test
    void cicloCompletoDeStatusEPedidoNaoEhApagado() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Ciclo", "2.00", 6);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));

        dao.atualizarStatus(pedido.getId(), StatusPedido.EM_PREPARO);
        dao.atualizarStatus(pedido.getId(), StatusPedido.PRONTO);
        dao.atualizarStatus(pedido.getId(), StatusPedido.ENTREGUE);

        assertEquals(StatusPedido.ENTREGUE, dao.buscarPorId(pedido.getId()).getStatus());
        assertTrue(dao.listarPedidos(StatusPedido.ENTREGUE).stream().anyMatch(p -> p.getId() == pedido.getId()));
        assertTrue(dao.listarPedidos(StatusPedido.PENDENTE).stream().noneMatch(p -> p.getId() == pedido.getId()));
        assertEquals(5, estoque(a), "entregar nao devolve estoque");
    }

    @Test
    void transicoesInvalidasSaoRejeitadas() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Transicoes", "2.00", 10);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));

        // pular etapas
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.PRONTO));
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.ENTREGUE));
        // voltar / repetir
        dao.atualizarStatus(pedido.getId(), StatusPedido.EM_PREPARO);
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.PENDENTE));
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.EM_PREPARO));
        // estados finais nao mudam
        dao.atualizarStatus(pedido.getId(), StatusPedido.PRONTO);
        dao.atualizarStatus(pedido.getId(), StatusPedido.ENTREGUE);
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), StatusPedido.CANCELADO));
        // pedido inexistente
        assertThrows(RegraNegocioException.class, () -> dao.atualizarStatus(999999, StatusPedido.EM_PREPARO));
    }

    @Test
    void compradorSoCancelaOProprioPedidoPendente() throws Exception {
        String dono = novoComprador();
        String outro = novoComprador();
        Produto a = novoProduto("Cancelamento do comprador", "2.00", 10);
        Pedido pedido = dao.criarPedido(dono, List.of(new ItemCarrinho(a, 2)));

        assertThrows(RegraNegocioException.class, () -> dao.cancelarComoComprador(pedido.getId(), outro));
        assertEquals(StatusPedido.PENDENTE, dao.buscarPorId(pedido.getId()).getStatus());

        dao.atualizarStatus(pedido.getId(), StatusPedido.EM_PREPARO);
        assertThrows(TransicaoInvalidaException.class, () -> dao.cancelarComoComprador(pedido.getId(), dono));
        assertEquals(8, estoque(a));

        Pedido outroPedido = dao.criarPedido(dono, List.of(new ItemCarrinho(a, 3)));
        dao.cancelarComoComprador(outroPedido.getId(), dono);
        assertEquals(8, estoque(a));
        assertEquals(StatusPedido.CANCELADO, dao.buscarPorId(outroPedido.getId()).getStatus());
    }

    @Test
    void listagemOrdenaDoMaisRecenteParaOMaisAntigoEUsaDataInformada() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Ordem", "1.00", 50);
        LocalDateTime agora = LocalDateTime.of(2030, 5, 10, 12, 0);
        Pedido antigo = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)), agora.minusDays(3));
        Pedido novo = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)), agora);

        List<Pedido> lista = dao.listarPorComprador(comprador);
        assertEquals(novo.getId(), lista.get(0).getId());
        assertEquals(antigo.getId(), lista.get(1).getId());
        assertEquals(agora.minusDays(3), lista.get(1).getCriadoEm());
    }

    @Test
    void precoAlteradoDesdeOCarrinhoImpedeOCheckoutSemGravarNada() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Preco mudou", "5.00", 10);
        // o carrinho guarda o preco visto pelo comprador (5,00)
        Produto visto = new Produto();
        visto.setId(a.getId());
        visto.setPreco(new BigDecimal("5.00"));

        Produto reajustado = new Produto("Preco mudou", "d", new BigDecimal("6.00"), 10, Categoria.SALGADO, null);
        reajustado.setId(a.getId());
        produtos.atualizar(reajustado);

        PrecoAlteradoException ex = assertThrows(PrecoAlteradoException.class,
                () -> dao.criarPedidoConfirmandoPrecos(comprador, List.of(new ItemCarrinho(visto, 2))));
        assertEquals(0, new BigDecimal("6.00").compareTo(ex.getAtual()));
        assertEquals(10, estoque(a), "nada baixou");
        assertTrue(dao.listarPorComprador(comprador).isEmpty());

        // depois de o carrinho ser atualizado para o preco atual, o checkout passa
        visto.setPreco(new BigDecimal("6.00"));
        var pedido = dao.criarPedidoConfirmandoPrecos(comprador, List.of(new ItemCarrinho(visto, 2)));
        assertEquals(0, new BigDecimal("12.00").compareTo(pedido.getTotal()));
    }
}
