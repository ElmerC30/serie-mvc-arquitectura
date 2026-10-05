package controlador;

import vista.VistaConsola;

/**
 * Coordinacion propia de la consola: el ciclo del menu y el orden de las preguntas.
 * Interpreta lo que captura la vista y delega los casos de uso en ControladorInventario.
 */
public class ControladorConsola {
    private final ControladorInventario casos;
    private final VistaConsola vista;

    public ControladorConsola(ControladorInventario casos, VistaConsola vista) {
        this.casos = casos;
        this.vista = vista;
    }

    /** Ciclo principal del menu. */
    public void iniciar() {
        int op;
        do {
            vista.mostrarMenu();
            op = Integer.parseInt(vista.pedirOpcion());
            switch (op) {
                case 1: agregar(); break;
                case 2: vender(); break;
                case 3: casos.listar(); break;
                case 4: casos.valorTotal(); break;
                default: break;
            }
        } while (op != 0);
    }

    private void agregar() {
        String nombre = vista.pedirNombre();
        int cantidad = Integer.parseInt(vista.pedirCantidad());
        double precio = Double.parseDouble(vista.pedirPrecio());
        casos.agregar(nombre, cantidad, precio);
    }

    private void vender() {
        String nombre = vista.pedirProducto();
        if (!casos.existe(nombre)) {
            vista.mostrarNoExiste();
            return;
        }
        int cantidad = Integer.parseInt(vista.pedirCantidadAVender());
        casos.vender(nombre, cantidad);
    }
}
