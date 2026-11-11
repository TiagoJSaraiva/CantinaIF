package dao;

/** Algum item do pedido nao tem estoque suficiente; nada e gravado. */
public class EstoqueInsuficienteException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final String produto;
    private final int disponivel;

    public EstoqueInsuficienteException(String produto, int disponivel) {
        super("Estoque insuficiente para \"" + produto + "\" (disponível: " + disponivel + ").");
        this.produto = produto;
        this.disponivel = disponivel;
    }

    public String getProduto() {
        return produto;
    }

    public int getDisponivel() {
        return disponivel;
    }
}
