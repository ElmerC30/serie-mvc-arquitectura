package modelo;

/** Los datos no cumplen las reglas de alta. */
public class DatosInvalidosException extends ReglaNegocioException {
    private static final long serialVersionUID = 1L;

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
