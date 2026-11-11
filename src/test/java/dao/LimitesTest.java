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
            "CANTINA_MAX_PEDIDOS_ABERTOS", "CANTINA_MAX_BYTES_IMAGENS" };

    @BeforeAll
    static void iniciar() {
        TestDb.start(LimitesTest.class);
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

        Limites.verificarNovoProduto(0);
        dao.create(new Produto("A", "d", new BigDecimal("1.00"), 1, Categoria.DOCE, new byte[600]));
        Limites.verificarNovoProduto(0);
        dao.create(new Produto("B", "d", new BigDecimal("1.00"), 1, Categoria.DOCE, null));
        LimiteAtingidoException e = assertThrows(LimiteAtingidoException.class, () -> Limites.verificarNovoProduto(0));
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
        new UserDao().create(new Comprador("LIM2", "lim2@x.demo", "senha1234", "Lim2"));
        Produto p = new Produto("Pedido limite", "d", new BigDecimal("2.00"), 50, Categoria.SALGADO, null);
        new ProductDao().create(p);
        PedidoDAO pedidos = new PedidoDAO();

        long abertos = new LimitesDao().contarPedidosAbertos();
        System.setProperty("CANTINA_MAX_PEDIDOS_ABERTOS", String.valueOf(abertos + 1));
        Limites.verificarNovoPedido();
        var pedido = pedidos.criarPedido("LIM2", List.of(new ItemCarrinho(p, 1)));
        assertThrows(LimiteAtingidoException.class, Limites::verificarNovoPedido);

        pedidos.atualizarStatus(pedido.getId(), model.StatusPedido.CANCELADO);
        Limites.verificarNovoPedido(); // cancelado nao conta como aberto
    }
}
