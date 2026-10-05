package persistencia;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import modelo.Libro;
import modelo.Prestamo;
import modelo.RepositorioBiblioteca;
import modelo.Socio;

/**
 * Persiste en tres archivos CSV dentro de una carpeta: libros.csv, socios.csv y prestamos.csv.
 * Carga todo al construirse y reescribe el archivo afectado en cada cambio.
 */
public class RepositorioArchivo implements RepositorioBiblioteca {
    private final Path libros;
    private final Path socios;
    private final Path prestamos;
    private final List<Libro> cacheLibros;
    private final List<Socio> cacheSocios;
    private final List<Prestamo> cachePrestamos;

    public RepositorioArchivo(Path carpeta) {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        libros = carpeta.resolve("libros.csv");
        socios = carpeta.resolve("socios.csv");
        prestamos = carpeta.resolve("prestamos.csv");
        cacheLibros = leer(libros, c -> new Libro(c.get(0), c.get(1), c.get(2)));
        cacheSocios = leer(socios, c -> new Socio(c.get(0), c.get(1), Double.parseDouble(c.get(2))));
        cachePrestamos = leer(prestamos, c -> new Prestamo(c.get(0), c.get(1), LocalDate.parse(c.get(2)),
                LocalDate.parse(c.get(3)), c.get(4).isEmpty() ? null : LocalDate.parse(c.get(4))));
    }

    @Override public List<Libro> listarLibros()        { return new ArrayList<>(cacheLibros); }
    @Override public List<Socio> listarSocios()        { return new ArrayList<>(cacheSocios); }
    @Override public List<Prestamo> listarPrestamos()  { return new ArrayList<>(cachePrestamos); }

    @Override public void insertarLibro(Libro l)         { cacheLibros.add(l); guardarLibros(); }
    @Override public void insertarSocio(Socio s)         { cacheSocios.add(s); guardarSocios(); }
    @Override public void insertarPrestamo(Prestamo p)   { cachePrestamos.add(p); guardarPrestamos(); }
    @Override public void actualizarSocio(Socio s)       { guardarSocios(); }
    @Override public void actualizarPrestamo(Prestamo p) { guardarPrestamos(); }

    private void guardarLibros() {
        List<String> l = new ArrayList<>();
        l.add("isbn,titulo,autor");
        for (Libro x : cacheLibros) {
            l.add(Csv.linea(x.getIsbn(), x.getTitulo(), x.getAutor()));
        }
        escribir(libros, l);
    }

    private void guardarSocios() {
        List<String> l = new ArrayList<>();
        l.add("id,nombre,multa");
        for (Socio x : cacheSocios) {
            l.add(Csv.linea(x.getId(), x.getNombre(), Double.toString(x.getMultaPendiente())));
        }
        escribir(socios, l);
    }

    private void guardarPrestamos() {
        List<String> l = new ArrayList<>();
        l.add("isbn,socio,fecha_prestamo,fecha_limite,fecha_devolucion");
        for (Prestamo x : cachePrestamos) {
            l.add(Csv.linea(x.getIsbn(), x.getSocioId(), x.getFechaPrestamo().toString(),
                    x.getFechaLimite().toString(),
                    x.getFechaDevolucion() == null ? "" : x.getFechaDevolucion().toString()));
        }
        escribir(prestamos, l);
    }

    private static <T> List<T> leer(Path archivo, Function<List<String>, T> convertir) {
        List<T> resultado = new ArrayList<>();
        if (!Files.exists(archivo)) {
            return resultado;
        }
        try {
            List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
            for (int i = 1; i < lineas.size(); i++) {
                if (!lineas.get(i).isBlank()) {
                    resultado.add(convertir.apply(Csv.separar(lineas.get(i))));
                }
            }
            return resultado;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + archivo, e);
        }
    }

    private static void escribir(Path archivo, List<String> lineas) {
        try {
            Path tmp = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            Files.write(tmp, lineas, StandardCharsets.UTF_8);
            Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + archivo, e);
        }
    }
}
