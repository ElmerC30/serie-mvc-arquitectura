package modelo;

/** Situacion de un socio calculada por el modelo: prestamos activos, multa y si puede pedir. */
public final class EstadoSocio {
    private final Socio socio;
    private final int prestamosActivos;

    EstadoSocio(Socio socio, int prestamosActivos) {
        this.socio = socio;
        this.prestamosActivos = prestamosActivos;
    }

    public Socio getSocio()             { return socio; }
    public int getPrestamosActivos()    { return prestamosActivos; }

    /** Aplica R1 y R4: sin multa y con menos del maximo de prestamos activos. */
    public boolean puedePedirPrestado() {
        return !socio.tieneMulta() && prestamosActivos < Biblioteca.MAX_PRESTAMOS_ACTIVOS;
    }
}
