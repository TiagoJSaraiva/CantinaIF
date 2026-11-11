package dao;

/** Violacao de uma regra de negocio (a mensagem pode ser mostrada ao usuario). */
public class RegraNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
