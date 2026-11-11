package servlets;

import dao.ProductDao;
import model.Produto;
import utils.ImageType;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@WebServlet("/image")
public class ImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String productIdParam = request.getParameter("id");
        if (productIdParam == null || productIdParam.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID do produto não fornecido.");
            return;
        }

        try {
            int productId = Integer.parseInt(productIdParam);
            Produto produto = productDao.buscarPorId(productId);

            if (produto != null && produto.getImagem() != null) {
                byte[] imagem = produto.getImagem();
                String tipo = ImageType.detect(imagem);
                if (tipo == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "Imagem não encontrada.");
                    return;
                }
                response.setContentType(tipo);
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Cache-Control", "private, max-age=300");
                response.setContentLength(imagem.length);
                try (OutputStream out = response.getOutputStream()) {
                    out.write(imagem);
                    out.flush();
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Imagem não encontrada.");
            }
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID do produto inválido.");
        } catch (Exception e) {
            System.err.println("Erro ao servir imagem: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro interno ao carregar a imagem.");
        }
    }
}
