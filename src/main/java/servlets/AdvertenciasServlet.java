package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.AdvertenciaDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Advertencia;

/**
 * Advertencias recebidas pelo comprador logado. GET /apiUser/advertencias lista (tambem liberado para conta
 * desativada, que ve o motivo); POST /apiUser/advertencias/lidas marca todas como lidas.
 */
@WebServlet({ "/apiUser/advertencias", "/apiUser/advertencias/lidas" })
public class AdvertenciasServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final AdvertenciaDao advertenciaDao = new AdvertenciaDao();

    /** Item enviado ao navegador. */
    static final class Item {
        final int id;
        final String vendedorNome;
        final String mensagem;
        final String criadaEm;
        final boolean lida;

        Item(Advertencia a) {
            this.id = a.id();
            this.vendedorNome = a.vendedorNome();
            this.mensagem = a.mensagem();
            this.criadaEm = Json.iso(a.criadaEm());
            this.lida = a.lida();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/apiUser/advertencias".equals(request.getServletPath())) {
            response.setHeader("Allow", "POST");
            Json.result(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false, "Use POST.");
            return;
        }
        try {
            List<Item> itens = advertenciaDao.listarDoComprador(Sessao.comprador(request).getProntuario())
                    .stream().map(Item::new).toList();
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("naoLidas", itens.stream().filter(i -> !i.lida).count());
            corpo.put("advertencias", itens);
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK, corpo);
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar as advertências.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/apiUser/advertencias/lidas".equals(request.getServletPath())) {
            Json.result(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false, "Use GET.");
            return;
        }
        try {
            int n = advertenciaDao.marcarLidas(Sessao.comprador(request).getProntuario(), LocalDateTime.now());
            Json.result(response, HttpServletResponse.SC_OK, true, n == 0 ? "Nada a marcar." : "Advertências marcadas como lidas.");
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar as advertências.");
        }
    }
}
