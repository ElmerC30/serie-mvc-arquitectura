package persistencia;

import static apoyo.Inventarios.articulo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import modelo.ArticuloDAO;
import modelo.Familia;
import modelo.LineaPedido;
import modelo.Tienda;
import modelo.TipoArticulo;

@DisplayName("Persistencia intercambiable - el mismo contrato con memoria y con archivo")
class ArticuloDAOTest {

    /** Contrato que cualquier ArticuloDAO debe cumplir para que la Tienda funcione igual. */
    abstract static class Contrato {
        abstract ArticuloDAO crear();

        @Test
        @DisplayName("Inserta, lista en orden de alta y busca por código")
        void insertarListarBuscar() {
            ArticuloDAO dao = crear();
            dao.insertar(articulo("A", "Saxofón alto Jean Paul AS-400GP", TipoArticulo.SAXOFON, Familia.ALTO, "12999.00", 4));
            dao.insertar(articulo("B", "Caña D'Addario Plasticover alto #2.5", TipoArticulo.CANA, Familia.ALTO, "89.00", 40));
            assertEquals(List.of("A", "B"), dao.listar().stream().map(a -> a.getCodigo()).toList());
            assertTrue(dao.buscar("B").isPresent());
            assertTrue(dao.buscar("Z").isEmpty());
        }

        @Test
        @DisplayName("La Tienda vende igual sin importar la implementación")
        void tiendaFuncionaIgual() throws Exception {
            Tienda t = new Tienda(crear());
            t.registrar(articulo("A", "Saxofón alto", TipoArticulo.SAXOFON, Familia.ALTO, "12999.00", 4));
            t.vender(List.of(new LineaPedido("A", 1)));
            assertEquals(3, t.buscar("A").getStock());
        }
    }

    @Nested
    @DisplayName("ArticuloDAOMemoria")
    class Memoria extends Contrato {
        @Override
        ArticuloDAO crear() {
            return new ArticuloDAOMemoria();
        }
    }

    @Nested
    @DisplayName("ArticuloDAOArchivo")
    class Archivo extends Contrato {
        @TempDir
        Path carpeta;

        @Override
        ArticuloDAO crear() {
            return new ArticuloDAOArchivo(carpeta.resolve("inventario.csv"));
        }

        @Test
        @DisplayName("Los cambios sobreviven a reabrir el archivo (incluye nombres con apóstrofo)")
        void persisteEntreEjecuciones() throws Exception {
            Path archivo = carpeta.resolve("tienda.csv");
            Tienda primera = new Tienda(new ArticuloDAOArchivo(archivo));
            primera.registrar(articulo("CAN", "Caña D'Addario Plasticover alto #2.5", TipoArticulo.CANA, Familia.ALTO, "89.00", 40));
            primera.vender(List.of(new LineaPedido("CAN", 25)));

            Tienda segunda = new Tienda(new ArticuloDAOArchivo(archivo));
            assertEquals(15, segunda.buscar("CAN").getStock());
            assertEquals("Caña D'Addario Plasticover alto #2.5", segunda.buscar("CAN").getNombre());
            assertEquals(0, new java.math.BigDecimal("89.00").compareTo(segunda.buscar("CAN").getPrecio()));
            assertTrue(segunda.buscar("CAN").requiereReabasto());
        }
    }
}
