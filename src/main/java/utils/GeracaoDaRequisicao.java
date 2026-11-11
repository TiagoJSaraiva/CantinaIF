package utils;

/** Geracao da base com a qual a sessao desta requisicao foi autenticada (por thread, durante a requisicao). */
public final class GeracaoDaRequisicao {

    private static final ThreadLocal<Long> ATUAL = new ThreadLocal<>();

    private GeracaoDaRequisicao() {
    }

    public static void definir(Long geracao) {
        if (geracao == null) {
            ATUAL.remove();
        } else {
            ATUAL.set(geracao);
        }
    }

    public static Long atual() {
        return ATUAL.get();
    }

    public static void limpar() {
        ATUAL.remove();
    }
}
