package servlet;

import dao.ProductDao;
import model.Produto;
import util.DataSourceManager.java;

//importar as acoisas do jakarta

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
