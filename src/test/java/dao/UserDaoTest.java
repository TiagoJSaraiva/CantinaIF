package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Comprador;
import utils.Database;

class UserDaoTest {

    private final UserDao dao = new UserDao();

    @BeforeAll
    static void iniciar() {
        TestDb.start(UserDaoTest.class);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @Test
    void cadastraComSenhaEmHashEAutenticaComSenhaCerta() throws Exception {
        assertTrue(dao.create(new Comprador("CJ1", "a@x.test", "senha1234", "Ana")));

        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT senha FROM Comprador WHERE prontuario = 'CJ1'");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            String guardada = rs.getString(1);
            assertNotEquals("senha1234", guardada);
            assertTrue(guardada.startsWith("$2"), "deve ser hash BCrypt");
        }

        Comprador logado = dao.autenticar("CJ1", "senha1234");
        assertNotNull(logado);
        assertEquals("Ana", logado.getNome());
        assertNull(logado.getSenha(), "a senha nao deve sair do DAO");
    }

    @Test
    void rejeitaSenhaErradaEUsuarioInexistente() throws Exception {
        dao.create(new Comprador("CJ2", "b@x.test", "senha1234", "Bia"));
        assertNull(dao.autenticar("CJ2", "errada123"));
        assertNull(dao.autenticar("NAOEXISTE", "senha1234"));
        assertNull(dao.autenticar("CJ2", null));
    }

    @Test
    void duplicidadeDeProntuarioOuEmailRetornaFalse() throws Exception {
        assertTrue(dao.create(new Comprador("CJ3", "c@x.test", "senha1234", "Caio")));
        assertFalse(dao.create(new Comprador("CJ3", "outro@x.test", "senha1234", "Outro")));
        assertFalse(dao.create(new Comprador("CJ4", "c@x.test", "senha1234", "Outro")));
    }

    @Test
    void contaNovaNasceAtivaSemVinculoELoginTrazOVinculo() throws Exception {
        assertTrue(dao.create(new Comprador("CJ5", "e@x.test", "senha1234", "Eva")));
        var estado = dao.estado("CJ5");
        assertTrue(estado.ativo());
        assertFalse(estado.vinculado());
        assertNull(dao.estado("NAOEXISTE"));

        TestDb.vendedor("VUSR");
        TestDb.vincular("CJ5", "VUSR");
        Comprador logado = dao.autenticar("CJ5", "senha1234");
        assertEquals("VUSR", logado.getVendedorProntuario());
        assertEquals("Cantina VUSR", logado.getVendedorNome());
        assertEquals("Cantina VUSR", dao.estado("CJ5").vendedorNome());
    }

    @Test
    void prontuarioNaoPodeRepetirEntreCompradorEVendedor() throws Exception {
        dao.create(new Comprador("CJ6", "f@x.test", "senha1234", "Fabi"));
        TestDb.vendedor("VUSR2");
        assertTrue(UserDao.prontuarioEmUso("CJ6"));
        assertTrue(UserDao.prontuarioEmUso("VUSR2"));
        assertFalse(UserDao.prontuarioEmUso("LIVRE1"));
    }
}
