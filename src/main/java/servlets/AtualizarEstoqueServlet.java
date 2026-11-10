package servlets;

import java.io.IOException;

import dao.ProductDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Ajuste rapido do estoque de um produto (vendedor). */
@WebServlet("/atualizarEstoque")
public class AtualizarEstoqueServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            int estoque = Integer.parseInt(request.getParameter("estoque"));
            if (estoque < 0) {
                Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "O estoque não pode ser negativo.");
                return;
            }
            if (productDao.atualizarEstoque(id, estoque)) {
                Json.result(response, HttpServletResponse.SC_OK, true, "Estoque atualizado.");
            } else {
                Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
            }
        } catch (NumberFormatException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Parâmetros inválidos.");
        } catch (Exception e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o estoque.");
        }
    }
}
