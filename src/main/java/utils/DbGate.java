package utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Trava de leitura/escrita entre o ACESSO AO BANCO e a restauracao da base de demonstracao. Quem usa o banco
 * ({@link Database#getConnection()}) segura a trava de LEITURA apenas enquanto a conexao esta aberta (curto, sem
 * I/O de rede no meio); o reset toma a de ESCRITA com tempo limite e, se nao conseguir, tenta de novo depois.
 * A requisicao HTTP em si nunca segura a trava (um cliente lento nao trava ninguem). Trava justa (FIFO).
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
