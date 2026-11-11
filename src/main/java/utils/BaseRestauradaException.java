package utils;

/**
 * A base de demonstracao foi restaurada depois do inicio da sessao desta requisicao: o usuario pode nao existir
 * mais. Lancada ANTES de qualquer acesso ao banco; o AuthFilter a transforma em redirecionamento ao login
 * (paginas) ou 401 com {@code restaurada=true} (APIs). Os servlets nao devem engoli-la.
 */
public class BaseRestauradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BaseRestauradaException() {
        super("A demonstração foi restaurada; entre novamente.");
    }
}
