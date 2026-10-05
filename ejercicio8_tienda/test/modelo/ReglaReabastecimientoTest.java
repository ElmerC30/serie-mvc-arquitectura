package modelo;

import static apoyo.Inventarios.BOQ_TENOR;
import static apoyo.Inventarios.CANA_ALTO;
import static apoyo.Inventarios.CANA_TENOR;
import static apoyo.Inventarios.SAX_TENOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import apoyo.Inventarios;

@DisplayName("Regla 2 - alerta de stock mínimo según el tipo de artículo")
class ReglaReabastecimientoTest {
    private Tienda tienda;

    @BeforeEach
    void preparar() {
        tienda = Inventarios.tiendaDePrueba();
    }

    private List<String> codigosEnAlerta() {
        return tienda.getAlertasReabasto().stream().map(Articulo::getCodigo).collect(Collectors.toList());
    }

    @Test
    @DisplayName("Al iniciar, sólo las cañas de tenor (18 < 20) están en alerta")
    void estadoInicial() {
        assertEquals(List.of(CANA_TENOR), codigosEnAlerta());
    }

    @Test
    @DisplayName("Vender cañas de alto hasta dejar 19 dispara la alerta; con 20 todavía no")
    void umbralDeCanasEsVeinte() throws Exception {
        tienda.vender(List.of(new LineaPedido(CANA_ALTO, 20)));
        assertFalse(tienda.buscar(CANA_ALTO).requiereReabasto(), "20 unidades no es menor que 20");
        tienda.vender(List.of(new LineaPedido(CANA_ALTO, 1)));
        assertTrue(codigosEnAlerta().contains(CANA_ALTO));
    }

    @Test
    @DisplayName("Las boquillas usan su propio umbral: 1 unidad (< 2) ya requiere reabasto")
    void umbralDeBoquillas() throws Exception {
        tienda.vender(List.of(new LineaPedido(BOQ_TENOR, 1)));
        assertTrue(codigosEnAlerta().contains(BOQ_TENOR));
    }

    @Test
    @DisplayName("Un saxofón sólo entra en alerta cuando se agota")
    void umbralDeSaxofones() throws Exception {
        tienda.vender(List.of(new LineaPedido(SAX_TENOR, 1)));
        assertFalse(codigosEnAlerta().contains(SAX_TENOR));
        tienda.vender(List.of(new LineaPedido(SAX_TENOR, 1)));
        assertTrue(codigosEnAlerta().contains(SAX_TENOR));
    }

    @Test
    @DisplayName("Reabastecer por encima del mínimo retira la alerta y avisa a los observadores")
    void reabastecerQuitaLaAlerta() throws Exception {
        int[] avisos = {0};
        tienda.agregarObservador(() -> avisos[0]++);
        tienda.reabastecer(CANA_TENOR, 12);
        assertEquals(30, tienda.buscar(CANA_TENOR).getStock());
        assertTrue(codigosEnAlerta().isEmpty());
        assertEquals(1, avisos[0]);
    }

    @Test
    @DisplayName("Reabastecer con cero o negativo se rechaza")
    void reabastoInvalido() {
        org.junit.jupiter.api.Assertions.assertThrows(DatosInvalidosException.class,
                () -> tienda.reabastecer(CANA_TENOR, 0));
    }
}
