package vista;

import java.util.List;

import modelo.Articulo;

/**
 * Lo que sabe hacer la vista del almacén. El controlador habla con esta interfaz y no con Swing,
 * de modo que puede probarse con una vista falsa.
 */
public interface VistaAlmacen {
    void mostrarInventario(List<Articulo> articulos);
    void mostrarAlertas(List<Articulo> alertas);
    void mostrarMensaje(String mensaje);
    void mostrarError(String mensaje);

    String getCodigoReabasto();
    String getCantidadReabasto();
    void limpiarReabasto();

    void alReabastecer(Runnable accion);
    void abrir();
}
