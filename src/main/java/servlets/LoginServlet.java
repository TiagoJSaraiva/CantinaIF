package servlets;

import dao.UserDao;
import model.Comprador;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet 
{
    private static final long serialVersionUID = 1L;
	
    private final UserDao userDao = new UserDao();

    private static final String prontuarioVendedor = "REMOVIDO";
    private static final String senhaVendedor = "REMOVIDO";
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String prontuario = request.getParameter("prontuario");
        String senha = request.getParameter("senha");
        
        
        if (prontuarioVendedor.equals(prontuario) && senhaVendedor.equals(senha)) {
            response.sendRedirect("registroProdutos.jsp");
            return;
        }
        
        Comprador comprador = userDao.encontrarPorProntuarioESenha(prontuario, senha);

        if (comprador != null) {
            HttpSession session = request.getSession();
            session.setAttribute("comprador", comprador);
            response.sendRedirect("home.jsp");
        } else {
            request.setAttribute("error", "Usuário ou senha inválidos.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
}
