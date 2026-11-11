package dao;

import java.math.BigDecimal;

/** O preco de um item mudou desde que ele entrou no carrinho; nada e gravado ate o comprador confirmar de novo. */
public class PrecoAlteradoException extends RegraNegocioException {

    private static final long serialVersionUID = 1L;

    private final String produto;
    private final BigDecimal anterior;
    private final BigDecimal atual;

    public PrecoAlteradoException(String produto, BigDecimal anterior, BigDecimal atual) {
        super("O preço de \"" + produto + "\" mudou de R$ " + anterior.setScale(2, java.math.RoundingMode.HALF_UP)
                + " para R$ " + atual.setScale(2, java.math.RoundingMode.HALF_UP)
                + ". Seu carrinho foi atualizado; confira e finalize novamente.");
        this.produto = produto;
        this.anterior = anterior;
        this.atual = atual;
    }

    public String getProduto() { return produto; }
    public BigDecimal getAnterior() { return anterior; }
    public BigDecimal getAtual() { return atual; }
}
