package servlets;

import dao.PedidoDAO;
import dao.RegraNegocioException;
import dao.TransicaoInvalidaException;
import model.Comprador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** O comprador cancela o proprio pedido enquanto ele esta pendente; o estoque volta. */
@WebServlet("/apiUser/pedidos/cancelar")
public class CancelarPedidoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Comprador comprador = (Comprador) request.getSession().getAttribute("comprador");
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Pedido inválido.");
            return;
        }
        try {
            pedidoDAO.cancelarComoComprador(id, comprador.getProntuario());
            Json.result(response, HttpServletResponse.SC_OK, true, "Pedido #" + id + " cancelado.");
        } catch (TransicaoInvalidaException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao cancelar o pedido.");
        }
    }
}
