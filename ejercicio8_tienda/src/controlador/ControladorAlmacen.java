package controlador;

import modelo.ReglaNegocioException;
import modelo.Tienda;
import vista.VistaAlmacen;

/**
 * Coordina la ventana del almacén: convierte la captura en datos, invoca al modelo y decide
 * qué debe mostrar la vista según el resultado. No contiene reglas de negocio.
 */
public class ControladorAlmacen {
    private final Tienda tienda;
    private final VistaAlmacen vista;

    public ControladorAlmacen(Tienda tienda, VistaAlmacen vista) {
        this.tienda = tienda;
        this.vista = vista;
        vista.alReabastecer(this::reabastecer);
        // Observer: la vista se refresca con cualquier cambio, venga del mostrador o de aquí.
        tienda.agregarObservador(this::refrescar);
    }

    public void iniciar() {
        refrescar();
        vista.abrir();
    }

    public void reabastecer() {
        String codigo = vista.getCodigoReabasto();
        int cantidad;
        try {
            cantidad = Integer.parseInt(vista.getCantidadReabasto().trim());
        } catch (NumberFormatException e) {
            vista.mostrarError("Las unidades deben ser un número entero");
            return;
        }
        try {
            int stock = tienda.reabastecer(codigo, cantidad);
            vista.limpiarReabasto();
            vista.mostrarMensaje("Entrada registrada: " + codigo + " ahora tiene " + stock + " unidades");
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    private void refrescar() {
        vista.mostrarInventario(tienda.getArticulos());
        vista.mostrarAlertas(tienda.getAlertasReabasto());
    }
}
