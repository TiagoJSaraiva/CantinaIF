package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.google.gson.Gson;

import dao.ProductDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Produto;

@WebServlet("/apiAdm/produtos")
public class APIProdutosADM extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String categoria = request.getParameter("categoria");

        List<Produto> produtos;
        if (categoria == null || categoria.equalsIgnoreCase("todos")) {
            produtos = productDao.listarProdutosDisponiveisADM();
        } else {
            produtos = productDao.listarProdutosPorCategoriaADM(categoria);
        }

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(new Gson().toJson(produtos));
        out.flush();
    }

}
