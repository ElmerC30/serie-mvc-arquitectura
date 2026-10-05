package controlador;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import modelo.Biblioteca;
import modelo.EstadoLibro;
import modelo.EstadoSocio;
import modelo.Libro;
import modelo.Socio;
import persistencia.RepositorioMemoria;
import soporte.RelojAjustable;
import vista.VistaBiblioteca;

/** El controlador se prueba con una vista falsa que solo registra lo que se le pide mostrar. */
class ControladorBibliotecaTest {

    static class VistaFalsa implements VistaBiblioteca {
        final List<String> exitos = new ArrayList<>();
        final List<String> errores = new ArrayList<>();
        List<EstadoLibro> libros;
        List<EstadoSocio> socios;

        @Override public void mostrarExito(String m)             { exitos.add(m); }
        @Override public void mostrarError(String m)             { errores.add(m); }
        @Override public void mostrarLibros(List<EstadoLibro> l)  { libros = l; }
        @Override public void mostrarSocios(List<EstadoSocio> s)  { socios = s; }
        @Override public void mostrarFecha(LocalDate hoy)         { }
    }

    private VistaFalsa vista;
    private ControladorBiblioteca c;
    private Biblioteca b;

    @BeforeEach
    void setUp() throws Exception {
        RelojAjustable reloj = new RelojAjustable(Clock.fixed(
                LocalDate.of(2026, 10, 4).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        b = new Biblioteca(new RepositorioMemoria(), reloj);
        b.registrarLibro(new Libro("L01", "Pedro Paramo", "Juan Rulfo"));
        b.registrarSocio(new Socio("S01", "Ana"));
        vista = new VistaFalsa();
        c = new ControladorBiblioteca(b, vista, reloj);
    }

    @Test
    @DisplayName("Controlador: un monto no numerico se informa como error sin llegar al modelo")
    void montoNoNumerico() {
        c.pagarMulta("S01", "abc");
        assertEquals(1, vista.errores.size());
        assertTrue(vista.errores.get(0).contains("abc"));
    }

    @Test
    @DisplayName("Controlador: la violacion de una regla del modelo llega a la vista como error")
    void reglaVioladaSeMuestra() {
        c.prestar("L01", "S01");
        c.prestar("L01", "S01");
        assertEquals(1, vista.exitos.size());
        assertTrue(vista.exitos.get(0).contains("11/10/2026"));
        assertEquals(1, vista.errores.size());
        assertTrue(vista.errores.get(0).contains("ya esta prestado"));
    }

    @Test
    @DisplayName("Controlador: avanzar dias y devolver tarde informa la multa con formato de dinero")
    void avanzarYDevolverTarde() {
        c.prestar("L01", "S01");
        c.avanzarDias("10");
        c.devolver("L01");
        assertTrue(vista.exitos.get(vista.exitos.size() - 1).contains("$15.00"), vista.exitos.toString());
        c.avanzarDias("-1");
        c.avanzarDias("x");
        assertEquals(2, vista.errores.size());
    }

    @Test
    @DisplayName("Controlador: refrescar entrega a la vista el estado calculado por el modelo")
    void refrescar() {
        c.prestar("L01", "S01");
        c.refrescar();
        assertFalse(vista.libros.get(0).isDisponible());
        assertEquals(1, vista.socios.get(0).getPrestamosActivos());
    }
}
