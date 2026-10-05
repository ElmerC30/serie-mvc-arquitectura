package controlador;

import modelo.Biblioteca;
import modelo.Libro;
import modelo.ObservadorBiblioteca;
import modelo.Prestamo;
import modelo.ReglaNegocioException;
import modelo.Socio;
import soporte.RelojAjustable;
import vista.Formato;
import vista.VistaBiblioteca;

/**
 * Casos de uso de la biblioteca, comunes a todas las vistas.
 * Aqui vive la coordinacion que en el Ejercicio 3 hubo que duplicar: convertir el texto
 * capturado, invocar al modelo y traducir el resultado o la excepcion a algo que la vista muestre.
 */
public class ControladorBiblioteca {
    private final Biblioteca biblioteca;
    private final VistaBiblioteca vista;
    private final RelojAjustable reloj;

    public ControladorBiblioteca(Biblioteca biblioteca, VistaBiblioteca vista, RelojAjustable reloj) {
        this.biblioteca = biblioteca;
        this.vista = vista;
        this.reloj = reloj;
    }

    public void suscribir(ObservadorBiblioteca observador) {
        biblioteca.agregarObservador(observador);
    }

    public void registrarLibro(String isbn, String titulo, String autor) {
        try {
            biblioteca.registrarLibro(new Libro(isbn.trim(), titulo.trim(), autor.trim()));
            vista.mostrarExito("Libro registrado: " + titulo.trim());
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void registrarSocio(String id, String nombre) {
        try {
            biblioteca.registrarSocio(new Socio(id.trim(), nombre.trim()));
            vista.mostrarExito("Socio registrado: " + nombre.trim());
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void prestar(String isbn, String socioId) {
        try {
            Prestamo p = biblioteca.prestar(isbn.trim(), socioId.trim());
            vista.mostrarExito("Prestado. Debe devolverse a mas tardar el " + Formato.fecha(p.getFechaLimite()));
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void devolver(String isbn) {
        try {
            double multa = biblioteca.devolver(isbn.trim());
            vista.mostrarExito(multa > 0
                    ? "Devuelto con retraso. Multa generada: " + Formato.dinero(multa)
                    : "Devuelto a tiempo.");
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    /** El monto llega como texto: convertirlo y rechazar "abc" es trabajo del controlador. */
    public void pagarMulta(String socioId, String montoTexto) {
        double monto;
        try {
            monto = Double.parseDouble(montoTexto.trim());
        } catch (NumberFormatException e) {
            vista.mostrarError("El monto debe ser un numero: \"" + montoTexto + "\"");
            return;
        }
        try {
            double resta = biblioteca.pagarMulta(socioId.trim(), monto);
            vista.mostrarExito("Pago registrado. Saldo pendiente: " + Formato.dinero(resta));
        } catch (ReglaNegocioException e) {
            vista.mostrarError(e.getMessage());
        }
    }

    public void avanzarDias(String diasTexto) {
        if (reloj == null) {
            vista.mostrarError("La simulacion de fechas no esta disponible");
            return;
        }
        long dias;
        try {
            dias = Long.parseLong(diasTexto.trim());
        } catch (NumberFormatException e) {
            vista.mostrarError("Los dias deben ser un numero entero: \"" + diasTexto + "\"");
            return;
        }
        if (dias <= 0) {
            vista.mostrarError("Los dias a avanzar deben ser positivos");
            return;
        }
        reloj.avanzarDias(dias);
        vista.mostrarExito("Se avanzaron " + dias + " dia(s)");
        biblioteca.fechaActualizada();
    }

    /** Vuelve a pintar todo lo que la vista muestra. */
    public void refrescar() {
        vista.mostrarFecha(biblioteca.hoy());
        vista.mostrarLibros(biblioteca.getEstadoLibros());
        vista.mostrarSocios(biblioteca.getEstadoSocios());
    }

    public void verLibros() {
        vista.mostrarLibros(biblioteca.getEstadoLibros());
    }

    public void verSocios() {
        vista.mostrarSocios(biblioteca.getEstadoSocios());
    }
}
