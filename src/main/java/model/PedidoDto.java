package model;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Representacao de pedido enviada ao navegador (JSON). */
public class PedidoDto {

    public static class Item {
        public final int produtoId;
        public final String nome;
        public final int quantidade;
        public final BigDecimal precoUnitario;
        public final BigDecimal subtotal;

        Item(ItemCarrinho i) {
            this.produtoId = i.getProduto().getId();
            this.nome = i.getProduto().getNome();
            this.quantidade = i.getQuantidade();
            this.precoUnitario = i.getProduto().getPreco();
            this.subtotal = i.getSubtotal();
        }
    }

    public static class Cliente {
        public final String prontuario;
        public final String nome;
        public final String email;

        Cliente(Comprador c) {
            this.prontuario = c.getProntuario();
            this.nome = c.getNome();
            this.email = c.getEmail();
        }
    }

    public final int id;
    public final String status;
    public final String statusRotulo;
    /** Data/hora de criacao em ISO-8601 com deslocamento de fuso. */
    public final String criadoEm;
    public final BigDecimal total;
    public final Cliente comprador;
    public final List<Item> itens = new ArrayList<>();
    /** Status para os quais o vendedor pode avancar/mudar este pedido. */
    public final List<String> proximos = new ArrayList<>();
    /** O comprador so pode cancelar enquanto o pedido esta pendente. */
    public final boolean podeCancelar;

    public PedidoDto(Pedido p, boolean incluirComprador) {
        this.id = p.getId();
        this.status = p.getStatus().name();
        this.statusRotulo = p.getStatus().getRotulo();
        this.criadoEm = p.getCriadoEm() == null ? null
                : p.getCriadoEm().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        this.total = p.getTotal();
        this.comprador = (incluirComprador && p.getComprador() != null) ? new Cliente(p.getComprador()) : null;
        for (ItemCarrinho i : p.getItens()) {
            itens.add(new Item(i));
        }
        for (StatusPedido s : p.getStatus().proximos()) {
            proximos.add(s.name());
        }
        this.podeCancelar = p.getStatus() == StatusPedido.PENDENTE;
    }
}
