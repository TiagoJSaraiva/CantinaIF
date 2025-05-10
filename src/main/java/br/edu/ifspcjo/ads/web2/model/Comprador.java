package model;

public class Comprador {
	private String prontuario;
	private String email;
	private String senha;
	
	public Comprador(String prontuario, String email, String senha) {
        this.prontuario = prontuario;
        this.email = email;
        this.senha = senha;
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
}
