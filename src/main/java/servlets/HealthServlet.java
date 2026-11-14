package servlets;

import java.io.IOException;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.Config;

/**
 * Rota publica e leve para health check da plataforma (sem login, sem sessao, sem acesso ao banco).
 * O cabecalho X-App-Commit traz o commit em execucao, para o CI conferir se o deploy chegou ao ar.
 */
@WebServlet("/health")
public class HealthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static final String CABECALHO_COMMIT = "X-App-Commit";
    static final String DESCONHECIDO = "desconhecido";
    private static final Pattern SHA = Pattern.compile("[0-9a-f]{7,40}");

    /**
     * Commit em execucao: RENDER_GIT_COMMIT (o Render define no deploy) ou APP_COMMIT (gravado na imagem a
     * partir do build arg RENDER_GIT_COMMIT; veja o Dockerfile).
     */
    static String commitAtual() {
        String valor = Config.get("RENDER_GIT_COMMIT");
        return normalizar(valor != null ? valor : Config.get("APP_COMMIT"));
    }

    /** Aceita apenas um SHA hexadecimal (nada de texto arbitrario em cabecalho); senao "desconhecido". */
    static String normalizar(String valor) {
        if (valor == null) {
            return DESCONHECIDO;
        }
        String sha = valor.trim().toLowerCase(Locale.ROOT);
        return SHA.matcher(sha).matches() ? sha : DESCONHECIDO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader(CABECALHO_COMMIT, commitAtual());
        response.getWriter().write("ok");
    }
}
