package modelo;

/** No existe el libro, socio o prestamo indicado. */
public class NoEncontradoException extends ReglaNegocioException {
    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
