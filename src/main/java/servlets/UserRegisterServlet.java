package servlets;

import dao.UserDao;
import model.Comprador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Cadastro de comprador com validacao basica e tratamento de duplicidade. */
@WebServlet("/register")
public class UserRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Pattern PRONTUARIO = Pattern.compile("[A-Za-z0-9]{1,10}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    static final int SENHA_MINIMA = 8;

    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String prontuario = valor(request, "prontuario");
        String email = valor(request, "email");
        String nome = valor(request, "nome");
        String senha = request.getParameter("senha");

        Map<String, String> erros = validar(prontuario, email, nome, senha);
        String erro = null;
        if (erros.isEmpty()) {
            try {
                if (userDao.create(new Comprador(prontuario, email, senha, nome))) {
                    response.sendRedirect("login.jsp?cadastro=ok");
                    return;
                }
                erro = "Prontuário ou e-mail já cadastrado.";
                erros.put("prontuario", "Este prontuário ou e-mail já está cadastrado.");
            } catch (SQLException e) {
                e.printStackTrace();
                erro = "Não foi possível concluir o cadastro. Tente novamente em instantes.";
            }
        } else {
            erro = "Corrija os campos destacados.";
        }
        request.setAttribute("error", erro);
        request.setAttribute("erros", erros);
        request.setAttribute("prontuario", prontuario);
        request.setAttribute("email", email);
        request.setAttribute("nome", nome);
        request.getRequestDispatcher("cadastro.jsp").forward(request, response);
    }

    /** Valida os campos; retorna mensagens por campo (vazio quando tudo e valido). */
    static Map<String, String> validar(String prontuario, String email, String nome, String senha) {
        Map<String, String> erros = new LinkedHashMap<>();
        if (!PRONTUARIO.matcher(prontuario).matches()) {
            erros.put("prontuario", "Use de 1 a 10 letras ou números.");
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            erros.put("email", "Informe um e-mail válido.");
        }
        if (nome.isEmpty() || nome.length() > 75) {
            erros.put("nome", "Informe seu nome (até 75 caracteres).");
        }
        if (senha == null || senha.length() < SENHA_MINIMA || senha.length() > 72) {
            erros.put("senha", "A senha deve ter de " + SENHA_MINIMA + " a 72 caracteres.");
        }
        return erros;
    }

    private static String valor(HttpServletRequest request, String nome) {
        String v = request.getParameter(nome);
        return v == null ? "" : v.trim();
    }
}
