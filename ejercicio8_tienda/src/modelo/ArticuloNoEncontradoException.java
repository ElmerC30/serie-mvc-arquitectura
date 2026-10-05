package modelo;

/** Se pidió un código que no existe en el inventario. */
public class ArticuloNoEncontradoException extends ReglaNegocioException {
    public ArticuloNoEncontradoException(String codigo) {
        super("No existe el artículo " + codigo);
    }
}
