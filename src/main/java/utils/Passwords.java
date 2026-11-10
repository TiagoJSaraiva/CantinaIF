package utils;

import at.favre.lib.crypto.bcrypt.BCrypt;

/** Hash e verificacao de senhas com BCrypt. */
public final class Passwords {

    private static final int COST = 10;

    private Passwords() {
    }

    /** Gera o hash BCrypt (60 caracteres) da senha informada. */
    public static String hash(String senha) {
        return BCrypt.withDefaults().hashToString(COST, senha.toCharArray());
    }

    /** Confere a senha contra um hash BCrypt; retorna false se algum dos dois for nulo ou invalido. */
    public static boolean verify(String senha, String hash) {
        if (senha == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.verifyer().verify(senha.toCharArray(), hash).verified;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
