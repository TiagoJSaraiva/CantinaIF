package servlets;

import dao.ProductDao;
import dao.RegraNegocioException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import utils.ImageType;
import utils.Limites;

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
        Map<String, String> excedeu = ProdutoForm.verificarLimiteDoCorpo(request);
        if (excedeu != null) {
            Json.erros(response, excedeu);
            return;
        }
        try {
            Limites.verificarNovoProduto(0); // teto de produtos antes de qualquer processamento
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
            return;
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao verificar os limites.");
            return;
        }
        ProdutoForm form = ProdutoForm.ler(request);
        if (form.limite != null) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, form.limite);
            return;
        }
        if (form.ocupado != null) {
            response.setHeader("Retry-After", "5");
            Json.result(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, false, form.ocupado);
            return;
        }
        if (!form.erros.isEmpty()) {
            Json.erros(response, form.erros);
            return;
        }
        try {
            Limites.verificarNovoProduto(form.produto.getImagem() == null ? 0 : form.produto.getImagem().length);
            productDao.create(form.produto);
            Json.result(response, HttpServletResponse.SC_OK, true, "Produto cadastrado com sucesso.");
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Ocorreu um erro inesperado ao registrar o produto. Tente novamente.");
        }
    }
}
