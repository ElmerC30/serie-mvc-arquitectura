package persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import modelo.Articulo;
import modelo.ArticuloDAO;
import modelo.Familia;
import modelo.TipoArticulo;

/**
 * Implementación que persiste en un archivo de texto separado por punto y coma.
 * Carga el archivo al construirse y lo reescribe completo, de forma atómica, en cada cambio.
 * Abrir, leer y escribir el archivo es asunto exclusivo de esta clase.
 */
public class ArticuloDAOArchivo implements ArticuloDAO {
    private static final String SEPARADOR = ";";
    private static final String ENCABEZADO = "codigo;nombre;tipo;familia;precio;stock";

    private final Path archivo;
    private final Map<String, Articulo> cache = new LinkedHashMap<>();

    public ArticuloDAOArchivo(Path archivo) {
        this.archivo = archivo;
        cargar();
    }

    @Override
    public List<Articulo> listar() {
        return new ArrayList<>(cache.values());
    }

    @Override
    public Optional<Articulo> buscar(String codigo) {
        return Optional.ofNullable(cache.get(codigo));
    }

    @Override
    public void insertar(Articulo articulo) {
        cache.put(articulo.getCodigo(), articulo);
        escribir();
    }

    @Override
    public void actualizar(Articulo articulo) {
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
                String[] c = lineas.get(i).split(SEPARADOR, -1);
                Articulo a = new Articulo(c[0], c[1], TipoArticulo.valueOf(c[2]), Familia.valueOf(c[3]),
                                          new BigDecimal(c[4]), Integer.parseInt(c[5]));
                cache.put(a.getCodigo(), a);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
    }

    private void escribir() {
        List<String> lineas = new ArrayList<>();
        lineas.add(ENCABEZADO);
        for (Articulo a : cache.values()) {
            lineas.add(String.join(SEPARADOR, limpiar(a.getCodigo()), limpiar(a.getNombre()),
                    a.getTipo().name(), a.getFamilia().name(), a.getPrecio().toPlainString(),
                    Integer.toString(a.getStock())));
        }
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, e);
        }
    }

    /** El separador no puede aparecer dentro de un campo. */
    private static String limpiar(String campo) {
        return campo.replace(SEPARADOR, ",").replace("\n", " ");
    }
}
