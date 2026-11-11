package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Produto;
import model.Produto.Categoria;

class ProductDaoTest {

    private final ProductDao dao = new ProductDao();

    @BeforeAll
    static void iniciar() {
        TestDb.start(ProductDaoTest.class);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    static BigDecimal preco(String v) {
        return new BigDecimal(v);
    }

    private Produto novo(String nome, int estoque, Categoria cat) throws Exception {
        Produto p = new Produto(nome, "desc", preco("5.50"), estoque, cat, null);
        dao.create(p);
        assertTrue(p.getId() > 0);
        return p;
    }

    @Test
    void criaEBuscaPorIdComPrecoExato() throws Exception {
        Produto p = novo("Coxinha", 10, Categoria.SALGADO);
        Produto lido = dao.buscarPorId(p.getId());
        assertEquals("Coxinha", lido.getNome());
        assertEquals(Categoria.SALGADO, lido.getCategoria());
        assertEquals(0, preco("5.50").compareTo(lido.getPreco()));
        assertNull(dao.buscarPorId(999999));
    }

    @Test
    void precoNaoPerdePrecisaoComoNoFloat() throws Exception {
        Produto p = new Produto("Preco 0,10", "d", preco("0.10"), 1, Categoria.DOCE, null);
        dao.create(p);
        assertEquals(0, preco("0.30").compareTo(dao.buscarPorId(p.getId()).getPreco().multiply(BigDecimal.valueOf(3))));
    }

    @Test
    void listagemInclueEsgotadosEIndicaImagemSemCarregarBytes() throws Exception {
        Produto esgotado = novo("Esgotado", 0, Categoria.DOCE);
        Produto comFoto = new Produto("Com foto", "d", preco("1.00"), 2, Categoria.DOCE, new byte[] { 1, 2, 3 });
        dao.create(comFoto);

        var lista = dao.listarProdutos("todos");
        assertTrue(lista.stream().anyMatch(p -> p.getId() == esgotado.getId() && p.getEstoque() == 0));
        Produto lido = lista.stream().filter(p -> p.getId() == comFoto.getId()).findFirst().orElseThrow();
        assertTrue(lido.isTemImagem());
        assertNull(lido.getImagem(), "a listagem nao carrega os bytes");
        assertFalse(lista.stream().filter(p -> p.getId() == esgotado.getId()).findFirst().orElseThrow().isTemImagem());
    }

    @Test
    void filtroPorCategoriaIgnoraCaixa() throws Exception {
        Produto p = novo("Suco", 3, Categoria.BEBIDA);
        assertTrue(dao.listarProdutos("bebida").stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(dao.listarProdutos("BEBIDA").stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(dao.listarProdutos("salgado").stream().noneMatch(x -> x.getId() == p.getId()));
    }

    @Test
    void atualizaProdutoEEstoque() throws Exception {
        Produto p = novo("Pao", 2, Categoria.LANCHE);
        Produto edit = new Produto("Pao de queijo", "novo", preco("7.25"), 9, Categoria.SALGADO, null);
        edit.setId(p.getId());
        assertTrue(dao.atualizar(edit));
        Produto lido = dao.buscarPorId(p.getId());
        assertEquals("Pao de queijo", lido.getNome());
        assertEquals(Categoria.SALGADO, lido.getCategoria());
        assertEquals(9, lido.getEstoque());
        assertEquals(0, preco("7.25").compareTo(lido.getPreco()));

        assertTrue(dao.atualizarEstoque(p.getId(), 0));
        assertEquals(0, dao.buscarResumo(p.getId()).getEstoque());
        assertFalse(dao.atualizarEstoque(999999, 1));
    }

    @Test
    void atualizarSemImagemPreservaImagemExistente() throws Exception {
        Produto p = new Produto("Com foto", "d", preco("1.00"), 1, Categoria.DOCE, new byte[] { 1, 2, 3 });
        dao.create(p);
        Produto edit = new Produto("Com foto 2", "d", preco("1.00"), 1, Categoria.DOCE, null);
        edit.setId(p.getId());
        dao.atualizar(edit);
        assertArrayEquals(new byte[] { 1, 2, 3 }, dao.buscarPorId(p.getId()).getImagem());
    }

    @Test
    void estoqueOuPrecoNegativoViolamConstraint() {
        assertThrows(java.sql.SQLException.class,
                () -> dao.create(new Produto("Ruim", "d", preco("1.00"), -1, Categoria.DOCE, null)));
        assertThrows(java.sql.SQLException.class,
                () -> dao.create(new Produto("Ruim", "d", preco("-1.00"), 1, Categoria.DOCE, null)));
    }

    @Test
    void ajusteDeEstoqueComparaETrocaSemSobrescreverBaixaConcorrente() throws Exception {
        Produto p = novo("CAS", 10, Categoria.SALGADO);
        // o vendedor viu 10; antes de ele gravar, um checkout baixou 3
        dao.atualizarEstoque(p.getId(), 7);
        assertEquals(ProductDao.ResultadoEstoque.CONFLITO, dao.atualizarEstoque(p.getId(), 10, 11));
        assertEquals(7, dao.estoqueAtual(p.getId()), "o valor concorrente foi preservado");
        assertEquals(ProductDao.ResultadoEstoque.OK, dao.atualizarEstoque(p.getId(), 7, 8));
        assertEquals(8, dao.estoqueAtual(p.getId()));
        assertEquals(ProductDao.ResultadoEstoque.NAO_ENCONTRADO, dao.atualizarEstoque(999999, 1, 2));
    }

    @Test
    void edicaoCompletaTambemComparaOEstoque() throws Exception {
        Produto p = novo("CAS2", 5, Categoria.DOCE);
        Produto edit = new Produto("CAS2 editado", "d", preco("2.00"), 9, Categoria.DOCE, null);
        edit.setId(p.getId());
        dao.atualizarEstoque(p.getId(), 4); // alguem comprou 1 depois que o vendedor abriu o formulario (viu 5)
        assertEquals(ProductDao.ResultadoEstoque.CONFLITO, dao.atualizar(edit, 5));
        assertEquals("CAS2", dao.buscarResumo(p.getId()).getNome(), "nada foi alterado");
        assertEquals(ProductDao.ResultadoEstoque.OK, dao.atualizar(edit, 4));
        assertEquals(9, dao.estoqueAtual(p.getId()));
    }

    @Test
    void ajustesConcorrentesDeEstoquePorComparaETrocaNaoPerdemAtualizacoes() throws Exception {
        Produto p = novo("CAS3", 0, Categoria.SALGADO);
        int threads = 8, porThread = 25;
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < porThread; i++) {
                    boolean ok = false;
                    while (!ok) { // tenta de novo ate conseguir incrementar em 1 (como o painel faria apos o 409)
                        int atual = dao.estoqueAtual(p.getId());
                        ok = dao.atualizarEstoque(p.getId(), atual, atual + 1) == ProductDao.ResultadoEstoque.OK;
                    }
                }
                return null;
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals(threads * porThread, dao.estoqueAtual(p.getId()));
    }
}
