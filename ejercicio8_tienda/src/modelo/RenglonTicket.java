package modelo;

import java.math.BigDecimal;

/** Un renglón ya calculado por el modelo: precio, descuento aplicado e importe final. */
public record RenglonTicket(String codigo, String nombre, int cantidad, BigDecimal precioUnitario,
                            int unidadesConDescuento, BigDecimal descuento, BigDecimal importe) {
}
