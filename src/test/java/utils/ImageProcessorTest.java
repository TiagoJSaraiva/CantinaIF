package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class ImageProcessorTest {

    private static byte[] codificar(BufferedImage img, String formato) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, formato, out));
        return out.toByteArray();
    }

    private static BufferedImage ruido(int w, int h, int tipo) {
        BufferedImage img = new BufferedImage(w, h, tipo);
        Random r = new Random(42);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, 0xFF000000 | r.nextInt(0xFFFFFF));
            }
        }
        return img;
    }

    private static BufferedImage ler(byte[] bytes) throws Exception {
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    @Test
    void imagemGrandeEReduzidaParaNoMaximo640pxE150KB() throws Exception {
        byte[] original = codificar(ruido(2500, 1900, BufferedImage.TYPE_INT_RGB), "png"); // ruido: dificil de comprimir
        byte[] saida = ImageProcessor.processar(original);

        assertTrue(saida.length <= ImageProcessor.BYTES_MAXIMOS, "tamanho: " + saida.length);
        assertEquals("image/jpeg", ImageType.detect(saida));
        BufferedImage img = ler(saida);
        assertTrue(Math.max(img.getWidth(), img.getHeight()) <= ImageProcessor.LADO_MAXIMO);
        assertTrue(Math.max(img.getWidth(), img.getHeight()) > 100, "nao pode encolher demais");
    }

    @Test
    void imagemPequenaNaoEAmpliada() throws Exception {
        BufferedImage img = ler(ImageProcessor.processar(codificar(ruido(80, 60, BufferedImage.TYPE_INT_RGB), "png")));
        assertEquals(80, img.getWidth());
        assertEquals(60, img.getHeight());
    }

    @Test
    void pngComTransparenciaViraJpegSobreFundoBranco() throws Exception {
        BufferedImage transparente = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB); // tudo transparente
        BufferedImage img = ler(ImageProcessor.processar(codificar(transparente, "png")));
        int rgb = img.getRGB(25, 25) & 0xFFFFFF;
        assertTrue((rgb >> 16) > 240 && ((rgb >> 8) & 0xFF) > 240 && (rgb & 0xFF) > 240, "esperado branco, veio " + Integer.toHexString(rgb));
    }

    @Test
    void gifEJpegSaoAceitos() throws Exception {
        assertNotNull(ImageProcessor.processar(codificar(ruido(100, 100, BufferedImage.TYPE_INT_RGB), "gif")));
        assertNotNull(ImageProcessor.processar(codificar(ruido(100, 100, BufferedImage.TYPE_INT_RGB), "jpeg")));
    }

    @Test
    void conteudoQueNaoDecodificaERejeitado() {
        assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar("isto nao e uma imagem".getBytes()));
        // cabecalho PNG valido, corpo truncado
        byte[] truncado = new byte[64];
        byte[] cab = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
        System.arraycopy(cab, 0, truncado, 0, cab.length);
        assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar(truncado));
        assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar(null));
    }

    @Test
    void webpNaoESuportado() {
        byte[] webp = "RIFF0000WEBPVP8 0000".getBytes();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar(webp));
        assertTrue(e.getMessage().contains("WebP"));
    }

    @Test
    void imagemComPixelsDemaisERejeitadaSemDecodificar() throws Exception {
        BufferedImage enorme = new BufferedImage(5200, 5200, BufferedImage.TYPE_BYTE_GRAY); // ~27 MP
        byte[] png = codificar(enorme, "png");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar(png));
        assertTrue(e.getMessage().contains("dimensões"));
    }

    @Test
    void bombaDePixelsDe4999x4999EhRejeitadaPeloCabecalhoSemDecodificar() throws Exception {
        BufferedImage bomba = new BufferedImage(4999, 4999, BufferedImage.TYPE_BYTE_GRAY); // ~25 MP, PNG minusculo
        byte[] png = codificar(bomba, "png");
        assertTrue(png.length < 200 * 1024, "o PNG e pequeno: " + png.length);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar(png));
        assertTrue(e.getMessage().contains("5 megapixels"));
    }

    @Test
    void imagemDe4_8MegapixelsEAceitaEReduzida() throws Exception {
        BufferedImage img = ruido(2400, 2000, BufferedImage.TYPE_INT_RGB);
        byte[] saida = ImageProcessor.processar(codificar(img, "png"));
        BufferedImage lida = ler(saida);
        assertTrue(Math.max(lida.getWidth(), lida.getHeight()) <= ImageProcessor.LADO_MAXIMO);
        assertTrue(saida.length <= ImageProcessor.BYTES_MAXIMOS);
    }

    @Test
    void apenasUmProcessamentoPorVezOutrosRecebemOcupado() throws Exception {
        long esperaOriginal = ImageProcessor.esperaMs;
        ImageProcessor.esperaMs = 150;
        try {
            byte[] png = codificar(ruido(1800, 1800, BufferedImage.TYPE_INT_RGB), "png");
            int n = 6;
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(n);
            java.util.concurrent.CountDownLatch largada = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicInteger ok = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger ocupados = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger emParalelo = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger maximo = new java.util.concurrent.atomic.AtomicInteger();
            for (int i = 0; i < n; i++) {
                pool.submit(() -> {
                    largada.await();
                    try {
                        ImageProcessor.processar(png);
                        ok.incrementAndGet();
                    } catch (ImageProcessor.OcupadoException e) {
                        ocupados.incrementAndGet();
                    }
                    return null;
                });
            }
            largada.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(120, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(n, ok.get() + ocupados.get());
            assertTrue(ok.get() >= 1);
            assertTrue(ocupados.get() >= 1, "com espera curta, alguns precisam receber 'ocupado'");
        } finally {
            ImageProcessor.esperaMs = esperaOriginal;
        }
    }

    @Test
    void semaforoLiberadoMesmoQuandoAImagemEInvalida() {
        for (int i = 0; i < 5; i++) {
            assertThrows(IllegalArgumentException.class, () -> ImageProcessor.processar("lixo".getBytes()));
        }
        assertDoesNotThrow(() -> ImageProcessor.processar(codificar(ruido(50, 50, BufferedImage.TYPE_INT_RGB), "png")));
    }
}
