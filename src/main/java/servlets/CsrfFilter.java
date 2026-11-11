package servlets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Protecao CSRF por token de sessao ("synchronizer token").
 * <ul>
 *   <li>Em GET de paginas .jsp garante que a sessao tenha o atributo {@code csrfToken}, que as paginas
 *       embutem em formularios ({@code _csrf}) e em {@code <meta name="csrf-token">}.</li>
 *   <li>Em qualquer metodo que altera estado (POST, PUT, DELETE, PATCH) exige o mesmo token no
 *       cabecalho {@code X-CSRF-Token} ou no parametro {@code _csrf}; caso contrario responde 403.</li>
 * </ul>
 */
public class CsrfFilter implements Filter {

    public static final String ATTR = "csrfToken";
    public static final String HEADER = "X-CSRF-Token";
    public static final String PARAM = "_csrf";

    private static final Set<String> METODOS_SEGUROS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    /** Sessoes anonimas (so para o token CSRF do login/cadastro) expiram em 5 minutos. */
    static final int SESSAO_ANONIMA_SEGUNDOS = 300;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (METODOS_SEGUROS.contains(request.getMethod())) {
            String caminho = request.getServletPath();
            if (caminho.endsWith(".jsp")) {
                HttpSession existente = request.getSession(false);
                if (existente != null) {
                    token(existente);
                } else if ("/login.jsp".equals(caminho) || "/cadastro.jsp".equals(caminho)) {
                    // visitante anonimo: so as telas de login/cadastro precisam de token; sessao curta
                    HttpSession nova = request.getSession(true);
                    nova.setMaxInactiveInterval(SESSAO_ANONIMA_SEGUNDOS);
                    token(nova);
                }
            }
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        String esperado = session == null ? null : (String) session.getAttribute(ATTR);
        String recebido = request.getHeader(HEADER);
        if (recebido == null) {
            recebido = request.getParameter(PARAM);
        }
        if (esperado == null || recebido == null || !iguais(esperado, recebido)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token CSRF ausente ou inválido.");
            return;
        }
        chain.doFilter(req, res);
    }

    /** Retorna o token CSRF da sessao, criando-o se necessario. */
    static String token(HttpSession session) {
        String token = (String) session.getAttribute(ATTR);
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(ATTR, token);
        }
        return token;
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
