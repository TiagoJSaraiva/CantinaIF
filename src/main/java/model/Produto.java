package model;

import java.math.BigDecimal;

public class Produto implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public enum Categoria {
        SALGADO,
        DOCE,
        LANCHE,
        BEBIDA
    }

    private int id;
    /** Dono do produto. */
    private String vendedorProntuario;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int estoque;
    private Categoria categoria;
    private byte[] imagem;
    private boolean temImagem;

    public Produto() {
    }

    public Produto(String nome, String descricao, BigDecimal preco, int estoque) {
        this(nome, descricao, preco, estoque, null, null);
    }

    public Produto(String nome, String descricao, BigDecimal preco, int estoque, Categoria categoria, byte[] imagem) {
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
        this.categoria = categoria;
        setImagem(imagem);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVendedorProntuario() { return vendedorProntuario; }
    public void setVendedorProntuario(String vendedorProntuario) { this.vendedorProntuario = vendedorProntuario; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public int getEstoque() { return estoque; }
    public void setEstoque(int estoque) { this.estoque = estoque; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public byte[] getImagem() { return imagem; }

    public void setImagem(byte[] imagem) {
        this.imagem = imagem;
        this.temImagem = imagem != null && imagem.length > 0;
    }

    /** Indica se o produto tem imagem, sem precisar carregar os bytes (listagens). */
    public boolean isTemImagem() { return temImagem; }
    public void setTemImagem(boolean temImagem) { this.temImagem = temImagem; }
}
