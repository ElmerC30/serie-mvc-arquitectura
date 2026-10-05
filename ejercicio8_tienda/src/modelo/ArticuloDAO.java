package modelo;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el modelo necesita del almacenamiento. El modelo define el contrato;
 * el paquete persistencia lo implementa y depende del modelo, nunca al revés.
 */
public interface ArticuloDAO {
    /** Todos los artículos en orden de alta. */
    List<Articulo> listar();

    Optional<Articulo> buscar(String codigo);

    void insertar(Articulo articulo);

    /** Guarda el nuevo estado de un artículo que ya existe (tras una venta o un reabasto). */
    void actualizar(Articulo articulo);
}
