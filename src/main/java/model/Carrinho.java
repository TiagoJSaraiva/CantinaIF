package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Carrinho de compras guardado na sessao do comprador. */
public class Carrinho implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /** Maximo de unidades de um mesmo item no carrinho. */
    public static final int MAX_POR_ITEM = 99;

    private final List<ItemCarrinho> itens = new ArrayList<>();

    public synchronized void adicionarProduto(Produto produto, int quantidade) {
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId() == produto.getId()) {
                item.setQuantidade(somaSegura(item.getQuantidade(), quantidade));
                return;
            }
        }
        itens.add(new ItemCarrinho(produto, Math.min(quantidade, MAX_POR_ITEM)));
    }

    /** Soma sem estouro de int e limitada a {@link #MAX_POR_ITEM}. */
    public static int somaSegura(int atual, int adicional) {
        long soma = (long) atual + (long) adicional;
        return (int) Math.max(0, Math.min(soma, MAX_POR_ITEM));
    }

    public synchronized int quantidadeDe(int produtoId) {
        for (ItemCarrinho item : itens) {
            if (item.getProduto().getId() == produtoId) {
                return item.getQuantidade();
            }
        }
        return 0;
    }

    /** Atualiza preco e estoque dos itens a partir de uma lista atual de produtos; remove os que nao existem mais. */
    public synchronized void atualizarDe(java.util.Map<Integer, Produto> atuais) {
        itens.removeIf(item -> !atuais.containsKey(item.getProduto().getId()));
        for (ItemCarrinho item : itens) {
            Produto novo = atuais.get(item.getProduto().getId());
            item.getProduto().setPreco(novo.getPreco());
            item.getProduto().setEstoque(novo.getEstoque());
            item.getProduto().setNome(novo.getNome());
            if (item.getQuantidade() > novo.getEstoque()) {
                item.setQuantidade(Math.max(1, novo.getEstoque()));
            }
        }
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
