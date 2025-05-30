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

@WebServlet("/api/produtos")
public class APIProdutosServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<Produto> produtos = productDao.listarProdutosDisponiveis();

        Gson gson = new Gson();
        String json = gson.toJson(produtos);

        PrintWriter out = response.getWriter();
        out.print(json);
        out.flush();
    }
}
