package model;

/** Representacao de produto enviada ao navegador (nunca inclui os bytes da imagem). */
public class ProdutoDto {

    private final int id;
    private final String nome;
    private final String descricao;
    private final float preco;
    private final int estoque;
    private final String categoria;
    private final boolean temImagem;

    public ProdutoDto(Produto p) {
        this.id = p.getId();
        this.nome = p.getNome();
        this.descricao = p.getDescricao();
        this.preco = p.getPreco();
        this.estoque = p.getEstoque();
        this.categoria = p.getCategoria() != null ? p.getCategoria().name().toLowerCase() : null;
        this.temImagem = p.getImagem() != null && p.getImagem().length > 0;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getDescricao() { return descricao; }
    public float getPreco() { return preco; }
    public int getEstoque() { return estoque; }
    public String getCategoria() { return categoria; }
    public boolean isTemImagem() { return temImagem; }
}
