package persistencia;

import java.util.ArrayList;
import java.util.List;

import modelo.Producto;
import modelo.ProductoDAO;

/** Implementacion en memoria: los datos se pierden al cerrar el programa. */
public class ProductoDAOMemoria implements ProductoDAO {
    private final List<Producto> productos = new ArrayList<>();

    @Override
    public List<Producto> listar() {
        return new ArrayList<>(productos);
    }

    @Override
    public void insertar(Producto producto) {
        productos.add(producto);
    }

    @Override
    public void actualizar(Producto producto) {
        // El objeto ya vive en esta lista: no hay nada que copiar.
    }
}
