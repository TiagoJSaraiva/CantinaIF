package model;

public class Produto {
	private String nome;
	private String descricao;
	private float preco;
	private int estoque;
	private bool disponivel;
	
	public Produto(String nome, String descricao, float preco, int estoque, bool disponivel) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
	this.disponivel = disponivel;
    }
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}
	
	public float getPreco() {
		return preco;
	}
	
	public void setPreco(float preco) {
		this.preco = preco;
	}
	
	public int getEstoque() {
		return estoque;
	}
	
	public void setEstoque(int estoque) {
		this.estoque = estoque;
	}

	public boolean isDisponivel() {
        	return this.disponivel;
    	}

    	public void setDisponivel(boolean disponivel) {
        	this.disponivel = disponivel;
    	}
}
