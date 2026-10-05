package modelo;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import persistencia.RepositorioMemoria;
import soporte.RelojAjustable;

/** Reglas de negocio del proyecto integrador, probadas sin ninguna vista. */
class BibliotecaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 4);
    private RelojAjustable reloj;
    private Biblioteca b;

    @BeforeEach
    void setUp() throws Exception {
        reloj = new RelojAjustable(java.time.Clock.fixed(
                HOY.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), java.time.ZoneOffset.UTC));
        b = new Biblioteca(new RepositorioMemoria(), reloj);
        for (int i = 1; i <= 5; i++) {
            b.registrarLibro(new Libro("L0" + i, "Libro " + i, "Autor " + i));
        }
        b.registrarSocio(new Socio("S01", "Ana"));
        b.registrarSocio(new Socio("S02", "Luis"));
    }

    @Nested
    @DisplayName("R1 - maximo 3 prestamos activos por socio")
    class LimiteDePrestamos {
        @Test
        @DisplayName("R1: el cuarto prestamo se rechaza y no se registra")
        void cuartoPrestamoSeRechaza() throws Exception {
            b.prestar("L01", "S01");
            b.prestar("L02", "S01");
            b.prestar("L03", "S01");
            assertThrows(LimitePrestamosException.class, () -> b.prestar("L04", "S01"));
            assertEquals(3, b.prestamosActivosDe("S01").size());
            assertTrue(b.estaDisponible("L04"));
        }

        @Test
        @DisplayName("R1: al devolver uno, el socio puede volver a pedir")
        void devolverLiberaCupo() throws Exception {
            b.prestar("L01", "S01");
            b.prestar("L02", "S01");
            b.prestar("L03", "S01");
            b.devolver("L02");
            assertDoesNotThrow(() -> b.prestar("L04", "S01"));
        }

        @Test
        @DisplayName("R1: el limite es por socio, no global")
        void limiteEsPorSocio() throws Exception {
            b.prestar("L01", "S01");
            b.prestar("L02", "S01");
            b.prestar("L03", "S01");
            assertDoesNotThrow(() -> b.prestar("L04", "S02"));
        }
    }

    @Nested
    @DisplayName("R2 - un libro prestado no se presta otra vez")
    class LibroPrestado {
        @Test
        @DisplayName("R2: prestar un libro ya prestado lanza LibroNoDisponibleException")
        void libroPrestadoNoSePresta() throws Exception {
            b.prestar("L01", "S01");
            assertThrows(LibroNoDisponibleException.class, () -> b.prestar("L01", "S02"));
            assertEquals(0, b.prestamosActivosDe("S02").size());
        }

        @Test
        @DisplayName("R2: tras devolverlo, el libro vuelve a estar disponible")
        void libroDevueltoVuelveADisponible() throws Exception {
            b.prestar("L01", "S01");
            b.devolver("L01");
            assertTrue(b.estaDisponible("L01"));
            assertDoesNotThrow(() -> b.prestar("L01", "S02"));
        }
    }

    @Nested
    @DisplayName("R3 - multa de $5 por dia de retraso (plazo 7 dias)")
    class Multas {
        @Test
        @DisplayName("R3: devolver dentro del plazo (dia 7) no genera multa")
        void aTiempoSinMulta() throws Exception {
            b.prestar("L01", "S01");
            reloj.avanzarDias(7);
            assertEquals(0.0, b.devolver("L01"), 0.0);
            assertFalse(b.buscarSocio("S01").tieneMulta());
        }

        @Test
        @DisplayName("R3: devolver 3 dias tarde genera $15 y se acumula en el socio")
        void tardeGeneraMulta() throws Exception {
            b.prestar("L01", "S01");
            b.prestar("L02", "S01");
            reloj.avanzarDias(10);
            assertEquals(15.0, b.devolver("L01"), 0.0);
            assertEquals(15.0, b.devolver("L02"), 0.0);
            assertEquals(30.0, b.buscarSocio("S01").getMultaPendiente(), 0.0);
        }

        @Test
        @DisplayName("R3: calculo puro de la multa en los limites")
        void calculoDeMulta() {
            LocalDate limite = HOY.plusDays(7);
            assertEquals(0.0, Biblioteca.calcularMulta(limite, limite.minusDays(1)), 0.0);
            assertEquals(0.0, Biblioteca.calcularMulta(limite, limite), 0.0);
            assertEquals(5.0, Biblioteca.calcularMulta(limite, limite.plusDays(1)), 0.0);
            assertEquals(150.0, Biblioteca.calcularMulta(limite, limite.plusDays(30)), 0.0);
        }

        @Test
        @DisplayName("R3: el prestamo aparece como vencido despues de la fecha limite")
        void vencimiento() throws Exception {
            b.prestar("L01", "S01");
            reloj.avanzarDias(8);
            EstadoLibro e = b.getEstadoLibros().get(0);
            assertFalse(e.isDisponible());
            assertTrue(e.isVencido());
            assertEquals("Ana", e.getNombreSocio());
        }

        @Test
        @DisplayName("R3: devolver un libro que no esta prestado se rechaza")
        void devolverSinPrestamo() {
            assertThrows(NoEncontradoException.class, () -> b.devolver("L01"));
        }
    }

    @Nested
    @DisplayName("R4 - socio con multa no puede pedir prestamos")
    class SocioConMulta {
        private void generarMulta() throws Exception {
            b.prestar("L01", "S01");
            reloj.avanzarDias(9);
            b.devolver("L01");      // 2 dias tarde = $10
        }

        @Test
        @DisplayName("R4: con multa pendiente el prestamo se rechaza")
        void conMultaNoPresta() throws Exception {
            generarMulta();
            assertThrows(SocioConMultaException.class, () -> b.prestar("L02", "S01"));
            assertFalse(b.getEstadoSocios().get(0).puedePedirPrestado());
        }

        @Test
        @DisplayName("R4: un pago parcial no basta; el pago total si lo habilita")
        void pagoParcialYTotal() throws Exception {
            generarMulta();
            assertEquals(4.0, b.pagarMulta("S01", 6.0), 1e-9);
            assertThrows(SocioConMultaException.class, () -> b.prestar("L02", "S01"));
            assertEquals(0.0, b.pagarMulta("S01", 4.0), 1e-9);
            assertDoesNotThrow(() -> b.prestar("L02", "S01"));
        }

        @Test
        @DisplayName("R4: pagos de cero, negativos o mayores a la deuda se rechazan")
        void pagosInvalidos() throws Exception {
            generarMulta();
            assertThrows(DatosInvalidosException.class, () -> b.pagarMulta("S01", 0));
            assertThrows(DatosInvalidosException.class, () -> b.pagarMulta("S01", -5));
            assertThrows(DatosInvalidosException.class, () -> b.pagarMulta("S01", 10.01));
            assertEquals(10.0, b.buscarSocio("S01").getMultaPendiente(), 0.0);
        }
    }

    @Nested
    @DisplayName("Altas y observadores")
    class AltasYObservadores {
        @Test
        @DisplayName("Altas: ISBN repetido, titulo vacio o socio repetido se rechazan")
        void altasInvalidas() {
            assertThrows(DatosInvalidosException.class, () -> b.registrarLibro(new Libro("L01", "Otro", "X")));
            assertThrows(DatosInvalidosException.class, () -> b.registrarLibro(new Libro("L09", " ", "X")));
            assertThrows(DatosInvalidosException.class, () -> b.registrarSocio(new Socio("S01", "Otra")));
            assertThrows(NoEncontradoException.class, () -> b.prestar("L99", "S01"));
            assertThrows(NoEncontradoException.class, () -> b.prestar("L01", "S99"));
        }

        @Test
        @DisplayName("Observadores: solo los cambios exitosos notifican")
        void notificaciones() throws Exception {
            AtomicInteger avisos = new AtomicInteger();
            b.agregarObservador(avisos::incrementAndGet);
            b.prestar("L01", "S01");                                                     // 1
            assertThrows(LibroNoDisponibleException.class, () -> b.prestar("L01", "S02")); // no
            b.devolver("L01");                                                           // 2
            b.fechaActualizada();                                                        // 3
            assertEquals(3, avisos.get());
        }
    }
}
