package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final AtomicLong agora = new AtomicLong(1_000_000L);
    private final RateLimiter limiter = new RateLimiter(3, 60_000L, agora::get);

    @Test
    void bloqueiaAposAtingirOLimiteDeFalhas() {
        assertFalse(limiter.bloqueado("1.1.1.1"));
        limiter.falha("1.1.1.1");
        limiter.falha("1.1.1.1");
        assertFalse(limiter.bloqueado("1.1.1.1"));
        limiter.falha("1.1.1.1");
        assertTrue(limiter.bloqueado("1.1.1.1"));
        assertTrue(limiter.segundosParaLiberar("1.1.1.1") > 0);
    }

    @Test
    void chavesSaoIndependentes() {
        for (int i = 0; i < 3; i++) {
            limiter.falha("a");
        }
        assertTrue(limiter.bloqueado("a"));
        assertFalse(limiter.bloqueado("b"));
    }

    @Test
    void liberaDepoisDaJanela() {
        for (int i = 0; i < 3; i++) {
            limiter.falha("a");
        }
        assertTrue(limiter.bloqueado("a"));
        agora.addAndGet(60_001L);
        assertFalse(limiter.bloqueado("a"));
        assertEquals(0, limiter.segundosParaLiberar("a"));
    }

    @Test
    void sucessoZeraOContador() {
        limiter.falha("a");
        limiter.falha("a");
        limiter.sucesso("a");
        limiter.falha("a");
        limiter.falha("a");
        assertFalse(limiter.bloqueado("a"));
    }

    @Test
    void tentarRegistraEBloqueiaNoLimite() {
        assertTrue(limiter.tentar("k"));
        assertTrue(limiter.tentar("k"));
        assertTrue(limiter.tentar("k"));
        assertFalse(limiter.tentar("k"));
        agora.addAndGet(60_001L);
        assertTrue(limiter.tentar("k"));
    }
}
