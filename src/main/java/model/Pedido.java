package model; // Verifique se o pacote 'model' está correto

import java.util.ArrayList;
import java.util.List;

public class Pedido {
    private int id;
    private Comprador comprador; // O comprador que fez este pedido
    private List<ItemCarrinho> itens; // A lista de itens (Produto + Quantidade) deste pedido

    // Construtor padrão (sem argumentos) - Essencial para o Gson
    public Pedido() {
        this.itens = new ArrayList<>(); // É crucial inicializar a lista aqui
    }

    // Construtor para criar um Pedido com um ID e um Comprador
    public Pedido(int id, Comprador comprador) {
        this.id = id;
        this.comprador = comprador;
        this.itens = new ArrayList<>(); // É crucial inicializar a lista aqui também
    }

    // Getters
    public int getId() {
        return id;
    }

    public Comprador getComprador() {
        return comprador;
    }

    public List<ItemCarrinho> getItens() { // Agora retorna List<ItemCarrinho>
        return itens;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setComprador(Comprador comprador) {
        this.comprador = comprador;
    }

    public void setItens(List<ItemCarrinho> itens) { // Agora aceita List<ItemCarrinho>
        this.itens = itens;
    }

    // MÉTODO 'addItem' QUE ESTÁ FALTANDO OU INCORRETO NO SEU ARQUIVO:
    // Método de conveniência para adicionar um único ItemCarrinho à lista
    public void addItem(ItemCarrinho item) {
        if (this.itens == null) { // Garante que a lista exista antes de adicionar (caso não tenha sido inicializada no construtor)
            this.itens = new ArrayList<>();
        }
        this.itens.add(item);
    }
}