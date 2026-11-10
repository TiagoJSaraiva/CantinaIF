package servlets;

import java.io.IOException;

import dao.ProductDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Produto;

/** Edicao dos dados de um produto (vendedor). A imagem nao e alterada aqui. */
@WebServlet("/editar-produto")
public class EditarProdutos extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String nome = request.getParameter("nome");
            String descricao = request.getParameter("descricao");
            float preco = Float.parseFloat(request.getParameter("preco"));
            int estoque = Integer.parseInt(request.getParameter("estoque"));
            Produto.Categoria categoria = Produto.Categoria.valueOf(request.getParameter("categoria").trim().toUpperCase());

            if (nome == null || nome.isBlank() || preco < 0 || estoque < 0) {
                Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Informe um nome e valores de preço e estoque não negativos.");
                return;
            }

            Produto produto = new Produto(nome.trim(), descricao, preco, estoque, categoria, null);
            produto.setId(id);
            if (productDao.atualizar(produto)) {
                Json.result(response, HttpServletResponse.SC_OK, true, "Produto atualizado.");
            } else {
                Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Produto não encontrado.");
            }
        } catch (NullPointerException | IllegalArgumentException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dados do produto inválidos.");
        } catch (Exception e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o produto.");
        }
    }
}
