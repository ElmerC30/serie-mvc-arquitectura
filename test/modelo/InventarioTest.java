package modelo;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ejercicio 2: pruebas de las reglas de negocio de Inventario sin ejecutar ninguna vista.
 * Ninguna prueba imprime un menu ni espera entrada del teclado.
 */
class InventarioTest {

    private Inventario inv;

    @BeforeEach
    void setUp() {
        inv = new Inventario(new persistencia.ProductoDAOMemoria());
    }

    @Test
    @DisplayName("Caso 1: vender mas de lo disponible lanza excepcion y deja el stock intacto")
    void noSePuedeVenderMasDelStockDisponible() throws Exception {
        inv.agregar(new Producto("Teclado", 5, 350.0));

        assertThrows(StockInsuficienteException.class,
                     () -> inv.vender("Teclado", 8));
        assertEquals(5, inv.buscar("Teclado").getCantidad());
    }

    @Test
    @DisplayName("Caso 2: vender una cantidad valida descuenta exactamente esa cantidad")
    void venderCantidadValidaDescuentaExactamente() throws Exception {
        inv.agregar(new Producto("Mouse", 10, 199.99));

        int quedan = inv.vender("Mouse", 3);

        assertEquals(7, quedan);
        assertEquals(7, inv.buscar("Mouse").getCantidad());
    }

    @Test
    @DisplayName("Caso 2b: vender exactamente todo el stock lo deja en cero")
    void venderTodoElStockLoDejaEnCero() throws Exception {
        inv.agregar(new Producto("Cable", 4, 12.5));

        assertEquals(0, inv.vender("Cable", 4));
    }

    @Test
    @DisplayName("Caso 3: agregar un producto con precio cero se rechaza")
    void agregarConPrecioCeroSeRechaza() {
        assertThrows(DatosInvalidosException.class,
                     () -> inv.agregar(new Producto("Monitor", 2, 0.0)));
        assertTrue(inv.getProductos().isEmpty());
    }

    @Test
    @DisplayName("Caso 3b: agregar un producto con precio negativo se rechaza")
    void agregarConPrecioNegativoSeRechaza() {
        assertThrows(DatosInvalidosException.class,
                     () -> inv.agregar(new Producto("Monitor", 2, -100.0)));
        assertFalse(inv.existe("Monitor"));
    }

    @Test
    @DisplayName("Caso 3c: nombre vacio o cantidad negativa tambien se rechazan")
    void agregarConNombreVacioOCantidadNegativaSeRechaza() {
        assertThrows(DatosInvalidosException.class, () -> inv.agregar(new Producto("", 1, 10.0)));
        assertThrows(DatosInvalidosException.class, () -> inv.agregar(new Producto("Hub", -1, 10.0)));
        assertEquals(0, inv.getProductos().size());
    }

    @Test
    @DisplayName("Caso 4: el valor total se calcula correctamente con varios productos")
    void valorTotalConVariosProductos() throws Exception {
        inv.agregar(new Producto("Teclado", 5, 350.0));   // 1750.00
        inv.agregar(new Producto("Mouse", 10, 199.99));   // 1999.90
        inv.agregar(new Producto("Cable", 4, 12.5));      //   50.00

        assertEquals(3799.90, inv.valorTotal(), 0.0001);
    }

    @Test
    @DisplayName("Caso 4b: el valor total refleja las ventas")
    void valorTotalDespuesDeVender() throws Exception {
        inv.agregar(new Producto("Teclado", 5, 350.0));
        inv.vender("Teclado", 2);

        assertEquals(1050.0, inv.valorTotal(), 0.0001);
    }

    @Test
    @DisplayName("Caso 4c: inventario vacio vale cero")
    void inventarioVacioValeCero() {
        assertEquals(0.0, inv.valorTotal(), 0.0);
    }

    @Test
    @DisplayName("Caso 5: buscar un producto inexistente lanza ProductoNoEncontradoException")
    void buscarInexistenteLanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class, () -> inv.buscar("Impresora"));
        assertFalse(inv.existe("Impresora"));
    }

    @Test
    @DisplayName("Caso 5b: vender un producto inexistente lanza ProductoNoEncontradoException")
    void venderInexistenteLanzaExcepcion() {
        assertThrows(ProductoNoEncontradoException.class, () -> inv.vender("Impresora", 1));
    }

    @Test
    @DisplayName("La lista de productos expuesta no se puede modificar desde fuera del modelo")
    void listaDeProductosEsDeSoloLectura() throws Exception {
        inv.agregar(new Producto("Teclado", 5, 350.0));
        assertThrows(UnsupportedOperationException.class,
                     () -> inv.getProductos().add(new Producto("Hack", 1, 1.0)));
    }
}
