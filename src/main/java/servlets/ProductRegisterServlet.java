package servlets;

import model.Produto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@WebServlet("/registrar-produto")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024 * 2, // 2MB
    maxFileSize = 1024 * 1024 * 10,     // 10MB
    maxRequestSize = 1024 * 1024 * 50   // 50MB
)
public class ProductRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String URL = "jdbc:mysql://localhost:3306/todoapp";
    private static final String USER = "root";
    private static final String PASSWORD = "";

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

            Produto produto = new Produto(nome, descricao, preco, estoque, imagemBytes);
            produto.setCategoria(categoria);

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO Produto (nome, descricao, preco, estoque, categoria, imagem) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria().name());
            
            if (produto.getImagem() != null) {
                stmt.setBytes(6, produto.getImagem());
            } else {
                stmt.setNull(6, java.sql.Types.BLOB);
            }
            
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            request.setAttribute("successMessage", "Produto registrado com sucesso!");
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
