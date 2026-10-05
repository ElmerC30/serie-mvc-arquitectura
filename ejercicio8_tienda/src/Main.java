import java.math.BigDecimal;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import controlador.ControladorAlmacen;
import controlador.ControladorMostrador;
import modelo.Articulo;
import modelo.ArticuloDAO;
import modelo.DatosInvalidosException;
import modelo.Familia;
import modelo.Tienda;
import modelo.TipoArticulo;
import persistencia.ArticuloDAOArchivo;
import persistencia.ArticuloDAOMemoria;
import vista.VentanaAlmacen;
import vista.VentanaMostrador;

/**
 * Ensambla el sistema de la tienda de instrumentos y abre las dos vistas sobre el mismo modelo.
 *
 * Persistencia intercambiable: para cambiar de almacenamiento se modifica UNA línea, la marcada DAO.
 *   new ArticuloDAOMemoria()                                      datos en memoria
 *   new ArticuloDAOArchivo(Paths.get("datos", "inventario.csv"))  datos persistidos en archivo
 */
public class Main {
    public static void main(String[] args) throws DatosInvalidosException {
        ArticuloDAO dao = new ArticuloDAOArchivo(Paths.get("datos", "inventario.csv")); // DAO
        Tienda tienda = new Tienda(dao);
        if (tienda.estaVacia()) {
            cargarInventarioInicial(tienda);
        }
        SwingUtilities.invokeLater(() -> {
            new ControladorAlmacen(tienda, new VentanaAlmacen()).iniciar();
            new ControladorMostrador(tienda, new VentanaMostrador()).iniciar();
        });
    }

    /** Inventario de arranque. Pasa por Tienda.registrar, así que también se valida. */
    static void cargarInventarioInicial(Tienda tienda) throws DatosInvalidosException {
        tienda.registrar(new Articulo("SAX-AS400GP", "Saxofón alto Jean Paul AS-400GP",
                TipoArticulo.SAXOFON, Familia.ALTO, new BigDecimal("12999.00"), 4));
        tienda.registrar(new Articulo("SAX-TS400", "Saxofón tenor Jean Paul TS-400",
                TipoArticulo.SAXOFON, Familia.TENOR, new BigDecimal("18499.00"), 2));
        tienda.registrar(new Articulo("BOQ-BL105-AL", "Boquilla Berg Larsen 105/1 SMS (alto)",
                TipoArticulo.BOQUILLA, Familia.ALTO, new BigDecimal("4290.00"), 5));
        tienda.registrar(new Articulo("BOQ-BL105-TN", "Boquilla Berg Larsen 105/1 SMS (tenor)",
                TipoArticulo.BOQUILLA, Familia.TENOR, new BigDecimal("4590.00"), 2));
        tienda.registrar(new Articulo("CAN-PCO-AL25", "Caña D'Addario Plasticover alto #2.5",
                TipoArticulo.CANA, Familia.ALTO, new BigDecimal("89.00"), 40));
        tienda.registrar(new Articulo("CAN-PCO-TN25", "Caña D'Addario Plasticover tenor #2.5",
                TipoArticulo.CANA, Familia.TENOR, new BigDecimal("109.00"), 18));
    }
}
