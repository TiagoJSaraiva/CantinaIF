package servlets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.Comprador;
import model.Vendedor;
import utils.Database;

class AuthFilterTest {

    private final AuthFilter filtro = new AuthFilter();

    private static HttpServletRequest requisicao(String metodo, String caminho, HttpSession sessao) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(metodo);
        when(req.getServletPath()).thenReturn(caminho);
        when(req.getContextPath()).thenReturn("");
        when(req.getSession(false)).thenReturn(sessao);
        return req;
    }

    private static HttpServletResponse resposta(StringWriter corpo) throws Exception {
        HttpServletResponse res = mock(HttpServletResponse.class);
        when(res.getWriter()).thenReturn(new PrintWriter(corpo));
        return res;
    }

    private static HttpSession sessao(String perfil, Object valor, Object geracao) {
        HttpSession s = mock(HttpSession.class);
        when(s.getAttribute(anyString())).thenReturn(null);
        if (perfil != null) {
            when(s.getAttribute(perfil)).thenReturn(valor);
        }
        when(s.getAttribute("geracao")).thenReturn(geracao);
        return s;
    }

    @Test
    void rotasPublicasPassam() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpServletRequest req = requisicao("GET", "/login.jsp", null);
        HttpServletResponse res = resposta(new StringWriter());
        filtro.doFilter(req, res, cadeia);
        verify(cadeia).doFilter(req, res);
    }

    @Test
    void paginaSemLoginRedirecionaEApiResponde401() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpServletResponse res = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/home.jsp", null), res, cadeia);
        verify(res).sendRedirect("/login.jsp");

        HttpServletResponse res2 = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/apiUser/produtos", null), res2, cadeia);
        verify(res2).setStatus(401);
        verifyNoInteractions(cadeia);
    }

    @Test
    void perfilCorretoPassaEPerfilErradoNaApiRecebe403() throws Exception {
        long geracao = Database.geracao();
        FilterChain cadeia = mock(FilterChain.class);
        HttpSession comprador = sessao("comprador", new Comprador(), geracao);

        HttpServletRequest ok = requisicao("GET", "/apiUser/produtos", comprador);
        HttpServletResponse resOk = resposta(new StringWriter());
        filtro.doFilter(ok, resOk, cadeia);
        verify(cadeia).doFilter(ok, resOk);

        HttpServletResponse resNegado = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/apiAdm/produtos", comprador), resNegado, cadeia);
        verify(resNegado).setStatus(403);
        verify(cadeia, times(1)).doFilter(any(), any());
    }

    @Test
    void sessaoDeUmaGeracaoAnteriorDaBaseEInvalidadaComAvisoDeRestauracao() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        long antiga = Database.geracao();
        Database.novaGeracao();
        HttpSession velha = sessao("comprador", new Comprador(), antiga);

        // pagina: redireciona ao login com o aviso
        HttpServletResponse resPagina = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/home.jsp", velha), resPagina, cadeia);
        verify(velha).invalidate();
        verify(resPagina).sendRedirect("/login.jsp?restaurada=1");

        // API: 401 com a marca "restaurada" e a mensagem
        HttpSession velha2 = sessao("vendedor", new Vendedor(), antiga);
        StringWriter corpo = new StringWriter();
        HttpServletResponse resApi = resposta(corpo);
        filtro.doFilter(requisicao("POST", "/api/pedidos/status", velha2), resApi, cadeia);
        verify(velha2).invalidate();
        verify(resApi).setStatus(401);
        assertTrue(corpo.toString().contains("\"restaurada\":true"));
        assertTrue(corpo.toString().contains("A demonstração foi restaurada; entre novamente."));
        verifyNoInteractions(cadeia);
    }

    @Test
    void sessaoDaGeracaoAtualContinuaValidaAposUmaNovaRestauracaoSoSeForReemitida() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpSession atual = sessao("comprador", new Comprador(), Database.geracao());
        HttpServletRequest req = requisicao("GET", "/home.jsp", atual);
        HttpServletResponse res = resposta(new StringWriter());
        filtro.doFilter(req, res, cadeia);
        verify(cadeia).doFilter(req, res);
        verify(atual, never()).invalidate();
        verify(res, never()).sendRedirect(anyString());
        verify(res, never()).setStatus(anyInt());
    }
}
