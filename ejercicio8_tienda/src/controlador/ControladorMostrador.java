package controlador;

import java.util.ArrayList;
import java.util.List;

import modelo.LineaPedido;
import modelo.ReglaNegocioException;
import modelo.Ticket;
import modelo.Tienda;
import vista.VistaMostrador;

/**
 * Coordina el mostrador. Guarda las líneas que el cliente va pidiendo (estado de la interacción,
 * no del negocio) y le pide al modelo que las valide y las cotice; nunca calcula precios ni decide
 * compatibilidades por su cuenta.
 */
public class ControladorMostrador {
    private final Tienda tienda;
    private final VistaMostrador vista;
    private final List<LineaPedido> pedido = new ArrayList<>();

    public ControladorMostrador(Tienda tienda, VistaMostrador vista) {
        this.tienda = tienda;
        this.vista = vista;
        vista.alAgregar(this::agregar);
        vista.alCobrar(this::cobrar);
        vista.alCancelar(this::cancelar);
        // Observer: si el almacén reabastece, el catálogo del mostrador muestra el nuevo stock.
        tienda.agregarObservador(() -> vista.mostrarCatalogo(tienda.getArticulos()));
    }

    public void iniciar() {
        vista.mostrarCatalogo(tienda.getArticulos());
        vista.mostrarPedido(Ticket.vacio());
        vista.abrir();
    }

    public void agregar() {
        int cantidad;
        try {
            cantidad = Integer.parseInt(vista.getCantidad().trim());
        } catch (NumberFormatException e) {
            vista.mostrarError("La cantidad debe ser un número entero");
            return;
        }
        List<LineaPedido> propuesta = new ArrayList<>(pedido);
        propuesta.add(new LineaPedido(vista.getCodigoSeleccionado(), cantidad));
        try {
            Ticket cotizacion = tienda.cotizar(propuesta);
            pedido.add(propuesta.get(propuesta.size() - 1));
            vista.mostrarPedido(cotizacion);
            vista.limpiarCantidad();
            vista.mostrarMensaje("Artículo agregado al pedido");
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void cobrar() {
        try {
            Ticket ticket = tienda.vender(pedido);
            pedido.clear();
            vista.mostrarPedido(Ticket.vacio());
            vista.mostrarVentaRealizada(ticket);
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void cancelar() {
        pedido.clear();
        vista.mostrarPedido(Ticket.vacio());
        vista.mostrarMensaje("Pedido cancelado");
    }

    /** Líneas capturadas hasta ahora (solo lectura). */
    public List<LineaPedido> getPedido() {
        return List.copyOf(pedido);
    }
}
