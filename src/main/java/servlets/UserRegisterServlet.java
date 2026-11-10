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

        String erro = validar(prontuario, email, nome, senha);
        if (erro == null) {
            try {
                if (userDao.create(new Comprador(prontuario, email, senha, nome))) {
                    response.sendRedirect("login.jsp?cadastro=ok");
                    return;
                }
                erro = "Prontuário ou e-mail já cadastrado.";
            } catch (SQLException e) {
                e.printStackTrace();
                erro = "Não foi possível concluir o cadastro. Tente novamente em instantes.";
            }
        }
        request.setAttribute("error", erro);
        request.setAttribute("prontuario", prontuario);
        request.setAttribute("email", email);
        request.setAttribute("nome", nome);
        request.getRequestDispatcher("cadastro.jsp").forward(request, response);
    }

    static String validar(String prontuario, String email, String nome, String senha) {
        if (!PRONTUARIO.matcher(prontuario).matches()) {
            return "O prontuário deve ter de 1 a 10 letras ou números.";
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            return "Informe um e-mail válido.";
        }
        if (nome.isEmpty() || nome.length() > 75) {
            return "Informe seu nome (até 75 caracteres).";
        }
        if (senha == null || senha.length() < SENHA_MINIMA || senha.length() > 72) {
            return "A senha deve ter de " + SENHA_MINIMA + " a 72 caracteres.";
        }
        return null;
    }

    private static String valor(HttpServletRequest request, String nome) {
        String v = request.getParameter(nome);
        return v == null ? "" : v.trim();
    }
}
