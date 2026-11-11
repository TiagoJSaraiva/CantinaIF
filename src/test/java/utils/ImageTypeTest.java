package utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class ImageTypeTest {

    private static byte[] bytes(int... v) {
        byte[] b = new byte[16];
        for (int i = 0; i < v.length; i++) {
            b[i] = (byte) v[i];
        }
        return b;
    }

    @Test
    void reconheceFormatosAceitos() {
        assertEquals("image/jpeg", ImageType.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals("image/png", ImageType.detect(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals("image/gif", ImageType.detect("GIF89a0000000000".getBytes(StandardCharsets.US_ASCII)));
        assertEquals("image/webp", ImageType.detect("RIFF0000WEBP0000".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void rejeitaOutrosConteudos() {
        assertNull(ImageType.detect("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageType.detect("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(StandardCharsets.UTF_8)));
        assertNull(ImageType.detect(new byte[3]));
        assertNull(ImageType.detect(null));
    }
}
