package modelo;

/** Se busca o se vende un producto que no existe en el inventario. */
public class ProductoNoEncontradoException extends Exception {
    public ProductoNoEncontradoException(String nombre) {
        super("No existe el producto " + nombre);
    }
}
