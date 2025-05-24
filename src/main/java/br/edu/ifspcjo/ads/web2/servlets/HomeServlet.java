package servlets;

import dao.ProductDao;
import model.Produto;
import utils.ConnectionFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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
