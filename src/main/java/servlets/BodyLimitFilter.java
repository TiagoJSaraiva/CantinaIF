package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import utils.ImageType;

/**
 * Primeiro filtro da cadeia: limite de tamanho do corpo de TODA requisicao.
 * <ul>
 *   <li>Com {@code Content-Length} acima do teto da rota: responde 413 sem ler o corpo.</li>
 *   <li>Sem {@code Content-Length} (chunked) ou com valor mentiroso: o {@code InputStream}/{@code Reader} entregue
 *       aos servlets conta os bytes e aborta ao passar do teto.</li>
 * </ul>
 * Tetos: 16 KB (JSON e formularios) e cerca de 2,1 MB nas rotas de upload de imagem. O que e lido pelo
 * proprio Tomcat (parametros de formulario e multipart) tem os limites do conector ({@code maxPostSize}) e do
 * {@code @MultipartConfig}.
 */
public class BodyLimitFilter implements Filter {

    public static final long LIMITE_PADRAO = 16 * 1024;
    public static final long LIMITE_UPLOAD = ImageType.MAX_BYTES + 128 * 1024;

    private static final Set<String> ROTAS_DE_UPLOAD = Set.of("/registrar-produto", "/editar-produto");

    /** Teto do corpo para o caminho informado. */
    public static long limiteDe(String caminho) {
        return ROTAS_DE_UPLOAD.contains(caminho) ? LIMITE_UPLOAD : LIMITE_PADRAO;
    }

    /** Lancada quando o corpo lido passa do teto (sem Content-Length confiavel). */
    public static class CorpoGrandeDemaisException extends IOException {
        private static final long serialVersionUID = 1L;

        public CorpoGrandeDemaisException(long limite) {
            super("Corpo da requisição acima do limite de " + limite + " bytes.");
        }
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String caminho = request.getServletPath();
        long limite = limiteDe(caminho);
        if (request.getContentLengthLong() > limite) {
            recusar(response, caminho, limite);
            return;
        }
        chain.doFilter(new RequisicaoLimitada(request, limite), res);
    }

    static void recusar(HttpServletResponse response, String caminho, long limite) throws IOException {
        response.setHeader("Connection", "close"); // o corpo nao sera lido: fecha a conexao
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("success", false);
        if (ROTAS_DE_UPLOAD.contains(caminho)) {
            corpo.put("message", "A imagem deve ter no máximo 2 MB.");
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("imagem", "A imagem deve ter no máximo 2 MB.");
            corpo.put("erros", erros);
        } else {
            corpo.put("message", "Requisição grande demais (máximo " + limite + " bytes).");
        }
        Json.write(response, 413, corpo);
    }

    /** Requisicao cujo corpo (InputStream/Reader) aborta ao passar do teto. */
    static final class RequisicaoLimitada extends HttpServletRequestWrapper {
        private final long limite;
        private ServletInputStream fluxo;

        RequisicaoLimitada(HttpServletRequest request, long limite) {
            super(request);
            this.limite = limite;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (fluxo == null) {
                fluxo = new FluxoContado(super.getInputStream(), limite);
            }
            return fluxo;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String enc = getCharacterEncoding();
            Charset cs = enc == null ? StandardCharsets.UTF_8 : Charset.forName(enc);
            return new BufferedReader(new InputStreamReader(getInputStream(), cs));
        }
    }

    static final class FluxoContado extends ServletInputStream {
        private final ServletInputStream origem;
        private final long limite;
        private long lidos;

        FluxoContado(ServletInputStream origem, long limite) {
            this.origem = origem;
            this.limite = limite;
        }

        private void contar(long n) throws IOException {
            if (n > 0) {
                lidos += n;
                if (lidos > limite) {
                    throw new CorpoGrandeDemaisException(limite);
                }
            }
        }

        @Override
        public int read() throws IOException {
            int b = origem.read();
            if (b >= 0) {
                contar(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = origem.read(buf, off, len);
            contar(n);
            return n;
        }

        @Override
        public boolean isFinished() {
            return origem.isFinished();
        }

        @Override
        public boolean isReady() {
            return origem.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            origem.setReadListener(listener);
        }

        @Override
        public void close() throws IOException {
            origem.close();
        }
    }
}
