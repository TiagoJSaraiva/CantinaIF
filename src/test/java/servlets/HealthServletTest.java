package servlets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class HealthServletTest {

    @Test
    void shaValidoEhMantidoEmMinusculas() {
        assertEquals("f57d344", HealthServlet.normalizar("f57d344"));
        String completo = "F57D3440123456789ABCDEF0123456789ABCDEF0";
        assertEquals(completo.toLowerCase(), HealthServlet.normalizar(" " + completo + "\n"));
    }

    @Test
    void valorAusenteOuInvalidoViraDesconhecido() {
        assertEquals("desconhecido", HealthServlet.normalizar(null));
        assertEquals("desconhecido", HealthServlet.normalizar(""));
        assertEquals("desconhecido", HealthServlet.normalizar("abc"));
        assertEquals("desconhecido", HealthServlet.normalizar("f57d344\r\nSet-Cookie: x=1"));
        assertEquals("desconhecido", HealthServlet.normalizar("main"));
    }

    @Test
    void respondeOkComCabecalhoDoCommit() throws Exception {
        HttpServletResponse res = mock(HttpServletResponse.class);
        StringWriter saida = new StringWriter();
        when(res.getWriter()).thenReturn(new PrintWriter(saida, true));

        new HealthServlet().doGet(mock(HttpServletRequest.class), res);

        verify(res).setStatus(200);
        verify(res).setHeader(eq("X-App-Commit"), anyString());
        assertEquals("ok", saida.toString());
    }
}
