package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado del inventario y reglas de negocio.
 * No imprime ni lee nada: rechaza las operaciones invalidas con excepciones propias.
 */
public class Inventario {
    private final List<Producto> productos = new ArrayList<>();

    /** Regla: el nombre no puede ir vacio, la cantidad no puede ser negativa y el precio debe ser positivo. */
    public void agregar(Producto p) throws DatosInvalidosException {
        if (p == null) {
            throw new DatosInvalidosException("El producto no puede ser nulo");
        }
        if (p.getNombre() == null || p.getNombre().isEmpty()) {
            throw new DatosInvalidosException("El nombre no puede ir vacio");
        }
        if (p.getCantidad() < 0) {
            throw new DatosInvalidosException("La cantidad no puede ser negativa");
        }
        if (p.getPrecio() <= 0) {
            throw new DatosInvalidosException("El precio debe ser mayor que cero");
        }
        productos.add(p);
    }

    /**
     * Regla: no se puede vender mas de lo que hay en stock.
     * @return las unidades que quedan despues de la venta
     */
    public int vender(String nombre, int cantidad)
            throws ProductoNoEncontradoException, StockInsuficienteException {
        Producto p = buscar(nombre);
        if (cantidad > p.getCantidad()) {
            throw new StockInsuficienteException(nombre, p.getCantidad(), cantidad);
        }
        p.setCantidad(p.getCantidad() - cantidad);
        return p.getCantidad();
    }

    /** Devuelve el primer producto con ese nombre o lanza excepcion si no existe. */
    public Producto buscar(String nombre) throws ProductoNoEncontradoException {
        for (Producto p : productos) {
            if (p.getNombre().equals(nombre)) {
                return p;
            }
        }
        throw new ProductoNoEncontradoException(nombre);
    }

    public boolean existe(String nombre) {
        for (Producto p : productos) {
            if (p.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    /** Vista de solo lectura de los productos, en orden de alta. */
    public List<Producto> getProductos() {
        return Collections.unmodifiableList(productos);
    }

    /** Suma de cantidad x precio de todos los productos. */
    public double valorTotal() {
        double total = 0;
        for (Producto p : productos) {
            total += p.getCantidad() * p.getPrecio();
        }
        return total;
    }
}
