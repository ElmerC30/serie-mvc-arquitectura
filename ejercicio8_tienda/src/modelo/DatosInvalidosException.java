package modelo;

/** Los datos de un artículo o de un pedido no cumplen las validaciones básicas. */
public class DatosInvalidosException extends ReglaNegocioException {
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
