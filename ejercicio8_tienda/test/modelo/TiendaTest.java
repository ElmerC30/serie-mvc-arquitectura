package modelo;

import static apoyo.Inventarios.CANA_ALTO;
import static apoyo.Inventarios.SAX_ALTO;
import static apoyo.Inventarios.SAX_TENOR;
import static apoyo.Inventarios.articulo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import apoyo.Inventarios;

@DisplayName("Tienda - validaciones básicas, stock y notificación")
class TiendaTest {
    private Tienda tienda;
    private int avisos;

    @BeforeEach
    void preparar() {
        tienda = Inventarios.tiendaDePrueba();
        avisos = 0;
        tienda.agregarObservador(() -> avisos++);
    }

    @Test
    @DisplayName("Vender más de lo disponible lanza excepción y deja el stock intacto")
    void stockInsuficiente() throws Exception {
        StockInsuficienteException e = assertThrows(StockInsuficienteException.class,
                () -> tienda.vender(List.of(new LineaPedido(SAX_TENOR, 3))));
        assertEquals(2, e.getDisponible());
        assertEquals(2, tienda.buscar(SAX_TENOR).getStock());
        assertEquals(0, avisos, "una venta rechazada no debe notificar");
    }

    @Test
    @DisplayName("Líneas repetidas del mismo artículo se suman al validar el stock")
    void lineasRepetidasSeSuman() {
        assertThrows(StockInsuficienteException.class, () -> tienda.vender(List.of(
                new LineaPedido(SAX_TENOR, 1), new LineaPedido(SAX_TENOR, 2))));
    }

    @Test
    @DisplayName("Una venta válida descuenta exactamente lo vendido y notifica una vez")
    void ventaValida() throws Exception {
        tienda.vender(List.of(new LineaPedido(SAX_ALTO, 1), new LineaPedido(CANA_ALTO, 4)));
        assertEquals(3, tienda.buscar(SAX_ALTO).getStock());
        assertEquals(36, tienda.buscar(CANA_ALTO).getStock());
        assertEquals(1, avisos);
    }

    @Test
    @DisplayName("Pedido vacío, cantidad cero o código inexistente se rechazan")
    void pedidosInvalidos() {
        assertThrows(DatosInvalidosException.class, () -> tienda.vender(List.of()));
        assertThrows(DatosInvalidosException.class, () -> tienda.vender(List.of(new LineaPedido(SAX_ALTO, 0))));
        assertThrows(ArticuloNoEncontradoException.class, () -> tienda.vender(List.of(new LineaPedido("NO-EXISTE", 1))));
    }

    @Test
    @DisplayName("Registrar un artículo con precio cero, nombre vacío o código repetido se rechaza")
    void altasInvalidas() {
        assertThrows(DatosInvalidosException.class, () -> tienda.registrar(
                articulo("X1", "Abrazadera", TipoArticulo.BOQUILLA, Familia.ALTO, "0", 1)));
        assertThrows(DatosInvalidosException.class, () -> tienda.registrar(
                articulo("X2", " ", TipoArticulo.BOQUILLA, Familia.ALTO, "100", 1)));
        assertThrows(DatosInvalidosException.class, () -> tienda.registrar(
                articulo(SAX_ALTO, "Duplicado", TipoArticulo.SAXOFON, Familia.ALTO, "100", 1)));
        assertEquals(0, avisos);
    }
}
