package modelo;

/** Persona que puede pedir libros prestados. Acumula multas por devoluciones tardias. */
public class Socio {
    private final String id;
    private final String nombre;
    private volatile double multaPendiente;

    public Socio(String id, String nombre) {
        this(id, nombre, 0.0);
    }

    public Socio(String id, String nombre, double multaPendiente) {
        this.id = id;
        this.nombre = nombre;
        this.multaPendiente = multaPendiente;
    }

    public String getId()              { return id; }
    public String getNombre()          { return nombre; }
    public double getMultaPendiente()  { return multaPendiente; }
    public boolean tieneMulta()        { return multaPendiente > 0; }

    void setMultaPendiente(double multa) { this.multaPendiente = multa; }
}
