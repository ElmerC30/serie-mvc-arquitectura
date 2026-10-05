package modelo;

/** Regla 2 - el libro ya esta prestado. */
public class LibroNoDisponibleException extends ReglaNegocioException {
    private static final long serialVersionUID = 1L;

    public LibroNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
