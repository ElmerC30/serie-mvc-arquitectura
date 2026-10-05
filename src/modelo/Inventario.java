package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Estado del inventario y reglas de negocio.
 * No imprime ni lee nada: rechaza las operaciones invalidas con excepciones propias
 * y avisa a sus observadores cuando su estado cambia.
 */
public class Inventario {
    /** Regla 2 del Ejercicio 5: capacidad maxima de almacen por producto. */
    public static final int MAX_UNIDADES = 50000;

    private final List<Producto> productos = new ArrayList<>();
    private final List<ObservadorInventario> observadores = new CopyOnWriteArrayList<>();

    public void agregarObservador(ObservadorInventario o) {
        observadores.add(o);
    }

    public void quitarObservador(ObservadorInventario o) {
        observadores.remove(o);
    }

    private void notificar() {
        for (ObservadorInventario o : observadores) {
            o.inventarioCambiado();
        }
    }

    /** Regla: el nombre no puede ir vacio, la cantidad no puede ser negativa y el precio debe ser positivo. */
    public void agregar(Producto p) throws DatosInvalidosException {
        synchronized (this) {
            if (p == null) {
                throw new DatosInvalidosException("El producto no puede ser nulo");
            }
            if (p.getNombre() == null || p.getNombre().isEmpty()) {
                throw new DatosInvalidosException("El nombre no puede ir vacio");
            }
            if (p.getCantidad() < 0) {
                throw new DatosInvalidosException("La cantidad no puede ser negativa");
            }
            if (p.getCantidad() > MAX_UNIDADES) {
                throw new DatosInvalidosException("No caben mas de " + MAX_UNIDADES + " unidades en almacen");
            }
            if (p.getPrecio() <= 0) {
                throw new DatosInvalidosException("El precio debe ser mayor que cero");
            }
            productos.add(p);
        }
        notificar();
    }

    /**
     * Regla: no se puede vender mas de lo que hay en stock.
     * @return las unidades que quedan despues de la venta
     */
    public int vender(String nombre, int cantidad)
            throws ProductoNoEncontradoException, StockInsuficienteException {
        int quedan;
        synchronized (this) {
            Producto p = buscar(nombre);
            if (cantidad > p.getCantidad()) {
                throw new StockInsuficienteException(nombre, p.getCantidad(), cantidad);
            }
            p.setCantidad(p.getCantidad() - cantidad);
            quedan = p.getCantidad();
        }
        notificar();
        return quedan;
    }

    /** Devuelve el primer producto con ese nombre o lanza excepcion si no existe. */
    public synchronized Producto buscar(String nombre) throws ProductoNoEncontradoException {
        for (Producto p : productos) {
            if (p.getNombre().equals(nombre)) {
                return p;
            }
        }
        throw new ProductoNoEncontradoException(nombre);
    }

    public synchronized boolean existe(String nombre) {
        for (Producto p : productos) {
            if (p.getNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    /** Copia de solo lectura de los productos, en orden de alta (segura entre hilos). */
    public synchronized List<Producto> getProductos() {
        return Collections.unmodifiableList(new ArrayList<>(productos));
    }

    /** Productos con stock critico (Ejercicio 5, requisito 5). */
    public synchronized List<Producto> getCriticos() {
        List<Producto> criticos = new ArrayList<>();
        for (Producto p : productos) {
            if (p.esCritico()) {
                criticos.add(p);
            }
        }
        return Collections.unmodifiableList(criticos);
    }

    /** Suma de cantidad x precio de todos los productos. */
    public synchronized double valorTotal() {
        double total = 0;
        for (Producto p : productos) {
            total += p.getCantidad() * p.getPrecio();
        }
        return total;
    }
}
