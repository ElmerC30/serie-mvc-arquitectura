package modelo;

/**
 * Entidad del dominio: un producto del inventario.
 * Reemplaza a las tres listas paralelas (nombres, cantidades, precios) del codigo original.
 */
public class Producto {
    private final String nombre;
    private volatile int cantidad;
    private final double precio;

    public Producto(String nombre, int cantidad, double precio) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getNombre()  { return nombre; }
    public int getCantidad()   { return cantidad; }
    public double getPrecio()  { return precio; }

    /** Valor monetario del producto en almacen (cantidad x precio). */
    public double getValor()   { return cantidad * precio; }

    /** Solo el Inventario modifica el stock, despues de validar la regla de negocio. */
    void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
