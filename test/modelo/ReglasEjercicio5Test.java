package modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ejercicio 5: las reglas clasificadas como "modelo" se prueban sin vista. */
class ReglasEjercicio5Test {

    @Test
    @DisplayName("Ej5 req.2: no se aceptan mas de 50000 unidades; 50000 exactas si")
    void maximoDeUnidadesEnAlmacen() throws Exception {
        Inventario inv = new Inventario(new persistencia.ProductoDAOMemoria());
        assertThrows(DatosInvalidosException.class,
                     () -> inv.agregar(new Producto("Tornillo", 50001, 0.5)));
        inv.agregar(new Producto("Tornillo", 50000, 0.5));
        assertEquals(50000, inv.buscar("Tornillo").getCantidad());
    }

    @Test
    @DisplayName("Ej5 req.5: un producto con menos de 10 unidades es critico; con 10 no")
    void umbralDeStockCritico() {
        assertTrue(new Producto("A", 9, 1.0).esCritico());
        assertTrue(new Producto("B", 0, 1.0).esCritico());
        assertFalse(new Producto("C", 10, 1.0).esCritico());
    }

    @Test
    @DisplayName("Ej5 req.5: un producto se vuelve critico al venderse por debajo del umbral")
    void ventaLoVuelveCritico() throws Exception {
        Inventario inv = new Inventario(new persistencia.ProductoDAOMemoria());
        inv.agregar(new Producto("Mouse", 12, 199.99));
        inv.agregar(new Producto("Teclado", 50, 350.0));
        assertTrue(inv.getCriticos().isEmpty());

        inv.vender("Mouse", 3);

        List<Producto> criticos = inv.getCriticos();
        assertEquals(1, criticos.size());
        assertEquals("Mouse", criticos.get(0).getNombre());
    }
}
