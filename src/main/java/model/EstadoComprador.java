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
}
