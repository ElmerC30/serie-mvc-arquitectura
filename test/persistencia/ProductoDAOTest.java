package persistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import modelo.Inventario;
import modelo.Producto;
import modelo.ProductoDAO;
import modelo.StockInsuficienteException;

/** Ejercicio 6: las mismas reglas del modelo funcionan igual con cualquier implementacion del DAO. */
class ProductoDAOTest {

    /** Escenario comun: se ejecuta igual con memoria y con archivo. */
    private void escenario(ProductoDAO dao) throws Exception {
        Inventario inv = new Inventario(dao);
        inv.agregar(new Producto("Teclado", 5, 350.0));
        inv.agregar(new Producto("Mouse, inalambrico", 10, 199.99));
        inv.vender("Mouse, inalambrico", 4);
        assertThrows(StockInsuficienteException.class, () -> inv.vender("Teclado", 6));
        assertEquals(5, inv.buscar("Teclado").getCantidad());
        assertEquals(6, inv.buscar("Mouse, inalambrico").getCantidad());
        assertEquals(5 * 350.0 + 6 * 199.99, inv.valorTotal(), 1e-9);
    }

    @Test
    @DisplayName("Ej6: las reglas del inventario funcionan con el DAO en memoria")
    void reglasConMemoria() throws Exception {
        escenario(new ProductoDAOMemoria());
    }

    @Test
    @DisplayName("Ej6: las reglas del inventario funcionan con el DAO de archivo")
    void reglasConArchivo(@TempDir Path dir) throws Exception {
        escenario(new ProductoDAOArchivo(dir.resolve("inv.csv")));
    }

    @Test
    @DisplayName("Ej6: el DAO de archivo conserva los datos entre ejecuciones (incluye comas en el nombre)")
    void archivoPersisteEntreInstancias(@TempDir Path dir) throws Exception {
        Path archivo = dir.resolve("inv.csv");
        escenario(new ProductoDAOArchivo(archivo));

        Inventario reabierto = new Inventario(new ProductoDAOArchivo(archivo));
        List<Producto> productos = reabierto.getProductos();
        assertEquals(2, productos.size());
        assertEquals("Mouse, inalambrico", productos.get(1).getNombre());
        assertEquals(6, productos.get(1).getCantidad());
        assertEquals(199.99, productos.get(1).getPrecio(), 0.0);
        assertEquals("nombre,cantidad,precio", Files.readAllLines(archivo).get(0));
    }

    @Test
    @DisplayName("Ej6: el DAO en memoria empieza vacio en cada ejecucion")
    void memoriaNoPersiste() throws Exception {
        new Inventario(new ProductoDAOMemoria()).agregar(new Producto("Teclado", 5, 350.0));
        assertTrue(new Inventario(new ProductoDAOMemoria()).getProductos().isEmpty());
    }
}
