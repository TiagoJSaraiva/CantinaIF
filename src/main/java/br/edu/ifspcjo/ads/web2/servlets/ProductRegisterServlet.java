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
            response.sendRedirect("erro.jsp"); // TEMOS QUE CRIAR UMA PÁGINA DE ERRO
            return;
        }

        try {
            float preco = Float.parseFloat(precoStr);
            int estoque = Integer.parseInt(estoqueStr);
            Produto.Categoria categoria = Produto.Categoria.valueOf(categoriaStr.toUpperCase());

            Produto produto = new Produto(nome, descricao, preco, estoque);
            produto.setCategoria(categoria);

            // Salvar no banco de dados
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);

            String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria().name());
            stmt.executeUpdate();

            stmt.close();
            conn.close();

            response.sendRedirect("registroProdutos.jsp"); // INTERESSANTE CRIAR UMA PÁGINA DE SUCESSO COM 1 BOTÃO QUE LEVA DE VOLTA A PÁGINA DE ADM

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("erro.jsp"); // NÃO TEMOS PÁGINA DE ERRO AINDA
        }
    }
}
