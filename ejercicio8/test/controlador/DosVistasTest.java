package controlador;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import modelo.Biblioteca;
import modelo.Libro;
import modelo.Socio;
import persistencia.RepositorioMemoria;
import soporte.RelojAjustable;
import vista.VistaConsola;
import vista.VistaSwing;

/** Dos vistas al mismo tiempo sobre el mismo modelo: cada una refleja lo que hace la otra. */
class DosVistasTest {

    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container) {
                T t = buscar((Container) c, nombre, tipo);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    @Test
    @DisplayName("Dos vistas: la ventana refleja un prestamo hecho en la consola y la consola una devolucion hecha en la ventana")
    void vistasSincronizadas() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Requiere entorno grafico");
        RelojAjustable reloj = new RelojAjustable(Clock.fixed(
                LocalDate.of(2026, 10, 4).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        Biblioteca b = new Biblioteca(new RepositorioMemoria(), reloj);
        b.registrarLibro(new Libro("L01", "Pedro Paramo", "Juan Rulfo"));
        b.registrarSocio(new Socio("S01", "Ana"));

        VistaSwing[] ventana = new VistaSwing[1];
        SwingUtilities.invokeAndWait(() -> {
            ventana[0] = new VistaSwing("prueba");
            new ControladorSwing(b, ventana[0], reloj);
        });

        // La consola presta L01 a S01.
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        VistaConsola consola = new VistaConsola(
                new ByteArrayInputStream("3\nL01\nS01\n0\n".getBytes(StandardCharsets.UTF_8)),
                new PrintStream(salida, true, "UTF-8"));
        ControladorConsola cc = new ControladorConsola(new ControladorBiblioteca(b, consola, reloj), consola);
        cc.activarAvisos();
        cc.iniciar();
        SwingUtilities.invokeAndWait(() -> { });

        JTable libros = buscar(ventana[0].getContentPane(), "tablaLibros", JTable.class);
        assertEquals("Prestado", libros.getValueAt(0, 3));
        assertEquals("Ana", libros.getValueAt(0, 4));

        // La ventana devuelve L01; la consola recibe el aviso y vuelve a mostrar el estado.
        int antes = salida.size();
        SwingUtilities.invokeAndWait(() -> {
            buscar(ventana[0].getContentPane(), "txtPrestamoIsbn", JTextField.class).setText("L01");
            buscar(ventana[0].getContentPane(), "btnDevolver", JButton.class).doClick();
        });
        String despues = salida.toString(StandardCharsets.UTF_8).substring(antes);
        assertTrue(despues.contains("[Aviso]"), despues);
        assertTrue(despues.contains("Disponible"), despues);
        assertEquals("Disponible", libros.getValueAt(0, 3));
        SwingUtilities.invokeAndWait(ventana[0]::dispose);
    }
}
