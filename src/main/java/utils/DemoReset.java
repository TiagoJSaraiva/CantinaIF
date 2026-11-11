package utils;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import seed.DemoDataSeeder;

/** Restaura os dados de demonstracao periodicamente (somente em modo demonstracao). */
public final class DemoReset {

    private static ScheduledExecutorService agendador;

    private DemoReset() {
    }

    /** Valor de CANTINA_DEMO_RESET_HOURS em horas (aceita fracao); padrao 6; 0 ou invalido-negativo desliga. */
    public static double horasConfiguradas() {
        String v = Config.get("CANTINA_DEMO_RESET_HOURS");
        if (v == null) {
            return 6;
        }
        try {
            return Double.parseDouble(v.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            System.err.println("CANTINA_DEMO_RESET_HOURS invalido (" + v + "); usando 6.");
            return 6;
        }
    }

    /** Agenda a restauracao a cada {@code horas}; 0 (ou menos) nao agenda nada. */
    public static synchronized void iniciar(double horas) {
        parar();
        if (horas <= 0) {
            return;
        }
        long segundos = Math.max(1, Math.round(horas * 3600));
        agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cantinaif-demo-reset");
            t.setDaemon(true);
            return t;
        });
        agendador.scheduleAtFixedRate(DemoReset::restaurar, segundos, segundos, TimeUnit.SECONDS);
    }

    public static synchronized void parar() {
        if (agendador != null) {
            agendador.shutdownNow();
            agendador = null;
        }
    }

    /** Tempo maximo (s) para obter a trava de escrita; se estourar, a restauracao e reagendada. */
    static final long ESPERA_RESET_SEGUNDOS = 10;
    /** Atraso (s) para tentar de novo quando nao foi possivel restaurar agora. */
    static final long REAGENDAR_SEGUNDOS = 30;

    /**
     * Aplica o seed segurando a trava de escrita. Retorna false (sem fazer nada) se as conexoes abertas nao
     * terminarem dentro de {@code timeoutMs}; os leitores nunca ficam esperando alem do proprio tempo limite.
     */
    public static boolean aplicarComTrava(DemoDataSeeder.Plano plano, LocalDateTime agora, long timeoutMs)
            throws Exception {
        if (!DbGate.escrita().tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            return false;
        }
        try (java.sql.Connection conn = Database.getConnectionSemTrava()) {
            DemoDataSeeder.aplicar(conn, plano, agora);
            Database.novaGeracao();
            return true;
        } finally {
            DbGate.escrita().unlock();
        }
    }

    /** Apaga e recria os dados de demonstracao. Nunca propaga excecao (para nao cancelar o agendamento). */
    static void restaurar() {
        try {
            // trabalho pesado (BCrypt, leitura das imagens) ANTES de pedir a trava
            DemoDataSeeder.Plano plano = DemoDataSeeder.preparar();
            if (aplicarComTrava(plano, LocalDateTime.now(), ESPERA_RESET_SEGUNDOS * 1000)) {
                System.out.println("[demo] base restaurada.");
            } else {
                System.err.println("[demo] ha conexoes abertas ha mais de " + ESPERA_RESET_SEGUNDOS
                        + " s; restauracao adiada em " + REAGENDAR_SEGUNDOS + " s.");
                reagendar();
            }
        } catch (Throwable t) {
            System.err.println("[demo] falha ao restaurar a base: " + t);
        }
    }

    private static synchronized void reagendar() {
        if (agendador != null) {
            agendador.schedule(DemoReset::restaurar, REAGENDAR_SEGUNDOS, TimeUnit.SECONDS);
        }
    }
}
