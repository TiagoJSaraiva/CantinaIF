package servlets;

import dao.ProductDao;
import model.Produto;
import model.Comprador;
import utils.ConnectionFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("comprador") != null) {
            Comprador comprador = (Comprador) session.getAttribute("comprador");
            request.setAttribute("mensagemBoasVindas", "Olá, " + comprador.getNome() + ", bem-vindo ao seu painel!");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
        } else {
            response.sendRedirect("login.jsp");
        }
        
        try (Connection connection = ConnectionFactory.getConnection()) {
            ProductDao productDao = new ProductDao(connection);
            List<Produto> produtos = productDao.listarProdutos();

            // Envia a lista para a página JSP
            request.setAttribute("produtos", produtos);
            request.getRequestDispatcher("/home.jsp").forward(request, response);

        } catch (Exception e) {
            throw new ServletException("Erro ao carregar produtos", e);
        }
    }
}
