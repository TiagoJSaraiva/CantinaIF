package servlets; // Ajuste o nome do pacote conforme a sua estrutura de projeto

import dao.PedidoDAO; // Ajuste o nome do pacote conforme a sua estrutura de projeto
import com.google.gson.Gson; // Certifique-se de que a biblioteca Gson está adicionada ao seu projeto!

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

@WebServlet("/api/deletarPedido") // Endpoint para deletar/concluir pedidos
public class DeletarPedidoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        String pedidoIdStr = request.getParameter("id"); // Pega o 'id' do pedido da requisição
        int pedidoId;
        try {
            pedidoId = Integer.parseInt(pedidoIdStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // HTTP 400 Bad Request
            out.print(gson.toJson(new ApiResponse("Erro: ID do pedido inválido.", false)));
            return;
        }

        PedidoDAO pedidoDAO = new PedidoDAO();
        try {
            pedidoDAO.deletarPedido(pedidoId); // Chama o método DAO para deletar o pedido
            response.setStatus(HttpServletResponse.SC_OK); // HTTP 200 OK
            out.print(gson.toJson(new ApiResponse("Pedido " + pedidoId + " concluído com sucesso!", true)));
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // HTTP 500 Internal Server Error
            out.print(gson.toJson(new ApiResponse("Erro ao concluir pedido: " + e.getMessage(), false)));
            System.err.println("Erro SQL ao deletar pedido: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Classe interna auxiliar para padronizar as respostas JSON (sucesso/erro)
    private static class ApiResponse {
        String message;
        boolean success;

        public ApiResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }
}