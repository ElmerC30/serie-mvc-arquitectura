package vista;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import modelo.Producto;

/**
 * Segunda vista "sin pantalla": presenta el listado del inventario como un archivo CSV.
 * El formato (separador, punto decimal, encabezado) es decision de esta vista.
 */
public class VistaCSV implements VistaInventario {
    private final Path destino;
    private final PrintStream log;

    public VistaCSV(Path destino) {
        this(destino, System.err);
    }

    public VistaCSV(Path destino, PrintStream log) {
        this.destino = destino;
        this.log = log;
    }

    public Path getDestino() { return destino; }

    /** Convierte la lista de productos en lineas CSV (sin escribir nada). */
    public static List<String> aLineasCSV(List<Producto> productos) {
        List<String> lineas = new ArrayList<>();
        lineas.add("nombre,cantidad,precio,valor");
        for (Producto p : productos) {
            lineas.add(String.format(Locale.ROOT, "%s,%d,%.2f,%.2f",
                    escapar(p.getNombre()), p.getCantidad(), p.getPrecio(), p.getValor()));
        }
        return lineas;
    }

    private static String escapar(String campo) {
        if (campo.contains(",") || campo.contains("\"") || campo.contains("\n")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    @Override
    public void mostrarListado(List<Producto> productos) {
        try {
            Files.write(destino, aLineasCSV(productos), StandardCharsets.UTF_8);
            log.println("Inventario exportado a " + destino.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + destino, e);
        }
    }

    @Override public void mostrarMensaje(String mensaje)   { log.println(mensaje); }
    @Override public void mostrarDatosInvalidos()          { mostrarMensaje("Datos invalidos"); }
    @Override public void mostrarAgregado()                { mostrarMensaje("Agregado."); }
    @Override public void mostrarNoExiste()                { mostrarMensaje("No existe"); }
    @Override public void mostrarStockInsuficiente()       { mostrarMensaje("Stock insuficiente"); }
    @Override public void mostrarVentaOk(int quedan)       { mostrarMensaje("Venta ok. Quedan " + quedan); }
    @Override public void mostrarValorTotal(double total)  {
        mostrarMensaje(String.format(Locale.ROOT, "Valor del inventario: %.2f", total));
    }
}
