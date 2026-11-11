package servlets;

import java.io.IOException;
import java.util.concurrent.locks.Lock;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import utils.DbGate;

/** Cada requisicao segura a trava de leitura de {@link DbGate}; o reset da demonstracao toma a de escrita. */
public class DbGateFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        Lock leitura = DbGate.leitura();
        leitura.lock();
        try {
            chain.doFilter(req, res);
        } finally {
            leitura.unlock();
        }
    }
}
