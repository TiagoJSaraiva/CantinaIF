package servlets;

import dao.ProductDao;
import model.Produto;

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
                response.setContentType("image/jpeg"); // Ou "image/png", "image/gif", etc. dependendo do tipo da imagem
                response.setContentLength(produto.getImagem().length);

                try (OutputStream out = response.getOutputStream()) {
                    out.write(produto.getImagem());
                    out.flush();
                }
            } else {
                // Se o produto ou a imagem não for encontrada, retorne uma imagem padrão ou 404
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Imagem não encontrada para o produto ID: " + productId);
            }

        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "ID do produto inválido.");
        } catch (Exception e) {
            System.err.println("Erro ao servir imagem: " + e.getMessage());
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro interno ao carregar a imagem.");
        }
    }
}
