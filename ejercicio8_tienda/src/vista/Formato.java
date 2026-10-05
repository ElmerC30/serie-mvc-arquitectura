package vista;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import modelo.Familia;
import modelo.TipoArticulo;

/** Decisiones de presentación compartidas por las vistas: moneda y etiquetas legibles. */
public final class Formato {
    private Formato() {
    }

    /** Pesos con separador de miles y dos decimales: $12,999.00 */
    public static String dinero(BigDecimal monto) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator(',');
        simbolos.setDecimalSeparator('.');
        return new DecimalFormat("$#,##0.00", simbolos).format(monto);
    }

    public static String tipo(TipoArticulo tipo) {
        switch (tipo) {
            case SAXOFON:  return "Saxofón";
            case BOQUILLA: return "Boquilla";
            case CANA:     return "Caña";
            default:       return tipo.name();
        }
    }

    public static String familia(Familia familia) {
        String n = familia.name().toLowerCase();
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }
}
