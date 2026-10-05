package controlador;

import javax.swing.SwingUtilities;

import modelo.Biblioteca;
import soporte.RelojAjustable;
import vista.VistaSwing;

/** Coordinacion propia de la ventana: conecta botones con los casos de uso compartidos. */
public class ControladorSwing {
    private final ControladorBiblioteca casos;
    private final VistaSwing vista;

    public ControladorSwing(Biblioteca biblioteca, VistaSwing vista, RelojAjustable reloj) {
        this.vista = vista;
        this.casos = new ControladorBiblioteca(biblioteca, vista, reloj);
        vista.alRegistrarLibro(e -> {
            casos.registrarLibro(vista.getIsbn(), vista.getTitulo(), vista.getAutor());
            vista.limpiarLibro();
        });
        vista.alRegistrarSocio(e -> {
            casos.registrarSocio(vista.getSocioId(), vista.getSocioNombre());
            vista.limpiarSocio();
        });
        vista.alPrestar(e -> casos.prestar(vista.getPrestamoIsbn(), vista.getPrestamoSocio()));
        vista.alDevolver(e -> casos.devolver(vista.getPrestamoIsbn()));
        vista.alPagar(e -> casos.pagarMulta(vista.getPagoSocio(), vista.getPagoMonto()));
        vista.alAvanzarDia(e -> casos.avanzarDias("1"));
        casos.suscribir(() -> {
            if (SwingUtilities.isEventDispatchThread()) {
                casos.refrescar();
            } else {
                SwingUtilities.invokeLater(casos::refrescar);
            }
        });
    }

    public void iniciar() {
        casos.refrescar();
        vista.setVisible(true);
    }
}
