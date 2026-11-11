package servlets;

import dao.ProductDao;
import model.Produto;
import utils.ImageType;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;

@WebServlet("/registrar-produto")
@MultipartConfig(
    fileSizeThreshold = 512 * 1024,
    maxFileSize = ImageType.MAX_BYTES,        // 2MB por imagem
    maxRequestSize = ImageType.MAX_BYTES + 64 * 1024
)
public class ProductRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String nome = request.getParameter("nome");
        String descricao = request.getParameter("descricao");
        String precoStr = request.getParameter("preco");
        String estoqueStr = request.getParameter("estoque");
        String categoriaStr = request.getParameter("categoria");
        Part filePart;
        try {
            filePart = request.getPart("imagem");
        } catch (IllegalStateException e) {
            erro(request, response, "A imagem deve ter no máximo 2 MB.");
            return;
        }

        if (nome == null || precoStr == null || categoriaStr == null || nome.isEmpty() || precoStr.isEmpty() || categoriaStr.isEmpty()) {
            request.setAttribute("errorMessage", "Por favor, preencha todos os campos obrigatórios (nome, preço e categoria).");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
            return;
        }

        try {
            float preco = Float.parseFloat(precoStr);
            int estoque = 0;
            if (estoqueStr != null && !estoqueStr.isEmpty()) {
                estoque = Integer.parseInt(estoqueStr);
            }

            if (preco < 0 || estoque < 0 || Float.isNaN(preco) || Float.isInfinite(preco)) {
                erro(request, response, "Preço e estoque não podem ser negativos.");
                return;
            }

            Produto.Categoria categoria = null;
            try {
                categoria = Produto.Categoria.valueOf(categoriaStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                request.setAttribute("errorMessage", "Categoria inválida. Por favor, escolha uma categoria existente.");
                request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
                return;
            }

            byte[] imagemBytes = null;
            if (filePart != null && filePart.getSize() > 0) {
                if (filePart.getSize() > ImageType.MAX_BYTES) {
                    erro(request, response, "A imagem deve ter no máximo 2 MB.");
                    return;
                }
                try (InputStream fileContent = filePart.getInputStream()) {
                    imagemBytes = fileContent.readAllBytes();
                }
                if (ImageType.detect(imagemBytes) == null) {
                    erro(request, response, "Envie uma imagem JPEG, PNG, GIF ou WebP válida.");
                    return;
                }
            }

            Produto produto = new Produto(nome, descricao, preco, estoque, categoria, imagemBytes);
            productDao.create(produto);

            response.sendRedirect("registroProdutos.jsp?produto=ok");

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Valores de preço ou estoque inválidos. Use apenas números.");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
        } catch (Exception e) {
            System.err.println("Erro ao registrar produto: " + e);
            request.setAttribute("errorMessage", "Ocorreu um erro inesperado ao registrar o produto. Tente novamente.");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
        }
    }

    private void erro(HttpServletRequest request, HttpServletResponse response, String mensagem)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", mensagem);
        request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
    }
}
