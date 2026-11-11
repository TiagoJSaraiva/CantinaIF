package utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import seed.DemoDataSeeder;

/**
 * Abre o pool de conexoes (e o schema/dados H2) no boot e o fecha no desligamento. Em modo demonstracao
 * publica as contas de demonstracao para a tela de login e agenda a restauracao periodica da base.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        if ("true".equalsIgnoreCase(Config.get("CANTINA_COOKIE_SECURE"))) {
            ctx.getSessionCookieConfig().setSecure(true);
        }
        Database.init();
        if (Database.isDemoMode()) {
            ctx.setAttribute("demoMode", Boolean.TRUE);
            ctx.setAttribute("demoVendedor", DemoDataSeeder.VENDEDOR_PRONTUARIO);
            ctx.setAttribute("demoComprador", DemoDataSeeder.COMPRADOR_PRONTUARIO);
            ctx.setAttribute("demoSenha", DemoDataSeeder.SENHA_DEMO);
            double horas = DemoReset.horasConfiguradas();
            ctx.setAttribute("demoResetHoras", horas);
            ctx.setAttribute("demoResetTexto", horas == Math.rint(horas) ? String.valueOf((long) horas) : String.valueOf(horas));
            DemoReset.iniciar(horas);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        DemoReset.parar();
        Database.shutdown();
    }
}
