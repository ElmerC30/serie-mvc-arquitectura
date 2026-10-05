package vista;

import java.time.LocalDate;
import java.util.List;

import modelo.EstadoLibro;
import modelo.EstadoSocio;

/** Lo que cualquier vista de la biblioteca sabe mostrar. Los controladores dependen de esta interfaz. */
public interface VistaBiblioteca {
    void mostrarExito(String mensaje);
    void mostrarError(String mensaje);
    void mostrarLibros(List<EstadoLibro> libros);
    void mostrarSocios(List<EstadoSocio> socios);
    void mostrarFecha(LocalDate hoy);
}
