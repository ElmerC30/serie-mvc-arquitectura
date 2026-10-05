package vista;

import java.util.List;

import modelo.Articulo;
import modelo.Ticket;

/** Lo que sabe hacer la vista del mostrador (punto de venta). */
public interface VistaMostrador {
    void mostrarCatalogo(List<Articulo> articulos);
    void mostrarPedido(Ticket cotizacion);
    void mostrarVentaRealizada(Ticket ticket);
    void mostrarMensaje(String mensaje);
    void mostrarError(String mensaje);

    String getCodigoSeleccionado();
    String getCantidad();
    void limpiarCantidad();

    void alAgregar(Runnable accion);
    void alCobrar(Runnable accion);
    void alCancelar(Runnable accion);
    void abrir();
}
