package utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpTest {

    @AfterEach
    void limpar() {
        System.clearProperty("CANTINA_BEHIND_PROXY");
        System.clearProperty("CANTINA_CLIENT_IP_HEADER");
    }

    private static HttpServletRequest req(String remoto, String cabecalho, String valor) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRemoteAddr()).thenReturn(remoto);
        if (cabecalho != null) {
            when(r.getHeader(cabecalho)).thenReturn(valor);
        }
        return r;
    }

    @Test
    void semConfiguracaoUsaOIpDoSocketOuDoRemoteIpValve() {
        assertEquals("10.0.0.5", ClientIp.de(req("10.0.0.5", "X-Client-IP", "203.0.113.9")));
        assertTrue(ClientIp.origem().contains("socket direto"));
    }

    @Test
    void cabecalhoConfiguradoSoValeAtrasDeProxyDeclarado() {
        System.setProperty("CANTINA_CLIENT_IP_HEADER", "X-Client-IP");
        assertEquals("10.0.0.5", ClientIp.de(req("10.0.0.5", "X-Client-IP", "203.0.113.9")),
                "sem CANTINA_BEHIND_PROXY=true o cabecalho (forjavel) e ignorado");

        System.setProperty("CANTINA_BEHIND_PROXY", "true");
        assertEquals("203.0.113.9", ClientIp.de(req("10.0.0.5", "X-Client-IP", "203.0.113.9")));
        assertTrue(ClientIp.origem().contains("cabecalho X-Client-IP"));
    }

    @Test
    void usaOPrimeiroIpDaListaEIgnoraValoresInvalidos() {
        System.setProperty("CANTINA_BEHIND_PROXY", "true");
        System.setProperty("CANTINA_CLIENT_IP_HEADER", "True-Client-IP");
        assertEquals("198.51.100.7", ClientIp.de(req("10.0.0.5", "True-Client-IP", "198.51.100.7, 10.1.1.1")));
        assertEquals("2001:db8::1", ClientIp.de(req("10.0.0.5", "True-Client-IP", "2001:db8::1")));
        assertEquals("10.0.0.5", ClientIp.de(req("10.0.0.5", "True-Client-IP", "<script>alert(1)</script>")));
        assertEquals("10.0.0.5", ClientIp.de(req("10.0.0.5", "True-Client-IP", "")));
        assertEquals("10.0.0.5", ClientIp.de(req("10.0.0.5", null, null)), "cabecalho ausente: recua para o socket/valve");
    }

    @Test
    void atrasDeProxySemCabecalhoIndicaRemoteIpValve() {
        System.setProperty("CANTINA_BEHIND_PROXY", "true");
        assertTrue(ClientIp.origem().contains("RemoteIpValve"));
    }
}
