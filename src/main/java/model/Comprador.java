package model; // Certifique-se de que o nome do pacote está correto

public class Comprador implements java.io.Serializable {
	private String prontuario;
	private String email;
	private String senha;
	private String nome;
	/** Vendedor vinculado (null = sem vinculo). Na sessao e so uma fotografia; o estado atual vem do banco. */
	private String vendedorProntuario;
	private String vendedorNome;
	private boolean ativo = true;
	
    // NOVO: Construtor padrão (sem argumentos) - Essencial para o Gson!
    public Comprador() {
    }

    // Construtor existente
	public Comprador(String prontuario, String email, String senha, String nome) {
        this.prontuario = prontuario;
        this.email = email;
        this.senha = senha;
        this.nome = nome;
    }
	
	public String getProntuario() {
		return prontuario;
	}
	
	public void setProntuario(String prontuario) {
		this.prontuario = prontuario;
	}
	
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getSenha() {
		return senha;
	}
	
	public void setSenha(String senha) {
		this.senha = senha;
	}
	
	public String getNome() {
		return nome;
	}
	
	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getVendedorProntuario() {
		return vendedorProntuario;
	}

	public void setVendedorProntuario(String vendedorProntuario) {
		this.vendedorProntuario = vendedorProntuario;
	}

	public String getVendedorNome() {
		return vendedorNome;
	}

	public void setVendedorNome(String vendedorNome) {
		this.vendedorNome = vendedorNome;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
}
