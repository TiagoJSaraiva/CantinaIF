package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import dao.ProductDao;
import dao.RegraNegocioException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.ImageType;
import utils.Limites;

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
        Map<String, String> excedeu = ProdutoForm.verificarLimiteDoCorpo(request);
        if (excedeu != null) {
            Json.erros(response, excedeu);
            return;
        }
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
            if (form.produto.getImagem() != null) {
                Limites.verificarImagem(form.produto.getImagem().length);
            }
            String anterior = request.getParameter("estoqueAnterior");
            ProductDao.ResultadoEstoque resultado;
            if (anterior != null && !anterior.isBlank()) {
                // o vendedor viu este estoque ao abrir o formulario: so grava se ele nao mudou (compara e troca)
                resultado = productDao.atualizar(form.produto, Integer.parseInt(anterior.trim()));
            } else {
                resultado = productDao.atualizar(form.produto) ? ProductDao.ResultadoEstoque.OK : ProductDao.ResultadoEstoque.NAO_ENCONTRADO;
            }
            switch (resultado) {
                case OK:
                    Json.result(response, HttpServletResponse.SC_OK, true, "Produto atualizado.");
                    break;
                case CONFLITO:
                    Json.result(response, HttpServletResponse.SC_CONFLICT, false,
                            "O estoque mudou (agora " + productDao.estoqueAtual(id) + ") desde que você abriu o formulário. "
                            + "Nada foi alterado; reabra o produto e confira.");
                    break;
                default:
                    Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
            }
        } catch (NumberFormatException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Estoque anterior inválido.");
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o produto.");
        }
    }
}
