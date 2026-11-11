package servlets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpSession;
import model.Carrinho;

class CarrinhoServletTest {

    private static HttpSession sessaoEmMemoria() {
        Map<String, Object> attrs = new ConcurrentHashMap<>();
        HttpSession s = mock(HttpSession.class);
        when(s.getAttribute(anyString())).thenAnswer(i -> attrs.get(i.getArgument(0)));
        doAnswer(i -> {
            Thread.yield(); // aumenta a chance de expor uma corrida na criacao
            attrs.put(i.getArgument(0), i.getArgument(1));
            return null;
        }).when(s).setAttribute(anyString(), any());
        return s;
    }

    @Test
    void requisicoesParalelasEmSessaoNovaCompartilhamUmUnicoCarrinho() throws Exception {
        for (int rodada = 0; rodada < 50; rodada++) {
            HttpSession sessao = sessaoEmMemoria();
            int n = 8;
            ExecutorService pool = Executors.newFixedThreadPool(n);
            CountDownLatch largada = new CountDownLatch(1);
            Set<Carrinho> vistos = ConcurrentHashMap.newKeySet();
            for (int i = 0; i < n; i++) {
                pool.submit(() -> {
                    largada.await();
                    vistos.add(CarrinhoServlet.obterOuCriarCarrinho(sessao));
                    return null;
                });
            }
            largada.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
            assertEquals(1, vistos.size(), "rodada " + rodada + ": mais de um carrinho criado");
        }
    }
}
