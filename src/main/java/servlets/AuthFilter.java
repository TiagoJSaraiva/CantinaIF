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
import dao.UserDao;
import model.Comprador;
import model.EstadoComprador;
import utils.BaseRestauradaException;
import utils.Database;
import utils.GeracaoDaRequisicao;

/**
 * Controle de acesso por perfil. Rotas do vendedor exigem "vendedor" na sessao; rotas do comprador
 * exigem "comprador". Paginas redirecionam ao login; APIs respondem 401 (sem sessao) ou 403 (perfil errado).
 */
public class AuthFilter implements Filter {

    /** Le o estado atual de um comprador (situacao e vinculo); injetavel nos testes. */
    @FunctionalInterface
    interface Estados {
        EstadoComprador de(String prontuario) throws java.sql.SQLException;
    }

    private final Estados estados;

    public AuthFilter() {
        this(prontuario -> new UserDao().estado(prontuario));
    }

    AuthFilter(Estados estados) {
        this.estados = estados;
    }

    /** Rotas exclusivas do vendedor (prefixo terminado em "/" ou caminho exato). */
    private static final List<String> VENDEDOR = List.of(
            "/registroProdutos.jsp", "/pedidos.jsp", "/compradores.jsp", "/apiAdm/", "/api/",
            "/registrar-produto", "/editar-produto", "/atualizarEstoque");

    /** Rotas exclusivas do comprador. */
    private static final List<String> COMPRADOR = List.of(
            "/home.jsp", "/Carrinho.jsp", "/meus-pedidos.jsp", "/conta.jsp", "/conta-desativada.jsp", "/carrinho",
            "/FinalizarCompraServlet", "/apiUser/");

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
                if (perfil.equals("comprador")) {
                    // o vendedor pode desativar o comprador (ou ele pode trocar de vendedor) a qualquer momento:
                    // o estado vem do banco a cada requisicao, nao da sessao
                    Comprador comprador = (Comprador) session.getAttribute("comprador");
                    EstadoComprador estado = estados.de(comprador.getProntuario());
                    if (estado == null) { // a conta nao existe mais
                        restaurada(request, response, session, path);
                        return;
                    }
                    request.setAttribute(Sessao.ESTADO, estado);
                    if (!estado.ativo() && !liberadoParaDesativado(request, path)) {
                        desativado(request, response, path);
                        return;
                    }
                    if (estado.ativo() && path.equals(PAGINA_DESATIVADA)) {
                        response.sendRedirect(request.getContextPath() + "/home.jsp");
                        return;
                    }
                }
                chain.doFilter(req, res);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
                Json.result(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, false, "Banco indisponível; tente novamente.");
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

    /** Pagina que o comprador desativado ve (aviso e advertencias). */
    static final String PAGINA_DESATIVADA = "/conta-desativada.jsp";

    /** Conta desativada so acessa o aviso e a lista de advertencias (e pode sair). */
    private static boolean liberadoParaDesativado(HttpServletRequest request, String path) {
        return path.equals(PAGINA_DESATIVADA)
                || (path.equals("/apiUser/advertencias") && "GET".equals(request.getMethod()));
    }

    /** Pagina: vai para o aviso de conta desativada; API: 403 {desativada: true}. */
    private static void desativado(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        if (path.endsWith(".jsp") || path.equals("/FinalizarCompraServlet")) {
            response.sendRedirect(request.getContextPath() + PAGINA_DESATIVADA);
            return;
        }
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("success", false);
        corpo.put("desativada", true);
        corpo.put("message", "Sua conta foi desativada pelo vendedor.");
        Json.write(response, HttpServletResponse.SC_FORBIDDEN, corpo);
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
