package model;

import java.time.LocalDateTime;

/** Advertencia enviada por um vendedor a um comprador. {@code lidaEm} e null enquanto nao foi lida. */
public record Advertencia(int id, String vendedorProntuario, String vendedorNome, String mensagem,
        LocalDateTime criadaEm, LocalDateTime lidaEm) {

    public boolean lida() {
        return lidaEm != null;
    }
}
