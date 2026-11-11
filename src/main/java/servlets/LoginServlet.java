package servlets;

import dao.UserDao;
import dao.VendedorDao;
import model.Comprador;
import model.Vendedor;
import utils.Database;
import utils.LoginGuard;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Login de vendedor e de comprador. O vendedor e consultado primeiro (tabela Vendedor);
 * se nao for vendedor, tenta como comprador. A sessao nunca guarda a senha.
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final int SESSAO_AUTENTICADA_SEGUNDOS = 30 * 60;

    private final UserDao userDao = new UserDao();
    private final VendedorDao vendedorDao = new VendedorDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String prontuario = request.getParameter("prontuario");
        String senha = request.getParameter("senha");

        if (prontuario == null || senha == null || prontuario.isBlank() || senha.isEmpty()) {
            falhar(request, response, "Informe prontuário e senha.");
            return;
        }
        prontuario = prontuario.trim();

        String ip = request.getRemoteAddr();
        LoginGuard guarda = LoginGuard.PADRAO;
        if (guarda.bloqueado(ip, prontuario)) {
            long segundos = guarda.segundosParaLiberar(ip, prontuario);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(segundos));
            falhar(request, response, "Muitas tentativas de login. Tente novamente em "
                    + Math.max(1, (segundos + 59) / 60) + " minuto(s).");
            return;
        }

        try {
            Vendedor vendedor = vendedorDao.autenticar(prontuario, senha);
            if (vendedor != null) {
                guarda.sucesso(prontuario);
                HttpSession session = novaSessao(request);
                session.setAttribute("vendedor", vendedor);
                session.setAttribute("geracao", Database.geracao());
                response.sendRedirect("registroProdutos.jsp");
                return;
            }

            Comprador comprador = userDao.autenticar(prontuario, senha);
            if (comprador != null) {
                guarda.sucesso(prontuario);
                HttpSession session = novaSessao(request);
                session.setAttribute("comprador", comprador);
                session.setAttribute("geracao", Database.geracao());
                response.sendRedirect("home.jsp");
                return;
            }
        } catch (IllegalStateException e) { // limite de sessoes ativas atingido (maxActiveSessions)
            servidorOcupado(request, response);
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            falhar(request, response, "Não foi possível entrar agora. Tente novamente em instantes.");
            return;
        }
        guarda.falha(ip, prontuario);
        falhar(request, response, "Usuário ou senha inválidos.");
    }

    private void servidorOcupado(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setHeader("Retry-After", "30");
        falhar(request, response, "O servidor está com muitas sessões abertas no momento. Tente novamente em instantes.");
    }

    /** Reaproveita a sessao (preservando o token CSRF) trocando o id para evitar fixacao de sessao. */
    private HttpSession novaSessao(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
            session.setMaxInactiveInterval(SESSAO_AUTENTICADA_SEGUNDOS);
            return session;
        }
        session.setMaxInactiveInterval(SESSAO_AUTENTICADA_SEGUNDOS); // a sessao anonima era curta (5 min)
        request.changeSessionId();
        session.removeAttribute("comprador");
        session.removeAttribute("vendedor");
        session.removeAttribute("carrinho");
        session.removeAttribute(CsrfFilter.ATTR);
        CsrfFilter.token(session); // novo token CSRF para a sessao autenticada
        return session;
    }

    private void falhar(HttpServletRequest request, HttpServletResponse response, String mensagem)
            throws ServletException, IOException {
        request.setAttribute("error", mensagem);
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}
