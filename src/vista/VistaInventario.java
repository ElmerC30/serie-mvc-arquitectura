package vista;

import java.util.List;

import modelo.Producto;

/**
 * Lo que cualquier vista del inventario sabe mostrar.
 * El controlador habla con esta interfaz, de modo que no le importa si la vista es
 * una consola, una ventana Swing o un archivo CSV.
 */
public interface VistaInventario {
    void mostrarMensaje(String mensaje);
    void mostrarDatosInvalidos();
    void mostrarAgregado();
    void mostrarNoExiste();
    void mostrarStockInsuficiente();
    void mostrarVentaOk(int quedan);
    void mostrarListado(List<Producto> productos);
    void mostrarValorTotal(double total);
}
