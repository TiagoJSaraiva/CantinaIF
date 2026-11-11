package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import utils.Database;

/** Contagens usadas para aplicar os tetos configuraveis (utils.Limites). */
public class LimitesDao {

    public long contarProdutos() throws SQLException {
        return escalar("SELECT COUNT(*) FROM Produto");
    }

    public long contarCompradores() throws SQLException {
        return escalar("SELECT COUNT(*) FROM Comprador");
    }

    /** Pedidos ainda nao finalizados (pendente, em preparo ou pronto). */
    public long contarPedidosAbertos() throws SQLException {
        return escalar("SELECT COUNT(*) FROM Pedido WHERE status IN ('PENDENTE', 'EM_PREPARO', 'PRONTO')");
    }

    /** Pedidos em aberto de um comprador. */
    public long contarPedidosAbertosDe(String prontuario) throws SQLException {
        try (Connection c = Database.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM Pedido WHERE comprador_prontuario = ? AND status IN ('PENDENTE', 'EM_PREPARO', 'PRONTO')")) {
            ps.setString(1, prontuario);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Total de bytes de imagens guardadas. */
    public long bytesDeImagens() throws SQLException {
        return escalar("SELECT COALESCE(SUM(LENGTH(imagem)), 0) FROM Produto");
    }

    private long escalar(String sql) throws SQLException {
        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
