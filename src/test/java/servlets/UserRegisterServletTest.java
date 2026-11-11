package servlets;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserRegisterServletTest {

    @Test
    void aceitaDadosValidos() {
        assertNull(UserRegisterServlet.validar("CJ123", "a@b.co", "Ana", "senha1234"));
    }

    @Test
    void rejeitaDadosInvalidos() {
        assertNotNull(UserRegisterServlet.validar("", "a@b.co", "Ana", "senha1234"));
        assertNotNull(UserRegisterServlet.validar("CJ<script>", "a@b.co", "Ana", "senha1234"));
        assertNotNull(UserRegisterServlet.validar("CJ12345678901", "a@b.co", "Ana", "senha1234"));
        assertNotNull(UserRegisterServlet.validar("CJ1", "sem-arroba", "Ana", "senha1234"));
        assertNotNull(UserRegisterServlet.validar("CJ1", "a@b.co", "", "senha1234"));
        assertNotNull(UserRegisterServlet.validar("CJ1", "a@b.co", "Ana", "curta"));
        assertNotNull(UserRegisterServlet.validar("CJ1", "a@b.co", "Ana", null));
    }
}
