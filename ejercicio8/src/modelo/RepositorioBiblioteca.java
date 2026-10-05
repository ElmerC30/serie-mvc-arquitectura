package modelo;

import java.util.List;

/**
 * Lo que el modelo necesita del almacenamiento. Las implementaciones viven en el paquete
 * persistencia y dependen de esta interfaz, nunca al reves.
 */
public interface RepositorioBiblioteca {
    List<Libro> listarLibros();
    List<Socio> listarSocios();
    List<Prestamo> listarPrestamos();

    void insertarLibro(Libro libro);
    void insertarSocio(Socio socio);
    void insertarPrestamo(Prestamo prestamo);

    /** Persiste el nuevo estado de un socio existente (multa). */
    void actualizarSocio(Socio socio);

    /** Persiste el nuevo estado de un prestamo existente (devolucion). */
    void actualizarPrestamo(Prestamo prestamo);
}
