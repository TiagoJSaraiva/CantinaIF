package seed;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import utils.Config;
import utils.Database;
import utils.Passwords;

/**
 * Dados de demonstracao de uma cantina de Instituto Federal. Totalmente deterministico: semente fixa e
 * datas RELATIVAS ao instante informado ({@code agora}), entao duas execucoes com o mesmo {@code agora}
 * geram exatamente os mesmos dados. Todas as pessoas, e-mails (dominio .demo) e CPFs sao ficticios.
 *
 * <p>Contas de demonstracao (senha {@value #SENHA_DEMO}):
 * vendedor {@value #VENDEDOR_PRONTUARIO} e comprador {@value #COMPRADOR_PRONTUARIO}.
 *
 * <p>Execucao manual contra o banco configurado em CANTINA_DB_URL (APAGA produtos, compradores, vendedores
 * e pedidos existentes e por isso exige confirmacao explicita):
 * <pre>
 *   ./mvnw -q compile exec:java -Dexec.args=--confirmar-apagar
 * </pre>
 * (ou defina CANTINA_SEED_CONFIRMAR=SIM no lugar do argumento).
 */
public final class DemoDataSeeder {

    public static final long SEMENTE = 20260610L;

    public static final String SENHA_DEMO = "demo1234";
    public static final String VENDEDOR_PRONTUARIO = "VEND001";
    public static final String VENDEDOR_EMAIL = "vendedor@cantinaif.demo";
    public static final String COMPRADOR_PRONTUARIO = "CJ3000001";
    public static final String COMPRADOR_EMAIL = "aluno@cantinaif.demo";

    private static final int TOTAL_PEDIDOS = 50;
    private static final int DIAS = 30;

    /** Resumo do que foi gravado. */
    public record Resultado(int produtos, int compradores, int pedidos) {
    }

    private record Prod(String slug, String nome, String descricao, String preco, int estoque, String categoria,
            boolean comImagem) {
    }

    private static Prod p(String slug, String nome, String descricao, String preco, int estoque, String categoria) {
        return new Prod(slug, nome, descricao, preco, estoque, categoria, true);
    }

    private static final List<Prod> CATALOGO = List.of(
        // salgados
        p("coxinha", "Coxinha de Frango", "Massa crocante recheada com frango desfiado temperado e catupiry.", "6.50", 40, "salgado"),
        p("pastel-queijo", "Pastel de Queijo", "Pastel frito na hora, com muito queijo mussarela derretido.", "7.00", 25, "salgado"),
        p("pastel-carne", "Pastel de Carne", "Pastel frito na hora, recheado com carne moída refogada com cebola.", "7.00", 0, "salgado"),
        p("pao-de-queijo", "Pão de Queijo (porção com 5)", "Pãozinho mineiro quentinho, feito com queijo minas.", "6.00", 30, "salgado"),
        p("empada", "Empada de Frango", "Massa amanteigada que desmancha na boca, com recheio cremoso de frango.", "6.00", 18, "salgado"),
        p("esfiha", "Esfiha de Carne", "Esfiha aberta assada, com carne temperada e um toque de limão.", "5.50", 22, "salgado"),
        p("quibe", "Quibe Frito", "Quibe tradicional de carne com trigo e hortelã. Última unidade do dia!", "6.00", 1, "salgado"),
        p("enroladinho", "Enroladinho de Salsicha", "Massa macia assada enrolada com salsicha.", "5.00", 35, "salgado"),
        // lanches
        p("misto-quente", "Misto Quente", "Pão de forma na chapa com presunto e queijo.", "8.00", 20, "lanche"),
        p("x-burguer", "X-Burguer da Cantina", "Pão de hambúrguer, carne grelhada, queijo, alface e tomate.", "14.00", 12, "lanche"),
        p("sanduiche-natural", "Sanduíche Natural Integral de Frango Desfiado com Cenoura Ralada, Alface e Maionese Caseira",
                "Pão integral, frango desfiado, cenoura, alface e maionese temperada feita na cantina. Opção leve para o intervalo.",
                "11.50", 10, "lanche"),
        p("cachorro-quente", "Cachorro-Quente", "Pão macio, salsicha, molho de tomate, milho, ervilha e batata palha.", "9.00", 15, "lanche"),
        p("pao-na-chapa", "Pão na Chapa", "Pão francês na chapa com manteiga.", "4.50", 40, "lanche"),
        p("hamburguer-frango", "Hambúrguer de Frango", "Hambúrguer de frango empanado com alface e maionese.", "13.00", 0, "lanche"),
        p("tapioca", "Tapioca de Queijo", "Tapioca fresquinha recheada com queijo coalho.", "8.50", 14, "lanche"),
        p("wrap-frango", "Wrap de Frango", "Wrap com frango grelhado, alface, tomate e molho de iogurte.", "12.00", 9, "lanche"),
        // doces
        p("brigadeiro", "Brigadeiro", "Brigadeiro tradicional de chocolate com granulado.", "3.00", 60, "doce"),
        p("beijinho", "Beijinho", "Docinho de coco com cravo, enrolado em açúcar.", "3.00", 55, "doce"),
        p("bolo-cenoura", "Fatia de Bolo de Cenoura com Chocolate", "Bolo fofinho de cenoura com cobertura de brigadeiro.", "6.50", 16, "doce"),
        p("pudim", "Pudim de Leite (fatia)", "Pudim cremoso de leite condensado com calda de caramelo.", "7.00", 12, "doce"),
        p("cookie", "Cookie de Chocolate", "Cookie macio com gotas de chocolate meio amargo.", "5.50", 28, "doce"),
        new Prod("pacoca", "Paçoca", "Paçoca de amendoim tradicional, rolha.", "2.00", 80, "doce", false),
        p("donut", "Donut Glaceado", "Rosquinha com cobertura de glacê rosa e confeitos coloridos.", "6.00", 0, "doce"),
        // bebidas
        p("suco-laranja", "Suco de Laranja 300 ml", "Suco natural de laranja, sem açúcar.", "5.00", 30, "bebida"),
        p("suco-maracuja", "Suco de Maracujá 300 ml", "Suco de maracujá com polpa da fruta, levemente adoçado.", "5.00", 26, "bebida"),
        p("refrigerante", "Refrigerante Lata 350 ml", "Refrigerante gelado em lata.", "6.00", 48, "bebida"),
        p("agua", "Água Mineral 500 ml", "Água mineral sem gás.", "3.00", 70, "bebida"),
        p("cafe", "Café Coado", "Café coado na hora, forte e quentinho.", "3.00", 50, "bebida"),
        p("achocolatado", "Achocolatado Gelado 200 ml", "Leite com chocolate, servido gelado.", "4.50", 24, "bebida"),
        p("cha-limao", "Chá Gelado de Limão 500 ml", "Chá preto gelado com limão, adoçado.", "5.00", 20, "bebida"));

    private static final List<String> NOMES = List.of(
        "Marina Teixeira", "Gabriel Nogueira", "Larissa Menezes", "Pedro Henrique Campos", "Beatriz Albuquerque",
        "Rafael Siqueira", "Camila Ferraz", "Lucas Barreto", "Juliana Matos", "Felipe Andrade", "Isabela Cordeiro",
        "Thiago Medeiros", "Fernanda Lacerda", "Mateus Vasconcelos", "Amanda Pimentel",
        // os tres ultimos nunca fizeram pedidos
        "Henrique Tavares", "Vitória Salgado", "Daniel Rezende");

    private static final int COMPRADORES_COM_PEDIDOS = 15;

    private DemoDataSeeder() {
    }

    // ------------------------------------------------------------------ API

    /** Apaga e recria os dados de demonstracao usando datas relativas a {@code agora}. */
    public static Resultado run(Connection conn, LocalDateTime agora) throws SQLException {
        boolean autoCommit = conn.getAutoCommit();
        try {
            conn.setAutoCommit(true);
            limparTudo(conn);
            conn.setAutoCommit(false);
            Resultado r = inserir(conn, agora);
            conn.commit();
            return r;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    /** Executa {@link #run(Connection, LocalDateTime)} no banco configurado. */
    public static Resultado run(LocalDateTime agora) throws SQLException {
        try (Connection conn = Database.getConnection()) {
            return run(conn, agora);
        }
    }

    /** Semeia somente se ainda nao houver nenhum vendedor (nao apaga nada). */
    public static boolean rodarSeVazio(LocalDateTime agora) throws SQLException {
        try (Connection conn = Database.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Vendedor")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return false;
            }
            run(conn, agora);
            return true;
        }
    }

    // ------------------------------------------------------------------ limpeza

    private static void limparTudo(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM Pedido_Produto");
            st.executeUpdate("DELETE FROM Pedido");
            st.executeUpdate("DELETE FROM Produto");
            st.executeUpdate("DELETE FROM Comprador");
            st.executeUpdate("DELETE FROM Vendedor");
            // reinicia os ids para o resultado ser identico a cada execucao
            boolean h2 = conn.getMetaData().getDatabaseProductName().toUpperCase().contains("H2");
            for (String tabela : new String[] { "Pedido", "Produto" }) {
                st.executeUpdate(h2 ? "ALTER TABLE " + tabela + " ALTER COLUMN id RESTART WITH 1"
                                    : "ALTER TABLE " + tabela + " AUTO_INCREMENT = 1");
            }
        }
    }

    // ------------------------------------------------------------------ insercao

    private static Resultado inserir(Connection conn, LocalDateTime agora) throws SQLException {
        Random r = new Random(SEMENTE);
        String hashDemo = Passwords.hash(SENHA_DEMO);
        String hashOutros = Passwords.hash("senha-demo-nao-publicada");

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO Vendedor (prontuario, email, senha) VALUES (?, ?, ?)")) {
            ps.setString(1, VENDEDOR_PRONTUARIO);
            ps.setString(2, VENDEDOR_EMAIL);
            ps.setString(3, hashDemo);
            ps.executeUpdate();
        }

        // produtos (ids 1..n na ordem do catalogo)
        List<BigDecimal> precos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Produto (nome, descricao, preco, estoque, categoria, imagem) VALUES (?, ?, ?, ?, ?, ?)")) {
            for (Prod prod : CATALOGO) {
                BigDecimal preco = new BigDecimal(prod.preco());
                precos.add(preco);
                ps.setString(1, prod.nome());
                ps.setString(2, prod.descricao());
                ps.setBigDecimal(3, preco);
                ps.setInt(4, prod.estoque());
                ps.setString(5, prod.categoria());
                byte[] imagem = prod.comImagem() ? imagem(prod.slug()) : null;
                if (imagem != null) {
                    ps.setBytes(6, imagem);
                } else {
                    ps.setNull(6, java.sql.Types.BLOB);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // compradores: o de demonstracao + os fictícios
        List<String> prontuarios = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Comprador (prontuario, email, senha, nome, cpf) VALUES (?, ?, ?, ?, ?)")) {
            prontuarios.add(COMPRADOR_PRONTUARIO);
            adicionarComprador(ps, COMPRADOR_PRONTUARIO, COMPRADOR_EMAIL, hashDemo, "Aluno Demonstração", cpfFicticio(r));
            Set<String> emails = new LinkedHashSet<>();
            for (int i = 0; i < NOMES.size(); i++) {
                String nome = NOMES.get(i);
                String prontuario = "CJ" + (3000002 + i);
                prontuarios.add(prontuario);
                adicionarComprador(ps, prontuario, emailDe(nome, emails), hashOutros, nome, cpfFicticio(r));
            }
            ps.executeBatch();
        }

        int pedidos = inserirPedidos(conn, r, agora, prontuarios, precos);
        return new Resultado(CATALOGO.size(), prontuarios.size(), pedidos);
    }

    private static void adicionarComprador(PreparedStatement ps, String prontuario, String email, String hash,
            String nome, String cpf) throws SQLException {
        ps.setString(1, prontuario);
        ps.setString(2, email);
        ps.setString(3, hash);
        ps.setString(4, nome);
        ps.setString(5, cpf);
        ps.addBatch();
    }

    private static int inserirPedidos(Connection conn, Random r, LocalDateTime agora, List<String> prontuarios,
            List<BigDecimal> precos) throws SQLException {
        try (PreparedStatement psPedido = conn.prepareStatement(
                "INSERT INTO Pedido (comprador_prontuario, status, criado_em, total) VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psItem = conn.prepareStatement(
                "INSERT INTO Pedido_Produto (pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)")) {

            int janelaMinutos = DIAS * 24 * 60 - 180;
            int[] contadorOutros = { 0 };
            for (int i = 0; i < TOTAL_PEDIDOS; i++) {
                // do mais antigo (i = 0) ao mais recente; mais pedidos recentes que antigos
                double fracao = (TOTAL_PEDIDOS - 1 - i) / (double) (TOTAL_PEDIDOS - 1);
                int minutosAtras = (int) Math.round(janelaMinutos * Math.pow(fracao, 1.4)) + r.nextInt(120);
                LocalDateTime criadoEm = agora.minusMinutes(minutosAtras);

                String status = statusDoPedido(i);
                String comprador = compradorDoPedido(i, r, prontuarios, contadorOutros);

                // 1 a 4 produtos distintos, 1 a 3 unidades de cada
                Set<Integer> escolhidos = new LinkedHashSet<>();
                int itens = 1 + r.nextInt(4);
                while (escolhidos.size() < itens) {
                    escolhidos.add(r.nextInt(CATALOGO.size()));
                }
                List<int[]> linhas = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                for (int idx : escolhidos) {
                    int quantidade = 1 + r.nextInt(3);
                    linhas.add(new int[] { idx + 1, quantidade });
                    total = total.add(precos.get(idx).multiply(BigDecimal.valueOf(quantidade)));
                }

                psPedido.setString(1, comprador);
                psPedido.setString(2, status);
                psPedido.setTimestamp(3, Timestamp.valueOf(criadoEm));
                psPedido.setBigDecimal(4, total);
                psPedido.executeUpdate();
                int pedidoId;
                try (ResultSet rs = psPedido.getGeneratedKeys()) {
                    rs.next();
                    pedidoId = rs.getInt(1);
                }
                for (int[] linha : linhas) {
                    psItem.setInt(1, pedidoId);
                    psItem.setInt(2, linha[0]);
                    psItem.setInt(3, linha[1]);
                    psItem.setBigDecimal(4, precos.get(linha[0] - 1));
                    psItem.addBatch();
                }
            }
            psItem.executeBatch();
        }
        return TOTAL_PEDIDOS;
    }

    /** Os 6 mais recentes cobrem os status em andamento; os demais sao entregues, com alguns cancelados. */
    static String statusDoPedido(int i) {
        if (i >= TOTAL_PEDIDOS - 2) {
            return "PENDENTE";
        }
        if (i >= TOTAL_PEDIDOS - 4) {
            return "EM_PREPARO";
        }
        if (i >= TOTAL_PEDIDOS - 6) {
            return "PRONTO";
        }
        return i % 7 == 3 ? "CANCELADO" : "ENTREGUE";
    }

    /** O comprador de demonstracao tem pedidos em varios status; os 3 ultimos compradores nao tem nenhum. */
    private static String compradorDoPedido(int i, Random r, List<String> prontuarios, int[] contadorOutros) {
        int[] doDemo = { 10, 12, 20, 28, 36, 44, TOTAL_PEDIDOS - 1 };
        for (int d : doDemo) {
            if (d == i) {
                r.nextInt(COMPRADORES_COM_PEDIDOS); // mantem a sequencia pseudoaleatoria estavel
                return prontuarios.get(0);
            }
        }
        // os primeiros 15 pedidos "dos outros" cobrem cada comprador uma vez (7 e coprimo com 15); depois, sorteio
        int n = contadorOutros[0]++;
        int idx = n < COMPRADORES_COM_PEDIDOS ? (n * 7) % COMPRADORES_COM_PEDIDOS : r.nextInt(COMPRADORES_COM_PEDIDOS);
        return prontuarios.get(1 + idx);
    }

    // ------------------------------------------------------------------ utilitarios

    private static byte[] imagem(String slug) {
        try (InputStream in = DemoDataSeeder.class.getResourceAsStream("/seed/img/" + slug + ".png")) {
            if (in == null) {
                throw new IllegalStateException("Imagem de seed ausente: " + slug);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String emailDe(String nome, Set<String> usados) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
        String[] partes = semAcento.split(" ");
        String base = partes[0] + "." + partes[partes.length - 1];
        String email = base + "@aluno.cantinaif.demo";
        int n = 2;
        while (!usados.add(email)) {
            email = base + n++ + "@aluno.cantinaif.demo";
        }
        return email;
    }

    /** CPF sintaticamente valido (digitos verificadores corretos), gerado - nao pertence a ninguem conhecido. */
    static String cpfFicticio(Random r) {
        int[] d = new int[11];
        do {
            for (int i = 0; i < 9; i++) {
                d[i] = r.nextInt(10);
            }
        } while (todosIguais(d, 9));
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder sb = new StringBuilder();
        for (int x : d) {
            sb.append(x);
        }
        return sb.toString();
    }

    private static boolean todosIguais(int[] d, int n) {
        for (int i = 1; i < n; i++) {
            if (d[i] != d[0]) {
                return false;
            }
        }
        return true;
    }

    private static int digito(int[] d, int n) {
        int soma = 0;
        for (int i = 0; i < n; i++) {
            soma += d[i] * (n + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Valida os 11 digitos de um CPF (usado nos testes). */
    public static boolean cpfValido(String cpf) {
        if (cpf == null || !cpf.matches("\\d{11}")) {
            return false;
        }
        int[] d = new int[11];
        for (int i = 0; i < 11; i++) {
            d[i] = cpf.charAt(i) - '0';
        }
        return !todosIguais(d, 11) && d[9] == digito(d, 9) && d[10] == digito(d, 10);
    }

    // ------------------------------------------------------------------ linha de comando

    /** Comando manual: apaga e recria os dados de demonstracao no banco de CANTINA_DB_URL. */
    public static void main(String[] args) throws Exception {
        boolean confirmado = "SIM".equalsIgnoreCase(Config.get("CANTINA_SEED_CONFIRMAR"));
        for (String a : args) {
            if ("--confirmar-apagar".equals(a)) {
                confirmado = true;
            }
        }
        if (Config.get("CANTINA_DB_URL") == null) {
            System.err.println("Defina CANTINA_DB_URL (e CANTINA_DB_USER / CANTINA_DB_PASSWORD) para escolher o banco.");
            System.exit(2);
        }
        if (!confirmado) {
            System.err.println("ATENCAO: este comando APAGA produtos, compradores, vendedores e pedidos do banco "
                    + "configurado e recria os dados de demonstracao.");
            System.err.println("Para confirmar, repita com o argumento --confirmar-apagar "
                    + "(ou defina CANTINA_SEED_CONFIRMAR=SIM).");
            System.exit(1);
        }
        Database.init();
        try (Connection conn = Database.getConnection()) {
            Database.aplicarSchema(conn);
            Resultado r = run(conn, LocalDateTime.now());
            System.out.println("Dados de demonstracao gravados: " + r.produtos() + " produtos, "
                    + r.compradores() + " compradores, " + r.pedidos() + " pedidos.");
            System.out.println("Vendedor: " + VENDEDOR_PRONTUARIO + " / " + SENHA_DEMO
                    + " | Comprador: " + COMPRADOR_PRONTUARIO + " / " + SENHA_DEMO);
        } finally {
            Database.shutdown();
        }
    }
}
