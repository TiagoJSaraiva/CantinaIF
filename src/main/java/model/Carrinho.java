package model;

import java.util.ArrayList;
import java.util.List;

public class Carrinho {
    private List<ItemCarrinho> itens;

    public Carrinho() {
        this.itens = new ArrayList<>();
    }

    public void adicionarProduto(Produto produto, int quantidade) {
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId() == produto.getId()) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                return;
            }
        }
        itens.add(new ItemCarrinho(produto, quantidade));
    }

    public void removerProduto(int produtoId) {
        itens.removeIf(item -> item.getProduto().getId() == produtoId);
    }

    public void limparCarrinho() {
        itens.clear();
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public double getTotal() {
        double total = 0;
        for (ItemCarrinho item : itens) {
            total += item.getSubtotal();
        }
        return total;
    }

    public boolean isVazio() {
        return itens.isEmpty();
    }
}