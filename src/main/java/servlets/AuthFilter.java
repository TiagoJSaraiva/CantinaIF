package servlets;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import utils.BaseRestauradaException;
import utils.Database;
import utils.GeracaoDaRequisicao;

/**
 * Controle de acesso por perfil. Rotas do vendedor exigem "vendedor" na sessao; rotas do comprador
 * exigem "comprador". Paginas redirecionam ao login; APIs respondem 401 (sem sessao) ou 403 (perfil errado).
 */
public class AuthFilter implements Filter {

    /** Rotas exclusivas do vendedor (prefixo terminado em "/" ou caminho exato). */
    private static final List<String> VENDEDOR = List.of(
            "/registroProdutos.jsp", "/pedidos.jsp", "/apiAdm/", "/api/",
            "/registrar-produto", "/editar-produto", "/atualizarEstoque");

    /** Rotas exclusivas do comprador. */
    private static final List<String> COMPRADOR = List.of(
            "/home.jsp", "/Carrinho.jsp", "/meus-pedidos.jsp", "/carrinho", "/FinalizarCompraServlet", "/apiUser/");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getServletPath();
        String perfil = null;
        if (corresponde(VENDEDOR, path)) {
            perfil = "vendedor";
        } else if (corresponde(COMPRADOR, path)) {
            perfil = "comprador";
        }
        if (perfil == null) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("geracao") != null
                && !session.getAttribute("geracao").equals(Database.geracao())) {
            // a base de demonstracao foi restaurada depois deste login: a conta pode nao existir mais
            restaurada(request, response, session, path);
            return;
        }
        if (session != null && session.getAttribute(perfil) != null) {
            response.setHeader("Cache-Control", "no-store");
            Object geracao = session.getAttribute("geracao");
            GeracaoDaRequisicao.definir(geracao instanceof Long ? (Long) geracao : null);
            try {
                chain.doFilter(req, res);
            } catch (BaseRestauradaException e) {
                restaurada(request, response, session, path);
            } catch (ServletException e) {
                if (e.getRootCause() instanceof BaseRestauradaException || e.getCause() instanceof BaseRestauradaException) {
                    restaurada(request, response, session, path);
                } else {
                    throw e;
                }
            } finally {
                GeracaoDaRequisicao.limpar();
            }
            return;
        }

        boolean autenticadoOutroPerfil = session != null
                && (session.getAttribute("vendedor") != null || session.getAttribute("comprador") != null);
        if (autenticadoOutroPerfil && !"GET".equals(request.getMethod())) {
            // requisicao que altera estado com o perfil errado: 403 (nao redireciona)
            Json.result(response, HttpServletResponse.SC_FORBIDDEN, false, "Acesso negado para este perfil.");
            return;
        }
        boolean pagina = path.endsWith(".jsp") || path.equals("/FinalizarCompraServlet")
                || path.equals("/registrar-produto");
        if (pagina) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
        } else if (autenticadoOutroPerfil) {
            Json.result(response, HttpServletResponse.SC_FORBIDDEN, false, "Acesso negado para este perfil.");
        } else {
            Json.result(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Faça login para continuar.");
        }
    }

    /** Invalida a sessao e manda entrar de novo (pagina) ou responde 401 com restaurada=true (API). */
    private static void restaurada(HttpServletRequest request, HttpServletResponse response, HttpSession session,
            String path) throws IOException {
        try {
            session.invalidate();
        } catch (IllegalStateException ja) {
            // ja invalidada
        }
        if (response.isCommitted()) {
            return;
        }
        if (path.endsWith(".jsp") || path.equals("/FinalizarCompraServlet") || path.equals("/registrar-produto")) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?restaurada=1");
        } else {
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("success", false);
            corpo.put("restaurada", true);
            corpo.put("message", "A demonstração foi restaurada; entre novamente.");
            Json.write(response, HttpServletResponse.SC_UNAUTHORIZED, corpo);
        }
    }

    private static boolean corresponde(List<String> rotas, String path) {
        for (String rota : rotas) {
            if (rota.endsWith("/") ? path.startsWith(rota) : path.equals(rota)) {
                return true;
            }
        }
        return false;
    }
}
