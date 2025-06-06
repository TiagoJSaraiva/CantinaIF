package model;

public class Produto 
{
    // Classe do tipo enum para o atributo Categoria
    public enum Categoria {
	SALGADO,
	DOCE,
	LANCHE,
	BEBIDA
    }
	
    private int id;
    private String nome;
    private String descricao;
    private float preco;
    private int estoque;
    private Categoria categoria;
    private byte[] imagem;
	
    // Construtor original
    public Produto(String nome, String descricao, float preco, int estoque) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.imagem = null; // Inicializa imagem como null por padrão neste construtor
    }

    // Construtor adicional para incluir a imagem
    public Produto(String nome, String descricao, float preco, int estoque, byte[] imagem) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.imagem = imagem;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
    
    public byte[] getImagem() {
        return imagem;
    }

    public void setImagem(byte[] imagem) {
        this.imagem = imagem;
    }
}
