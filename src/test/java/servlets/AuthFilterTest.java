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

    /** Comprador ativo e vinculado (sem banco). */
    private final AuthFilter filtro = new AuthFilter(p -> new model.EstadoComprador(true, "V1", "Cantina", 0));

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

    @Test
    void postComPerfilErradoRecebe403MesmoEmRotaDePagina() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        HttpSession comprador = sessao("comprador", new Comprador(), Database.geracao());
        HttpServletResponse res = resposta(new StringWriter());
        filtro.doFilter(requisicao("POST", "/registrar-produto", comprador), res, cadeia);
        verify(res).setStatus(403);
        verify(res, never()).sendRedirect(anyString());
        verifyNoInteractions(cadeia);

        HttpSession vendedor = sessao("vendedor", new Vendedor(), Database.geracao());
        HttpServletResponse res2 = resposta(new StringWriter());
        filtro.doFilter(requisicao("POST", "/FinalizarCompraServlet", vendedor), res2, cadeia);
        verify(res2).setStatus(403);
    }

    @Test
    void baseRestauradaNoMeioDaRequisicaoViraRedirecionamentoOu401NuncaErro500() throws Exception {
        long geracao = Database.geracao();
        HttpSession comprador = sessao("comprador", new Comprador(), geracao);

        FilterChain estoura = mock(FilterChain.class);
        doThrow(new utils.BaseRestauradaException()).when(estoura).doFilter(any(), any());

        HttpServletResponse resPagina = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/home.jsp", comprador), resPagina, estoura);
        verify(comprador).invalidate();
        verify(resPagina).sendRedirect("/login.jsp?restaurada=1");

        HttpSession vendedor = sessao("vendedor", new Vendedor(), geracao);
        StringWriter corpo = new StringWriter();
        HttpServletResponse resApi = resposta(corpo);
        filtro.doFilter(requisicao("POST", "/atualizarEstoque", vendedor), resApi, estoura);
        verify(resApi).setStatus(401);
        assertTrue(corpo.toString().contains("\"restaurada\":true"));
        assertNull(utils.GeracaoDaRequisicao.atual(), "a geracao da thread e sempre limpa");

        // a excecao vinda embrulhada em ServletException (como o Jasper faz) tambem e tratada
        FilterChain embrulhada = mock(FilterChain.class);
        doThrow(new jakarta.servlet.ServletException("jsp", new utils.BaseRestauradaException())).when(embrulhada).doFilter(any(), any());
        HttpSession outra = sessao("comprador", new Comprador(), geracao);
        HttpServletResponse res3 = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/Carrinho.jsp", outra), res3, embrulhada);
        verify(res3).sendRedirect("/login.jsp?restaurada=1");
    }

    @Test
    void outrasExcecoesContinuamPropagando() throws Exception {
        HttpSession comprador = sessao("comprador", new Comprador(), Database.geracao());
        FilterChain estoura = mock(FilterChain.class);
        doThrow(new IllegalStateException("outra")).when(estoura).doFilter(any(), any());
        assertThrows(IllegalStateException.class,
                () -> filtro.doFilter(requisicao("GET", "/home.jsp", comprador), resposta(new StringWriter()), estoura));
        assertNull(utils.GeracaoDaRequisicao.atual());
    }

    private static Comprador compradorCom(String prontuario) {
        Comprador c = new Comprador();
        c.setProntuario(prontuario);
        return c;
    }

    @Test
    void compradorDesativadoSoVeOAvisoEAsAdvertencias() throws Exception {
        AuthFilter comDesativado = new AuthFilter(p -> new model.EstadoComprador(false, "V1", "Cantina", 1));
        HttpSession sessao = sessao("comprador", compradorCom("CJ9"), Database.geracao());
        FilterChain cadeia = mock(FilterChain.class);

        // paginas vao para o aviso, inclusive sessoes abertas antes da desativacao
        for (String pagina : new String[] { "/home.jsp", "/Carrinho.jsp", "/meus-pedidos.jsp", "/conta.jsp" }) {
            HttpServletResponse res = resposta(new StringWriter());
            comDesativado.doFilter(requisicao("GET", pagina, sessao), res, cadeia);
            verify(res).sendRedirect("/conta-desativada.jsp");
        }
        // APIs: 403 com a marca "desativada"
        for (String[] api : new String[][] { { "GET", "/apiUser/produtos" }, { "POST", "/carrinho" },
                { "POST", "/apiUser/vinculo" }, { "POST", "/FinalizarCompraServlet" }, { "POST", "/apiUser/pedidos/cancelar" } }) {
            StringWriter corpo = new StringWriter();
            HttpServletResponse res = resposta(corpo);
            comDesativado.doFilter(requisicao(api[0], api[1], sessao), res, cadeia);
            if (api[1].equals("/FinalizarCompraServlet")) {
                verify(res).sendRedirect("/conta-desativada.jsp");
            } else {
                verify(res).setStatus(403);
                assertTrue(corpo.toString().contains("\"desativada\":true"), api[1]);
            }
        }
        verifyNoInteractions(cadeia);

        // o aviso e a lista de advertencias continuam acessiveis
        HttpServletRequest aviso = requisicao("GET", "/conta-desativada.jsp", sessao);
        HttpServletResponse resAviso = resposta(new StringWriter());
        comDesativado.doFilter(aviso, resAviso, cadeia);
        verify(cadeia).doFilter(aviso, resAviso);
        HttpServletRequest lista = requisicao("GET", "/apiUser/advertencias", sessao);
        HttpServletResponse resLista = resposta(new StringWriter());
        comDesativado.doFilter(lista, resLista, cadeia);
        verify(cadeia).doFilter(lista, resLista);
        // marcar como lidas nao
        HttpServletResponse resLidas = resposta(new StringWriter());
        comDesativado.doFilter(requisicao("POST", "/apiUser/advertencias/lidas", sessao), resLidas, cadeia);
        verify(resLidas).setStatus(403);
    }

    @Test
    void compradorAtivoNaPaginaDeContaDesativadaVoltaParaOCatalogo() throws Exception {
        HttpSession sessao = sessao("comprador", compradorCom("CJ8"), Database.geracao());
        FilterChain cadeia = mock(FilterChain.class);
        HttpServletResponse res = resposta(new StringWriter());
        filtro.doFilter(requisicao("GET", "/conta-desativada.jsp", sessao), res, cadeia);
        verify(res).sendRedirect("/home.jsp");
        verifyNoInteractions(cadeia);
    }

    @Test
    void contaQueNaoExisteMaisEInvalidada() throws Exception {
        AuthFilter semConta = new AuthFilter(p -> null);
        HttpSession sessao = sessao("comprador", compradorCom("SUMIU"), Database.geracao());
        FilterChain cadeia = mock(FilterChain.class);
        HttpServletResponse res = resposta(new StringWriter());
        semConta.doFilter(requisicao("GET", "/home.jsp", sessao), res, cadeia);
        verify(sessao).invalidate();
        verify(res).sendRedirect("/login.jsp?restaurada=1");
        verifyNoInteractions(cadeia);
    }
}
