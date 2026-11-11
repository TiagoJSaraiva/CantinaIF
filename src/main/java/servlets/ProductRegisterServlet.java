package servlets;

import dao.ProductDao;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import utils.ImageType;

/** Cria um produto (multipart: campos + imagem opcional). Responde JSON. */
@WebServlet("/registrar-produto")
@MultipartConfig(
    fileSizeThreshold = 512 * 1024,
    maxFileSize = ImageType.MAX_BYTES,
    maxRequestSize = ImageType.MAX_BYTES + 64 * 1024
)
public class ProductRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProdutoForm form = ProdutoForm.ler(request);
        if (!form.erros.isEmpty()) {
            Json.erros(response, form.erros);
            return;
        }
        try {
            productDao.create(form.produto);
            Json.result(response, HttpServletResponse.SC_OK, true, "Produto cadastrado com sucesso.");
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Ocorreu um erro inesperado ao registrar o produto. Tente novamente.");
        }
    }
}
