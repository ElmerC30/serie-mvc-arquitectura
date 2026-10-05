package vista;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Decisiones de presentacion compartidas por las vistas: fechas y montos. */
public final class Formato {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale MX = Locale.forLanguageTag("es-MX");

    private Formato() { }

    public static String fecha(LocalDate f) {
        return f == null ? "-" : FECHA.format(f);
    }

    public static String dinero(double monto) {
        return String.format(MX, "$%,.2f", monto);
    }
}
