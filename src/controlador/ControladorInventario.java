package controlador;

import modelo.DatosInvalidosException;
import modelo.Inventario;
import modelo.Producto;
import modelo.ProductoNoEncontradoException;
import modelo.StockInsuficienteException;
import vista.VistaConsola;

/**
 * Controlador: interpreta la entrada que captura la vista, invoca al modelo
 * y decide que debe mostrar la vista segun el resultado.
 */
public class ControladorInventario {
    private final Inventario inventario;
    private final VistaConsola vista;

    public ControladorInventario(Inventario inventario, VistaConsola vista) {
        this.inventario = inventario;
        this.vista = vista;
    }

    /** Ciclo principal del menu. */
    public void iniciar() {
        int op;
        do {
            vista.mostrarMenu();
            op = Integer.parseInt(vista.pedirOpcion());
            switch (op) {
                case 1: agregar(); break;
                case 2: vender(); break;
                case 3: listar(); break;
                case 4: valorTotal(); break;
                default: break;
            }
        } while (op != 0);
    }

    private void agregar() {
        String nombre = vista.pedirNombre();
        int cantidad = Integer.parseInt(vista.pedirCantidad());
        double precio = Double.parseDouble(vista.pedirPrecio());
        try {
            inventario.agregar(new Producto(nombre, cantidad, precio));
            vista.mostrarAgregado();
        } catch (DatosInvalidosException e) {
            vista.mostrarDatosInvalidos();
        }
    }

    private void vender() {
        String nombre = vista.pedirProducto();
        if (!inventario.existe(nombre)) {
            vista.mostrarNoExiste();
            return;
        }
        int cantidad = Integer.parseInt(vista.pedirCantidadAVender());
        try {
            int quedan = inventario.vender(nombre, cantidad);
            vista.mostrarVentaOk(quedan);
        } catch (StockInsuficienteException e) {
            vista.mostrarStockInsuficiente();
        } catch (ProductoNoEncontradoException e) {
            vista.mostrarNoExiste();
        }
    }

    private void listar() {
        vista.mostrarListado(inventario.getProductos());
    }

    private void valorTotal() {
        vista.mostrarValorTotal(inventario.valorTotal());
    }
}
