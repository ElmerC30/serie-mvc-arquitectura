package modelo;

import java.math.BigDecimal;

/** Entidad del dominio: un artículo del inventario de la tienda. */
public class Articulo {
    private final String codigo;
    private final String nombre;
    private final TipoArticulo tipo;
    private final Familia familia;
    private final BigDecimal precio;
    private volatile int stock;

    public Articulo(String codigo, String nombre, TipoArticulo tipo, Familia familia,
                    BigDecimal precio, int stock) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.familia = familia;
        this.precio = precio;
        this.stock = stock;
    }

    public String getCodigo()       { return codigo; }
    public String getNombre()       { return nombre; }
    public TipoArticulo getTipo()   { return tipo; }
    public Familia getFamilia()     { return familia; }
    public BigDecimal getPrecio()   { return precio; }
    public int getStock()           { return stock; }

    /** Regla 2: el stock quedó por debajo del mínimo que exige su tipo. */
    public boolean requiereReabasto() {
        return stock < tipo.getStockMinimo();
    }

    /** Regla 1: un accesorio sólo sirve a saxofones de su misma familia. */
    public boolean esCompatibleCon(Familia familiaSaxofon) {
        return familia == familiaSaxofon;
    }

    /** Sólo la Tienda modifica el stock, después de validar las reglas. */
    void setStock(int stock) { this.stock = stock; }
}
