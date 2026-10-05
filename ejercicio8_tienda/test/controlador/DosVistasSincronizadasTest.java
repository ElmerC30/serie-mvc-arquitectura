package controlador;

import static apoyo.Inventarios.BOQ_TENOR;
import static apoyo.Inventarios.CANA_TENOR;
import static apoyo.Inventarios.SAX_ALTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import apoyo.Inventarios;
import modelo.Tienda;

@DisplayName("Dos vistas sobre el mismo modelo, sincronizadas con Observer")
class DosVistasSincronizadasTest {
    private Tienda tienda;
    private VistasFalsas.Almacen almacen;
    private VistasFalsas.Mostrador mostrador;
    private ControladorMostrador caja;
    private ControladorAlmacen bodega;

    @BeforeEach
    void preparar() {
        tienda = Inventarios.tiendaDePrueba();
        almacen = new VistasFalsas.Almacen();
        mostrador = new VistasFalsas.Mostrador();
        bodega = new ControladorAlmacen(tienda, almacen);
        caja = new ControladorMostrador(tienda, mostrador);
        bodega.iniciar();
        caja.iniciar();
    }

    @Test
    @DisplayName("Una venta en el mostrador actualiza el almacén y su lista de alertas")
    void ventaEnMostradorSeVeEnAlmacen() {
        mostrador.capturar(BOQ_TENOR, "1");
        caja.agregar();
        int refrescosAntes = almacen.refrescos;
        caja.cobrar();

        assertNotNull(mostrador.vendido);
        assertEquals(refrescosAntes + 1, almacen.refrescos);
        assertTrue(almacen.alertas.stream().anyMatch(a -> a.getCodigo().equals(BOQ_TENOR)));
    }

    @Test
    @DisplayName("Un reabasto en el almacén actualiza el stock que ve el mostrador")
    void reabastoEnAlmacenSeVeEnMostrador() {
        almacen.codigo = CANA_TENOR;
        almacen.cantidad = "12";
        almacen.reabastecer.run();

        assertNull(almacen.ultimoError);
        assertEquals(30, mostrador.stockEnCatalogo(CANA_TENOR));
        assertTrue(almacen.alertas.isEmpty());
    }

    @Test
    @DisplayName("El controlador no agrega al pedido una línea que el modelo rechaza")
    void lineaIncompatibleNoEntraAlPedido() {
        mostrador.capturar(SAX_ALTO, "1");
        caja.agregar();
        mostrador.capturar(BOQ_TENOR, "1");
        caja.agregar();

        assertEquals(1, caja.getPedido().size());
        assertNotNull(mostrador.ultimoError);
    }

    @Test
    @DisplayName("Texto no numérico en cantidad se informa sin llegar al modelo")
    void cantidadNoNumerica() {
        mostrador.capturar(SAX_ALTO, "abc");
        caja.agregar();
        assertEquals(0, caja.getPedido().size());
        assertEquals("La cantidad debe ser un número entero", mostrador.ultimoError);
    }
}
