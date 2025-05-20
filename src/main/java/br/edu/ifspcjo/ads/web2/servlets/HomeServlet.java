package servlet;

import dao.ProductDao;
import model.Produto;
import util.DataSourceManager.java;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try (Connection connection = ConnectionFactory.getConnection()) {
            ProductDao productDao = new ProductDao(connection);
            List<Produto> produtos = productDao.listarProdutos();

            request.setAttribute("produtos", produtos);
            request.getRequestDispatcher("/home.jsp").forward(request, response);
        } catch (Exception e) {
            throw new ServletException("Erro ao carregar produtos", e);
        }
    }
}
