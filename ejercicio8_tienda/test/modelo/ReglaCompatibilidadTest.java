package modelo;

import static apoyo.Inventarios.BOQ_ALTO;
import static apoyo.Inventarios.BOQ_TENOR;
import static apoyo.Inventarios.CANA_ALTO;
import static apoyo.Inventarios.CANA_TENOR;
import static apoyo.Inventarios.SAX_ALTO;
import static apoyo.Inventarios.SAX_TENOR;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import apoyo.Inventarios;

@DisplayName("Regla 1 - compatibilidad de boquillas y cañas con el saxofón")
class ReglaCompatibilidadTest {
    private Tienda tienda;

    @BeforeEach
    void preparar() {
        tienda = Inventarios.tiendaDePrueba();
    }

    @Test
    @DisplayName("Saxofón alto con boquilla y cañas de alto: se acepta")
    void kitDeLaMismaFamiliaSeAcepta() {
        assertDoesNotThrow(() -> tienda.vender(List.of(
                new LineaPedido(SAX_ALTO, 1), new LineaPedido(BOQ_ALTO, 1), new LineaPedido(CANA_ALTO, 5))));
    }

    @Test
    @DisplayName("Saxofón alto con boquilla de tenor: se rechaza")
    void boquillaDeOtraFamiliaSeRechaza() {
        assertThrows(AccesorioIncompatibleException.class, () -> tienda.vender(List.of(
                new LineaPedido(SAX_ALTO, 1), new LineaPedido(BOQ_TENOR, 1))));
    }

    @Test
    @DisplayName("Saxofón tenor con cañas de alto: se rechaza")
    void canaDeOtraFamiliaSeRechaza() {
        assertThrows(AccesorioIncompatibleException.class, () -> tienda.cotizar(List.of(
                new LineaPedido(SAX_TENOR, 1), new LineaPedido(CANA_ALTO, 10))));
    }

    @Test
    @DisplayName("Si el pedido trae un alto y un tenor, cada accesorio puede ser de cualquiera de los dos")
    void dosFamiliasEnElMismoPedido() {
        assertDoesNotThrow(() -> tienda.cotizar(List.of(
                new LineaPedido(SAX_ALTO, 1), new LineaPedido(SAX_TENOR, 1),
                new LineaPedido(BOQ_TENOR, 1), new LineaPedido(CANA_ALTO, 3))));
    }

    @Test
    @DisplayName("Accesorios sueltos (sin saxofón) se venden como refacción de cualquier familia")
    void accesoriosSueltosNoSeValidan() {
        assertDoesNotThrow(() -> tienda.vender(List.of(
                new LineaPedido(CANA_ALTO, 2), new LineaPedido(CANA_TENOR, 2))));
    }

    @Test
    @DisplayName("Un pedido incompatible no descuenta stock de ninguna línea (venta atómica)")
    void rechazoNoTocaElInventario() throws Exception {
        assertThrows(AccesorioIncompatibleException.class, () -> tienda.vender(List.of(
                new LineaPedido(SAX_ALTO, 1), new LineaPedido(CANA_ALTO, 5), new LineaPedido(BOQ_TENOR, 1))));
        assertEquals(4, tienda.buscar(SAX_ALTO).getStock());
        assertEquals(40, tienda.buscar(CANA_ALTO).getStock());
        assertEquals(2, tienda.buscar(BOQ_TENOR).getStock());
    }
}
