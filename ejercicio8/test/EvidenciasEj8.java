import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import controlador.ControladorBiblioteca;
import controlador.ControladorConsola;
import controlador.ControladorSwing;
import modelo.Biblioteca;
import modelo.Libro;
import modelo.Socio;
import persistencia.RepositorioMemoria;
import soporte.RelojAjustable;
import vista.VistaConsola;
import vista.VistaSwing;

/**
 * Evidencia del proyecto integrador: la consola presta libros, la ventana avanza el reloj y
 * devuelve uno con retraso; ambas vistas se capturan / registran.
 */
public class EvidenciasEj8 {
    public static void main(String[] args) throws Exception {
        File carpeta = new File(args.length > 0 ? args[0] : "evidencias");
        carpeta.mkdirs();
        RelojAjustable reloj = new RelojAjustable(Clock.fixed(
                LocalDate.of(2026, 10, 4).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        Biblioteca b = new Biblioteca(new RepositorioMemoria(), reloj);
        b.registrarLibro(new Libro("L01", "Cien anios de soledad", "G. Garcia Marquez"));
        b.registrarLibro(new Libro("L02", "Pedro Paramo", "Juan Rulfo"));
        b.registrarLibro(new Libro("L03", "Design Patterns", "Gamma et al."));
        b.registrarLibro(new Libro("L04", "Clean Architecture", "Robert C. Martin"));
        b.registrarSocio(new Socio("S01", "Ana Lopez"));
        b.registrarSocio(new Socio("S02", "Luis Perez"));

        VistaSwing[] v = new VistaSwing[1];
        SwingUtilities.invokeAndWait(() -> {
            v[0] = new VistaSwing("Biblioteca - Prestamos");
            new ControladorSwing(b, v[0], reloj).iniciar();
            v[0].setVisible(false);
        });

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        String guion = "3\nL01\nS01\n3\nL03\nS01\n3\nL02\nS02\n0\n";
        VistaConsola consola = new VistaConsola(new ByteArrayInputStream(guion.getBytes(StandardCharsets.UTF_8)),
                                                new PrintStream(salida, true, "UTF-8"));
        ControladorConsola cc = new ControladorConsola(new ControladorBiblioteca(b, consola, reloj), consola);
        cc.activarAvisos();
        cc.iniciar();
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> capturar(v[0], new File(carpeta, "ej8_1_ventana_tras_prestamos_en_consola.png")));

        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < 9; i++) {
                presionar(v[0], "btnAvanzarDia");
            }
            capturar(v[0], new File(carpeta, "ej8_2_ventana_9_dias_despues_vencidos.png"));
            escribir(v[0], "txtPrestamoIsbn", "L01");
            presionar(v[0], "btnDevolver");
            escribir(v[0], "txtPrestamoIsbn", "L02");
            escribir(v[0], "txtPrestamoSocio", "S01");
            presionar(v[0], "btnPrestar");
            capturar(v[0], new File(carpeta, "ej8_3_ventana_multa_y_prestamo_rechazado.png"));
            v[0].dispose();
        });
        Files.writeString(new File(carpeta, "ej8_consola.txt").toPath(), salida.toString(StandardCharsets.UTF_8));
        System.out.println("Consola: " + new File(carpeta, "ej8_consola.txt"));
        System.exit(0);
    }

    static void escribir(JFrame v, String campo, String texto) {
        buscar(v.getContentPane(), campo, JTextField.class).setText(texto);
    }

    static void presionar(JFrame v, String boton) {
        buscar(v.getContentPane(), boton, JButton.class).doClick();
    }

    static <T extends Component> T buscar(Container raiz, String nombre, Class<T> tipo) {
        for (Component c : raiz.getComponents()) {
            if (nombre.equals(c.getName()) && tipo.isInstance(c)) {
                return tipo.cast(c);
            }
            if (c instanceof Container) {
                T t = buscar((Container) c, nombre, tipo);
                if (t != null) {
                    return t;
                }
            }
        }
        return null;
    }

    static void capturar(JFrame frame, File destino) {
        try {
            frame.setSize(1100, 600);
            frame.validate();
            Container contenido = frame.getContentPane();
            int barra = 26;
            BufferedImage img = new BufferedImage(contenido.getWidth(), contenido.getHeight() + barra,
                                                  BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(0x2B, 0x57, 0x9A));
            g.fillRect(0, 0, img.getWidth(), barra);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 13f));
            g.drawString(frame.getTitle(), 10, 18);
            g.translate(0, barra);
            contenido.printAll(g);
            g.dispose();
            ImageIO.write(img, "png", destino);
            System.out.println("Captura: " + destino);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
