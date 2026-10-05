package modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Ejercicio 4: el modelo notifica sus cambios sin conocer a quien lo escucha. */
class ObservadorInventarioTest {

    @Test
    @DisplayName("Ej4: agregar y vender notifican a todos los observadores registrados")
    void cambiosExitososNotifican() throws Exception {
        Inventario inv = new Inventario();
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        inv.agregarObservador(a::incrementAndGet);
        inv.agregarObservador(b::incrementAndGet);

        inv.agregar(new Producto("Teclado", 5, 350.0));
        inv.vender("Teclado", 2);

        assertEquals(2, a.get());
        assertEquals(2, b.get());
    }

    @Test
    @DisplayName("Ej4: las operaciones rechazadas no notifican")
    void operacionesRechazadasNoNotifican() throws Exception {
        Inventario inv = new Inventario();
        inv.agregar(new Producto("Teclado", 5, 350.0));
        AtomicInteger avisos = new AtomicInteger();
        inv.agregarObservador(avisos::incrementAndGet);

        assertThrows(StockInsuficienteException.class, () -> inv.vender("Teclado", 9));
        assertThrows(ProductoNoEncontradoException.class, () -> inv.vender("Mouse", 1));
        assertThrows(DatosInvalidosException.class, () -> inv.agregar(new Producto("X", 1, 0)));

        assertEquals(0, avisos.get());
    }

    @Test
    @DisplayName("Ej4: el observador ve el estado ya actualizado y puede darse de baja")
    void observadorVeEstadoNuevoYSePuedeQuitar() throws Exception {
        Inventario inv = new Inventario();
        inv.agregar(new Producto("Mouse", 10, 199.99));
        List<Integer> visto = new ArrayList<>();
        ObservadorInventario o = () -> visto.add(inv.getProductos().get(0).getCantidad());
        inv.agregarObservador(o);

        inv.vender("Mouse", 3);
        inv.quitarObservador(o);
        inv.vender("Mouse", 1);

        assertEquals(List.of(7), visto);
    }

    @Test
    @DisplayName("Ej4: ningun archivo del paquete modelo menciona clases de vista o controlador")
    void elModeloNoConoceALasVistas() throws IOException {
        Path modelo = Paths.get("src", "modelo");
        try (Stream<Path> archivos = Files.list(modelo)) {
            for (Path f : (Iterable<Path>) archivos::iterator) {
                String codigo = Files.readString(f);
                for (String prohibido : new String[] {"import vista", "import controlador", "Vista",
                                                       "Controlador", "javax.swing", "System.out", "Scanner"}) {
                    assertFalse(codigo.contains(prohibido), f.getFileName() + " contiene " + prohibido);
                }
            }
        }
    }
}
