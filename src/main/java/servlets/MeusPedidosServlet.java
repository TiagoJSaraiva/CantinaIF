package servlets;

import dao.PedidoDAO;
import model.Comprador;
import model.PedidoDto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/** Historico de pedidos do comprador logado. */
@WebServlet("/apiUser/pedidos")
public class MeusPedidosServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final PedidoDAO pedidoDAO = new PedidoDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Comprador comprador = (Comprador) request.getSession().getAttribute("comprador");
        try {
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK,
                    pedidoDAO.listarPorComprador(comprador.getProntuario()).stream()
                            .map(p -> new PedidoDto(p, false)).toList());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar seus pedidos.");
        }
    }
}
