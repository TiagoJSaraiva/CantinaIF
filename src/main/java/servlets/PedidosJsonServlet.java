package servlets;

import dao.PedidoDAO;
import model.PedidoDto;
import model.StatusPedido;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Pedidos para o painel do vendedor; filtro opcional ?status=PENDENTE. */
@WebServlet("/api/pedidos")
public class PedidosJsonServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        StatusPedido filtro = null;
        String param = request.getParameter("status");
        if (param != null && !param.isBlank()) {
            try {
                filtro = StatusPedido.valueOf(param.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Status inválido.");
                return;
            }
        }
        try {
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK,
                    pedidoDAO.listarPedidos(filtro).stream().map(p -> new PedidoDto(p, true)).toList());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar os pedidos.");
        }
    }
}
