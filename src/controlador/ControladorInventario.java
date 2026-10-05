package controlador;

import modelo.DatosInvalidosException;
import modelo.Inventario;
import modelo.ObservadorInventario;
import modelo.Producto;
import modelo.ProductoNoEncontradoException;
import modelo.StockInsuficienteException;
import vista.VistaInventario;

/**
 * Casos de uso del inventario, independientes de la tecnologia de la vista.
 * Invoca al modelo y decide que debe mostrar la vista segun el resultado.
 * Lo reutilizan el controlador de consola y el de Swing.
 */
public class ControladorInventario {
    private final Inventario inventario;
    private final VistaInventario vista;

    public ControladorInventario(Inventario inventario, VistaInventario vista) {
        this.inventario = inventario;
        this.vista = vista;
    }

    /** Registra a quien deba enterarse de los cambios del modelo (Ejercicio 4). */
    public void suscribir(ObservadorInventario observador) {
        inventario.agregarObservador(observador);
    }

    public boolean existe(String nombre) {
        return inventario.existe(nombre);
    }

    public void agregar(String nombre, int cantidad, double precio) {
        try {
            inventario.agregar(new Producto(nombre, cantidad, precio));
            vista.mostrarAgregado();
        } catch (DatosInvalidosException e) {
            vista.mostrarDatosInvalidos();
        }
    }

    public void vender(String nombre, int cantidad) {
        try {
            int quedan = inventario.vender(nombre, cantidad);
            vista.mostrarVentaOk(quedan);
        } catch (StockInsuficienteException e) {
            vista.mostrarStockInsuficiente();
        } catch (ProductoNoEncontradoException e) {
            vista.mostrarNoExiste();
        }
    }

    public void listar() {
        vista.mostrarListado(inventario.getProductos());
    }

    public void valorTotal() {
        vista.mostrarValorTotal(inventario.valorTotal());
    }
}
