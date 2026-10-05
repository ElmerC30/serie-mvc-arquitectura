import java.nio.file.Paths;
import java.time.Clock;

import javax.swing.SwingUtilities;

import controlador.ControladorBiblioteca;
import controlador.ControladorConsola;
import controlador.ControladorSwing;
import modelo.Biblioteca;
import modelo.Libro;
import modelo.ReglaNegocioException;
import modelo.RepositorioBiblioteca;
import modelo.Socio;
import persistencia.RepositorioArchivo;
import persistencia.RepositorioMemoria;
import soporte.RelojAjustable;
import vista.VistaConsola;
import vista.VistaSwing;

/**
 * Proyecto integrador (Ejercicio 8): sistema de prestamos de biblioteca.
 * Ensambla modelo, vistas, controladores y persistencia.
 *
 * Uso:
 *   java Main              consola y ventana Swing al mismo tiempo (vistas sincronizadas)
 *   java Main --consola    solo consola
 *   java Main --gui        solo ventana
 *
 * Persistencia intercambiable: cambiar UNA linea, la marcada como REPOSITORIO.
 *   new RepositorioMemoria()                       datos en memoria
 *   new RepositorioArchivo(Paths.get("datos"))      datos en archivos CSV dentro de ./datos
 */
public class Main {
    public static void main(String[] args) throws ReglaNegocioException {
        RepositorioBiblioteca repositorio = new RepositorioMemoria(); // REPOSITORIO
        RelojAjustable reloj = new RelojAjustable(Clock.systemDefaultZone());
        Biblioteca biblioteca = new Biblioteca(repositorio, reloj);
        if (biblioteca.getLibros().isEmpty()) {
            cargarEjemplo(biblioteca);
        }

        String modo = args.length > 0 ? args[0] : "--dual";
        if (!modo.equals("--consola")) {
            SwingUtilities.invokeLater(() ->
                    new ControladorSwing(biblioteca, new VistaSwing("Biblioteca - Prestamos"), reloj).iniciar());
        }
        if (modo.equals("--gui")) {
            return;
        }
        VistaConsola vista = new VistaConsola();
        ControladorConsola consola = new ControladorConsola(new ControladorBiblioteca(biblioteca, vista, reloj), vista);
        if (modo.equals("--dual")) {
            consola.activarAvisos();
        }
        consola.iniciar();
        System.exit(0);
    }

    private static void cargarEjemplo(Biblioteca b) throws ReglaNegocioException {
        b.registrarLibro(new Libro("L01", "Cien anios de soledad", "G. Garcia Marquez"));
        b.registrarLibro(new Libro("L02", "Pedro Paramo", "Juan Rulfo"));
        b.registrarLibro(new Libro("L03", "Design Patterns", "Gamma et al."));
        b.registrarLibro(new Libro("L04", "Clean Architecture", "Robert C. Martin"));
        b.registrarSocio(new Socio("S01", "Ana Lopez"));
        b.registrarSocio(new Socio("S02", "Luis Perez"));
    }
}
