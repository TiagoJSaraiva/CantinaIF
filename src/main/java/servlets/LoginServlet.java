package servlets;

import dao.UserDao;
import dao.VendedorDao;
import model.Comprador;
import model.Vendedor;

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

        try {
            Vendedor vendedor = vendedorDao.autenticar(prontuario, senha);
            if (vendedor != null) {
                HttpSession session = novaSessao(request);
                session.setAttribute("vendedor", vendedor);
                response.sendRedirect("registroProdutos.jsp");
                return;
            }

            Comprador comprador = userDao.autenticar(prontuario, senha);
            if (comprador != null) {
                HttpSession session = novaSessao(request);
                session.setAttribute("comprador", comprador);
                response.sendRedirect("home.jsp");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            falhar(request, response, "Não foi possível entrar agora. Tente novamente em instantes.");
            return;
        }
        falhar(request, response, "Usuário ou senha inválidos.");
    }

    /** Reaproveita a sessao (preservando o token CSRF) trocando o id para evitar fixacao de sessao. */
    private HttpSession novaSessao(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return request.getSession(true);
        }
        request.changeSessionId();
        session.removeAttribute("comprador");
        session.removeAttribute("vendedor");
        session.removeAttribute("carrinho");
        return session;
    }

    private void falhar(HttpServletRequest request, HttpServletResponse response, String mensagem)
            throws ServletException, IOException {
        request.setAttribute("error", mensagem);
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}
