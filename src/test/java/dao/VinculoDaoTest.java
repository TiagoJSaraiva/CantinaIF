package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Comprador;

class VinculoDaoTest {

    private static final VinculoDao dao = new VinculoDao();
    private static final UserDao usuarios = new UserDao();
    private static final LocalDateTime T0 = LocalDateTime.of(2030, 3, 1, 9, 0, 0);
    private static final String A = "VVA";
    private static final String B = "VVB";
    private static int seq;

    @BeforeAll
    static void iniciar() throws Exception {
        TestDb.start(VinculoDaoTest.class);
        TestDb.vendedor(A);
        TestDb.vendedor(B);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    private static String comprador() throws Exception {
        String p = "CV" + (++seq);
        assertTrue(usuarios.create(new Comprador(p, p + "@x.test", "senha1234", "Comprador " + seq)));
        return p;
    }

    @Test
    void codigoTemFormatoCurtoSemCaracteresAmbiguosEExpiraEmUmMinuto() throws Exception {
        VinculoDao.Codigo c = dao.gerarCodigo(A, T0);
        assertEquals(VinculoDao.TAMANHO, c.codigo().length());
        assertTrue(c.codigo().chars().allMatch(ch -> VinculoDao.ALFABETO.indexOf(ch) >= 0), c.codigo());
        assertEquals(T0.plusSeconds(60), c.expiraEm());
    }

    @Test
    void compradorSeVinculaComCodigoValidoEPassaAVerOVendedor() throws Exception {
        String comprador = comprador();
        assertFalse(usuarios.estado(comprador).vinculado(), "conta nova nasce sem vinculo");

        VinculoDao.Codigo c = dao.gerarCodigo(A, T0);
        // digitado em minusculas, com espaco e hifen: normalizado
        String digitado = c.codigo().substring(0, 3).toLowerCase() + "- " + c.codigo().substring(3);
        VinculoDao.Vinculo v = dao.resgatar(digitado, comprador, T0.plusSeconds(30));
        assertEquals(A, v.vendedorProntuario());
        assertEquals("Cantina " + A, v.vendedorNome());
        assertFalse(v.trocou());
        assertEquals(A, usuarios.estado(comprador).vendedorProntuario());
    }

    @Test
    void codigoExpiradoNaoVale() throws Exception {
        String comprador = comprador();
        VinculoDao.Codigo c = dao.gerarCodigo(A, T0);
        assertThrows(VinculoDao.CodigoInvalidoException.class, () -> dao.resgatar(c.codigo(), comprador, T0.plusSeconds(60)));
        assertThrows(VinculoDao.CodigoInvalidoException.class, () -> dao.resgatar(c.codigo(), comprador, T0.plusMinutes(5)));
        assertNull(usuarios.estado(comprador).vendedorProntuario());
    }

    @Test
    void codigoEDeUsoUnico() throws Exception {
        String primeiro = comprador();
        String segundo = comprador();
        VinculoDao.Codigo c = dao.gerarCodigo(A, T0);
        dao.resgatar(c.codigo(), primeiro, T0.plusSeconds(1));
        assertThrows(VinculoDao.CodigoInvalidoException.class, () -> dao.resgatar(c.codigo(), segundo, T0.plusSeconds(2)));
        assertNull(usuarios.estado(segundo).vendedorProntuario());
    }

    @Test
    void gerarOutroCodigoInvalidaOAnteriorNaoUsado() throws Exception {
        String comprador = comprador();
        VinculoDao.Codigo antigo = dao.gerarCodigo(B, T0);
        VinculoDao.Codigo novo = dao.gerarCodigo(B, T0.plusSeconds(5));
        assertThrows(VinculoDao.CodigoInvalidoException.class, () -> dao.resgatar(antigo.codigo(), comprador, T0.plusSeconds(6)));
        assertEquals(B, dao.resgatar(novo.codigo(), comprador, T0.plusSeconds(6)).vendedorProntuario());
    }

    @Test
    void codigoMalFormadoOuInexistenteEInvalido() throws Exception {
        String comprador = comprador();
        for (String ruim : new String[] { null, "", "ABC", "ABCDEFG", "ABCD0O", "ÀBCDEF", "AAAAAA" }) {
            assertThrows(VinculoDao.CodigoInvalidoException.class, () -> dao.resgatar(ruim, comprador, T0), String.valueOf(ruim));
        }
    }

    @Test
    void trocaDeVendedorComNovoCodigo() throws Exception {
        String comprador = comprador();
        dao.resgatar(dao.gerarCodigo(A, T0).codigo(), comprador, T0);
        VinculoDao.Vinculo v = dao.resgatar(dao.gerarCodigo(B, T0).codigo(), comprador, T0);
        assertTrue(v.trocou());
        assertEquals(B, usuarios.estado(comprador).vendedorProntuario());
        // codigo do mesmo vendedor: nao e "troca"
        assertFalse(dao.resgatar(dao.gerarCodigo(B, T0).codigo(), comprador, T0).trocou());
    }

    @Test
    void compradorDesativadoNaoTrocaDeVendedor() throws Exception {
        String comprador = comprador();
        dao.resgatar(dao.gerarCodigo(A, T0).codigo(), comprador, T0);
        assertTrue(usuarios.definirAtivo(comprador, A, false));
        VinculoDao.Codigo deB = dao.gerarCodigo(B, T0);
        RegraNegocioException e = assertThrows(RegraNegocioException.class, () -> dao.resgatar(deB.codigo(), comprador, T0));
        assertTrue(e.getMessage().contains("desativada"));
        assertEquals(A, usuarios.estado(comprador).vendedorProntuario());
        // o codigo nao foi consumido pela tentativa recusada
        String outro = comprador();
        assertEquals(B, dao.resgatar(deB.codigo(), outro, T0).vendedorProntuario());
    }

    @Test
    void usoConcorrenteDoMesmoCodigoVinculaSoUmComprador() throws Exception {
        int n = 8;
        List<String> compradores = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            compradores.add(comprador());
        }
        VinculoDao.Codigo c = dao.gerarCodigo(A, T0);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        List<Future<Boolean>> resultados = new java.util.ArrayList<>();
        for (String comprador : compradores) {
            Callable<Boolean> tarefa = () -> {
                try {
                    dao.resgatar(c.codigo(), comprador, T0.plusSeconds(1));
                    return true;
                } catch (VinculoDao.CodigoInvalidoException e) {
                    return false;
                }
            };
            resultados.add(pool.submit(tarefa));
        }
        int sucessos = 0;
        for (Future<Boolean> f : resultados) {
            sucessos += f.get() ? 1 : 0;
        }
        pool.shutdown();
        assertEquals(1, sucessos);
    }

    @Test
    void codigosGeradosSaoDiferentes() throws Exception {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            vistos.add(dao.gerarCodigo(i % 2 == 0 ? A : B, T0.plusMinutes(10)).codigo());
        }
        assertTrue(vistos.size() >= 49, "codigos repetidos demais: " + vistos.size());
    }
}
