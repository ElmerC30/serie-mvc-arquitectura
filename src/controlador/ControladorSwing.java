package controlador;

import java.nio.file.Path;

import modelo.Inventario;
import vista.VistaCSV;
import vista.VistaSwing;

/**
 * Coordinacion propia de la ventana Swing: conecta los botones con los casos de uso.
 * Como la ventana no tiene un ciclo de preguntas, la conversion de texto a numero
 * se valida aqui y se informa a la vista en lugar de terminar el programa.
 */
public class ControladorSwing {
    private final Inventario inventario;
    private final ControladorInventario casos;
    private final VistaSwing vista;

    public ControladorSwing(Inventario inventario, VistaSwing vista) {
        this.inventario = inventario;
        this.vista = vista;
        this.casos = new ControladorInventario(inventario, vista);
        vista.alAgregar(e -> agregar());
        vista.alVender(e -> vender());
        vista.alValorTotal(e -> casos.valorTotal());
        vista.alExportar(e -> exportar(vista.pedirArchivoDestino()));
    }

    public void iniciar() {
        refrescar();
        vista.setVisible(true);
    }

    private void agregar() {
        try {
            int cantidad = Integer.parseInt(vista.getCantidad().trim());
            double precio = Double.parseDouble(vista.getPrecio().trim());
            casos.agregar(vista.getNombre(), cantidad, precio);
            vista.limpiarAlta();
        } catch (NumberFormatException e) {
            vista.mostrarDatosInvalidos();
        }
        refrescar();
    }

    private void vender() {
        try {
            int cantidad = Integer.parseInt(vista.getCantidadVenta().trim());
            casos.vender(vista.getProducto(), cantidad);
        } catch (NumberFormatException e) {
            vista.mostrarDatosInvalidos();
        }
        refrescar();
    }

    /** Reutiliza el mismo caso de uso "listar" con otra vista: la de CSV. */
    public void exportar(Path destino) {
        if (destino == null) {
            return;
        }
        new ControladorInventario(inventario, new VistaCSV(destino)).listar();
        vista.mostrarMensaje("Exportado a " + destino.getFileName());
    }

    private void refrescar() {
        casos.listar();
        casos.valorTotal();
    }
}
