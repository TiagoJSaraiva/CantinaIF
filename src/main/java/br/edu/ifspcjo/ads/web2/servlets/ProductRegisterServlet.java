package servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Produto;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

@WebServlet("/registrar-produto")
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

            Produto produto = new Produto(nome, descricao, preco, estoque);
            produto.setCategoria(categoria);

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO Produto (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria().name());
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
