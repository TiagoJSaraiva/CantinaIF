package utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Limite simples de eventos por chave (tentativas de login, operacoes de escrita): no maximo {@code maxFalhas} falhas dentro de uma janela deslizante,
 * por chave (o servlet usa o endereco IP). Em memoria; nao e compartilhado entre instancias.
 */
public final class RateLimiter {

    private static final int LIMITE_DE_CHAVES = 5_000;

    private final int maxFalhas;
    private final long janelaMs;
    private final LongSupplier relogio;
    private final Map<String, Deque<Long>> falhas = new HashMap<>();

    public RateLimiter(int maxFalhas, long janelaMs, LongSupplier relogio) {
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

    /** Registra um evento se ainda houver folga e retorna true; retorna false (sem registrar) se bloqueado. */
    public synchronized boolean tentar(String chave) {
        if (bloqueado(chave)) {
            return false;
        }
        falha(chave);
        return true;
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
