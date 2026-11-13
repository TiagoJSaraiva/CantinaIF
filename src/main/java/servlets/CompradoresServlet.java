package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dao.AdvertenciaDao;
import dao.LimiteAtingidoException;
import dao.RegraNegocioException;
import dao.UserDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.CompradorResumo;

/**
 * Gestao dos compradores vinculados ao vendedor logado (somente os dele):
 * <ul>
 *   <li>GET  /api/compradores - tabela com os numeros de cada comprador;</li>
 *   <li>POST /api/compradores/situacao {prontuario, ativo} - desativa (sem acesso a nada) ou reativa;</li>
 *   <li>POST /api/compradores/advertencia {prontuario, mensagem} - envia uma advertencia, que o comprador ve.</li>
 * </ul>
 */
@WebServlet({ "/api/compradores", "/api/compradores/situacao", "/api/compradores/advertencia" })
public class CompradoresServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final UserDao userDao = new UserDao();
    private final AdvertenciaDao advertenciaDao = new AdvertenciaDao();

    /** Linha da tabela enviada ao navegador. */
    static final class Linha {
        final String prontuario;
        final String nome;
        final String email;
        final boolean ativo;
        final int pedidos;
        final int canceladosPeloComprador;
        final int canceladosPeloVendedor;
        final String ultimoPedido;
        final int advertencias;

        Linha(CompradorResumo r) {
            this.prontuario = r.prontuario();
            this.nome = r.nome();
            this.email = r.email();
            this.ativo = r.ativo();
            this.pedidos = r.pedidos();
            this.canceladosPeloComprador = r.canceladosPeloComprador();
            this.canceladosPeloVendedor = r.canceladosPeloVendedor();
            this.ultimoPedido = Json.iso(r.ultimoPedido());
            this.advertencias = r.advertencias();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/api/compradores".equals(request.getServletPath())) {
            response.setHeader("Allow", "POST");
            Json.result(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false, "Use POST.");
            return;
        }
        try {
            List<Linha> linhas = userDao.listarDoVendedor(Sessao.vendedorProntuario(request)).stream().map(Linha::new).toList();
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK, linhas);
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao carregar os compradores.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String vendedor = Sessao.vendedorProntuario(request);
        String prontuario = request.getParameter("prontuario");
        if (prontuario == null || prontuario.isBlank()) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Comprador inválido.");
            return;
        }
        try {
            switch (request.getServletPath()) {
                case "/api/compradores/situacao" -> situacao(request, response, vendedor, prontuario.trim());
                case "/api/compradores/advertencia" -> advertencia(request, response, vendedor, prontuario.trim());
                default -> Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Ação desconhecida.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Erro ao atualizar o comprador.");
        }
    }

    private void situacao(HttpServletRequest request, HttpServletResponse response, String vendedor, String prontuario)
            throws IOException, SQLException {
        String ativo = request.getParameter("ativo");
        if (!"true".equals(ativo) && !"false".equals(ativo)) {
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, "Situação inválida.");
            return;
        }
        boolean ativar = Boolean.parseBoolean(ativo);
        if (!userDao.definirAtivo(prontuario, vendedor, ativar)) {
            Json.result(response, HttpServletResponse.SC_NOT_FOUND, false, "Comprador não encontrado entre os seus compradores.");
            return;
        }
        Json.result(response, HttpServletResponse.SC_OK, true,
                ativar ? "Comprador " + prontuario + " reativado." : "Comprador " + prontuario + " desativado: ele não tem mais acesso.");
    }

    private void advertencia(HttpServletRequest request, HttpServletResponse response, String vendedor, String prontuario)
            throws IOException, SQLException {
        try {
            advertenciaDao.criar(prontuario, vendedor, request.getParameter("mensagem"), LocalDateTime.now());
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("success", true);
            corpo.put("message", "Advertência enviada para " + prontuario + ".");
            Json.write(response, HttpServletResponse.SC_OK, corpo);
        } catch (LimiteAtingidoException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (RegraNegocioException e) {
            int status = e.getMessage().contains("não encontrado") ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST;
            Json.result(response, status, false, e.getMessage());
        }
    }
}
