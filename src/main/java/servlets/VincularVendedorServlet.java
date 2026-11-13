package servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import dao.RegraNegocioException;
import dao.VinculoDao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Carrinho;
import model.Comprador;
import utils.RateLimiter;

/**
 * O comprador digita o codigo fornecido pelo vendedor e passa a ver os produtos dele (trocando de vendedor, se ja
 * tinha um). Ao trocar, o carrinho e esvaziado (os itens eram do vendedor anterior). Tentativas erradas sao limitadas
 * por conta, alem do limite geral de escrita por IP.
 */
@WebServlet("/apiUser/vinculo")
public class VincularVendedorServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** 5 codigos errados por conta a cada 10 minutos. */
    static final RateLimiter ERROS = new RateLimiter(5, 10 * 60_000L, System::currentTimeMillis);

    private final VinculoDao vinculoDao = new VinculoDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Comprador comprador = Sessao.comprador(request);
        String chave = comprador.getProntuario();
        if (ERROS.bloqueado(chave)) {
            long segundos = ERROS.segundosParaLiberar(chave);
            response.setHeader("Retry-After", String.valueOf(segundos));
            Json.result(response, 429, false, "Muitos códigos errados. Tente de novo em "
                    + Math.max(1, (segundos + 59) / 60) + " minuto(s).");
            return;
        }
        try {
            VinculoDao.Vinculo v = vinculoDao.resgatar(request.getParameter("codigo"), chave, LocalDateTime.now());
            ERROS.sucesso(chave);
            if (v.trocou()) {
                HttpSession session = request.getSession(false);
                Carrinho carrinho = session == null ? null : (Carrinho) session.getAttribute("carrinho");
                if (carrinho != null) {
                    carrinho.limparCarrinho();
                }
            }
            Map<String, Object> corpo = new LinkedHashMap<>();
            corpo.put("success", true);
            corpo.put("vendedorNome", v.vendedorNome());
            corpo.put("trocou", v.trocou());
            corpo.put("message", v.trocou()
                    ? "Agora você compra de " + v.vendedorNome() + ". Seu carrinho foi esvaziado; os pedidos antigos continuam em Meus pedidos."
                    : "Conta vinculada a " + v.vendedorNome() + ".");
            Json.write(response, HttpServletResponse.SC_OK, corpo);
        } catch (VinculoDao.CodigoInvalidoException e) {
            ERROS.falha(chave);
            Json.result(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage());
        } catch (RegraNegocioException e) {
            Json.result(response, HttpServletResponse.SC_CONFLICT, false, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            Json.result(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false, "Não foi possível vincular agora.");
        }
    }
}
