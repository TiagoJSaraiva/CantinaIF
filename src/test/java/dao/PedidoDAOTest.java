package dao;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import model.Comprador;
import model.ItemCarrinho;
import model.Pedido;
import model.Produto;
import model.Produto.Categoria;

class PedidoDAOTest {

    @BeforeAll
    static void iniciar() {
        TestDb.start(PedidoDAOTest.class);
    }

    @AfterAll
    static void encerrar() {
        TestDb.stop();
    }

    @Test
    void criaListaEConcluiPedido() throws Exception {
        new UserDao().create(new Comprador("CJ10", "p@x.test", "senha1234", "Paulo"));
        ProductDao produtos = new ProductDao();
        Produto a = new Produto("Coxinha", "d", 5f, 10, Categoria.SALGADO, null);
        Produto b = new Produto("Suco", "d", 4f, 10, Categoria.BEBIDA, null);
        produtos.create(a);
        produtos.create(b);

        Pedido pedido = new Pedido();
        pedido.setComprador(new Comprador("CJ10", null, null, null));
        pedido.addItem(new ItemCarrinho(a, 2));
        pedido.addItem(new ItemCarrinho(b, 1));

        PedidoDAO dao = new PedidoDAO();
        dao.criarPedido(pedido);
        assertTrue(pedido.getId() > 0);

        List<Pedido> todos = dao.listarTodosPedidosComDetalhes();
        Pedido lido = todos.stream().filter(p -> p.getId() == pedido.getId()).findFirst().orElseThrow();
        assertEquals("Paulo", lido.getComprador().getNome());
        assertEquals(2, lido.getItens().size());
        assertNull(lido.getComprador().getSenha());

        dao.deletarPedido(pedido.getId());
        assertTrue(dao.listarTodosPedidosComDetalhes().stream().noneMatch(p -> p.getId() == pedido.getId()));
    }

    @Test
    void pedidoComCompradorInexistenteFazRollback() {
        Pedido pedido = new Pedido();
        pedido.setComprador(new Comprador("NAOEXISTE", null, null, null));
        assertThrows(java.sql.SQLException.class, () -> new PedidoDAO().criarPedido(pedido));
    }
}
