package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Advertencia;
import model.Comprador;

class AdvertenciaDaoTest {

    private static final AdvertenciaDao dao = new AdvertenciaDao();
    private static final UserDao usuarios = new UserDao();
    private static final LocalDateTime T0 = LocalDateTime.of(2030, 4, 2, 10, 0);
    private static final String V = "VADV";
    private static final String OUTRO = "VADV2";
    private static int seq;

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(AdvertenciaDaoTest.class);
        TestDb.vendedor(V);
        TestDb.vendedor(OUTRO);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
        System.clearProperty("CANTINA_MAX_ADVERTENCIAS_POR_COMPRADOR");
    }

    private static String comprador(String vendedor) throws Exception {
        String p = "CA" + (++seq);
        assertTrue(usuarios.create(new Comprador(p, p + "@x.test", "senha1234", "Comprador " + seq)));
        TestDb.vincular(p, vendedor);
        return p;
    }

    @Test
    void vendedorAdverteCompradorQueVeAsNaoLidasAteMarcarComoLidas() throws Exception {
        String c = comprador(V);
        dao.criar(c, V, "  Cancelamentos estão frequentes.  ", T0);
        dao.criar(c, V, "Segunda advertência", T0.plusMinutes(1));

        assertEquals(2, usuarios.estado(c).naoLidas());
        List<Advertencia> lista = dao.listarDoComprador(c);
        assertEquals(2, lista.size());
        assertEquals("Segunda advertência", lista.get(0).mensagem(), "mais recente primeiro");
        assertEquals("Cancelamentos estão frequentes.", lista.get(1).mensagem(), "espacos das pontas removidos");
        assertEquals("Cantina " + V, lista.get(0).vendedorNome());
        assertFalse(lista.get(0).lida());

        assertEquals(2, dao.marcarLidas(c, T0.plusMinutes(2)));
        assertEquals(0, usuarios.estado(c).naoLidas());
        assertTrue(dao.listarDoComprador(c).stream().allMatch(Advertencia::lida));
        assertEquals(0, dao.marcarLidas(c, T0.plusMinutes(3)));
    }

    @Test
    void soAdverteCompradorVinculadoAoProprioVendedor() throws Exception {
        String deOutro = comprador(OUTRO);
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> dao.criar(deOutro, V, "Olá", T0));
        assertTrue(e.getMessage().contains("não encontrado"));
        assertThrows(RegraNegocioException.class, () -> dao.criar("NAOEXISTE", V, "Olá", T0));
        assertTrue(dao.listarDoComprador(deOutro).isEmpty());
    }

    @Test
    void mensagemTemTamanhoValidado() throws Exception {
        String c = comprador(V);
        for (String ruim : new String[] { null, "", "  ", "ok", "x".repeat(AdvertenciaDao.MENSAGEM_MAXIMA + 1) }) {
            assertThrows(RegraNegocioException.class, () -> dao.criar(c, V, ruim, T0));
        }
        dao.criar(c, V, "x".repeat(AdvertenciaDao.MENSAGEM_MAXIMA), T0);
    }

    @Test
    void tetoDeAdvertenciasPorComprador() throws Exception {
        String c = comprador(V);
        System.setProperty("CANTINA_MAX_ADVERTENCIAS_POR_COMPRADOR", "2");
        try {
            dao.criar(c, V, "primeira", T0);
            dao.criar(c, V, "segunda", T0);
            assertThrows(LimiteAtingidoException.class, () -> dao.criar(c, V, "terceira", T0));
        } finally {
            System.clearProperty("CANTINA_MAX_ADVERTENCIAS_POR_COMPRADOR");
        }
    }

    @Test
    void listaDoVendedorTrazSoSeusCompradoresComOsNumeros() throws Exception {
        String c = comprador(V);
        String deOutro = comprador(OUTRO);
        dao.criar(c, V, "Atenção aos cancelamentos", T0);

        var lista = usuarios.listarDoVendedor(V);
        assertTrue(lista.stream().anyMatch(r -> r.prontuario().equals(c) && r.advertencias() >= 1 && r.ativo()));
        assertTrue(lista.stream().noneMatch(r -> r.prontuario().equals(deOutro)));

        assertFalse(usuarios.definirAtivo(deOutro, V, false), "nao desativa comprador de outro vendedor");
        assertTrue(usuarios.estado(deOutro).ativo());
        assertTrue(usuarios.definirAtivo(c, V, false));
        assertFalse(usuarios.estado(c).ativo());
        assertTrue(usuarios.listarDoVendedor(V).stream().anyMatch(r -> r.prontuario().equals(c) && !r.ativo()));
        assertTrue(usuarios.definirAtivo(c, V, true));
        assertTrue(usuarios.estado(c).ativo());
    }
}
