package controlador;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import modelo.Inventario;
import vista.VistaSwing;

/**
 * Ejercicio 3: prueba de humo de la vista Swing manejada por su controlador.
 * Se "presionan" los botones por codigo; la ventana nunca se muestra.
 */
class ControladorSwingTest {

    private Inventario inventario;
    private VistaSwing vista;
    private ControladorSwing controlador;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeFalse(java.awt.GraphicsEnvironment.isHeadless(), "Requiere entorno grafico");
        inventario = new Inventario();
        SwingUtilities.invokeAndWait(() -> {
            vista = new VistaSwing();
            controlador = new ControladorSwing(inventario, vista);
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (vista != null) {
            SwingUtilities.invokeAndWait(vista::dispose);
        }
    }

    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container) {
                T encontrado = buscar((Container) c, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private void escribir(String campo, String texto) {
        buscar(vista.getContentPane(), campo, JTextField.class).setText(texto);
    }

    private void presionar(String boton) throws Exception {
        SwingUtilities.invokeAndWait(() -> buscar(vista.getContentPane(), boton, JButton.class).doClick());
    }

    private String estado() {
        return buscar(vista.getContentPane(), "lblEstado", JLabel.class).getText();
    }

    @Test
    @DisplayName("Swing: agregar desde la ventana llena la tabla y actualiza el modelo")
    void agregarDesdeLaVentana() throws Exception {
        escribir("txtNombre", "Teclado");
        escribir("txtCantidad", "5");
        escribir("txtPrecio", "350");
        presionar("btnAgregar");

        JTable tabla = buscar(vista.getContentPane(), "tabla", JTable.class);
        assertEquals("Agregado.", estado());
        assertEquals(1, tabla.getRowCount());
        assertEquals("Teclado", tabla.getValueAt(0, 0));
        assertEquals(5, tabla.getValueAt(0, 1));
        assertEquals(5, inventario.buscar("Teclado").getCantidad());
    }

    @Test
    @DisplayName("Swing: vender mas del stock muestra 'Stock insuficiente' y no cambia el modelo")
    void venderDesdeLaVentana() throws Exception {
        escribir("txtNombre", "Mouse");
        escribir("txtCantidad", "10");
        escribir("txtPrecio", "199.99");
        presionar("btnAgregar");

        escribir("txtProducto", "Mouse");
        escribir("txtCantidadVenta", "3");
        presionar("btnVender");
        assertEquals("Venta ok. Quedan 7", estado());

        escribir("txtCantidadVenta", "50");
        presionar("btnVender");
        assertEquals("Stock insuficiente", estado());
        assertEquals(7, inventario.buscar("Mouse").getCantidad());
    }

    @Test
    @DisplayName("Ej5 req.6: la vista pinta de rojo solo las filas que el modelo considera criticas")
    void filasCriticasEnRojo() throws Exception {
        escribir("txtNombre", "Mouse");
        escribir("txtCantidad", "4");
        escribir("txtPrecio", "199.99");
        presionar("btnAgregar");
        escribir("txtNombre", "Teclado");
        escribir("txtCantidad", "40");
        escribir("txtPrecio", "350");
        presionar("btnAgregar");

        JTable tabla = buscar(vista.getContentPane(), "tabla", JTable.class);
        // El componente del renderer se reutiliza entre celdas: se lee el color justo despues de prepararlo.
        java.awt.Color colorMouse = tabla.prepareRenderer(tabla.getCellRenderer(0, 0), 0, 0).getForeground();
        java.awt.Color colorTeclado = tabla.prepareRenderer(tabla.getCellRenderer(1, 0), 1, 0).getForeground();
        assertTrue(vista.esFilaCritica(0));
        assertFalse(vista.esFilaCritica(1));
        assertEquals(VistaSwing.COLOR_CRITICO, colorMouse);
        assertNotEquals(VistaSwing.COLOR_CRITICO, colorTeclado);
    }

    @Test
    @DisplayName("Swing: texto no numerico se informa como datos invalidos sin cerrar el programa")
    void entradaNoNumerica() throws Exception {
        escribir("txtNombre", "Hub");
        escribir("txtCantidad", "abc");
        escribir("txtPrecio", "10");
        presionar("btnAgregar");
        assertEquals("Datos invalidos", estado());
        assertTrue(inventario.getProductos().isEmpty());
    }

    @Test
    @DisplayName("CSV: exportar desde Swing reutiliza el caso de uso listar con la vista CSV")
    void exportarCSV(@TempDir Path dir) throws Exception {
        escribir("txtNombre", "Cable");
        escribir("txtCantidad", "4");
        escribir("txtPrecio", "12.5");
        presionar("btnAgregar");

        Path csv = dir.resolve("inv.csv");
        SwingUtilities.invokeAndWait(() -> controlador.exportar(csv));
        List<String> lineas = Files.readAllLines(csv);
        assertEquals(List.of("nombre,cantidad,precio,valor", "Cable,4,12.50,50.00"), lineas);
    }
}
