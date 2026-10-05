package modelo;

/** Regla 4 - el socio tiene multa pendiente. */
public class SocioConMultaException extends ReglaNegocioException {
    private static final long serialVersionUID = 1L;

    public SocioConMultaException(String mensaje) {
        super(mensaje);
    }
}
