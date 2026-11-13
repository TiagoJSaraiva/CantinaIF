package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Vendedor;
import seed.DemoDataSeeder;
import utils.Database;

class VendedorDaoTest {

    private final VendedorDao dao = new VendedorDao();

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(VendedorDaoTest.class);
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.run(c, LocalDateTime.of(2030, 1, 15, 10, 0));
        }
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @Test
    void vendedorDemoSemeadoAutentica() throws Exception {
        Vendedor v = dao.autenticar(DemoDataSeeder.VENDEDOR_PRONTUARIO, DemoDataSeeder.SENHA_DEMO);
        assertNotNull(v);
        assertEquals(DemoDataSeeder.VENDEDOR_EMAIL, v.getEmail());
        assertEquals(DemoDataSeeder.VENDEDOR_NOME, v.getNome());
        assertNull(v.getSenha());
    }

    @Test
    void senhaErradaOuCredencialAntigaFixaNaoAutentica() throws Exception {
        assertNull(dao.autenticar(DemoDataSeeder.VENDEDOR_PRONTUARIO, "errada"));
        assertNull(dao.autenticar("REMOVIDO", "REMOVIDO"));
    }

    @Test
    void criaNovoVendedorComHash() throws Exception {
        assertTrue(dao.create("VEND9", "Lanchonete Nove", "v9@x.test", "outrasenha1"));
        assertFalse(dao.create("VEND9", "Outra", "v9b@x.test", "outrasenha1"));
        assertFalse(dao.create("VEND10", "Outra", "v9@x.test", "outrasenha1"), "e-mail repetido");
        Vendedor v = dao.autenticar("VEND9", "outrasenha1");
        assertNotNull(v);
        assertEquals("Lanchonete Nove", v.getNome());
    }
}
