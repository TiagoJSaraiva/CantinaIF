package servlets;

import java.io.IOException;
import java.sql.SQLException;

import dao.ProductDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ImageType;

/** Edicao dos dados de um produto (vendedor); a imagem so e trocada se um novo arquivo for enviado. */
@WebServlet("/editar-produto")
@MultipartConfig(
    fileSizeThreshold = 512 * 1024,
    maxFileSize = ImageType.MAX_BYTES,
    maxRequestSize = ImageType.MAX_BYTES + 64 * 1024
)
public class EditarProdutos extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Produto inválido.");
            return;
        }
        ProdutoForm form = ProdutoForm.ler(request);
        if (!form.erros.isEmpty()) {
            Json.erros(response, form.erros);
            return;
        }
        form.produto.setId(id);
        try {
            if (productDao.atualizar(form.produto)) {
                Json.result(response, HttpServletResponse.SC_OK, true, "Produto atualizado.");
            } else {
                Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o produto.");
        }
    }
}
