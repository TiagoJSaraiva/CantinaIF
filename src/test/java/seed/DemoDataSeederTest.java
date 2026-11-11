package seed;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dao.PedidoDAO;
import dao.ProductDao;
import dao.UserDao;
import model.ItemCarrinho;
import model.Produto;
import utils.Database;

class DemoDataSeederTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2030, 1, 15, 10, 0);

    @BeforeAll
    static void iniciar() {
        Database.shutdown();
        Database.init("jdbc:h2:mem:t_seed;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", null, null);
    }

    @AfterAll
    static void encerrar() {
        Database.shutdown();
    }

    /** Fotografia textual de todo o conteudo (exceto hashes de senha, que usam salt aleatorio). */
    private static String fotografia(Connection c) throws SQLException {
        StringBuilder sb = new StringBuilder();
        String[] consultas = {
            "SELECT id, nome, descricao, preco, estoque, categoria, LENGTH(imagem) FROM Produto ORDER BY id",
            "SELECT prontuario, email, nome, cpf FROM Comprador ORDER BY prontuario",
            "SELECT prontuario, email FROM Vendedor ORDER BY prontuario",
            "SELECT id, comprador_prontuario, status, criado_em, total FROM Pedido ORDER BY id",
            "SELECT pedido_id, produto_id, quantidade, preco_unitario FROM Pedido_Produto ORDER BY pedido_id, produto_id" };
        try (Statement st = c.createStatement()) {
            for (String q : consultas) {
                try (ResultSet rs = st.executeQuery(q)) {
                    int n = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        for (int i = 1; i <= n; i++) {
                            sb.append(rs.getString(i)).append('|');
                        }
                        sb.append('\n');
                    }
                }
                sb.append("--\n");
            }
        }
        return sb.toString();
    }

    private static int inteiro(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void duasExecucoesSeguidasGeramResultadoIdentico() throws Exception {
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.Resultado r1 = DemoDataSeeder.run(c, AGORA);
            String a = fotografia(c);
            DemoDataSeeder.Resultado r2 = DemoDataSeeder.run(c, AGORA);
            String b = fotografia(c);
            assertEquals(r1, r2);
            assertEquals(a, b);
            assertFalse(a.isBlank());
        }
    }

    @Test
    void restauracaoDesfazAlteracoesFeitasPorVisitantes() throws Exception {
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.run(c, AGORA);
            String original = fotografia(c);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("UPDATE Produto SET estoque = 0, nome = 'vandalizado'");
                st.executeUpdate("DELETE FROM Pedido_Produto");
                st.executeUpdate("DELETE FROM Pedido");
            }
            assertNotEquals(original, fotografia(c));
            DemoDataSeeder.run(c, AGORA);
            assertEquals(original, fotografia(c));
        }
    }

    @Test
    void quantidadesEPerfisDoConjunto() throws Exception {
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.Resultado r = DemoDataSeeder.run(c, AGORA);
            assertTrue(r.produtos() >= 28 && r.produtos() <= 32, "produtos: " + r.produtos());
            assertTrue(r.compradores() >= 15 && r.compradores() <= 20, "compradores: " + r.compradores());
            assertTrue(r.pedidos() >= 40 && r.pedidos() <= 60, "pedidos: " + r.pedidos());
            assertEquals(1, inteiro(c, "SELECT COUNT(*) FROM Vendedor"));

            assertEquals(3, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE estoque = 0"));
            assertEquals(1, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE estoque = 1"));
            assertEquals(1, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE LENGTH(nome) > 80"));
            assertEquals(1, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE imagem IS NULL"));
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE LENGTH(imagem) > 30720"));
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Produto WHERE estoque < 0 OR preco <= 0"));
            for (String cat : List.of("salgado", "lanche", "doce", "bebida")) {
                assertTrue(inteiro(c, "SELECT COUNT(*) FROM Produto WHERE categoria = '" + cat + "'") >= 5, cat);
            }

            assertEquals(3, inteiro(c, "SELECT COUNT(*) FROM Comprador WHERE prontuario NOT IN "
                    + "(SELECT comprador_prontuario FROM Pedido)"));
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Comprador WHERE email NOT LIKE '%.demo'"));
        }
    }

    @Test
    void cpfsSaoValidosEUnicos() throws Exception {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement()) {
            DemoDataSeeder.run(c, AGORA);
            Set<String> vistos = new HashSet<>();
            try (ResultSet rs = st.executeQuery("SELECT cpf FROM Comprador")) {
                while (rs.next()) {
                    String cpf = rs.getString(1);
                    assertTrue(DemoDataSeeder.cpfValido(cpf), cpf);
                    assertTrue(vistos.add(cpf));
                }
            }
        }
        assertTrue(DemoDataSeeder.cpfValido("52998224725"));
        assertFalse(DemoDataSeeder.cpfValido("52998224726"));
        assertFalse(DemoDataSeeder.cpfValido("11111111111"));
    }

    @Test
    void pedidosCobremTodosOsStatusNosUltimos30DiasComTotaisConsistentes() throws Exception {
        try (Connection c = Database.getConnection(); Statement st = c.createStatement()) {
            DemoDataSeeder.run(c, AGORA);
            for (String status : List.of("PENDENTE", "EM_PREPARO", "PRONTO", "ENTREGUE", "CANCELADO")) {
                assertTrue(inteiro(c, "SELECT COUNT(*) FROM Pedido WHERE status = '" + status + "'") > 0, status);
            }
            try (ResultSet rs = st.executeQuery("SELECT criado_em FROM Pedido")) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp(1);
                    LocalDateTime d = t.toLocalDateTime();
                    assertFalse(d.isAfter(AGORA), "pedido no futuro: " + d);
                    assertFalse(d.isBefore(AGORA.minusDays(30)), "pedido com mais de 30 dias: " + d);
                }
            }
            // total do pedido = soma(quantidade x preco_unitario); preco_unitario = preco do produto
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Pedido p WHERE p.total <> "
                    + "(SELECT SUM(pp.quantidade * pp.preco_unitario) FROM Pedido_Produto pp WHERE pp.pedido_id = p.id)"));
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Pedido_Produto pp JOIN Produto pr ON pr.id = pp.produto_id "
                    + "WHERE pp.preco_unitario <> pr.preco OR pp.quantidade < 1"));
            assertEquals(0, inteiro(c, "SELECT COUNT(*) FROM Pedido p WHERE NOT EXISTS "
                    + "(SELECT 1 FROM Pedido_Produto pp WHERE pp.pedido_id = p.id)"));
            // o comprador de demonstracao tem historico variado
            assertTrue(inteiro(c, "SELECT COUNT(DISTINCT status) FROM Pedido WHERE comprador_prontuario = '"
                    + DemoDataSeeder.COMPRADOR_PRONTUARIO + "'") >= 3);
        }
    }

    @Test
    void contasDeDemonstracaoAutenticamEAplicacaoContinuaFuncionandoAposOSeed() throws Exception {
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.run(c, AGORA);
        }
        assertNotNull(new UserDao().autenticar(DemoDataSeeder.COMPRADOR_PRONTUARIO, DemoDataSeeder.SENHA_DEMO));
        assertNull(new UserDao().autenticar(DemoDataSeeder.COMPRADOR_PRONTUARIO, "outra"));

        // ids gerados apos o seed nao colidem com os semeados
        ProductDao produtos = new ProductDao();
        Produto novo = new Produto("Novo", "d", new BigDecimal("1.00"), 5, Produto.Categoria.DOCE, null);
        produtos.create(novo);
        assertEquals(31, novo.getId());
        var pedido = new PedidoDAO().criarPedido(DemoDataSeeder.COMPRADOR_PRONTUARIO, List.of(new ItemCarrinho(novo, 2)));
        assertEquals(51, pedido.getId());
        assertEquals(3, produtos.buscarResumo(novo.getId()).getEstoque());
    }

    @Test
    void rodarSeVazioNaoApagaNada() throws Exception {
        try (Connection c = Database.getConnection()) {
            DemoDataSeeder.run(c, AGORA);
            String antes = fotografia(c);
            assertFalse(DemoDataSeeder.rodarSeVazio(AGORA.plusDays(1)));
            assertEquals(antes, fotografia(c));
        }
    }
}
