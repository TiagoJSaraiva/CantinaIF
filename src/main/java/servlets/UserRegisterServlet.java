package servlets;

import dao.RegraNegocioException;
import dao.UserDao;
import dao.VendedorDao;
import dao.VinculoDao;
import model.Comprador;
import utils.Limites;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Cadastro de conta, com validacao basica e tratamento de duplicidade. Dois perfis (parametro {@code perfil}):
 * <ul>
 *   <li>{@code aluno} (padrao): comprador; pode informar o codigo de vinculo de um vendedor ja no cadastro
 *       (opcional). Se o codigo falhar, a conta e criada assim mesmo e o aluno se vincula depois de entrar.</li>
 *   <li>{@code vendedor}: dono de uma cantina, com o nome dela.</li>
 * </ul>
 * O prontuario e unico entre compradores e vendedores (o login procura nas duas tabelas).
 */
@WebServlet("/register")
public class UserRegisterServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final Pattern PRONTUARIO = Pattern.compile("[A-Za-z0-9]{1,10}");
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    static final int SENHA_MINIMA = 8;

    private final UserDao userDao = new UserDao();
    private final VendedorDao vendedorDao = new VendedorDao();
    private final VinculoDao vinculoDao = new VinculoDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        boolean vendedor = "vendedor".equals(request.getParameter("perfil"));
        String prontuario = valor(request, "prontuario");
        String email = valor(request, "email");
        String nome = valor(request, vendedor ? "loja" : "nome");
        String codigo = vendedor ? "" : valor(request, "codigo");
        String senha = request.getParameter("senha");

        Map<String, String> erros = vendedor ? validarVendedor(prontuario, email, nome, senha)
                : validar(prontuario, email, nome, senha);
        String erro = null;
        if (erros.isEmpty()) {
            try {
                if (UserDao.prontuarioEmUso(prontuario)) {
                    duplicado(erros);
                    erro = "Prontuário ou e-mail já cadastrado.";
                } else if (vendedor) {
                    Limites.verificarNovoVendedor();
                    if (vendedorDao.create(prontuario, nome, email, senha)) {
                        response.sendRedirect("login.jsp?cadastro=vendedor");
                        return;
                    }
                    duplicado(erros);
                    erro = "Prontuário ou e-mail já cadastrado.";
                } else {
                    Limites.verificarNovoComprador();
                    if (userDao.create(new Comprador(prontuario, email, senha, nome))) {
                        response.sendRedirect("login.jsp?cadastro=ok" + vincular(codigo, prontuario));
                        return;
                    }
                    duplicado(erros);
                    erro = "Prontuário ou e-mail já cadastrado.";
                }
            } catch (RegraNegocioException e) {
                erro = e.getMessage();
            } catch (SQLException e) {
                e.printStackTrace();
                erro = "Não foi possível concluir o cadastro. Tente novamente em instantes.";
            }
        } else {
            erro = "Corrija os campos destacados.";
        }
        request.setAttribute("error", erro);
        request.setAttribute("erros", erros);
        request.setAttribute("perfil", vendedor ? "vendedor" : "aluno");
        request.setAttribute("prontuario", prontuario);
        request.setAttribute("email", email);
        request.setAttribute(vendedor ? "loja" : "nome", nome);
        request.setAttribute("codigo", codigo);
        request.getRequestDispatcher("cadastro.jsp").forward(request, response);
    }

    /** Tenta o codigo de vinculo informado no cadastro; devolve o sufixo da URL do login com o resultado. */
    private String vincular(String codigo, String prontuario) {
        if (codigo.isEmpty()) {
            return "";
        }
        try {
            vinculoDao.resgatar(codigo, prontuario, LocalDateTime.now());
            return "&vinculo=ok";
        } catch (RegraNegocioException e) {
            return "&vinculo=falhou";
        } catch (SQLException e) {
            e.printStackTrace();
            return "&vinculo=falhou";
        }
    }

    private static void duplicado(Map<String, String> erros) {
        erros.put("prontuario", "Este prontuário ou e-mail já está cadastrado.");
    }

    /** Valida os campos do aluno; retorna mensagens por campo (vazio quando tudo e valido). */
    static Map<String, String> validar(String prontuario, String email, String nome, String senha) {
        Map<String, String> erros = comuns(prontuario, email, senha);
        if (nome.isEmpty() || nome.length() > 75) {
            erros.put("nome", "Informe seu nome (até 75 caracteres).");
        }
        return erros;
    }

    /** Valida os campos do vendedor ({@code loja} = nome da cantina). */
    static Map<String, String> validarVendedor(String prontuario, String email, String loja, String senha) {
        Map<String, String> erros = comuns(prontuario, email, senha);
        if (loja.length() < 2 || loja.length() > 75) {
            erros.put("loja", "Informe o nome da cantina (de 2 a 75 caracteres).");
        }
        return erros;
    }

    private static Map<String, String> comuns(String prontuario, String email, String senha) {
        Map<String, String> erros = new LinkedHashMap<>();
        if (!PRONTUARIO.matcher(prontuario).matches()) {
            erros.put("prontuario", "Use de 1 a 10 letras ou números.");
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            erros.put("email", "Informe um e-mail válido.");
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
