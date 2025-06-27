package servlets; // Ajuste o nome do pacote conforme a sua estrutura de projeto

import dao.PedidoDAO; // Ajuste o nome do pacote conforme a sua estrutura de projeto
import model.Pedido;   // Ajuste o nome do pacote conforme a sua estrutura de projeto
import com.google.gson.Gson; // Certifique-se de que a biblioteca Gson está adicionada ao seu projeto!

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/pedidos") // Este é o endpoint que o JavaScript vai chamar
public class PedidosJsonServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Configura o tipo de conteúdo da resposta para JSON e o charset para UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 2. Instancia o DAO para acessar os dados do banco de dados
        PedidoDAO pedidoDAO = new PedidoDAO();
        
        // 3. Busca a lista de todos os pedidos com seus detalhes completos
        List<Pedido> pedidos = pedidoDAO.listarTodosPedidosComDetalhes();

        // 4. Converte a lista de objetos Java (Pedidos) para uma string JSON usando Gson
        Gson gson = new Gson();
        String jsonPedidos = gson.toJson(pedidos);

        // 5. Envia a string JSON como resposta HTTP
        PrintWriter out = response.getWriter();
        out.print(jsonPedidos);
        out.flush(); // Garante que o conteúdo seja enviado imediatamente
    }
}