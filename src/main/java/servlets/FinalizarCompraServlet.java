package servlets;

import dao.PedidoDAO;
import dao.RegraNegocioException;
import model.Carrinho;
import model.Comprador;
import model.Pedido;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Finaliza a compra: cria o pedido e baixa o estoque na mesma transacao. Se faltar estoque,
 * nada e gravado e o comprador volta ao carrinho com a mensagem.
 */
@WebServlet("/FinalizarCompraServlet")
public class FinalizarCompraServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        Carrinho carrinho = (Carrinho) session.getAttribute("carrinho");
        Comprador comprador = (Comprador) session.getAttribute("comprador");

        if (carrinho == null || carrinho.isVazio()) {
            voltar(request, response, "Seu carrinho está vazio. Adicione produtos para finalizar a compra.");
            return;
        }

        try {
            Pedido pedido = pedidoDAO.criarPedido(comprador.getProntuario(), carrinho.getItens());
            carrinho.limparCarrinho();
            response.sendRedirect("meus-pedidos.jsp?novo=" + pedido.getId());
        } catch (RegraNegocioException e) {
            voltar(request, response, e.getMessage() + " Ajuste seu carrinho e tente novamente.");
        } catch (SQLException e) {
            e.printStackTrace();
            voltar(request, response, "Erro ao finalizar sua compra. Tente novamente mais tarde.");
        }
    }

    private void voltar(HttpServletRequest request, HttpServletResponse response, String mensagem)
            throws ServletException, IOException {
        request.setAttribute("errorMessage", mensagem);
        request.getRequestDispatcher("Carrinho.jsp").forward(request, response);
    }
}
