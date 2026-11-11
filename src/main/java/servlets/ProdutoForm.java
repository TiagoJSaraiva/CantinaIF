package servlets;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import model.Produto;
import utils.ImageProcessor;
import utils.ImageType;

/** Leitura e validacao (no servidor) dos campos de produto enviados pelo painel do vendedor. */
final class ProdutoForm {

    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");
    private static final int ESTOQUE_MAXIMO = 1_000_000;

    /** Mensagens de erro por campo (vazio quando valido). */
    final Map<String, String> erros = new LinkedHashMap<>();
    final Produto produto = new Produto();

    private ProdutoForm() {
    }

    /**
     * Se o corpo multipart passar dos limites (ex.: imagem acima de 2 MB) o container descarta todos os
     * parametros; detecta isso cedo para responder com a mensagem correta. Retorna null se esta tudo bem.
     */
    static Map<String, String> verificarLimiteDoCorpo(HttpServletRequest request) throws IOException, ServletException {
        String tipo = request.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("multipart/")) {
            return null;
        }
        try {
            request.getParts();
            return null;
        } catch (IllegalStateException e) {
            Map<String, String> erros = new LinkedHashMap<>();
            erros.put("imagem", "A imagem deve ter no máximo 2 MB.");
            return erros;
        }
    }

    static ProdutoForm ler(HttpServletRequest request) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        ProdutoForm f = new ProdutoForm();
        Produto p = f.produto;

        String nome = valor(request, "nome");
        if (nome.isEmpty()) {
            f.erros.put("nome", "Informe o nome do produto.");
        } else if (nome.length() > 100) {
            f.erros.put("nome", "O nome deve ter no máximo 100 caracteres.");
        }
        p.setNome(nome);

        String descricao = valor(request, "descricao");
        if (descricao.length() > 400) {
            f.erros.put("descricao", "A descrição deve ter no máximo 400 caracteres.");
        }
        p.setDescricao(descricao);

        try {
            BigDecimal preco = new BigDecimal(valor(request, "preco").replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
            if (preco.signum() < 0 || preco.compareTo(PRECO_MAXIMO) > 0) {
                f.erros.put("preco", "Informe um preço entre 0,00 e 99.999.999,99.");
            }
            p.setPreco(preco);
        } catch (NumberFormatException e) {
            f.erros.put("preco", "Informe um preço válido, por exemplo 6,50.");
        }

        try {
            int estoque = Integer.parseInt(valor(request, "estoque"));
            if (estoque < 0 || estoque > ESTOQUE_MAXIMO) {
                f.erros.put("estoque", "O estoque deve estar entre 0 e " + ESTOQUE_MAXIMO + ".");
            }
            p.setEstoque(estoque);
        } catch (NumberFormatException e) {
            f.erros.put("estoque", "Informe o estoque como número inteiro.");
        }

        try {
            p.setCategoria(Produto.Categoria.valueOf(valor(request, "categoria").toUpperCase()));
        } catch (IllegalArgumentException e) {
            f.erros.put("categoria", "Escolha uma categoria.");
        }

        f.lerImagem(request);
        return f;
    }

    private void lerImagem(HttpServletRequest request) throws IOException, ServletException {
        String tipo = request.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("multipart/")) {
            return;
        }
        Part parte;
        try {
            parte = request.getPart("imagem");
        } catch (IllegalStateException e) {
            erros.put("imagem", "A imagem deve ter no máximo 2 MB.");
            return;
        }
        if (parte == null || parte.getSize() == 0) {
            return;
        }
        if (parte.getSize() > ImageType.MAX_BYTES) {
            erros.put("imagem", "A imagem deve ter no máximo 2 MB.");
            return;
        }
        byte[] bytes;
        try (InputStream in = parte.getInputStream()) {
            bytes = in.readAllBytes();
        }
        try {
            // redimensiona e recomprime (JPEG, ate 640 px e 150 KB); rejeita o que nao decodifica
            produto.setImagem(ImageProcessor.processar(bytes));
        } catch (IllegalArgumentException e) {
            erros.put("imagem", e.getMessage());
        }
    }

    private static String valor(HttpServletRequest request, String nome) {
        String v = request.getParameter(nome);
        return v == null ? "" : v.trim();
    }
}
