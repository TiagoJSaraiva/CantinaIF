// AVISO: Ainda não testado por prolemas com a página home.

package servlets;

import dao.ProductDao;
import model.Produto;
import model.Produto.Categoria;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/registerProduct")
public class ProductRegisterServlet extends HttpServlet 
{
	private static final long serialVersionUID = 1L;
	
	private Connection connection;

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String nome = request.getParameter("nome");
        String descricao = request.getParameter("descricao");
        float preco = Float.parseFloat(request.getParameter("preco"));
        int estoque = Integer.parseInt(request.getParameter("estoque"));
        Categoria categoria = Categoria.valueOf(request.getParameter("categoria").toUpperCase());

        Produto produto = new Produto(nome, descricao, preco, estoque);
        produto.setCategoria(categoria);

        ProductDao dao = new ProductDao(connection);
        
        try {
            dao.create(produto);
            response.sendRedirect("registroProdutos.jsp");
        } catch (SQLException e) {
            throw new ServletException("Erro ao cadastrar produto", e);
        }
    }
}
