package servlets;

import java.sql.SQLException;

import dao.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import model.Comprador;
import model.EstadoComprador;
import model.Vendedor;

/** Acesso ao usuario logado. As rotas ja passaram pelo AuthFilter, entao o perfil esperado esta na sessao. */
final class Sessao {

    /** Atributo de requisicao com o {@link EstadoComprador} lido pelo AuthFilter. */
    static final String ESTADO = "estadoComprador";

    private Sessao() {
    }

    static Vendedor vendedor(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s == null ? null : (Vendedor) s.getAttribute("vendedor");
    }

    /** Prontuario do vendedor logado (dono dos produtos e pedidos que ele pode ver e alterar). */
    static String vendedorProntuario(HttpServletRequest request) {
        Vendedor v = vendedor(request);
        return v == null ? null : v.getProntuario();
    }

    static Comprador comprador(HttpServletRequest request) {
        HttpSession s = request.getSession(false);
        return s == null ? null : (Comprador) s.getAttribute("comprador");
    }

    /** Estado atual do comprador logado (do banco, nao da sessao). */
    static EstadoComprador estado(HttpServletRequest request) throws SQLException {
        Object e = request.getAttribute(ESTADO);
        if (e instanceof EstadoComprador estado) {
            return estado;
        }
        Comprador c = comprador(request);
        return c == null ? null : new UserDao().estado(c.getProntuario());
    }
}
