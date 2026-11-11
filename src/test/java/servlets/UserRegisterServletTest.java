package servlets;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserRegisterServletTest {

    @Test
    void aceitaDadosValidos() {
        assertTrue(UserRegisterServlet.validar("CJ123", "a@b.co", "Ana", "senha1234").isEmpty());
    }

    @Test
    void errosVemPorCampo() {
        var erros = UserRegisterServlet.validar("", "x", "", "1");
        assertEquals(java.util.Set.of("prontuario", "email", "nome", "senha"), erros.keySet());
        assertTrue(UserRegisterServlet.validar("CJ1", "a@b.co", "Ana", "curta").containsKey("senha"));
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertFalse(UserRegisterServlet.validar("", "a@b.co", "Ana", "senha1234").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ<script>", "a@b.co", "Ana", "senha1234").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ12345678901", "a@b.co", "Ana", "senha1234").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ1", "sem-arroba", "Ana", "senha1234").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ1", "a@b.co", "", "senha1234").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ1", "a@b.co", "Ana", "curta").isEmpty());
        assertFalse(UserRegisterServlet.validar("CJ1", "a@b.co", "Ana", null).isEmpty());
    }
}
