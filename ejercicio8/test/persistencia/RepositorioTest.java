package persistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import modelo.Biblioteca;
import modelo.Libro;
import modelo.RepositorioBiblioteca;
import modelo.Socio;
import modelo.SocioConMultaException;
import soporte.RelojAjustable;

/** Persistencia intercambiable: el mismo escenario con memoria y con archivo. */
class RepositorioTest {

    private static RelojAjustable reloj() {
        return new RelojAjustable(Clock.fixed(LocalDate.of(2026, 10, 4).atStartOfDay(ZoneOffset.UTC).toInstant(),
                                              ZoneOffset.UTC));
    }

    private static void escenario(RepositorioBiblioteca repo) throws Exception {
        RelojAjustable reloj = reloj();
        Biblioteca b = new Biblioteca(repo, reloj);
        b.registrarLibro(new Libro("L01", "Cien anios, de soledad", "G. \"Gabo\" Garcia"));
        b.registrarSocio(new Socio("S01", "Ana"));
        b.prestar("L01", "S01");
        reloj.avanzarDias(9);
        assertEquals(10.0, b.devolver("L01"), 0.0);
        assertThrows(SocioConMultaException.class, () -> b.prestar("L01", "S01"));
    }

    @Test
    @DisplayName("Persistencia: las reglas funcionan igual con RepositorioMemoria")
    void conMemoria() throws Exception {
        escenario(new RepositorioMemoria());
    }

    @Test
    @DisplayName("Persistencia: las reglas funcionan igual con RepositorioArchivo")
    void conArchivo(@TempDir Path dir) throws Exception {
        escenario(new RepositorioArchivo(dir));
    }

    @Test
    @DisplayName("Persistencia: RepositorioArchivo conserva libros, socios, multas y prestamos al reabrir")
    void archivoSobreviveAlReinicio(@TempDir Path dir) throws Exception {
        escenario(new RepositorioArchivo(dir));
        Biblioteca reabierta = new Biblioteca(new RepositorioArchivo(dir), reloj());
        assertEquals("Cien anios, de soledad", reabierta.buscarLibro("L01").getTitulo());
        assertEquals("G. \"Gabo\" Garcia", reabierta.buscarLibro("L01").getAutor());
        assertEquals(10.0, reabierta.buscarSocio("S01").getMultaPendiente(), 0.0);
        assertTrue(reabierta.estaDisponible("L01"));
        assertThrows(SocioConMultaException.class, () -> reabierta.prestar("L01", "S01"));
    }
}
