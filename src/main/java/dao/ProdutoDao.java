// AVISO: Ainda não testado por prolemas com a página home.

package dao;

import model.Produto;
import utils.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDao {
    private Connection connection;

    public ProdutoDao(Connection connection) {
        this.connection = connection;
    }

	public ProdutoDao() {
		// TODO Auto-generated constructor stub
	}

	//para criação de produtos
    public void create(Produto produto) throws SQLException {
        String sql = "INSERT INTO produtos (nome, descricao, preco, estoque, categoria) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setFloat(3, produto.getPreco());
            stmt.setInt(4, produto.getEstoque());
            stmt.setString(5, produto.getCategoria().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
        }
    }

 // Listar todos os produtos
    public List<Produto> listarTodosProdutos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produtos;
    }

    // Listar produtos com quantidade disponível > 0
    public List<Produto> listarProdutosDisponiveis() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT * FROM Produto WHERE estoque > 0";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produtos;
    }

    // Listar produtos por categoria (com opção de incluir/excluir estoque zero)
    //public List<Produto> listarProdutosPorCategoria(String categoriaNome, boolean incluirEstoqueZero) {
      //  List<Produto> produtos = new ArrayList<>();
        //String sql = "SELECT * FROM Produto WHERE categoria = ?";
     //   if (!incluirEstoqueZero) {
      //      sql += " AND estoque > 0";
     //   }

      //  try (Connection conn = ConnectionFactory.getConnection();
     //        PreparedStatement stmt = conn.prepareStatement(sql)) {
//
  //          stmt.setString(1, categoriaNome);
    //        try (ResultSet rs = stmt.executeQuery()) {
      //          while (rs.next()) {
        //            produtos.add(mapearProduto(rs));
          //      }
 //           }

   //     } catch (SQLException e) {
     //     e.printStackTrace();
      //  }

  //      return produtos;
 //   }

    // Método auxiliar para mapear os dados do ResultSet para o objeto Produto
    private Produto mapearProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto(null, null, 0, 0);
        produto.setId(rs.getInt("id"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setPreco(rs.getFloat("preco"));
        produto.setEstoque(rs.getInt("estoque"));
        produto.setCategoria(((Produto) rs).getCategoria());
        // Adicione mais campos conforme sua classe Produto

        return produto;
    }

    
    //para o carrinho
    public void reservarEstoque(int produtoId, int quantidade) throws SQLException {
        String sql = "UPDATE produtos SET estoque = estoque - ? WHERE id = ? AND estoque >= ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            stmt.executeUpdate();
        }
    }

    private Connection getConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	public Produto buscarPorId(int id) {
        Produto produto = null;
        String sql = "SELECT * FROM produtos WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                produto = new Produto(
                    rs.getString("nome"),
                    rs.getString("descricao"),
                    rs.getFloat("preco"),
                    rs.getInt("estoque")
                );
                produto.setId(rs.getInt("id"));
                produto.setCategoria(Produto.Categoria.valueOf(rs.getString("categoria")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return produto;
    }

}
