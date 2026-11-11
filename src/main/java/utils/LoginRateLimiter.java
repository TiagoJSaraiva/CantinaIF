package utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Limite simples de tentativas de login: no maximo {@code maxFalhas} falhas dentro de uma janela deslizante,
 * por chave (o servlet usa o endereco IP). Em memoria; nao e compartilhado entre instancias.
 */
public final class LoginRateLimiter {

    /** 8 falhas a cada 5 minutos por IP. */
    public static final LoginRateLimiter PADRAO = new LoginRateLimiter(8, 5 * 60_000L, System::currentTimeMillis);

    private static final int LIMITE_DE_CHAVES = 5_000;

    private final int maxFalhas;
    private final long janelaMs;
    private final LongSupplier relogio;
    private final Map<String, Deque<Long>> falhas = new HashMap<>();

    public LoginRateLimiter(int maxFalhas, long janelaMs, LongSupplier relogio) {
        this.maxFalhas = maxFalhas;
        this.janelaMs = janelaMs;
        this.relogio = relogio;
    }

    /** True se a chave ja atingiu o limite de falhas dentro da janela. */
    public synchronized boolean bloqueado(String chave) {
        Deque<Long> d = falhas.get(chave);
        if (d == null) {
            return false;
        }
        expirar(d);
        if (d.isEmpty()) {
            falhas.remove(chave);
            return false;
        }
        return d.size() >= maxFalhas;
    }

    /** Segundos ate a chave poder tentar de novo (0 se nao esta bloqueada). */
    public synchronized long segundosParaLiberar(String chave) {
        if (!bloqueado(chave)) {
            return 0;
        }
        long libera = falhas.get(chave).peekFirst() + janelaMs;
        return Math.max(1, (libera - relogio.getAsLong() + 999) / 1000);
    }

    public synchronized void falha(String chave) {
        if (falhas.size() > LIMITE_DE_CHAVES) {
            falhas.values().removeIf(d -> { expirar(d); return d.isEmpty(); });
        }
        Deque<Long> d = falhas.computeIfAbsent(chave, k -> new ArrayDeque<>());
        expirar(d);
        d.addLast(relogio.getAsLong());
    }

    public synchronized void sucesso(String chave) {
        falhas.remove(chave);
    }

    private void expirar(Deque<Long> d) {
        long limite = relogio.getAsLong() - janelaMs;
        while (!d.isEmpty() && d.peekFirst() <= limite) {
            d.pollFirst();
        }
    }
}
