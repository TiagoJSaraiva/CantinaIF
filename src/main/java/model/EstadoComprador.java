package model;

/**
 * Estado atual de um comprador, lido do banco a cada requisicao (o vendedor pode desativa-lo ou o comprador pode
 * trocar de vendedor a qualquer momento; a sessao nao e confiavel para isso).
 *
 * @param vendedorProntuario vendedor vinculado, ou null
 * @param vendedorNome       nome da cantina vinculada, ou null
 * @param naoLidas           advertencias ainda nao lidas
 */
public record EstadoComprador(boolean ativo, String vendedorProntuario, String vendedorNome, int naoLidas) {

    public boolean vinculado() {
        return vendedorProntuario != null;
    }

    // getters no estilo JavaBean: o EL do Tomcat 10.1 (EL 5.0) nao le componentes de record
    public boolean isAtivo() { return ativo; }
    public boolean isVinculado() { return vinculado(); }
    public String getVendedorNome() { return vendedorNome; }
    public int getNaoLidas() { return naoLidas; }
}
