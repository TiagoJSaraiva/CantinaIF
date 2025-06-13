package servlets;

import com.google.gson.Gson;
import dao.ProductDao;
import model.Produto;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/apiUser/produtos")
public class APIUserProdutos extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String categoria = request.getParameter("categoria");

        List<Produto> produtos;
        if (categoria == null || categoria.equalsIgnoreCase("todos")) {
            produtos = productDao.listarProdutosDisponiveisUser();
        } else {
            produtos = productDao.listarProdutosPorCategoriaUser(categoria);
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(produtos));
        out.flush();
    }

}
