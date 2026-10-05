package modelo;

import static apoyo.Inventarios.BOQ_ALTO;
import static apoyo.Inventarios.CANA_ALTO;
import static apoyo.Inventarios.CANA_TENOR;
import static apoyo.Inventarios.SAX_ALTO;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import apoyo.Inventarios;

@DisplayName("Regla 3 - descuento de kit de inicio (15 % en 1 boquilla y hasta 10 cañas por saxofón)")
class ReglaKitDeInicioTest {
    private Tienda tienda;

    @BeforeEach
    void preparar() {
        tienda = Inventarios.tiendaDePrueba();
    }

    private static BigDecimal pesos(String monto) {
        return new BigDecimal(monto);
    }

    @Test
    @DisplayName("Kit completo: saxofón + boquilla + 10 cañas llevan descuento en los accesorios")
    void kitCompleto() throws Exception {
        Ticket t = tienda.cotizar(List.of(
                new LineaPedido(SAX_ALTO, 1), new LineaPedido(BOQ_ALTO, 1), new LineaPedido(CANA_ALTO, 10)));
        // 4290 x 0.15 = 643.50 ; 89 x 10 x 0.15 = 133.50
        assertEquals(0, pesos("777.00").compareTo(t.getDescuentoTotal()));
        assertEquals(0, pesos("18179.00").compareTo(t.getSubtotal()));
        assertEquals(0, pesos("17402.00").compareTo(t.getTotal()));
    }

    @Test
    @DisplayName("El saxofón nunca lleva descuento")
    void saxofonSinDescuento() throws Exception {
        Ticket t = tienda.cotizar(List.of(new LineaPedido(SAX_ALTO, 1)));
        assertEquals(0, BigDecimal.ZERO.compareTo(t.getDescuentoTotal()));
    }

    @Test
    @DisplayName("Sólo las primeras 10 cañas por saxofón llevan descuento")
    void topeDeCanas() throws Exception {
        Ticket t = tienda.cotizar(List.of(new LineaPedido(SAX_ALTO, 1), new LineaPedido(CANA_ALTO, 15)));
        RenglonTicket canas = t.getRenglones().get(1);
        assertEquals(10, canas.unidadesConDescuento());
        assertEquals(0, pesos("133.50").compareTo(canas.descuento()));
        assertEquals(0, pesos("1201.50").compareTo(canas.importe()));
    }

    @Test
    @DisplayName("Dos saxofones duplican el cupo: 2 boquillas con descuento, la tercera no")
    void cupoProporcionalALosSaxofones() throws Exception {
        Ticket t = tienda.cotizar(List.of(new LineaPedido(SAX_ALTO, 2), new LineaPedido(BOQ_ALTO, 3)));
        assertEquals(2, t.getRenglones().get(1).unidadesConDescuento());
        assertEquals(0, pesos("1287.00").compareTo(t.getDescuentoTotal()));
    }

    @Test
    @DisplayName("El cupo se reparte entre líneas repetidas del mismo artículo")
    void cupoCompartidoEntreLineas() throws Exception {
        Ticket t = tienda.cotizar(List.of(new LineaPedido(SAX_ALTO, 1),
                new LineaPedido(CANA_ALTO, 6), new LineaPedido(CANA_ALTO, 6)));
        assertEquals(6, t.getRenglones().get(1).unidadesConDescuento());
        assertEquals(4, t.getRenglones().get(2).unidadesConDescuento());
    }

    @Test
    @DisplayName("Accesorios comprados sin saxofón pagan precio completo")
    void sinSaxofonNoHayKit() throws Exception {
        Ticket t = tienda.cotizar(List.of(new LineaPedido(CANA_TENOR, 5)));
        assertEquals(0, BigDecimal.ZERO.compareTo(t.getDescuentoTotal()));
        assertEquals(0, pesos("545.00").compareTo(t.getTotal()));
    }

    @Test
    @DisplayName("Cotizar no modifica el stock; vender sí")
    void cotizarNoVende() throws Exception {
        List<LineaPedido> pedido = List.of(new LineaPedido(SAX_ALTO, 1), new LineaPedido(CANA_ALTO, 10));
        tienda.cotizar(pedido);
        assertEquals(40, tienda.buscar(CANA_ALTO).getStock());
        Ticket vendido = tienda.vender(pedido);
        assertEquals(30, tienda.buscar(CANA_ALTO).getStock());
        assertEquals(0, pesos("13755.50").compareTo(vendido.getTotal()));
    }
}
