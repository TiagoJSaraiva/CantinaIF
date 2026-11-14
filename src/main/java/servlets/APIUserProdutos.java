package servlets;

import dao.ProductDao;
import model.EstadoComprador;
import model.ProdutoDto;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogo do comprador: os produtos do vendedor ao qual ele esta vinculado (os esgotados aparecem, mas nao podem ser
 * comprados). Sem vinculo responde 409 {semVendedor: true} e a pagina mostra como se vincular.
 */
@WebServlet("/apiUser/produtos")
public class APIUserProdutos extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final ProductDao productDao = new ProductDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            EstadoComprador estado = Sessao.estado(request);
            if (estado == null || !estado.vinculado()) {
                Map<String, Object> corpo = new LinkedHashMap<>();
                corpo.put("success", false);
                corpo.put("semVendedor", true);
                corpo.put("message", "Vincule sua conta a um vendedor para ver os produtos.");
                Json.write(response, HttpServletResponse.SC_CONFLICT, corpo);
                return;
            }
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK,
                    productDao.listarProdutos(estado.vendedorProntuario(), request.getParameter("categoria"))
                            .stream().map(ProdutoDto::new).toList());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar os produtos.");
        }
    }
}
