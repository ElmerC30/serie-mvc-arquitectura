package modelo;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de cotizar o vender un pedido. Los montos ya vienen calculados por el modelo. */
public final class Ticket {
    private final List<RenglonTicket> renglones;

    public Ticket(List<RenglonTicket> renglones) {
        this.renglones = List.copyOf(renglones);
    }

    public static Ticket vacio() {
        return new Ticket(List.of());
    }

    public List<RenglonTicket> getRenglones() { return renglones; }

    public boolean estaVacio() { return renglones.isEmpty(); }

    public BigDecimal getSubtotal() {
        return renglones.stream()
                .map(r -> r.precioUnitario().multiply(BigDecimal.valueOf(r.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getDescuentoTotal() {
        return renglones.stream().map(RenglonTicket::descuento).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotal() {
        return renglones.stream().map(RenglonTicket::importe).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
