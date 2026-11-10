package servlets;

import dao.ProductDao;
import model.Produto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;

@WebServlet("/registrar-produto")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,     // 10MB
    maxRequestSize = 1024 * 1024 * 50   // 50MB
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
        Part filePart = request.getPart("imagem");

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
                try (InputStream fileContent = filePart.getInputStream()) {
                    imagemBytes = fileContent.readAllBytes();
                }
            }

            Produto produto = new Produto(nome, descricao, preco, estoque, categoria, imagemBytes);
            productDao.create(produto);

            response.sendRedirect("registroProdutos.jsp");

        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Valores de preço ou estoque inválidos. Use apenas números.");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("errorMessage", "Ocorreu um erro inesperado ao registrar o produto. Tente novamente.");
            request.getRequestDispatcher("registroProdutos.jsp").forward(request, response);
        }
    }
}
