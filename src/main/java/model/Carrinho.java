package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Carrinho de compras guardado na sessao do comprador. */
public class Carrinho implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final List<ItemCarrinho> itens = new ArrayList<>();

    public synchronized void adicionarProduto(Produto produto, int quantidade) {
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId() == produto.getId()) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                return;
            }
        }
        itens.add(new ItemCarrinho(produto, quantidade));
    }

    /** Define a quantidade absoluta de um item ja existente; retorna false se o item nao esta no carrinho. */
    public synchronized boolean alterarQuantidade(int produtoId, int quantidade) {
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId() == produtoId) {
                item.setQuantidade(quantidade);
                return true;
            }
        }
        return false;
    }

    public synchronized void removerProduto(int produtoId) {
        itens.removeIf(item -> item.getProduto().getId() == produtoId);
    }

    public synchronized void limparCarrinho() {
        itens.clear();
    }

    /** Copia dos itens (seguro para iterar). */
    public synchronized List<ItemCarrinho> getItens() {
        return new ArrayList<>(itens);
    }

    public synchronized int getQuantidadeTotal() {
        int total = 0;
        for (ItemCarrinho item : itens) {
            total += item.getQuantidade();
        }
        return total;
    }

    public synchronized BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    public synchronized boolean isVazio() {
        return itens.isEmpty();
    }
}
