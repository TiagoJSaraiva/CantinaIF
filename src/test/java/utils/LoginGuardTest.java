package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import seed.DemoDataSeeder;

class LoginGuardTest {

    private final AtomicLong agora = new AtomicLong(1_000_000L);
    private final AtomicBoolean demo = new AtomicBoolean(false);
    // 3 falhas por conta em 1 min; 10 por IP em 5 min
    private final LoginGuard guarda = new LoginGuard(3, 60_000L, 10, 300_000L, agora::get, demo::get);

    @Test
    void bloqueiaPorContaSemPunirOutrasContasDoMesmoIp() {
        for (int i = 0; i < 3; i++) {
            guarda.falha("1.1.1.1", "CJ100");
        }
        assertTrue(guarda.bloqueado("1.1.1.1", "CJ100"));
        assertTrue(guarda.bloqueado("9.9.9.9", "cj100"), "o bloqueio e da conta, de qualquer IP (e sem diferenciar maiusculas)");
        assertFalse(guarda.bloqueado("1.1.1.1", "CJ200"), "outra conta no mesmo IP continua livre");
        assertTrue(guarda.segundosParaLiberar("1.1.1.1", "CJ100") > 0);
    }

    @Test
    void ipsAtrasDoMesmoNatNaoSaoBloqueadosPorPoucasFalhas() {
        // 5 pessoas diferentes erram uma vez cada no mesmo IP: ninguem e bloqueado
        for (int i = 0; i < 5; i++) {
            guarda.falha("10.0.0.1", "CJ30" + i);
        }
        for (int i = 0; i < 5; i++) {
            assertFalse(guarda.bloqueado("10.0.0.1", "CJ30" + i));
        }
        assertFalse(guarda.bloqueado("10.0.0.1", "CJ999"));
    }

    @Test
    void tetoPorIpBloqueiaVarreduraDeContas() {
        for (int i = 0; i < 10; i++) {
            guarda.falha("2.2.2.2", "CJ" + i); // 10 contas diferentes, uma falha cada
        }
        assertTrue(guarda.bloqueado("2.2.2.2", "CJ-qualquer"));
        assertFalse(guarda.bloqueado("3.3.3.3", "CJ-qualquer"));
    }

    @Test
    void sucessoZeraOContadorDaConta() {
        guarda.falha("1.1.1.1", "CJ500");
        guarda.falha("1.1.1.1", "CJ500");
        guarda.sucesso("CJ500");
        guarda.falha("1.1.1.1", "CJ500");
        assertFalse(guarda.bloqueado("1.1.1.1", "CJ500"));
    }

    @Test
    void liberaDepoisDaJanela() {
        for (int i = 0; i < 3; i++) {
            guarda.falha("1.1.1.1", "CJ600");
        }
        agora.addAndGet(60_001L);
        assertFalse(guarda.bloqueado("1.1.1.1", "CJ600"));
    }

    @Test
    void contasDeDemoFicamForaDoBloqueioPorContaSomenteNoModoDemonstracao() {
        demo.set(true);
        for (int i = 0; i < 6; i++) {
            guarda.falha("4.4.4.4", DemoDataSeeder.VENDEDOR_PRONTUARIO);
        }
        assertFalse(guarda.bloqueado("4.4.4.4", DemoDataSeeder.VENDEDOR_PRONTUARIO), "senha publica: nao pode derrubar a demo");
        assertFalse(guarda.bloqueado("5.5.5.5", DemoDataSeeder.VENDEDOR_PRONTUARIO));
        // mas o teto por IP continua valendo
        for (int i = 0; i < 5; i++) {
            guarda.falha("4.4.4.4", DemoDataSeeder.COMPRADOR_PRONTUARIO);
        }
        assertTrue(guarda.bloqueado("4.4.4.4", DemoDataSeeder.COMPRADOR_PRONTUARIO));
        assertFalse(guarda.bloqueado("5.5.5.5", DemoDataSeeder.COMPRADOR_PRONTUARIO));

        demo.set(false);
        for (int i = 0; i < 3; i++) {
            guarda.falha("6.6.6.6", DemoDataSeeder.VENDEDOR_PRONTUARIO);
        }
        assertTrue(guarda.bloqueado("7.7.7.7", DemoDataSeeder.VENDEDOR_PRONTUARIO), "fora da demo as contas sao protegidas");
    }
}
