package utils;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

import seed.DemoDataSeeder;

/**
 * Limite de tentativas de login em duas camadas:
 * <ul>
 *   <li>por CONTA (prontuario): poucas falhas numa janela curta (padrao 5 em 3 minutos);</li>
 *   <li>por IP: um teto bem mais alto (padrao 40 em 5 minutos), para nao punir todos os que dividem um NAT
 *       ou a borda de um proxy.</li>
 * </ul>
 * No modo demonstracao as contas de demonstracao (senha publica) NUNCA sao bloqueadas, nem por conta nem por IP:
 * bloquea-las derrubaria a demo para todos (e se todos os visitantes aparecerem com o mesmo IP, atras de uma borda
 * nao confiavel, um so visitante travaria o login de todos). O teto por IP vale para as demais contas.
 * Em memoria, por instancia. Uma desvantagem conhecida do bloqueio por conta: quem erra a senha de proposito
 * consegue travar a conta de outra pessoa por alguns minutos.
 */
public final class LoginGuard {

    public static final LoginGuard PADRAO = new LoginGuard(5, 3 * 60_000L, 40, 5 * 60_000L,
            System::currentTimeMillis, Database::isDemoMode);

    private final RateLimiter porConta;
    private final RateLimiter porIp;
    private final BooleanSupplier modoDemo;

    public LoginGuard(int falhasPorConta, long janelaContaMs, int falhasPorIp, long janelaIpMs,
            LongSupplier relogio, BooleanSupplier modoDemo) {
        this.porConta = new RateLimiter(falhasPorConta, janelaContaMs, relogio);
        this.porIp = new RateLimiter(falhasPorIp, janelaIpMs, relogio);
        this.modoDemo = modoDemo;
    }

    private static String chave(String prontuario) {
        return prontuario == null ? "" : prontuario.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contaIsenta(String prontuario) {
        if (!modoDemo.getAsBoolean()) {
            return false;
        }
        String k = chave(prontuario);
        return k.equals(DemoDataSeeder.VENDEDOR_PRONTUARIO.toLowerCase(Locale.ROOT))
                || k.equals(DemoDataSeeder.COMPRADOR_PRONTUARIO.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean bloqueado(String ip, String prontuario) {
        if (contaIsenta(prontuario)) {
            return false; // contas de demo (senha publica): nunca bloqueadas, nem por IP (um colapso de IPs nao derruba a demo)
        }
        return porIp.bloqueado(ip) || porConta.bloqueado(chave(prontuario));
    }

    /** Segundos ate liberar (o maior entre os dois bloqueios aplicaveis). */
    public synchronized long segundosParaLiberar(String ip, String prontuario) {
        if (contaIsenta(prontuario)) {
            return 0;
        }
        return Math.max(porIp.segundosParaLiberar(ip), porConta.segundosParaLiberar(chave(prontuario)));
    }

    public synchronized void falha(String ip, String prontuario) {
        porIp.falha(ip);
        if (!contaIsenta(prontuario)) {
            porConta.falha(chave(prontuario));
        }
    }

    /** Login correto: zera o contador da conta (o do IP continua, para limitar varredura de contas). */
    public synchronized void sucesso(String prontuario) {
        porConta.sucesso(chave(prontuario));
    }
}
