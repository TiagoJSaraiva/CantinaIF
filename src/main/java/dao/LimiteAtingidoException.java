package dao;

/** Um teto da demonstracao foi atingido (a mensagem pode ser mostrada ao usuario). */
public class LimiteAtingidoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    /** Mensagem completa, ja pronta para o usuario. */
    public LimiteAtingidoException(String mensagem) {
        super(mensagem);
    }

    public LimiteAtingidoException(String o_que, long maximo) {
        super("Limite da demonstração atingido (" + o_que + ": máximo " + maximo
                + "); a base é restaurada periodicamente.");
    }
}
