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

    /** Apaga e recria os dados de demonstracao. Nunca propaga excecao (para nao cancelar o agendamento). */
    static void restaurar() {
        try {
            DemoDataSeeder.Resultado r = DemoDataSeeder.run(LocalDateTime.now());
            System.out.println("[demo] base restaurada: " + r.produtos() + " produtos, " + r.pedidos() + " pedidos.");
        } catch (Throwable t) {
            System.err.println("[demo] falha ao restaurar a base: " + t);
        }
    }
}
