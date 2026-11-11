package dao;

import static org.junit.jupiter.api.Assertions.*;

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

    private Produto novo(String nome, int estoque, Categoria cat) throws Exception {
        Produto p = new Produto(nome, "desc", 5.5f, estoque, cat, null);
        dao.create(p);
        assertTrue(p.getId() > 0);
        return p;
    }

    @Test
    void criaEBuscaPorId() throws Exception {
        Produto p = novo("Coxinha", 10, Categoria.SALGADO);
        Produto lido = dao.buscarPorId(p.getId());
        assertEquals("Coxinha", lido.getNome());
        assertEquals(Categoria.SALGADO, lido.getCategoria());
        assertEquals(5.5f, lido.getPreco(), 0.001f);
        assertNull(dao.buscarPorId(999999));
    }

    @Test
    void listagemDoCompradorOcultaEstoqueZeroEVendedorVeTudo() throws Exception {
        Produto semEstoque = novo("Esgotado", 0, Categoria.DOCE);
        Produto comEstoque = novo("Brigadeiro", 4, Categoria.DOCE);
        assertTrue(dao.listarProdutosDisponiveisUser().stream().noneMatch(p -> p.getId() == semEstoque.getId()));
        assertTrue(dao.listarProdutosDisponiveisUser().stream().anyMatch(p -> p.getId() == comEstoque.getId()));
        assertTrue(dao.listarProdutosDisponiveisADM().stream().anyMatch(p -> p.getId() == semEstoque.getId()));
    }

    @Test
    void filtroPorCategoriaIgnoraCaixa() throws Exception {
        Produto p = novo("Suco", 3, Categoria.BEBIDA);
        assertTrue(dao.listarProdutosPorCategoriaUser("bebida").stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(dao.listarProdutosPorCategoriaUser("BEBIDA").stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(dao.listarProdutosPorCategoriaADM("salgado").stream().noneMatch(x -> x.getId() == p.getId()));
    }

    @Test
    void atualizaProdutoEEstoque() throws Exception {
        Produto p = novo("Pao", 2, Categoria.LANCHE);
        Produto edit = new Produto("Pao de queijo", "novo", 7.25f, 9, Categoria.SALGADO, null);
        edit.setId(p.getId());
        assertTrue(dao.atualizar(edit));
        Produto lido = dao.buscarPorId(p.getId());
        assertEquals("Pao de queijo", lido.getNome());
        assertEquals(Categoria.SALGADO, lido.getCategoria());
        assertEquals(9, lido.getEstoque());

        assertTrue(dao.atualizarEstoque(p.getId(), 0));
        assertEquals(0, dao.buscarPorId(p.getId()).getEstoque());
        assertFalse(dao.atualizarEstoque(999999, 1));
    }

    @Test
    void atualizarSemImagemPreservaImagemExistente() throws Exception {
        Produto p = new Produto("Com foto", "d", 1f, 1, Categoria.DOCE, new byte[] { 1, 2, 3 });
        dao.create(p);
        Produto edit = new Produto("Com foto 2", "d", 1f, 1, Categoria.DOCE, null);
        edit.setId(p.getId());
        dao.atualizar(edit);
        assertArrayEquals(new byte[] { 1, 2, 3 }, dao.buscarPorId(p.getId()).getImagem());
    }

    @Test
    void estoqueNegativoViolaConstraint() {
        assertThrows(java.sql.SQLException.class,
                () -> dao.create(new Produto("Ruim", "d", 1f, -1, Categoria.DOCE, null)));
    }

    @Test
    void reservarEstoqueNaoPermiteFicarNegativo() throws Exception {
        Produto p = novo("Limitado", 2, Categoria.LANCHE);
        dao.reservarEstoque(p.getId(), 2);
        assertEquals(0, dao.buscarPorId(p.getId()).getEstoque());
        assertThrows(java.sql.SQLException.class, () -> dao.reservarEstoque(p.getId(), 1));
    }
}
