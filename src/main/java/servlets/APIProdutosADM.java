package servlets;

import dao.ProductDao;
import model.ProdutoDto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Lista de produtos do painel do vendedor. */
@WebServlet("/apiAdm/produtos")
public class APIProdutosADM extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Json.write(response, HttpServletResponse.SC_OK,
                    productDao.listarProdutos(request.getParameter("categoria")).stream().map(ProdutoDto::new).toList());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar os produtos.");
        }
    }
}
