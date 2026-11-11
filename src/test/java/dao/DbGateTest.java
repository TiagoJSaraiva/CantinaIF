package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import seed.DemoDataSeeder;
import utils.BaseRestauradaException;
import utils.Database;
import utils.DemoReset;
import utils.GeracaoDaRequisicao;

class DbGateTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2030, 1, 15, 10, 0);

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(DbGateTest.class);
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.run(c, AGORA);
        }
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @Test
    void resetNaoRetornaNemTravaLeitoresQuandoHaConexaoAbertaPorMuitoTempo() throws Exception {
        DemoDataSeeder.Plano plano = DemoDataSeeder.preparar();
        Connection presa = Database.getConnection(); // conexao aberta = trava de leitura segurada
        try {
            long ini = System.nanoTime();
            assertFalse(DemoReset.aplicarComTrava(plano, AGORA, 300), "sem conseguir a trava, o reset desiste");
            long ms = (System.nanoTime() - ini) / 1_000_000;
            assertTrue(ms < 3000, "o reset nao pode esperar indefinidamente: " + ms + " ms");
        } finally {
            presa.close();
        }
        // liberada a conexao, o reset passa
        assertTrue(DemoReset.aplicarComTrava(plano, AGORA, 5000));
    }

    @Test
    void leitorQueChegaDepoisDoResetPendenteNaoFicaPresoParaSempre() throws Exception {
        DemoDataSeeder.Plano plano = DemoDataSeeder.preparar();
        Connection presa = Database.getConnection();
        CountDownLatch resetTentando = new CountDownLatch(1);
        AtomicReference<Boolean> resultadoReset = new AtomicReference<>();
        Thread reset = new Thread(() -> {
            try {
                resetTentando.countDown();
                resultadoReset.set(DemoReset.aplicarComTrava(plano, AGORA, 1000));
            } catch (Exception e) {
                resultadoReset.set(null);
            }
        });
        reset.start();
        resetTentando.await();
        Thread.sleep(100); // o reset esta na fila da trava (justa) esperando a conexao presa

        long ini = System.nanoTime();
        AtomicReference<Throwable> erro = new AtomicReference<>();
        Thread leitor = new Thread(() -> {
            try (Connection c = Database.getConnection()) {
                assertNotNull(c);
            } catch (Throwable t) {
                erro.set(t);
            }
        });
        leitor.start();
        leitor.join(5000);
        reset.join(5000);
        presa.close();

        assertFalse(leitor.isAlive(), "o leitor nao pode ficar bloqueado alem do tempo limite do reset");
        assertNull(erro.get());
        assertEquals(Boolean.FALSE, resultadoReset.get());
        assertTrue((System.nanoTime() - ini) / 1_000_000 < 4000);
    }

    @Test
    void conexaoFechadaLiberaATravaMesmoComFechamentoDuplo() throws Exception {
        Connection c = Database.getConnection();
        c.close();
        c.close(); // fechar de novo nao pode liberar a trava duas vezes
        assertTrue(DemoReset.aplicarComTrava(DemoDataSeeder.preparar(), AGORA, 2000));
    }

    @Test
    void sessaoDeGeracaoAnteriorNaoAcessaOBancoNemDeixaTravaPresa() throws Exception {
        long atual = Database.geracao();
        GeracaoDaRequisicao.definir(atual - 1); // sessao autenticada antes de uma restauracao
        try {
            assertThrows(BaseRestauradaException.class, Database::getConnection);
        } finally {
            GeracaoDaRequisicao.limpar();
        }
        // a trava foi devolvida: o reset consegue
        assertTrue(DemoReset.aplicarComTrava(DemoDataSeeder.preparar(), AGORA, 2000));
        // e uma requisicao da geracao correta usa o banco normalmente
        GeracaoDaRequisicao.definir(Database.geracao());
        try (Connection c = Database.getConnection()) {
            assertNotNull(c);
        } finally {
            GeracaoDaRequisicao.limpar();
        }
    }

    @Test
    void restauracaoMudaAGeracaoEInvalidaSessoesAntigas() throws Exception {
        long antes = Database.geracao();
        assertTrue(DemoReset.aplicarComTrava(DemoDataSeeder.preparar(), AGORA, 5000));
        assertTrue(Database.geracao() > antes);
        GeracaoDaRequisicao.definir(antes);
        try {
            assertThrows(BaseRestauradaException.class, Database::getConnection);
        } finally {
            GeracaoDaRequisicao.limpar();
        }
        assertEquals(TimeUnit.SECONDS.toMillis(1), 1000); // sanidade: unidades do teste
    }
}
