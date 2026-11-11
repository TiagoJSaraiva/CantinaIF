package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Pedido de um comprador. Os itens guardam o preco unitario do momento da compra. */
public class Pedido {

    private int id;
    private Comprador comprador;
    private List<ItemCarrinho> itens = new ArrayList<>();
    private StatusPedido status = StatusPedido.PENDENTE;
    private LocalDateTime criadoEm;
    private BigDecimal total = BigDecimal.ZERO;

    public Pedido() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Comprador getComprador() { return comprador; }
    public void setComprador(Comprador comprador) { this.comprador = comprador; }

    public List<ItemCarrinho> getItens() { return itens; }
    public void setItens(List<ItemCarrinho> itens) { this.itens = itens; }

    public void addItem(ItemCarrinho item) { this.itens.add(item); }

    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
