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
 *   <li>CANTINA_MAX_PRODUTOS (padrao da demo: 300, somando todos os vendedores)</li>
 *   <li>CANTINA_MAX_PRODUTOS_POR_VENDEDOR (padrao da demo: 40)</li>
 *   <li>CANTINA_MAX_VENDEDORES (padrao da demo: 30)</li>
 *   <li>CANTINA_MAX_ADVERTENCIAS_POR_COMPRADOR (padrao da demo: 50)</li>
 *   <li>CANTINA_MAX_COMPRADORES (padrao da demo: 200)</li>
 *   <li>CANTINA_MAX_PEDIDOS_ABERTOS (padrao da demo: 100)</li>
 *   <li>CANTINA_MAX_BYTES_IMAGENS (padrao da demo: 12582912 = 12 MB, somando todas as imagens)</li>
 *   <li>CANTINA_MAX_PEDIDOS_ABERTOS_POR_COMPRADOR (padrao da demo: 5)</li>
 * </ul>
 */
public final class Limites {

    public static final int PADRAO_PRODUTOS = 300;
    public static final int PADRAO_PRODUTOS_POR_VENDEDOR = 40;
    public static final int PADRAO_VENDEDORES = 30;
    public static final int PADRAO_ADVERTENCIAS_POR_COMPRADOR = 50;
    public static final int PADRAO_COMPRADORES = 200;
    public static final int PADRAO_PEDIDOS_ABERTOS = 100;
    public static final long PADRAO_BYTES_IMAGENS = 12L * 1024 * 1024;
    public static final int PADRAO_PEDIDOS_ABERTOS_POR_COMPRADOR = 5;

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
    public static long maxProdutosPorVendedor() { return valor("CANTINA_MAX_PRODUTOS_POR_VENDEDOR", PADRAO_PRODUTOS_POR_VENDEDOR); }
    public static long maxVendedores() { return valor("CANTINA_MAX_VENDEDORES", PADRAO_VENDEDORES); }
    public static long maxAdvertenciasPorComprador() { return valor("CANTINA_MAX_ADVERTENCIAS_POR_COMPRADOR", PADRAO_ADVERTENCIAS_POR_COMPRADOR); }
    public static long maxCompradores() { return valor("CANTINA_MAX_COMPRADORES", PADRAO_COMPRADORES); }
    public static long maxPedidosAbertos() { return valor("CANTINA_MAX_PEDIDOS_ABERTOS", PADRAO_PEDIDOS_ABERTOS); }
    public static long maxPedidosAbertosPorComprador() { return valor("CANTINA_MAX_PEDIDOS_ABERTOS_POR_COMPRADOR", PADRAO_PEDIDOS_ABERTOS_POR_COMPRADOR); }
    public static long maxBytesImagens() { return valor("CANTINA_MAX_BYTES_IMAGENS", PADRAO_BYTES_IMAGENS); }

    /** Antes de criar um produto do vendedor (com a imagem ja processada, ou 0 se nao houver). */
    public static void verificarNovoProduto(String vendedor, long bytesDaImagem) throws SQLException, LimiteAtingidoException {
        long max = maxProdutos();
        if (max > 0 && DAO.contarProdutos() >= max) {
            throw new LimiteAtingidoException("produtos", max);
        }
        long porVendedor = maxProdutosPorVendedor();
        if (porVendedor > 0 && vendedor != null && DAO.contarProdutosDe(vendedor) >= porVendedor) {
            throw new LimiteAtingidoException("produtos por vendedor", porVendedor);
        }
        verificarImagem(bytesDaImagem);
    }

    public static void verificarNovoVendedor() throws SQLException, LimiteAtingidoException {
        long max = maxVendedores();
        if (max > 0 && DAO.contarVendedores() >= max) {
            throw new LimiteAtingidoException("contas de vendedor", max);
        }
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

    /** Antes de criar um pedido: teto global de pedidos em aberto e teto por comprador. */
    public static void verificarNovoPedido(String prontuario) throws SQLException, LimiteAtingidoException {
        verificarNovoPedido();
        long max = maxPedidosAbertosPorComprador();
        if (max > 0 && prontuario != null) {
            long abertos = DAO.contarPedidosAbertosDe(prontuario);
            if (abertos >= max) {
                throw new LimiteAtingidoException("Você já tem " + abertos + " pedidos em aberto (máximo " + max
                        + " na demonstração). Aguarde a retirada ou cancele um antes de fazer outro.");
            }
        }
    }
}
