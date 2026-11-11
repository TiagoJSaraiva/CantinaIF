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
 * No modo demonstracao as contas de demonstracao (senha publica) ficam fora do bloqueio por conta: bloquea-las
 * derrubaria a demo para todos; elas continuam sujeitas ao teto por IP.
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
        return porIp.bloqueado(ip) || (!contaIsenta(prontuario) && porConta.bloqueado(chave(prontuario)));
    }

    /** Segundos ate liberar (o maior entre os dois bloqueios aplicaveis). */
    public synchronized long segundosParaLiberar(String ip, String prontuario) {
        long a = porIp.segundosParaLiberar(ip);
        long b = contaIsenta(prontuario) ? 0 : porConta.segundosParaLiberar(chave(prontuario));
        return Math.max(a, b);
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
