package utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Decodifica, redimensiona e recomprime toda imagem enviada: lado maior de ate {@value #LADO_MAXIMO} px,
 * saida JPEG de ate {@value #BYTES_MAXIMOS} bytes. Isso limita a memoria usada pelas imagens e descarta
 * metadados (EXIF etc.). O que nao decodifica e rejeitado.
 */
public final class ImageProcessor {

    public static final int LADO_MAXIMO = 640;
    public static final int BYTES_MAXIMOS = 150 * 1024;
    /**
     * Protecao contra "bombas de descompressao": a largura e a altura sao lidas do cabecalho ANTES de decodificar e
     * imagens com mais de ~5 megapixels sao rejeitadas. As que passam sao decodificadas com subamostragem, ja perto
     * do tamanho final, e so uma imagem e processada por vez em toda a aplicacao.
     */
    public static final long PIXELS_MAXIMOS = 5_000_000L;

    /** Tempo maximo de espera pela vez de processar (ms); acima disso a requisicao recebe "ocupado". */
    static volatile long esperaMs = 3000;

    private static final java.util.concurrent.Semaphore SEMAFORO = new java.util.concurrent.Semaphore(1, true);

    /** Ha outro processamento de imagem em andamento e a vez nao chegou a tempo. */
    public static class OcupadoException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public OcupadoException() {
            super("Muitos envios de imagem ao mesmo tempo; tente novamente em instantes.");
        }
    }

    private static final float[] QUALIDADES = { 0.85f, 0.7f, 0.55f, 0.4f, 0.3f };

    private ImageProcessor() {
    }

    /** @throws IllegalArgumentException com mensagem apropriada para o usuario se a imagem nao for aceitavel */
    public static byte[] processar(byte[] entrada) {
        String tipo = ImageType.detect(entrada);
        if (tipo == null) {
            throw new IllegalArgumentException("Envie uma imagem JPEG, PNG ou GIF válida.");
        }
        if (tipo.equals("image/webp")) {
            throw new IllegalArgumentException("Imagens WebP não são suportadas; envie JPEG, PNG ou GIF.");
        }
        boolean obteve;
        try {
            obteve = SEMAFORO.tryAcquire(esperaMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcupadoException();
        }
        if (!obteve) {
            throw new OcupadoException();
        }
        try {
            return processarExclusivo(entrada);
        } finally {
            SEMAFORO.release();
        }
    }

    private static byte[] processarExclusivo(byte[] entrada) {
        BufferedImage origem = decodificar(entrada);
        BufferedImage atual = reduzir(origem, LADO_MAXIMO);
        try {
            for (int tentativa = 0; tentativa < 8; tentativa++) {
                for (float q : QUALIDADES) {
                    byte[] jpeg = codificar(atual, q);
                    if (jpeg.length <= BYTES_MAXIMOS) {
                        return jpeg;
                    }
                }
                int novoLado = (int) (Math.max(atual.getWidth(), atual.getHeight()) * 0.8);
                if (novoLado < 32) {
                    break;
                }
                atual = reduzir(atual, novoLado);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível processar a imagem.", e);
        }
        throw new IllegalArgumentException("Não foi possível reduzir a imagem para o tamanho permitido.");
    }

    private static BufferedImage decodificar(byte[] entrada) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(entrada))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(in);
            if (!leitores.hasNext()) {
                throw new IllegalArgumentException("Não foi possível ler a imagem enviada.");
            }
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(in, true, true);
                long largura = leitor.getWidth(0);
                long altura = leitor.getHeight(0);
                if (largura <= 0 || altura <= 0 || largura * altura > PIXELS_MAXIMOS) {
                    throw new IllegalArgumentException("A imagem tem dimensões grandes demais (máximo de 5 megapixels).");
                }
                // subamostragem: decodifica ja perto do tamanho final (lado >= LADO_MAXIMO), sem materializar tudo
                int fator = (int) Math.max(1, Math.max(largura, altura) / LADO_MAXIMO);
                ImageReadParam param = leitor.getDefaultReadParam();
                param.setSourceSubsampling(fator, fator, 0, 0);
                BufferedImage img = leitor.read(0, param);
                if (img == null) {
                    throw new IllegalArgumentException("Não foi possível ler a imagem enviada.");
                }
                return img;
            } finally {
                leitor.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e;
            }
            throw new IllegalArgumentException("Não foi possível ler a imagem enviada.", e);
        }
    }

    /** Reduz (nunca amplia) para que o lado maior seja no maximo {@code ladoMaximo}; achata transparencia sobre branco. */
    private static BufferedImage reduzir(BufferedImage origem, int ladoMaximo) {
        int w = origem.getWidth();
        int h = origem.getHeight();
        double fator = Math.min(1.0, (double) ladoMaximo / Math.max(w, h));
        int nw = Math.max(1, (int) Math.round(w * fator));
        int nh = Math.max(1, (int) Math.round(h * fator));
        BufferedImage saida = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            if (fator < 0.5) {
                // reducao em etapas: evita serrilhado em reducoes grandes
                BufferedImage meio = origem;
                int mw = w, mh = h;
                while (mw / 2 >= nw && mh / 2 >= nh) {
                    mw /= 2;
                    mh /= 2;
                    BufferedImage passo = new BufferedImage(mw, mh, BufferedImage.TYPE_INT_RGB);
                    Graphics2D gp = passo.createGraphics();
                    gp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    gp.setColor(Color.WHITE);
                    gp.fillRect(0, 0, mw, mh);
                    gp.drawImage(meio, 0, 0, mw, mh, null);
                    gp.dispose();
                    meio = passo;
                }
                g.drawImage(meio, 0, 0, nw, nh, null);
            } else {
                g.drawImage(origem, 0, 0, nw, nh, null);
            }
        } finally {
            g.dispose();
        }
        return saida;
    }

    private static byte[] codificar(BufferedImage img, float qualidade) throws IOException {
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpeg");
        if (!escritores.hasNext()) {
            throw new IOException("Sem codificador JPEG");
        }
        ImageWriter escritor = escritores.next();
        try {
            ImageWriteParam param = escritor.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(qualidade);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(saida)) {
                escritor.setOutput(ios);
                escritor.write(null, new IIOImage(img, null, null), param);
            }
            return saida.toByteArray();
        } finally {
            escritor.dispose();
        }
    }
}
