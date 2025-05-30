package servlets;

import com.google.gson.Gson;
import dao.ProdutoDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Produto;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@WebServlet("/api/produtos")
public class APIProdutosServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final ProdutoDao produtoDao = new ProdutoDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<Produto> produtos = Collections.emptyList();
        PrintWriter out = response.getWriter();
        String json = "[]";

        try {
            HttpSession session = request.getSession(false); // Não cria nova sessão se não existir
            boolean isAdmin = (session != null && session.getAttribute("isAdmin") != null) ? 
                              (Boolean) session.getAttribute("isAdmin") : false;
            
            String categoria = request.getParameter("categoria");

            if (categoria != null && !categoria.isEmpty() && !"todos".equalsIgnoreCase(categoria)) {
                // Se categoria for especificada, lista por categoria
                produtos = produtoDao.listarProdutosPorCategoria(categoria, isAdmin);
            } else {
                // Se não houver categoria ou for 'todos'
                if (isAdmin) {
                    produtos = produtoDao.listarTodosProdutos();
                } else {
                    produtos = produtoDao.listarProdutosDisponiveis();
                }
            }

            Gson gson = new Gson();
            json = gson.toJson(produtos);
            out.print(json);
            out.flush();

       
        } catch (Exception e) {
            System.err.println("Unexpected error in APIProdutosServlet: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"error\": \"Unexpected server error: " + e.getMessage() + "\"}");
            out.flush();
        }
    }
}