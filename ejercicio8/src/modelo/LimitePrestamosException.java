package modelo;

/** Regla 1 - el socio ya tiene el maximo de prestamos activos. */
public class LimitePrestamosException extends ReglaNegocioException {
    private static final long serialVersionUID = 1L;

    public LimitePrestamosException(String mensaje) {
        super(mensaje);
    }
}
