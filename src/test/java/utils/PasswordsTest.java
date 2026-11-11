package utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordsTest {

    @Test
    void hashNaoEhASenhaEUsaSaltAleatorio() {
        String h1 = Passwords.hash("segredo123");
        String h2 = Passwords.hash("segredo123");
        assertNotEquals("segredo123", h1);
        assertNotEquals(h1, h2);
        assertEquals(60, h1.length());
    }

    @Test
    void verificaSenhaCertaEErrada() {
        String h = Passwords.hash("segredo123");
        assertTrue(Passwords.verify("segredo123", h));
        assertFalse(Passwords.verify("Segredo123", h));
        assertFalse(Passwords.verify("", h));
    }

    @Test
    void entradasNulasOuHashInvalidoNaoLancamExcecao() {
        assertFalse(Passwords.verify(null, "x"));
        assertFalse(Passwords.verify("a", null));
        assertFalse(Passwords.verify("a", "senha-em-texto-puro"));
    }
}
