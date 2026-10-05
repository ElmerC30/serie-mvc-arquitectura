package modelo;

/** Los datos de un producto violan una regla de negocio (nombre vacio, cantidad negativa, precio <= 0). */
public class DatosInvalidosException extends Exception {
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
