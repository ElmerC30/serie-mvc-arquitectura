package modelo;

/** Raíz de los rechazos del modelo: la operación viola una regla del negocio. */
public abstract class ReglaNegocioException extends Exception {
    protected ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
