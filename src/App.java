import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import controlador.ControladorConsola;
import controlador.ControladorInventario;
import controlador.ControladorSwing;
import modelo.Inventario;
import vista.VistaCSV;
import vista.VistaConsola;
import vista.VistaSwing;

/**
 * Ensambla modelo, vista y controlador y arranca la aplicacion.
 * Uso:
 *   java App                  vista de consola (comportamiento original)
 *   java App --gui            vista grafica Swing
 *   java App --csv archivo    consola y, al salir, exporta el inventario con la vista CSV
 *   java App --dual           consola y ventana Swing al mismo tiempo sobre el mismo modelo
 *   java App --dos-ventanas   dos ventanas Swing al mismo tiempo sobre el mismo modelo
 */
public class App {
    public static void main(String[] args) {
        Inventario inventario = new Inventario();
        String modo = args.length > 0 ? args[0] : "";

        if (modo.equals("--gui")) {
            SwingUtilities.invokeLater(() -> new ControladorSwing(inventario, new VistaSwing()).iniciar());
            return;
        }
        if (modo.equals("--dos-ventanas")) {
            SwingUtilities.invokeLater(() -> {
                new ControladorSwing(inventario, new VistaSwing("Inventario - Ventana A")).iniciar();
                new ControladorSwing(inventario, new VistaSwing("Inventario - Ventana B")).iniciar();
            });
            return;
        }

        VistaConsola vista = new VistaConsola();
        ControladorConsola consola = new ControladorConsola(new ControladorInventario(inventario, vista), vista);

        if (modo.equals("--dual")) {
            SwingUtilities.invokeLater(() -> new ControladorSwing(inventario, new VistaSwing()).iniciar());
            consola.activarAvisos();
        }

        consola.iniciar();

        if (modo.equals("--csv") && args.length > 1) {
            new ControladorInventario(inventario, new VistaCSV(Paths.get(args[1]))).listar();
        }
        if (modo.equals("--dual")) {
            System.exit(0);
        }
    }
}
