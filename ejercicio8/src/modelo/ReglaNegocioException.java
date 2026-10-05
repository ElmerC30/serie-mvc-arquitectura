package modelo;

/** Base de todas las violaciones de reglas de negocio de la biblioteca. */
public class ReglaNegocioException extends Exception {
    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
