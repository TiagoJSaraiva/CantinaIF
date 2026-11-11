package model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusPedidoTest {

    @Test
    void fluxoNormalAvancaUmPassoPorVez() {
        assertTrue(StatusPedido.PENDENTE.podeIrPara(StatusPedido.EM_PREPARO));
        assertTrue(StatusPedido.EM_PREPARO.podeIrPara(StatusPedido.PRONTO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.ENTREGUE));
    }

    @Test
    void naoPermitePularRetrocederNemSairDeEstadoFinal() {
        assertFalse(StatusPedido.PENDENTE.podeIrPara(StatusPedido.PRONTO));
        assertFalse(StatusPedido.PENDENTE.podeIrPara(StatusPedido.ENTREGUE));
        assertFalse(StatusPedido.PRONTO.podeIrPara(StatusPedido.PENDENTE));
        assertFalse(StatusPedido.PENDENTE.podeIrPara(StatusPedido.PENDENTE));
        assertTrue(StatusPedido.ENTREGUE.isFinal());
        assertTrue(StatusPedido.CANCELADO.isFinal());
        for (StatusPedido s : StatusPedido.values()) {
            assertFalse(StatusPedido.ENTREGUE.podeIrPara(s));
            assertFalse(StatusPedido.CANCELADO.podeIrPara(s));
        }
    }

    @Test
    void qualquerEstadoNaoFinalPodeSerCancelado() {
        assertTrue(StatusPedido.PENDENTE.podeIrPara(StatusPedido.CANCELADO));
        assertTrue(StatusPedido.EM_PREPARO.podeIrPara(StatusPedido.CANCELADO));
        assertTrue(StatusPedido.PRONTO.podeIrPara(StatusPedido.CANCELADO));
    }
}
