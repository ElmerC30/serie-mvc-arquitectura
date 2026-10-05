package modelo;

/**
 * Clase de artículo que vende la tienda.
 * Cada tipo conoce su stock mínimo (Regla 2): las cañas son consumibles de alta rotación y se
 * reabastecen mucho antes que un instrumento.
 */
public enum TipoArticulo {
    SAXOFON(1),
    BOQUILLA(2),
    CANA(20);

    private final int stockMinimo;

    TipoArticulo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    /** Por debajo de este número de unidades el artículo requiere reabastecimiento. */
    public int getStockMinimo() { return stockMinimo; }

    /** Boquillas y cañas son accesorios: dependen de la familia de un saxofón. */
    public boolean esAccesorio() { return this != SAXOFON; }
}
