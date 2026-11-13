package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import dao.VinculoDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * O vendedor gera um codigo de vinculo: vale 60 s e uma unica vez. Gerar outro invalida o anterior ainda nao usado.
 * Responde {success, codigo, segundos} (o navegador conta o tempo a partir da resposta, sem depender do relogio dele).
 */
@WebServlet("/api/vinculo/codigo")
public class GerarCodigoVinculoServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final VinculoDao vinculoDao = new VinculoDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            VinculoDao.Codigo codigo = vinculoDao.gerarCodigo(Sessao.vendedorProntuario(request), LocalDateTime.now());
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("success", true);
            corpo.put("codigo", codigo.codigo());
            corpo.put("segundos", VinculoDao.VALIDADE.toSeconds());
            response.setHeader("Cache-Control", "no-store");
            Json.write(response, HttpServletResponse.SC_OK, corpo);
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Não foi possível gerar o código.");
        }
    }
}
