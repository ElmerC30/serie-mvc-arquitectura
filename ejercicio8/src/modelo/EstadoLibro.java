package modelo;

import java.time.LocalDate;

/**
 * Situacion de un libro calculada por el modelo: si esta prestado, a quien,
 * hasta cuando y si ya esta vencido. Las vistas solo deciden como mostrarlo.
 */
public final class EstadoLibro {
    private final Libro libro;
    private final Prestamo prestamoActivo;
    private final String nombreSocio;
    private final boolean vencido;

    EstadoLibro(Libro libro, Prestamo prestamoActivo, String nombreSocio, boolean vencido) {
        this.libro = libro;
        this.prestamoActivo = prestamoActivo;
        this.nombreSocio = nombreSocio;
        this.vencido = vencido;
    }

    public Libro getLibro()            { return libro; }
    public boolean isDisponible()      { return prestamoActivo == null; }
    public String getNombreSocio()     { return nombreSocio; }
    public boolean isVencido()         { return vencido; }

    public LocalDate getFechaLimite() {
        return prestamoActivo == null ? null : prestamoActivo.getFechaLimite();
    }
}
