package apoyo;

import java.math.BigDecimal;

import modelo.Articulo;
import modelo.DatosInvalidosException;
import modelo.Familia;
import modelo.Tienda;
import modelo.TipoArticulo;
import persistencia.ArticuloDAOMemoria;

/** Inventario de prueba compartido por las pruebas (en memoria, sin interfaz). */
public final class Inventarios {
    public static final String SAX_ALTO = "SAX-AS400GP";
    public static final String SAX_TENOR = "SAX-TS400";
    public static final String BOQ_ALTO = "BOQ-BL105-AL";
    public static final String BOQ_TENOR = "BOQ-BL105-TN";
    public static final String CANA_ALTO = "CAN-PCO-AL25";
    public static final String CANA_TENOR = "CAN-PCO-TN25";

    private Inventarios() {
    }

    public static Tienda tiendaDePrueba() {
        Tienda t = new Tienda(new ArticuloDAOMemoria());
        try {
            t.registrar(articulo(SAX_ALTO, "Saxofón alto Jean Paul AS-400GP", TipoArticulo.SAXOFON, Familia.ALTO, "12999.00", 4));
            t.registrar(articulo(SAX_TENOR, "Saxofón tenor Jean Paul TS-400", TipoArticulo.SAXOFON, Familia.TENOR, "18499.00", 2));
            t.registrar(articulo(BOQ_ALTO, "Boquilla Berg Larsen 105/1 SMS (alto)", TipoArticulo.BOQUILLA, Familia.ALTO, "4290.00", 5));
            t.registrar(articulo(BOQ_TENOR, "Boquilla Berg Larsen 105/1 SMS (tenor)", TipoArticulo.BOQUILLA, Familia.TENOR, "4590.00", 2));
            t.registrar(articulo(CANA_ALTO, "Caña D'Addario Plasticover alto #2.5", TipoArticulo.CANA, Familia.ALTO, "89.00", 40));
            t.registrar(articulo(CANA_TENOR, "Caña D'Addario Plasticover tenor #2.5", TipoArticulo.CANA, Familia.TENOR, "109.00", 18));
        } catch (DatosInvalidosException e) {
            throw new IllegalStateException(e);
        }
        return t;
    }

    public static Articulo articulo(String codigo, String nombre, TipoArticulo tipo, Familia familia,
                                    String precio, int stock) {
        return new Articulo(codigo, nombre, tipo, familia, new BigDecimal(precio), stock);
    }
}
