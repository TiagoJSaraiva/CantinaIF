package dao;

import model.StatusPedido;

/** Mudanca de status nao permitida pelo ciclo de vida do pedido. */
public class TransicaoInvalidaException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    public TransicaoInvalidaException(StatusPedido de, StatusPedido para) {
        super("Não é possível mudar o pedido de \"" + de.getRotulo() + "\" para \"" + para.getRotulo() + "\".");
    }

    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
