package servlets;

import dao.PedidoDAO;
import dao.RegraNegocioException;
import dao.TransicaoInvalidaException;
import model.StatusPedido;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Acao explicita do vendedor: muda o status de um pedido dele (id, status). Para cancelar, o motivo e obrigatorio
 * (parametro motivo) e aparece para o comprador. Nada e apagado.
 */
@WebServlet("/api/pedidos/status")
public class PedidoStatusServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id;
        StatusPedido novo;
        try {
            id = Integer.parseInt(request.getParameter("id"));
            novo = StatusPedido.valueOf(request.getParameter("status").trim().toUpperCase());
        } catch (RuntimeException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Pedido ou status inválido.");
            return;
        }
        try {
            pedidoDAO.atualizarStatus(id, Sessao.vendedorProntuario(request), novo, request.getParameter("motivo"));
            Json.result(response, HttpServletResponse.SC_OK, true, "Pedido #" + id + ": " + novo.getRotulo() + ".");
        } catch (PedidoDAO.MotivoInvalidoException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage());
        } catch (TransicaoInvalidaException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o pedido.");
        }
    }
}
