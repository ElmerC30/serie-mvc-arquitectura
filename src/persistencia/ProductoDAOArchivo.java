package persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import modelo.Producto;
import modelo.ProductoDAO;

/**
 * Implementacion que persiste en un archivo CSV (nombre,cantidad,precio).
 * Al construirse carga el archivo si existe; cada cambio reescribe el archivo completo.
 * Abrir, leer y escribir el archivo es asunto de esta clase, no del modelo.
 */
public class ProductoDAOArchivo implements ProductoDAO {
    private static final String ENCABEZADO = "nombre,cantidad,precio";

    private final Path archivo;
    private final List<Producto> cache = new ArrayList<>();

    public ProductoDAOArchivo(Path archivo) {
        this.archivo = archivo;
        cargar();
    }

    public Path getArchivo() { return archivo; }

    @Override
    public List<Producto> listar() {
        return new ArrayList<>(cache);
    }

    @Override
    public void insertar(Producto producto) {
        cache.add(producto);
        escribir();
    }

    @Override
    public void actualizar(Producto producto) {
        escribir();
    }

    private void cargar() {
        if (!Files.exists(archivo)) {
            return;
        }
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            for (int i = 1; i < lineas.size(); i++) {
                if (lineas.get(i).isBlank()) {
                    continue;
                }
                List<String> campos = separar(lineas.get(i));
                cache.add(new Producto(campos.get(0), Integer.parseInt(campos.get(1)),
                                       Double.parseDouble(campos.get(2))));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
    }

    private void escribir() {
        List<String> lineas = new ArrayList<>();
        lineas.add(ENCABEZADO);
        for (Producto p : cache) {
            // Double.toString conserva el valor exacto del precio al volver a leerlo.
            lineas.add(String.format(Locale.ROOT, "%s,%d,%s",
                    escapar(p.getNombre()), p.getCantidad(), Double.toString(p.getPrecio())));
        }
        try {
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            Files.move(temporal, archivo, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, e);
        }
    }

    static String escapar(String campo) {
        if (campo.contains(",") || campo.contains("\"")) {
            return "\"" + campo.replace("\"", "\"\"") + "\"";
        }
        return campo;
    }

    /** Separa una linea CSV respetando comillas. */
    static List<String> separar(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
