package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import model.Advertencia;
import utils.Database;
import utils.Limites;

/** Advertencias que um vendedor envia aos compradores vinculados a ele. */
public class AdvertenciaDao {

    public static final int MENSAGEM_MAXIMA = 500;

    /**
     * Registra uma advertencia. So e permitida para um comprador vinculado a {@code vendedor}.
     *
     * @throws RegraNegocioException comprador nao e desse vendedor, mensagem invalida ou teto atingido
     */
    public int criar(String comprador, String vendedor, String mensagem, LocalDateTime agora)
            throws SQLException, RegraNegocioException {
        String texto = mensagem == null ? "" : mensagem.strip();
        if (texto.length() < 3 || texto.length() > MENSAGEM_MAXIMA) {
            throw new RegraNegocioException("A advertência deve ter de 3 a " + MENSAGEM_MAXIMA + " caracteres.");
        }
        try (Connection conn = Database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Comprador WHERE prontuario = ? AND vendedor_prontuario = ?")) {
                ps.setString(1, comprador);
                ps.setString(2, vendedor);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        throw new RegraNegocioException("Comprador não encontrado entre os seus compradores.");
                    }
                }
            }
            long max = Limites.maxAdvertenciasPorComprador();
            if (max > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM Advertencia WHERE comprador_prontuario = ?")) {
                    ps.setString(1, comprador);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getLong(1) >= max) {
                            throw new LimiteAtingidoException("advertências por comprador", max);
                        }
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Advertencia "
                    + "(comprador_prontuario, vendedor_prontuario, mensagem, criada_em) VALUES (?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, comprador);
                ps.setString(2, vendedor);
                ps.setString(3, texto);
                ps.setTimestamp(4, Timestamp.valueOf(agora));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }

    /** Advertencias recebidas pelo comprador, da mais recente para a mais antiga. */
    public List<Advertencia> listarDoComprador(String comprador) throws SQLException {
        List<Advertencia> lista = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT a.id, a.vendedor_prontuario, v.nome, a.mensagem, "
                     + "a.criada_em, a.lida_em FROM Advertencia a JOIN Vendedor v ON v.prontuario = a.vendedor_prontuario "
                     + "WHERE a.comprador_prontuario = ? ORDER BY a.criada_em DESC, a.id DESC")) {
            ps.setString(1, comprador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp lida = rs.getTimestamp(6);
                    lista.add(new Advertencia(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getTimestamp(5).toLocalDateTime(), lida == null ? null : lida.toLocalDateTime()));
                }
            }
        }
        return lista;
    }

    /** Marca como lidas todas as advertencias ainda nao lidas do comprador; devolve quantas foram marcadas. */
    public int marcarLidas(String comprador, LocalDateTime agora) throws SQLException {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE Advertencia SET lida_em = ? WHERE comprador_prontuario = ? AND lida_em IS NULL")) {
            ps.setTimestamp(1, Timestamp.valueOf(agora));
            ps.setString(2, comprador);
            return ps.executeUpdate();
        }
    }
}
