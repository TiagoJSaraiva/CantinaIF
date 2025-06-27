package utils; // Garanta que este é o pacote correto

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException; // Importe SQLException para lançar a exceção de forma mais específica

public class ConnectionFactory {
    // --- Credenciais do Banco de Dados ---
    // URL de conexão com o MySQL, incluindo parâmetros importantes para evitar avisos/erros.
    private static final String URL = "jdbc:mysql://localhost:3306/todoapp?useSSL=false&serverTimezone=UTC";
    
    // Nome de usuário do seu banco de dados MySQL.
    private static final String USER = "root"; 
    
    // ATENÇÃO: SENHA DO SEU USUÁRIO MYSQL.
    // Você mencionou 'REMOVIDO' anteriormente. Se esta for sua senha real, use-a.
    // Se a senha for realmente vazia, mantenha "".
    private static final String PASSWORD = "REMOVIDO"; // <--- AJUSTE SUA SENHA AQUI SE NÃO FOR ESTA

    /**
     * Retorna uma nova conexão com o banco de dados.
     * @return Objeto Connection para o banco de dados.
     * @throws SQLException Se ocorrer um erro ao estabelecer a conexão.
     */
    public static Connection getConnection() throws SQLException { // Agora lança SQLException
        try {
            // Carrega o driver JDBC do MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Estabelece e retorna a conexão
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            // Se o driver não for encontrado (JAR do MySQL Connector faltando no classpath)
            System.err.println("Erro: Driver JDBC do MySQL não encontrado no classpath.");
            e.printStackTrace();
            throw new SQLException("Driver JDBC do MySQL não encontrado.", e); // Lança SQLException específica
        }
        // Não é necessário um catch para SQLException aqui, pois getConnection já a lança.
        // Se houver problemas com URL, USER, PASSWORD, DriverManager.getConnection() já lançará SQLException.
    }

    /**
     * Método auxiliar para fechar conexões de forma segura.
     * Pode ser usado em blocos finally dos seus DAOs.
     * @param conn A conexão a ser fechada.
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar a conexão: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}