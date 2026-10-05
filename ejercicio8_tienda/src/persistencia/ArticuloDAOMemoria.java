package persistencia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import modelo.Articulo;
import modelo.ArticuloDAO;

/** Implementación en memoria: los datos se pierden al cerrar el programa. */
public class ArticuloDAOMemoria implements ArticuloDAO {
    private final Map<String, Articulo> articulos = new LinkedHashMap<>();

    @Override
    public List<Articulo> listar() {
        return new ArrayList<>(articulos.values());
    }

    @Override
    public Optional<Articulo> buscar(String codigo) {
        return Optional.ofNullable(articulos.get(codigo));
    }

    @Override
    public void insertar(Articulo articulo) {
        articulos.put(articulo.getCodigo(), articulo);
    }

    @Override
    public void actualizar(Articulo articulo) {
        // El objeto ya vive en este mapa: no hay nada que copiar.
    }
}
