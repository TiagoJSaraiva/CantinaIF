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
    private static final String V = "VPED";
    private static final String OUTRO = "VPED2";
    private static final String MOTIVO = "Produto acabou";

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(PedidoDAOTest.class);
        TestDb.vendedor(V);
        TestDb.vendedor(OUTRO);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    private static String novoComprador() throws Exception {
        String prontuario = "CJ" + (++seq);
        assertTrue(new UserDao().create(new Comprador(prontuario, prontuario + "@x.test", "senha1234", "Comprador " + seq)));
        TestDb.vincular(prontuario, V);
        return prontuario;
    }

    private static Produto novoProduto(String nome, String preco, int estoque) throws Exception {
        return novoProduto(V, nome, preco, estoque);
    }

    private static Produto novoProduto(String vendedor, String nome, String preco, int estoque) throws Exception {
        Produto p = new Produto(nome, "d", new BigDecimal(preco), estoque, Categoria.SALGADO, null);
        p.setVendedorProntuario(vendedor);
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
        reajustado.setVendedorProntuario(V);
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
        assertThrows(RegraNegocioException.class, () -> dao.criarPedido("NAOEXISTE", List.of(new ItemCarrinho(a, 2))));
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

        dao.atualizarStatus(pedido.getId(), V, StatusPedido.CANCELADO, MOTIVO);
        assertEquals(6, estoque(a));
        assertEquals(StatusPedido.CANCELADO, dao.buscarPorId(pedido.getId()).getStatus());

        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.CANCELADO, MOTIVO));
        assertEquals(6, estoque(a), "cancelar de novo nao devolve estoque em dobro");
    }

    @Test
    void cicloCompletoDeStatusEPedidoNaoEhApagado() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Ciclo", "2.00", 6);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));

        dao.atualizarStatus(pedido.getId(), V, StatusPedido.EM_PREPARO, null);
        dao.atualizarStatus(pedido.getId(), V, StatusPedido.PRONTO, null);
        dao.atualizarStatus(pedido.getId(), V, StatusPedido.ENTREGUE, null);

        assertEquals(StatusPedido.ENTREGUE, dao.buscarPorId(pedido.getId()).getStatus());
        assertTrue(dao.listarPedidos(V, StatusPedido.ENTREGUE).stream().anyMatch(p -> p.getId() == pedido.getId()));
        assertTrue(dao.listarPedidos(V, StatusPedido.PENDENTE).stream().noneMatch(p -> p.getId() == pedido.getId()));
        assertEquals(5, estoque(a), "entregar nao devolve estoque");
    }

    @Test
    void transicoesInvalidasSaoRejeitadas() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Transicoes", "2.00", 10);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));

        // pular etapas
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.PRONTO, null));
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.ENTREGUE, null));
        // voltar / repetir
        dao.atualizarStatus(pedido.getId(), V, StatusPedido.EM_PREPARO, null);
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.PENDENTE, null));
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.EM_PREPARO, null));
        // estados finais nao mudam
        dao.atualizarStatus(pedido.getId(), V, StatusPedido.PRONTO, null);
        dao.atualizarStatus(pedido.getId(), V, StatusPedido.ENTREGUE, null);
        assertThrows(TransicaoInvalidaException.class, () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.CANCELADO, MOTIVO));
        // pedido inexistente
        assertThrows(RegraNegocioException.class, () -> dao.atualizarStatus(999999, V, StatusPedido.EM_PREPARO, null));
    }

    @Test
    void compradorSoCancelaOProprioPedidoPendente() throws Exception {
        String dono = novoComprador();
        String outro = novoComprador();
        Produto a = novoProduto("Cancelamento do comprador", "2.00", 10);
        Pedido pedido = dao.criarPedido(dono, List.of(new ItemCarrinho(a, 2)));

        assertThrows(RegraNegocioException.class, () -> dao.cancelarComoComprador(pedido.getId(), outro));
        assertEquals(StatusPedido.PENDENTE, dao.buscarPorId(pedido.getId()).getStatus());

        dao.atualizarStatus(pedido.getId(), V, StatusPedido.EM_PREPARO, null);
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
    void numeroDoPedidoESequenciaDoProprioAlunoEmQualquerCantina() throws Exception {
        String ana = novoComprador();
        String bia = novoComprador();
        Produto a = novoProduto("Numeracao", "1.00", 50);
        Produto b = novoProduto(OUTRO, "Numeracao outra cantina", "1.00", 50);
        LocalDateTime t = LocalDateTime.of(2031, 1, 10, 9, 0);
        Pedido ana1 = dao.criarPedido(ana, List.of(new ItemCarrinho(a, 1)), t);
        Pedido bia1 = dao.criarPedido(bia, List.of(new ItemCarrinho(a, 1)), t.plusHours(1));
        Pedido ana2 = dao.criarPedido(ana, List.of(new ItemCarrinho(a, 2)), t.plusHours(2));
        dao.cancelarComoComprador(ana2.getId(), ana); // cancelado continua contando
        TestDb.vincular(ana, OUTRO);
        Pedido ana3 = dao.criarPedido(ana, List.of(new ItemCarrinho(b, 1)), t.plusHours(3));
        TestDb.vincular(ana, V);

        List<Pedido> daAna = dao.listarPorComprador(ana);
        assertEquals(List.of(3, 2, 1), daAna.stream().map(Pedido::getNumeroDoComprador).toList());
        assertEquals(List.of(ana3.getId(), ana2.getId(), ana1.getId()), daAna.stream().map(Pedido::getId).toList());
        assertEquals(1, dao.listarPorComprador(bia).get(0).getNumeroDoComprador());

        // o vendedor ve o mesmo numero que o aluno
        Pedido visto = dao.listarPedidos(V, null).stream().filter(p -> p.getId() == bia1.getId()).findFirst().orElseThrow();
        assertEquals(1, visto.getNumeroDoComprador());
        assertEquals(3, dao.listarPedidos(OUTRO, null).stream().filter(p -> p.getId() == ana3.getId())
                .findFirst().orElseThrow().getNumeroDoComprador());
        assertEquals(2, dao.buscarPorId(ana2.getId()).getNumeroDoComprador());
        assertEquals(2, new model.PedidoDto(dao.buscarPorId(ana2.getId()), false).numero);
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
        reajustado.setVendedorProntuario(V);
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

    @Test
    void pedidoEDoVendedorVinculadoEProdutoDeOutroVendedorERecusado() throws Exception {
        String comprador = novoComprador();
        Produto meu = novoProduto("Do vendedor", "2.00", 5);
        Produto alheio = novoProduto(OUTRO, "De outro vendedor", "2.00", 5);

        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(meu, 1)));
        assertEquals(V, pedido.getVendedorProntuario());
        assertEquals(V, dao.buscarPorId(pedido.getId()).getVendedorProntuario());
        assertEquals("Cantina " + V, dao.buscarPorId(pedido.getId()).getVendedorNome());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(meu, 1), new ItemCarrinho(alheio, 1))));
        assertTrue(ex.getMessage().contains("não é do seu vendedor"));
        assertEquals(4, estoque(meu), "nada baixou no pedido recusado");
        assertEquals(5, estoque(alheio));

        // o outro vendedor nao ve nem altera o pedido
        assertTrue(dao.listarPedidos(OUTRO, null).stream().noneMatch(p -> p.getId() == pedido.getId()));
        assertThrows(RegraNegocioException.class,
                () -> dao.atualizarStatus(pedido.getId(), OUTRO, StatusPedido.CANCELADO, MOTIVO));
        assertEquals(StatusPedido.PENDENTE, dao.buscarPorId(pedido.getId()).getStatus());
    }

    @Test
    void compradorSemVinculoOuDesativadoNaoFazPedido() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Sem vinculo", "2.00", 5);
        TestDb.vincular(comprador, null);
        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1))));
        assertTrue(ex.getMessage().contains("Vincule"));

        TestDb.vincular(comprador, V);
        assertTrue(new UserDao().definirAtivo(comprador, V, false));
        ex = assertThrows(RegraNegocioException.class, () -> dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1))));
        assertTrue(ex.getMessage().contains("desativada"));
        assertEquals(5, estoque(a));
    }

    @Test
    void cancelamentoPeloVendedorExigeMotivoQueFicaRegistrado() throws Exception {
        String comprador = novoComprador();
        Produto a = novoProduto("Motivo", "2.00", 5);
        Pedido pedido = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 2)));

        for (String ruim : new String[] { null, "", "  ", "ab", "x".repeat(PedidoDAO.MOTIVO_MAXIMO + 1) }) {
            assertThrows(PedidoDAO.MotivoInvalidoException.class,
                    () -> dao.atualizarStatus(pedido.getId(), V, StatusPedido.CANCELADO, ruim));
        }
        assertEquals(StatusPedido.PENDENTE, dao.buscarPorId(pedido.getId()).getStatus());
        assertEquals(3, estoque(a));

        dao.atualizarStatus(pedido.getId(), V, StatusPedido.CANCELADO, "  Acabou o recheio  ");
        Pedido lido = dao.buscarPorId(pedido.getId());
        assertEquals(StatusPedido.CANCELADO, lido.getStatus());
        assertEquals("VENDEDOR", lido.getCanceladoPor());
        assertEquals("Acabou o recheio", lido.getMotivoCancelamento());
        assertEquals(5, estoque(a));

        Pedido outro = dao.criarPedido(comprador, List.of(new ItemCarrinho(a, 1)));
        dao.cancelarComoComprador(outro.getId(), comprador);
        assertEquals("COMPRADOR", dao.buscarPorId(outro.getId()).getCanceladoPor());
        assertNull(dao.buscarPorId(outro.getId()).getMotivoCancelamento());
    }
}
