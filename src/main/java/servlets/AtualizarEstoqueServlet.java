package servlets;

import java.io.IOException;

import dao.ProductDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ajuste rapido do estoque de um produto (vendedor). Recebe o estoque que o vendedor viu ("esperado") e o novo
 * valor; a gravacao e atomica (compara e troca no SQL) e responde 409 se o estoque mudou nesse meio tempo.
 */
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
            int esperado = Integer.parseInt(request.getParameter("esperado"));
            if (estoque < 0 || estoque > 1_000_000) {
                Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "O estoque deve estar entre 0 e 1000000.");
                return;
            }
            switch (productDao.atualizarEstoque(id, esperado, estoque)) {
                case OK:
                    Json.result(response, HttpServletResponse.SC_OK, true, "Estoque atualizado.");
                    break;
                case CONFLITO:
                    Json.result(response, HttpServletResponse.SC_CONFLICT, false,
                            "O estoque mudou (agora " + productDao.estoqueAtual(id)
                            + ") por outra operação, por exemplo um pedido. Nada foi alterado; confira e tente de novo.");
                    break;
                default:
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
