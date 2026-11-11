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

    /**
     * Visitantes anonimos NAO ganham sessao. Nas telas de login e cadastro o token CSRF e "double submit": um
     * cookie aleatorio (SameSite=Strict, HttpOnly) e o mesmo valor num campo oculto, comparados no servidor.
     */
    static final String COOKIE_ANONIMO = "csrf_anon";
    private static final Set<String> TELAS_ANONIMAS = Set.of("/login.jsp", "/cadastro.jsp");
    private static final Set<String> ACOES_ANONIMAS = Set.of("/login", "/register");
    private static final java.util.regex.Pattern FORMATO_TOKEN = java.util.regex.Pattern.compile("[A-Za-z0-9_-]{43}");

    /** Atributo da requisicao com o token que os formularios devem embutir (da sessao ou o anonimo). */
    public static final String ATTR_FORM = "csrfForm";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String caminho = request.getServletPath();

        if (METODOS_SEGUROS.contains(request.getMethod())) {
            if (caminho.endsWith(".jsp")) {
                HttpSession existente = request.getSession(false);
                if (existente != null) {
                    request.setAttribute(ATTR_FORM, token(existente));
                } else if (TELAS_ANONIMAS.contains(caminho)) {
                    request.setAttribute(ATTR_FORM, tokenAnonimo(request, response));
                }
            }
            chain.doFilter(req, res);
            return;
        }

        String recebido = request.getHeader(HEADER);
        if (recebido == null) {
            recebido = request.getParameter(PARAM);
        }
        HttpSession session = request.getSession(false);
        String esperado = session == null ? null : (String) session.getAttribute(ATTR);
        String valido = null;
        if (esperado != null && recebido != null && iguais(esperado, recebido)) {
            valido = esperado;
        } else if (recebido != null && ACOES_ANONIMAS.contains(caminho)) {
            String anonimo = cookieAnonimo(request);
            if (anonimo != null && iguais(anonimo, recebido)) {
                valido = anonimo;
            }
        }
        if (valido == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token CSRF ausente ou inválido.");
            return;
        }
        request.setAttribute(ATTR_FORM, valido); // para re-exibir o formulario (ex.: erro de login) sem sessao
        chain.doFilter(req, res);
    }

    private static String cookieAnonimo(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie c : cookies) {
            if (COOKIE_ANONIMO.equals(c.getName()) && c.getValue() != null && FORMATO_TOKEN.matcher(c.getValue()).matches()) {
                return c.getValue();
            }
        }
        return null;
    }

    /** Reaproveita o cookie anonimo valido ou emite um novo (sem criar sessao). */
    private static String tokenAnonimo(HttpServletRequest request, HttpServletResponse response) {
        String atual = cookieAnonimo(request);
        if (atual != null) {
            return atual;
        }
        String novo = novoToken();
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie(COOKIE_ANONIMO, novo);
        String contexto = request.getContextPath();
        cookie.setPath(contexto == null || contexto.isEmpty() ? "/" : contexto);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(30 * 60);
        cookie.setSecure(request.isSecure() || "true".equalsIgnoreCase(utils.Config.get("CANTINA_COOKIE_SECURE")));
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
        return novo;
    }

    private static String novoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Retorna o token CSRF da sessao, criando-o se necessario. */
    static String token(HttpSession session) {
        String token = (String) session.getAttribute(ATTR);
        if (token == null) {
            token = novoToken();
            session.setAttribute(ATTR, token);
        }
        return token;
    }

    private static boolean iguais(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
