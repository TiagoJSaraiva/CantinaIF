package model;

import java.time.LocalDateTime;

/**
 * Linha da tabela de compradores do vendedor: dados do comprador e numeros do relacionamento com ESSE vendedor.
 *
 * @param canceladosPeloComprador pedidos que o proprio comprador cancelou
 * @param canceladosPeloVendedor  pedidos que o vendedor cancelou
 * @param ultimoPedido            data do pedido mais recente (null se nunca pediu)
 */
public record CompradorResumo(String prontuario, String nome, String email, boolean ativo, int pedidos,
        int canceladosPeloComprador, int canceladosPeloVendedor, LocalDateTime ultimoPedido, int advertencias) {
}
