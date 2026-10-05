package persistencia;

import java.util.ArrayList;
import java.util.List;

import modelo.Libro;
import modelo.Prestamo;
import modelo.RepositorioBiblioteca;
import modelo.Socio;

/** Datos en memoria: se pierden al cerrar el programa. */
public class RepositorioMemoria implements RepositorioBiblioteca {
    private final List<Libro> libros = new ArrayList<>();
    private final List<Socio> socios = new ArrayList<>();
    private final List<Prestamo> prestamos = new ArrayList<>();

    @Override public List<Libro> listarLibros()        { return new ArrayList<>(libros); }
    @Override public List<Socio> listarSocios()        { return new ArrayList<>(socios); }
    @Override public List<Prestamo> listarPrestamos()  { return new ArrayList<>(prestamos); }

    @Override public void insertarLibro(Libro libro)          { libros.add(libro); }
    @Override public void insertarSocio(Socio socio)          { socios.add(socio); }
    @Override public void insertarPrestamo(Prestamo prestamo) { prestamos.add(prestamo); }

    // Los objetos ya viven en estas listas: no hay nada que copiar.
    @Override public void actualizarSocio(Socio socio)          { }
    @Override public void actualizarPrestamo(Prestamo prestamo) { }
}
