package model;

public class Pedido {
	private int id;
	private Comprador comprador;
	private Produto produto;
	
	public Pedido(int id, Comprador comprador, Produto produto) {
        this.id = id;
        this.comprador = comprador;
        this.produto = produto;
    }
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public Comprador getComprador() {
		return comprador;
	}
	
	public void setComprador(Comprador comprador) {
		this.comprador = comprador;
	}
	
	public Produto getProduto() {
		return produto;
	}
	
	public void setProduto(Produto produto) {
		this.produto = produto;
	}
}
