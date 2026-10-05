package modelo;

import java.time.LocalDate;

/** Prestamo de un libro a un socio. Esta activo mientras no tenga fecha de devolucion. */
public class Prestamo {
    private final String isbn;
    private final String socioId;
    private final LocalDate fechaPrestamo;
    private final LocalDate fechaLimite;
    private volatile LocalDate fechaDevolucion;

    public Prestamo(String isbn, String socioId, LocalDate fechaPrestamo, LocalDate fechaLimite,
                    LocalDate fechaDevolucion) {
        this.isbn = isbn;
        this.socioId = socioId;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaLimite = fechaLimite;
        this.fechaDevolucion = fechaDevolucion;
    }

    public String getIsbn()                { return isbn; }
    public String getSocioId()             { return socioId; }
    public LocalDate getFechaPrestamo()    { return fechaPrestamo; }
    public LocalDate getFechaLimite()      { return fechaLimite; }
    public LocalDate getFechaDevolucion()  { return fechaDevolucion; }
    public boolean estaActivo()            { return fechaDevolucion == null; }

    /** Un prestamo activo esta vencido si hoy ya paso la fecha limite. */
    public boolean estaVencido(LocalDate hoy) {
        return estaActivo() && hoy.isAfter(fechaLimite);
    }

    void registrarDevolucion(LocalDate fecha) { this.fechaDevolucion = fecha; }
}
