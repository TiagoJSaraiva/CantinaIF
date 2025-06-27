package dao; // Garanta que este é o pacote correto para seus DAOs

import model.Comprador; // Garanta que o pacote model está correto
import model.Pedido;     // Garanta que o pacote model está correto
import model.Produto;    // Garanta que o pacote model está correto
import model.ItemCarrinho; // Garanta que o pacote model está correto e que ItemCarrinho é sua classe para itens de pedido

import utils.ConnectionFactory; // Importa sua classe de fábrica de conexão

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // Importar para Statement.RETURN_GENERATED_KEYS
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PedidoDAO {

    public PedidoDAO() {
        // Construtor padrão
    }

    /**
     * Lista todos os pedidos pendentes com todos os seus detalhes:
     * Comprador (nome, prontuário, email) e
     * Itens do Pedido (Produto e Quantidade de cada produto no pedido).
     *
     * @return Uma lista de objetos Pedido, cada um populado com seu Comprador e ItensCarrinho.
     */
    public List<Pedido> listarTodosPedidosComDetalhes() {
        // Usamos um Map para garantir que cada Pedido seja instanciado apenas uma vez
        // e que os itens sejam adicionados ao pedido correto.
        Map<Integer, Pedido> pedidosMap = new HashMap<>();

        // SQL que junta todas as tabelas necessárias:
        // Pedido (p), Comprador (c), Pedido_Produto (pp), Produto (prod)
        String sql = "SELECT p.id AS pedido_id, " +
                     "c.prontuario AS comprador_prontuario, c.nome AS comprador_nome, c.email AS comprador_email, " +
                     "prod.id AS produto_id, prod.nome AS produto_nome, prod.descricao AS produto_descricao, " +
                     "prod.preco AS produto_preco, prod.estoque AS produto_estoque, prod.categoria AS produto_categoria, " +
                     "pp.quantidade AS item_quantidade " +
                     "FROM Pedido p " +
                     "JOIN Comprador c ON p.comprador_prontuario = c.prontuario " +
                     "JOIN Pedido_Produto pp ON p.id = pp.pedido_id " +
                     "JOIN Produto prod ON pp.produto_id = prod.id " +
                     "ORDER BY p.id ASC, prod.nome ASC"; // Ordena para facilitar a agregação

        // Usa try-with-resources para garantir que a conexão e os statements sejam fechados automaticamente.
        try (Connection conn = ConnectionFactory.getConnection(); // Obtém a conexão da sua fábrica
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int pedidoId = rs.getInt("pedido_id");

                // --- 1. Recuperar ou Criar o Objeto Pedido ---
                Pedido pedido = pedidosMap.get(pedidoId);
                if (pedido == null) {
                    // Se o pedido ainda não está no mapa, cria um novo Pedido e seu Comprador.
                    Comprador comprador = new Comprador();
                    comprador.setProntuario(rs.getString("comprador_prontuario"));
                    comprador.setNome(rs.getString("comprador_nome"));
                    comprador.setEmail(rs.getString("comprador_email"));
                    // IMPORTANTE: NÃO carregar a senha do comprador aqui por segurança.

                    pedido = new Pedido();
                    pedido.setId(pedidoId);
                    pedido.setComprador(comprador);
                    pedidosMap.put(pedidoId, pedido);
                }

                // --- 2. Criar e Adicionar o Item do Pedido (ItemCarrinho) ---
                // Cria o objeto Produto para o item do pedido
                Produto produto = new Produto();
                produto.setId(rs.getInt("produto_id"));
                produto.setNome(rs.getString("produto_nome"));
                produto.setDescricao(rs.getString("produto_descricao"));
                produto.setPreco(rs.getFloat("produto_preco")); // Usando getFloat() conforme sua classe Produto
                produto.setEstoque(rs.getInt("produto_estoque"));
                
                // Converte a string da categoria para o enum Produto.Categoria
                String categoriaString = rs.getString("produto_categoria");
                if (categoriaString != null && !categoriaString.isEmpty()) {
                    try {
                    	produto.setCategoria(Produto.Categoria.valueOf(categoriaString.trim().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Erro: Categoria '" + categoriaString + "' inválida para o produto ID: " + produto.getId());
                    }
                }
                // Não carregar a imagem aqui, pois ela é pesada e não necessária para a listagem inicial do pedido.

                // Cria o ItemCarrinho e adiciona ao Pedido.
                int quantidade = rs.getInt("item_quantidade");
                ItemCarrinho itemCarrinho = new ItemCarrinho(produto, quantidade);
                pedido.addItem(itemCarrinho);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar pedidos com detalhes: " + e.getMessage());
            e.printStackTrace();
        }
        // Retorna a lista de todos os pedidos únicos que foram coletados no mapa.
        return new ArrayList<>(pedidosMap.values());
    }

    /**
     * Salva um novo pedido no banco de dados, incluindo seus itens.
     * Esta operação é transacional para garantir atomicidade.
     *
     * @param pedido O objeto Pedido a ser salvo, contendo o Comprador e a lista de ItemCarrinho.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    public void criarPedido(Pedido pedido) throws SQLException {
        Connection conn = null;
        PreparedStatement stmtPedido = null;
        PreparedStatement stmtItemPedido = null;
        ResultSet rs = null; // Para pegar o ID gerado do pedido

        // SQL para inserir um novo pedido
        String sqlInsertPedido = "INSERT INTO Pedido (comprador_prontuario) VALUES (?)";
        // SQL para inserir um item no Pedido_Produto
        String sqlInsertItemPedido = "INSERT INTO Pedido_Produto (pedido_id, produto_id, quantidade) VALUES (?, ?, ?)";

        try {
            conn = ConnectionFactory.getConnection(); // Obtém a conexão da sua fábrica
            conn.setAutoCommit(false); // Inicia a transação para garantir que ambas as inserções (pedido e itens) ocorram ou falhem juntas

            // 1. Inserir o Pedido principal e obter o ID gerado (AUTO_INCREMENT)
            stmtPedido = conn.prepareStatement(sqlInsertPedido, Statement.RETURN_GENERATED_KEYS);
            stmtPedido.setString(1, pedido.getComprador().getProntuario()); // Pega o prontuário do comprador do objeto Pedido
            stmtPedido.executeUpdate(); // Executa a inserção do pedido

            // Obtém o ID gerado para o pedido recém-inserido
            rs = stmtPedido.getGeneratedKeys();
            if (rs.next()) {
                pedido.setId(rs.getInt(1)); // Define o ID gerado no objeto Pedido
            } else {
                throw new SQLException("Não foi possível obter o ID gerado para o pedido.");
            }

            // 2. Inserir cada ItemCarrinho na tabela Pedido_Produto
            stmtItemPedido = conn.prepareStatement(sqlInsertItemPedido);
            for (ItemCarrinho item : pedido.getItens()) { // Itera sobre os itens do pedido
                stmtItemPedido.setInt(1, pedido.getId());               // ID do pedido recém-criado
                stmtItemPedido.setInt(2, item.getProduto().getId());    // ID do produto do item do carrinho
                stmtItemPedido.setInt(3, item.getQuantidade());         // Quantidade do produto no item do carrinho
                stmtItemPedido.addBatch(); // Adiciona esta inserção ao batch
            }
            stmtItemPedido.executeBatch(); // Executa todas as inserções de itens em um único batch

            conn.commit(); // Confirma todas as operações da transação se tudo correu bem
        } catch (SQLException e) {
            // Se algo der errado, tenta fazer rollback da transação
            if (conn != null) {
                try {
                    conn.rollback(); // Desfaz todas as operações para manter a consistência do DB
                } catch (SQLException ex) {
                    System.err.println("Erro ao fazer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Erro ao criar pedido: " + e.getMessage());
            throw e; // Lança a exceção para que o Servlet que chamou possa tratá-la
        } finally {
            // Garante que todos os recursos (ResultSet, PreparedStatements, Connection) sejam fechados
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmtPedido != null) try { stmtPedido.close(); } catch (SQLException e) {}
            if (stmtItemPedido != null) try { stmtItemPedido.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    /**
     * Deleta um pedido do banco de dados, incluindo seus itens associados.
     * Esta operação é transacional para garantir que ou tudo é deletado, ou nada é.
     *
     * @param pedidoId O ID do pedido a ser deletado.
     * @throws SQLException Se ocorrer um erro no banco de dados.
     */
    public void deletarPedido(int pedidoId) throws SQLException {
        // SQL para deletar itens na tabela Pedido_Produto (chave estrangeira)
        String sqlDeleteItens = "DELETE FROM Pedido_Produto WHERE pedido_id = ?";
        // SQL para deletar o pedido na tabela Pedido
        String sqlDeletePedido = "DELETE FROM Pedido WHERE id = ?";

        Connection conn = null;
        PreparedStatement stmtItens = null;
        PreparedStatement stmtPedido = null;

        try {
            conn = ConnectionFactory.getConnection(); // Obtém a conexão da sua fábrica
            conn.setAutoCommit(false); // Inicia uma transação para garantir atomicidade

            // 1. Deleta os itens associados ao pedido na tabela Pedido_Produto
            stmtItens = conn.prepareStatement(sqlDeleteItens);
            stmtItens.setInt(1, pedidoId);
            stmtItens.executeUpdate(); // Executa a exclusão dos itens

            // 2. Deleta o próprio pedido na tabela Pedido
            stmtPedido = conn.prepareStatement(sqlDeletePedido);
            stmtPedido.setInt(1, pedidoId);
            stmtPedido.executeUpdate(); // Executa a exclusão do pedido

            conn.commit(); // Confirma todas as operações se tudo correu bem
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Desfaz todas as operações para manter a consistência do DB
                } catch (SQLException ex) {
                    System.err.println("Erro ao fazer rollback: " + ex.getMessage());
                }
            }
            System.err.println("Erro ao deletar pedido: " + e.getMessage());
            throw e; // Lança a exceção para que o Servlet possa tratá-la
        } finally {
            // Garante que os recursos sejam fechados em qualquer cenário (sucesso ou erro).
            if (stmtItens != null) try { stmtItens.close(); } catch (SQLException e) {}
            if (stmtPedido != null) try { stmtPedido.close(); } catch (SQLException e) {}
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
}