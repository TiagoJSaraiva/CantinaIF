package dao;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Vendedor;
import utils.Database;

class VendedorDaoTest {

    private final VendedorDao dao = new VendedorDao();

    @BeforeAll
    static void iniciar() {
        TestDb.start(VendedorDaoTest.class);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @Test
    void vendedorDemoCriadoNoBootAutentica() throws Exception {
        Vendedor v = dao.autenticar(Database.DEMO_VENDEDOR_PRONTUARIO, Database.DEMO_VENDEDOR_SENHA);
        assertNotNull(v);
        assertEquals(Database.DEMO_VENDEDOR_EMAIL, v.getEmail());
        assertNull(v.getSenha());
    }

    @Test
    void senhaErradaOuCredencialAntigaFixaNaoAutentica() throws Exception {
        assertNull(dao.autenticar(Database.DEMO_VENDEDOR_PRONTUARIO, "errada"));
        assertNull(dao.autenticar("REMOVIDO", "REMOVIDO"));
    }

    @Test
    void criaNovoVendedorComHash() throws Exception {
        assertTrue(dao.create("VEND9", "v9@x.test", "outrasenha1"));
        assertFalse(dao.create("VEND9", "v9b@x.test", "outrasenha1"));
        assertNotNull(dao.autenticar("VEND9", "outrasenha1"));
    }
}
