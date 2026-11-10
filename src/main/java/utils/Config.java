package utils;

/** Leitura de configuracao: variavel de ambiente e, em seguida, propriedade de sistema. */
public final class Config {

    private Config() {
    }

    /** Retorna o valor configurado ou null se ausente/vazio. */
    public static String get(String nome) {
        String valor = System.getenv(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getProperty(nome);
        }
        return (valor == null || valor.isBlank()) ? null : valor;
    }
}
