package utils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Origem do IP do cliente usado pelos limites (login, escrita).
 * <ol>
 *   <li>Com {@code CANTINA_BEHIND_PROXY=true} e {@code CANTINA_CLIENT_IP_HEADER=<cabecalho>}: o valor desse
 *       cabecalho (um cabecalho de IP do cliente definido pela borda, ex.: o que a CDN/plataforma preenche); se
 *       ausente ou invalido, cai no passo seguinte.</li>
 *   <li>Caso contrario, {@code request.getRemoteAddr()}: o IP do socket ou, atras de proxy confiavel, o IP que o
 *       RemoteIpValve do Tomcat extraiu de X-Forwarded-For.</li>
 * </ol>
 * O cabecalho configurado so e usado quando a aplicacao foi declarada atras de proxy (CANTINA_BEHIND_PROXY=true);
 * senao um cliente poderia forjar o proprio IP. Nada alem do IP resolvido e da origem e registrado em log.
 */
public final class ClientIp {

    private static final Logger LOG = Logger.getLogger("cantinaif.ip");
    private static final Pattern IP = Pattern.compile("[0-9A-Fa-f:.]{2,45}");
    private static final AtomicLong JANELA_LOG = new AtomicLong();
    private static final AtomicLong LINHAS_NA_JANELA = new AtomicLong();
    private static final long MAX_LINHAS_POR_MINUTO = 20;

    private ClientIp() {
    }

    static boolean atrasDeProxy() {
        return "true".equalsIgnoreCase(Config.get("CANTINA_BEHIND_PROXY"));
    }

    /** Nome do cabecalho de IP do cliente configurado, ou null se nao se aplica. */
    static String cabecalhoConfigurado() {
        String nome = Config.get("CANTINA_CLIENT_IP_HEADER");
        return (nome != null && atrasDeProxy()) ? nome.trim() : null;
    }

    /** IP do cliente para efeito de limites. */
    public static String de(HttpServletRequest request) {
        String nome = cabecalhoConfigurado();
        if (nome != null) {
            String valor = request.getHeader(nome);
            if (valor != null) {
                String primeiro = valor.split(",")[0].trim();
                if (IP.matcher(primeiro).matches()) {
                    return primeiro;
                }
            }
        }
        return request.getRemoteAddr();
    }

    /** Descricao da origem do IP em uso (para logs e verificacao pos-deploy). */
    public static String origem() {
        String nome = cabecalhoConfigurado();
        if (nome != null) {
            return "cabecalho " + nome + " (com recuo para o IP do RemoteIpValve/socket)";
        }
        if (atrasDeProxy()) {
            return "RemoteIpValve (X-Forwarded-For de proxies confiaveis; CANTINA_TRUSTED_PROXIES)";
        }
        return "socket direto (sem proxy declarado)";
    }

    /** Linha de INFO na inicializacao dizendo qual origem de IP esta em uso. */
    public static void registrarInicializacao() {
        LOG.info("[ip] origem do IP do cliente: " + origem());
    }

    /** INFO a cada bloqueio por limite: tipo, IP resolvido e origem (no maximo 20 linhas por minuto). */
    public static void registrarBloqueio(String tipo, String ip) {
        long agora = System.currentTimeMillis();
        long inicio = JANELA_LOG.get();
        if (agora - inicio > 60_000L && JANELA_LOG.compareAndSet(inicio, agora)) {
            LINHAS_NA_JANELA.set(0);
        }
        if (LINHAS_NA_JANELA.incrementAndGet() <= MAX_LINHAS_POR_MINUTO) {
            LOG.info("[limite] bloqueio de " + tipo + " para o IP " + ip + " (origem do IP: " + origem() + ")");
        }
    }
}
