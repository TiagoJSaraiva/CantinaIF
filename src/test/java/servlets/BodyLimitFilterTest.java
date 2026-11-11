package servlets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class BodyLimitFilterTest {

    private final BodyLimitFilter filtro = new BodyLimitFilter();

    private static ServletInputStream fluxoDe(byte[] dados) {
        ByteArrayInputStream in = new ByteArrayInputStream(dados);
        return new ServletInputStream() {
            @Override public int read() { return in.read(); }
            @Override public int read(byte[] b, int off, int len) { return in.read(b, off, len); }
            @Override public boolean isFinished() { return in.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener l) { }
        };
    }

    private static HttpServletRequest req(String caminho, long contentLength, byte[] corpo) throws Exception {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getServletPath()).thenReturn(caminho);
        when(r.getContentLengthLong()).thenReturn(contentLength);
        when(r.getInputStream()).thenReturn(fluxoDe(corpo));
        return r;
    }

    private static HttpServletResponse resp(StringWriter saida) throws Exception {
        HttpServletResponse r = mock(HttpServletResponse.class);
        when(r.getWriter()).thenReturn(new PrintWriter(saida));
        return r;
    }

    @Test
    void contentLengthAcimaDoTetoRecebe413SemLerOCorpo() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        StringWriter saida = new StringWriter();
        HttpServletResponse res = resp(saida);
        HttpServletRequest r = req("/carrinho", 300L * 1024 * 1024, new byte[0]);

        filtro.doFilter(r, res, cadeia);

        verify(res).setStatus(413);
        verify(res).setHeader("Connection", "close");
        verifyNoInteractions(cadeia);
        verify(r, never()).getInputStream();
        assertTrue(saida.toString().contains("\"success\":false"));
    }

    @Test
    void rotasDeUploadTemTetoMaiorQueAsDemais() {
        assertEquals(BodyLimitFilter.LIMITE_PADRAO, BodyLimitFilter.limiteDe("/carrinho"));
        assertEquals(BodyLimitFilter.LIMITE_PADRAO, BodyLimitFilter.limiteDe("/login"));
        assertEquals(BodyLimitFilter.LIMITE_UPLOAD, BodyLimitFilter.limiteDe("/registrar-produto"));
        assertEquals(BodyLimitFilter.LIMITE_UPLOAD, BodyLimitFilter.limiteDe("/editar-produto"));
        assertTrue(BodyLimitFilter.LIMITE_UPLOAD > 2 * 1024 * 1024);
        assertTrue(BodyLimitFilter.LIMITE_UPLOAD < 3 * 1024 * 1024);
    }

    @Test
    void uploadAcimaDoTetoRecebe413ComErroDeImagem() throws Exception {
        FilterChain cadeia = mock(FilterChain.class);
        StringWriter saida = new StringWriter();
        HttpServletResponse res = resp(saida);
        filtro.doFilter(req("/registrar-produto", 5L * 1024 * 1024, new byte[0]), res, cadeia);
        verify(res).setStatus(413);
        assertTrue(saida.toString().contains("\"imagem\""));
        verifyNoInteractions(cadeia);
    }

    @Test
    void corpoDentroDoTetoPassaIntacto() throws Exception {
        byte[] dados = "{\"produtoId\":1}".getBytes();
        HttpServletRequest r = req("/carrinho", dados.length, dados);
        FilterChain cadeia = mock(FilterChain.class);
        doAnswer(inv -> {
            ServletRequest recebida = inv.getArgument(0);
            assertArrayEquals(dados, recebida.getInputStream().readAllBytes());
            return null;
        }).when(cadeia).doFilter(any(), any());

        filtro.doFilter(r, resp(new StringWriter()), cadeia);
        verify(cadeia).doFilter(any(), any());
    }

    @Test
    void semContentLengthOFluxoAbortaAoPassarDoTeto() throws Exception {
        byte[] enorme = new byte[(int) BodyLimitFilter.LIMITE_PADRAO + 100]; // chunked: Content-Length = -1
        HttpServletRequest r = req("/carrinho", -1, enorme);
        FilterChain cadeia = mock(FilterChain.class);
        doAnswer(inv -> {
            ServletRequest recebida = inv.getArgument(0);
            assertThrows(BodyLimitFilter.CorpoGrandeDemaisException.class, () -> recebida.getInputStream().readAllBytes());
            return null;
        }).when(cadeia).doFilter(any(), any());

        filtro.doFilter(r, resp(new StringWriter()), cadeia);
        verify(cadeia).doFilter(any(), any());
    }

    @Test
    void readerTambemEContado() throws Exception {
        byte[] enorme = new byte[(int) BodyLimitFilter.LIMITE_PADRAO + 1];
        java.util.Arrays.fill(enorme, (byte) 'a');
        HttpServletRequest r = req("/carrinho", -1, enorme);
        FilterChain cadeia = mock(FilterChain.class);
        doAnswer(inv -> {
            ServletRequest recebida = inv.getArgument(0);
            IOException e = assertThrows(IOException.class, () -> {
                var reader = recebida.getReader();
                char[] buf = new char[4096];
                while (reader.read(buf) >= 0) {
                    // consome
                }
            });
            assertTrue(e instanceof BodyLimitFilter.CorpoGrandeDemaisException);
            return null;
        }).when(cadeia).doFilter(any(), any());

        filtro.doFilter(r, resp(new StringWriter()), cadeia);
        verify(cadeia).doFilter(any(), any());
    }
}
