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
    void visitanteAnonimoNuncaGanhaSessaoEOTokenDasTelasDeLoginViraCookieDuploSubmit() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);

        HttpServletRequest login = req("GET", "/login.jsp", null);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        filtro.doFilter(login, resp, cadeia);
        verify(login, never()).getSession(true);
        verify(login, never()).getSession();
        org.mockito.ArgumentCaptor<jakarta.servlet.http.Cookie> cookie = org.mockito.ArgumentCaptor.forClass(jakarta.servlet.http.Cookie.class);
        verify(resp).addCookie(cookie.capture());
        assertEquals("csrf_anon", cookie.getValue().getName());
        assertTrue(cookie.getValue().isHttpOnly());
        assertEquals("Strict", cookie.getValue().getAttribute("SameSite"));
        ArgumentCaptorHelper.verificaAtributoDoFormulario(login, cookie.getValue().getValue());

        for (String caminho : new String[] { "/erro.jsp", "/health", "/image", "/home.jsp" }) {
            HttpServletRequest r = req("GET", caminho, null);
            filtro.doFilter(r, mock(HttpServletResponse.class), cadeia);
            verify(r, never()).getSession(true);
            verify(r, never()).getSession();
        }
    }

    @Test
    void milharesDeGetsAnonimosNaoCriamSessoes() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpServletRequest r = req("GET", "/login.jsp", null);
        for (int i = 0; i < 5000; i++) {
            filtro.doFilter(r, mock(HttpServletResponse.class), cadeia);
        }
        verify(r, never()).getSession(true);
        verify(r, never()).getSession();
    }

    @Test
    void cookieAnonimoJaValidoEReaproveitadoSemNovoCookie() throws Exception {
        String token = "A".repeat(43);
        HttpServletRequest r = req("GET", "/cadastro.jsp", null);
        when(r.getCookies()).thenReturn(new jakarta.servlet.http.Cookie[] { new jakarta.servlet.http.Cookie("csrf_anon", token) });
        HttpServletResponse resp = mock(HttpServletResponse.class);
        filtro.doFilter(r, resp, mock(FilterChain.class));
        verify(resp, never()).addCookie(any());
        ArgumentCaptorHelper.verificaAtributoDoFormulario(r, token);
    }

    @Test
    void loginECadastroAceitamDuploSubmitSemSessaoMasOutrasRotasNao() throws Exception {
        String token = "B".repeat(43);
        jakarta.servlet.http.Cookie[] cookies = { new jakarta.servlet.http.Cookie("csrf_anon", token) };
        FilterChain cadeia = mock(FilterChain.class);

        for (String caminho : new String[] { "/login", "/register" }) {
            HttpServletRequest ok = req("POST", caminho, null);
            when(ok.getCookies()).thenReturn(cookies);
            when(ok.getParameter("_csrf")).thenReturn(token);
            HttpServletResponse r = mock(HttpServletResponse.class);
            filtro.doFilter(ok, r, cadeia);
            verify(cadeia).doFilter(ok, r);
            verify(ok, never()).getSession(true);
            ArgumentCaptorHelper.verificaAtributoDoFormulario(ok, token);
        }

        HttpServletRequest errado = req("POST", "/login", null);
        when(errado.getCookies()).thenReturn(cookies);
        when(errado.getParameter("_csrf")).thenReturn("C".repeat(43));
        HttpServletResponse r1 = mock(HttpServletResponse.class);
        filtro.doFilter(errado, r1, cadeia);
        verify(r1).sendError(eq(403), anyString());

        HttpServletRequest semCookie = req("POST", "/login", null);
        when(semCookie.getParameter("_csrf")).thenReturn(token);
        HttpServletResponse r2 = mock(HttpServletResponse.class);
        filtro.doFilter(semCookie, r2, cadeia);
        verify(r2).sendError(eq(403), anyString());

        // o cookie anonimo nao vale para rotas que alteram estado depois do login
        HttpServletRequest carrinho = req("POST", "/carrinho", null);
        when(carrinho.getCookies()).thenReturn(cookies);
        when(carrinho.getHeader("X-CSRF-Token")).thenReturn(token);
        HttpServletResponse r3 = mock(HttpServletResponse.class);
        filtro.doFilter(carrinho, r3, cadeia);
        verify(r3).sendError(eq(403), anyString());
    }

    /** Auxiliar para conferir o atributo que os formularios usam. */
    static final class ArgumentCaptorHelper {
        static void verificaAtributoDoFormulario(HttpServletRequest r, String esperado) {
            verify(r).setAttribute(CsrfFilter.ATTR_FORM, esperado);
        }
    }

    @Test
    void paginaComSessaoExistenteRecebeTokenSeAindaNaoTiver() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpSession existente = sessaoComToken(null);
        HttpServletRequest r = req("GET", "/home.jsp", existente);
        filtro.doFilter(r, mock(HttpServletResponse.class), cadeia);
        assertNotNull(existente.getAttribute(CsrfFilter.ATTR));
        verify(r).setAttribute(eq(CsrfFilter.ATTR_FORM), anyString());
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
