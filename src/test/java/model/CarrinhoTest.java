package model;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class CarrinhoTest {

    private static Produto produto(int id, String preco, int estoque) {
        Produto p = new Produto("P" + id, "d", new BigDecimal(preco), estoque);
        p.setId(id);
        return p;
    }

    @Test
    void somaSeguraNaoEstouraIntENaoPassaDoMaximo() {
        assertEquals(Carrinho.MAX_POR_ITEM, Carrinho.somaSegura(Integer.MAX_VALUE, 5));
        assertEquals(Carrinho.MAX_POR_ITEM, Carrinho.somaSegura(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(7, Carrinho.somaSegura(3, 4));
        assertEquals(0, Carrinho.somaSegura(Integer.MIN_VALUE, 1));
    }

    @Test
    void adicionarRepetidoAcumulaLimitadoAoMaximo() {
        Carrinho c = new Carrinho();
        Produto p = produto(1, "2.50", 500);
        c.adicionarProduto(p, 60);
        c.adicionarProduto(p, 60);
        assertEquals(Carrinho.MAX_POR_ITEM, c.quantidadeDe(1));
        assertEquals(1, c.getItens().size());
    }

    @Test
    void totalEQuantidadeTotal() {
        Carrinho c = new Carrinho();
        c.adicionarProduto(produto(1, "6.50", 10), 2);
        c.adicionarProduto(produto(2, "0.10", 10), 3);
        assertEquals(0, new BigDecimal("13.30").compareTo(c.getTotal()));
        assertEquals(5, c.getQuantidadeTotal());
    }

    @Test
    void adicoesSimultaneasNaoPerdemItens() throws Exception {
        Carrinho c = new Carrinho();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            final int id = i + 1;
            pool.submit(() -> {
                largada.await();
                c.adicionarProduto(produto(id, "1.00", 100), 1);
                c.adicionarProduto(produto(1000, "1.00", 100), 1);
                return null;
            });
        }
        largada.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(threads + 1, c.getItens().size());
        assertEquals(threads, c.quantidadeDe(1000));
    }

    @Test
    void atualizarDeTrocaPrecoEstoqueERemoveOQueSumiu() {
        Carrinho c = new Carrinho();
        c.adicionarProduto(produto(1, "5.00", 10), 5);
        c.adicionarProduto(produto(2, "3.00", 10), 1);
        Produto novo1 = produto(1, "5.50", 3);
        c.atualizarDe(Map.of(1, novo1));

        List<ItemCarrinho> itens = c.getItens();
        assertEquals(1, itens.size());
        assertEquals(0, new BigDecimal("5.50").compareTo(itens.get(0).getProduto().getPreco()));
        assertEquals(3, itens.get(0).getQuantidade(), "quantidade reduzida ao estoque atual");
    }
}
