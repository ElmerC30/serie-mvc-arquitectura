import controlador.ControladorInventario;
import modelo.Inventario;
import vista.VistaConsola;

/** Ensambla modelo, vista y controlador y arranca la aplicacion. */
public class App {
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        VistaConsola vista = new VistaConsola();
        ControladorInventario controlador = new ControladorInventario(inventario, vista);
        controlador.iniciar();
    }
}
