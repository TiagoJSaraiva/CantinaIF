package model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Ciclo de vida de um pedido: PENDENTE -> EM_PREPARO -> PRONTO -> ENTREGUE.
 * Pedidos nao entregues podem ser CANCELADOS (devolvendo o estoque). ENTREGUE e CANCELADO sao finais.
 */
public enum StatusPedido {
    PENDENTE("Pendente"),
    EM_PREPARO("Em preparo"),
    PRONTO("Pronto"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusPedido(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Status para os quais este status pode transitar. */
    public Set<StatusPedido> proximos() {
        switch (this) {
            case PENDENTE:
                return EnumSet.of(EM_PREPARO, CANCELADO);
            case EM_PREPARO:
                return EnumSet.of(PRONTO, CANCELADO);
            case PRONTO:
                return EnumSet.of(ENTREGUE, CANCELADO);
            default:
                return EnumSet.noneOf(StatusPedido.class);
        }
    }

    public boolean podeIrPara(StatusPedido novo) {
        return proximos().contains(novo);
    }

    public boolean isFinal() {
        return proximos().isEmpty();
    }
}
