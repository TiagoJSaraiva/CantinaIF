package utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Trava de leitura/escrita entre as requisicoes e a restauracao da base de demonstracao: cada requisicao
 * segura a trava de LEITURA (varias ao mesmo tempo); o reset toma a de ESCRITA (espera as requisicoes em
 * andamento e segura as novas ate terminar). Assim ninguem ve a base no meio de um reset.
 * A trava e justa (FIFO) para o reset nao ser adiado indefinidamente.
 */
public final class DbGate {

    private static final ReentrantReadWriteLock TRAVA = new ReentrantReadWriteLock(true);

    private DbGate() {
    }

    public static Lock leitura() {
        return TRAVA.readLock();
    }

    public static Lock escrita() {
        return TRAVA.writeLock();
    }
}
