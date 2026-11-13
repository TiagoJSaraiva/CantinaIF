package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Comprador;
import model.ItemCarrinho;
import model.Produto;
import model.Produto.Categoria;
import utils.Limites;

class LimitesTest {

    private static final String[] VARIAVEIS = { "CANTINA_MAX_PRODUTOS", "CANTINA_MAX_COMPRADORES",
            "CANTINA_MAX_PEDIDOS_ABERTOS", "CANTINA_MAX_BYTES_IMAGENS", "CANTINA_MAX_PRODUTOS_POR_VENDEDOR",
            "CANTINA_MAX_VENDEDORES" };

    private static final String V = "VLIM";

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(LimitesTest.class);
        TestDb.vendedor(V);
    }

    private static Produto produto(String nome, BigDecimal preco, int estoque, Categoria cat, byte[] img) {
        Produto p = new Produto(nome, "d", preco, estoque, cat, img);
        p.setVendedorProntuario(V);
        return p;
    }

    private static void comprador(String prontuario) throws Exception {
        new UserDao().create(new Comprador(prontuario, prontuario.toLowerCase() + "@x.demo", "senha1234", prontuario));
        TestDb.vincular(prontuario, V);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @AfterEach
    void limparPropriedades() {
        for (String v : VARIAVEIS) {
            System.clearProperty(v);
        }
    }

    @Test
    void foraDoModoDemonstracaoNaoHaTetoPorPadrao() {
        assertEquals(0, Limites.maxProdutos());
        assertEquals(0, Limites.maxCompradores());
        assertEquals(0, Limites.maxPedidosAbertos());
        assertEquals(0, Limites.maxBytesImagens());
    }

    @Test
    void variaveisDefinemOsTetos() {
        System.setProperty("CANTINA_MAX_PRODUTOS", "7");
        System.setProperty("CANTINA_MAX_BYTES_IMAGENS", "1000");
        assertEquals(7, Limites.maxProdutos());
        assertEquals(1000, Limites.maxBytesImagens());
        System.setProperty("CANTINA_MAX_PRODUTOS", "lixo");
        assertEquals(0, Limites.maxProdutos());
        System.setProperty("CANTINA_MAX_PRODUTOS", "-3");
        assertEquals(0, Limites.maxProdutos());
    }

    @Test
    void tetoDeProdutosEDeBytesDeImagem() throws Exception {
        ProductDao dao = new ProductDao();
        long existentes = new LimitesDao().contarProdutos();
        System.setProperty("CANTINA_MAX_PRODUTOS", String.valueOf(existentes + 2));

        Limites.verificarNovoProduto(V, 0);
        dao.create(produto("A", new BigDecimal("1.00"), 1, Categoria.DOCE, new byte[600]));
        Limites.verificarNovoProduto(V, 0);
        dao.create(produto("B", new BigDecimal("1.00"), 1, Categoria.DOCE, null));
        LimiteAtingidoException e = assertThrows(LimiteAtingidoException.class, () -> Limites.verificarNovoProduto(V, 0));
        assertTrue(e.getMessage().contains("restaurada periodicamente"));

        System.clearProperty("CANTINA_MAX_PRODUTOS");
        long usados = new LimitesDao().bytesDeImagens();
        System.setProperty("CANTINA_MAX_BYTES_IMAGENS", String.valueOf(usados + 100));
        Limites.verificarImagem(100);
        assertThrows(LimiteAtingidoException.class, () -> Limites.verificarImagem(101));
        Limites.verificarImagem(0); // sem imagem nova nunca bloqueia
    }

    @Test
    void tetoDeCompradores() throws Exception {
        long existentes = new LimitesDao().contarCompradores();
        System.setProperty("CANTINA_MAX_COMPRADORES", String.valueOf(existentes + 1));
        Limites.verificarNovoComprador();
        assertTrue(new UserDao().create(new Comprador("LIM1", "lim1@x.demo", "senha1234", "Lim")));
        assertThrows(LimiteAtingidoException.class, Limites::verificarNovoComprador);
    }

    @Test
    void tetoDePedidosEmAbertoIgnoraConcluidos() throws Exception {
        comprador("LIM2");
        Produto p = produto("Pedido limite", new BigDecimal("2.00"), 50, Categoria.SALGADO, null);
        new ProductDao().create(p);
        PedidoDAO pedidos = new PedidoDAO();

        long abertos = new LimitesDao().contarPedidosAbertos();
        System.setProperty("CANTINA_MAX_PEDIDOS_ABERTOS", String.valueOf(abertos + 1));
        Limites.verificarNovoPedido();
        var pedido = pedidos.criarPedido("LIM2", List.of(new ItemCarrinho(p, 1)));
        assertThrows(LimiteAtingidoException.class, Limites::verificarNovoPedido);

        pedidos.atualizarStatus(pedido.getId(), V, model.StatusPedido.CANCELADO, "teste");
        Limites.verificarNovoPedido(); // cancelado nao conta como aberto
    }

    @Test
    void tetoDePedidosEmAbertoPorComprador() throws Exception {
        comprador("LIM3");
        comprador("LIM4");
        Produto p = produto("Por comprador", new BigDecimal("1.00"), 100, Categoria.SALGADO, null);
        new ProductDao().create(p);
        PedidoDAO pedidos = new PedidoDAO();
        System.setProperty("CANTINA_MAX_PEDIDOS_ABERTOS_POR_COMPRADOR", "2");
        try {
            Limites.verificarNovoPedido("LIM3");
            pedidos.criarPedido("LIM3", List.of(new ItemCarrinho(p, 1)));
            Limites.verificarNovoPedido("LIM3");
            var segundo = pedidos.criarPedido("LIM3", List.of(new ItemCarrinho(p, 1)));
            LimiteAtingidoException e = assertThrows(LimiteAtingidoException.class, () -> Limites.verificarNovoPedido("LIM3"));
            assertTrue(e.getMessage().contains("2 pedidos em aberto"));
            Limites.verificarNovoPedido("LIM4"); // outro comprador nao e afetado

            pedidos.atualizarStatus(segundo.getId(), V, model.StatusPedido.CANCELADO, "teste"); // cancelado libera a vaga
            Limites.verificarNovoPedido("LIM3");
        } finally {
            System.clearProperty("CANTINA_MAX_PEDIDOS_ABERTOS_POR_COMPRADOR");
        }
    }

    @Test
    void tetoDeProdutosPorVendedorEDeVendedores() throws Exception {
        String outro = TestDb.vendedor("VLIM2");
        ProductDao dao = new ProductDao();
        long meus = new LimitesDao().contarProdutosDe(V);
        System.setProperty("CANTINA_MAX_PRODUTOS_POR_VENDEDOR", String.valueOf(meus + 1));
        Limites.verificarNovoProduto(V, 0);
        dao.create(produto("Por vendedor", new BigDecimal("1.00"), 1, Categoria.DOCE, null));
        LimiteAtingidoException e = assertThrows(LimiteAtingidoException.class, () -> Limites.verificarNovoProduto(V, 0));
        assertTrue(e.getMessage().contains("produtos por vendedor"));
        Limites.verificarNovoProduto(outro, 0); // o teto e de cada vendedor

        long vendedores = new LimitesDao().contarVendedores();
        System.setProperty("CANTINA_MAX_VENDEDORES", String.valueOf(vendedores + 1));
        Limites.verificarNovoVendedor();
        TestDb.vendedor("VLIM3");
        assertThrows(LimiteAtingidoException.class, Limites::verificarNovoVendedor);
    }
}
