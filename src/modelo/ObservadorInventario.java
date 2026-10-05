package modelo;

/**
 * Quien quiera enterarse de que el inventario cambio implementa esta interfaz.
 * El modelo solo sabe que existen observadores; no sabe quienes son ni que haran con el aviso.
 */
@FunctionalInterface
public interface ObservadorInventario {
    /** Se invoca despues de cada cambio exitoso del inventario. */
    void inventarioCambiado();
}
