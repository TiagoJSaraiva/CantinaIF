package utils;

import java.sql.SQLException;

import dao.LimiteAtingidoException;
import dao.LimitesDao;

/**
 * Tetos configuraveis por variavel de ambiente para evitar esgotamento de memoria numa demonstracao publica.
 * Em modo demonstracao (H2) valem os padroes abaixo; fora dele nao ha teto, a menos que a variavel seja definida.
 * Valor 0 (ou negativo) = sem limite.
 *
 * <ul>
 *   <li>CANTINA_MAX_PRODUTOS (padrao da demo: 60)</li>
 *   <li>CANTINA_MAX_COMPRADORES (padrao da demo: 200)</li>
 *   <li>CANTINA_MAX_PEDIDOS_ABERTOS (padrao da demo: 100)</li>
 *   <li>CANTINA_MAX_BYTES_IMAGENS (padrao da demo: 12582912 = 12 MB, somando todas as imagens)</li>
 * </ul>
 */
public final class Limites {

    public static final int PADRAO_PRODUTOS = 60;
    public static final int PADRAO_COMPRADORES = 200;
    public static final int PADRAO_PEDIDOS_ABERTOS = 100;
    public static final long PADRAO_BYTES_IMAGENS = 12L * 1024 * 1024;

    private static final LimitesDao DAO = new LimitesDao();

    private Limites() {
    }

    static long valor(String variavel, long padraoDemo) {
        String v = Config.get(variavel);
        long n;
        if (v == null) {
            n = Database.isDemoMode() ? padraoDemo : 0;
        } else {
            try {
                n = Long.parseLong(v.trim());
            } catch (NumberFormatException e) {
                n = Database.isDemoMode() ? padraoDemo : 0;
            }
        }
        return Math.max(0, n);
    }

    public static long maxProdutos() { return valor("CANTINA_MAX_PRODUTOS", PADRAO_PRODUTOS); }
    public static long maxCompradores() { return valor("CANTINA_MAX_COMPRADORES", PADRAO_COMPRADORES); }
    public static long maxPedidosAbertos() { return valor("CANTINA_MAX_PEDIDOS_ABERTOS", PADRAO_PEDIDOS_ABERTOS); }
    public static long maxBytesImagens() { return valor("CANTINA_MAX_BYTES_IMAGENS", PADRAO_BYTES_IMAGENS); }

    /** Antes de criar um produto (com a imagem ja processada, ou 0 se nao houver). */
    public static void verificarNovoProduto(long bytesDaImagem) throws SQLException, LimiteAtingidoException {
        long max = maxProdutos();
        if (max > 0 && DAO.contarProdutos() >= max) {
            throw new LimiteAtingidoException("produtos", max);
        }
        verificarImagem(bytesDaImagem);
    }

    /** Antes de gravar uma imagem nova (criar ou trocar). */
    public static void verificarImagem(long bytesDaImagem) throws SQLException, LimiteAtingidoException {
        long max = maxBytesImagens();
        if (max > 0 && bytesDaImagem > 0 && DAO.bytesDeImagens() + bytesDaImagem > max) {
            throw new LimiteAtingidoException("espaço total de imagens", max);
        }
    }

    public static void verificarNovoComprador() throws SQLException, LimiteAtingidoException {
        long max = maxCompradores();
        if (max > 0 && DAO.contarCompradores() >= max) {
            throw new LimiteAtingidoException("contas cadastradas", max);
        }
    }

    public static void verificarNovoPedido() throws SQLException, LimiteAtingidoException {
        long max = maxPedidosAbertos();
        if (max > 0 && DAO.contarPedidosAbertos() >= max) {
            throw new LimiteAtingidoException("pedidos em aberto", max);
        }
    }
}
