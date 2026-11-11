package servlets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

class CsrfFilterTest {

    private final CsrfFilter filtro = new CsrfFilter();

    private static HttpSession sessaoComToken(String token) {
        Map<String, Object> attrs = new HashMap<>();
        if (token != null) {
            attrs.put(CsrfFilter.ATTR, token);
        }
        HttpSession s = mock(HttpSession.class);
        when(s.getAttribute(anyString())).thenAnswer(i -> attrs.get(i.getArgument(0)));
        doAnswer(i -> attrs.put(i.getArgument(0), i.getArgument(1))).when(s).setAttribute(anyString(), any());
        return s;
    }

    private static HttpServletRequest req(String metodo, String caminho, HttpSession existente) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getMethod()).thenReturn(metodo);
        when(r.getServletPath()).thenReturn(caminho);
        when(r.getSession(false)).thenReturn(existente);
        return r;
    }

    @Test
    void postSemSessaoOuSemTokenOuComTokenErradoRecebe403() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);

        HttpServletResponse r1 = mock(HttpServletResponse.class);
        filtro.doFilter(req("POST", "/login", null), r1, cadeia);
        verify(r1).sendError(eq(403), anyString());

        HttpServletResponse r2 = mock(HttpServletResponse.class);
        filtro.doFilter(req("POST", "/carrinho", sessaoComToken("abc")), r2, cadeia);
        verify(r2).sendError(eq(403), anyString());

        HttpServletRequest comErrado = req("PUT", "/carrinho", sessaoComToken("abc"));
        when(comErrado.getHeader("X-CSRF-Token")).thenReturn("outro");
        HttpServletResponse r3 = mock(HttpServletResponse.class);
        filtro.doFilter(comErrado, r3, cadeia);
        verify(r3).sendError(eq(403), anyString());
        verifyNoInteractions(cadeia);
    }

    @Test
    void tokenCertoNoCabecalhoOuNoCampoPassa() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);

        HttpServletRequest porHeader = req("DELETE", "/carrinho", sessaoComToken("tok123"));
        when(porHeader.getHeader("X-CSRF-Token")).thenReturn("tok123");
        HttpServletResponse r1 = mock(HttpServletResponse.class);
        filtro.doFilter(porHeader, r1, cadeia);
        verify(cadeia).doFilter(porHeader, r1);

        HttpServletRequest porCampo = req("POST", "/register", sessaoComToken("tok123"));
        when(porCampo.getParameter("_csrf")).thenReturn("tok123");
        HttpServletResponse r2 = mock(HttpServletResponse.class);
        filtro.doFilter(porCampo, r2, cadeia);
        verify(cadeia).doFilter(porCampo, r2);
        verify(r1, never()).sendError(anyInt(), anyString());
    }

    @Test
    void visitanteAnonimoSoGanhaSessaoNasTelasDeLoginECadastroEElaEhCurta() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);

        HttpSession nova = sessaoComToken(null);
        HttpServletRequest login = req("GET", "/login.jsp", null);
        when(login.getSession(true)).thenReturn(nova);
        filtro.doFilter(login, mock(HttpServletResponse.class), cadeia);
        verify(nova).setMaxInactiveInterval(300);
        assertNotNull(nova.getAttribute(CsrfFilter.ATTR));

        HttpServletRequest erroJsp = req("GET", "/erro.jsp", null);
        filtro.doFilter(erroJsp, mock(HttpServletResponse.class), cadeia);
        verify(erroJsp, never()).getSession(true);

        HttpServletRequest health = req("GET", "/health", null);
        filtro.doFilter(health, mock(HttpServletResponse.class), cadeia);
        verify(health, never()).getSession(true);
        verify(health, never()).getSession();
    }

    @Test
    void paginaComSessaoExistenteRecebeTokenSeAindaNaoTiver() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpSession existente = sessaoComToken(null);
        filtro.doFilter(req("GET", "/home.jsp", existente), mock(HttpServletResponse.class), cadeia);
        assertNotNull(existente.getAttribute(CsrfFilter.ATTR));
    }

    @Test
    void tokenEAleatorioEEstavelNaSessao() {
        HttpSession s = sessaoComToken(null);
        String t1 = CsrfFilter.token(s);
        assertEquals(t1, CsrfFilter.token(s));
        assertTrue(t1.length() >= 40);
        assertNotEquals(t1, CsrfFilter.token(sessaoComToken(null)));
    }
}
