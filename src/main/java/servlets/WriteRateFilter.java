package servlets;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ClientIp;
import utils.RateLimiter;

/**
 * Limite de taxa para operacoes de escrita (cadastro, produtos, estoque, carrinho, checkout, status de pedido),
 * por IP do cliente (o IP real quando atras de proxy confiavel; veja RemoteIpValve). Em memoria, por instancia.
 */
public class WriteRateFilter implements Filter {

    /** 90 operacoes de escrita por minuto por IP (o painel do vendedor faz varias chamadas seguidas). */
    static final RateLimiter GERAL = new RateLimiter(90, 60_000L, System::currentTimeMillis);
    /** 6 cadastros de conta por 10 minutos por IP. */
    static final RateLimiter CADASTRO = new RateLimiter(6, 10 * 60_000L, System::currentTimeMillis);

    private static final Set<String> CAMINHOS = Set.of(
            "/register", "/registrar-produto", "/editar-produto", "/atualizarEstoque", "/carrinho",
            "/FinalizarCompraServlet", "/apiUser/pedidos/cancelar", "/api/pedidos/status");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String metodo = request.getMethod();
        String caminho = request.getServletPath();
        boolean escrita = !("GET".equals(metodo) || "HEAD".equals(metodo) || "OPTIONS".equals(metodo));
        if (escrita && CAMINHOS.contains(caminho)) {
            String ip = ClientIp.de(request);
            boolean permitido = GERAL.tentar(ip);
            if (permitido && "/register".equals(caminho)) {
                permitido = CADASTRO.tentar(ip);
            }
            if (!permitido) {
                ClientIp.registrarBloqueio("escrita", ip);
                response.setHeader("Retry-After", "60");
                if ("/register".equals(caminho) || "/FinalizarCompraServlet".equals(caminho)) {
                    response.sendError(429, "Muitas requisições.");
                } else {
                    Json.result(response, 429, false, "Muitas requisições em pouco tempo. Aguarde um instante e tente de novo.");
                }
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
