package modelo;

import java.util.List;

/**
 * Puerto de persistencia que el modelo necesita (Ejercicio 6).
 * El modelo define QUE necesita guardar; COMO se guarda lo deciden las implementaciones
 * del paquete persistencia, que dependen del modelo y no al reves.
 */
public interface ProductoDAO {
    /** Todos los productos en orden de alta. */
    List<Producto> listar();

    /** Da de alta un producto nuevo. */
    void insertar(Producto producto);

    /** Persiste el nuevo estado de un producto que ya existe (por ejemplo, tras una venta). */
    void actualizar(Producto producto);
}
