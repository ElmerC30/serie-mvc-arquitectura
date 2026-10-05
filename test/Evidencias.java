import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import controlador.ControladorSwing;
import modelo.Inventario;
import vista.VistaSwing;

/**
 * Genera capturas PNG de la vista Swing pintandola en memoria (no requiere mostrarla).
 * Uso: java Evidencias <carpeta>
 */
public class Evidencias {

    public static void main(String[] args) throws Exception {
        File carpeta = new File(args.length > 0 ? args[0] : "evidencias");
        carpeta.mkdirs();
        java.util.Set<String> modos = new java.util.HashSet<>(java.util.Arrays.asList(args));
        if (modos.contains("ej3")) {
            ej3(carpeta);
        }
        if (modos.contains("ej4")) {
            dosVistas(carpeta);
        }
        if (modos.contains("ej5")) {
            ej5(carpeta);
        }
        System.exit(0);
    }

    /** Ejercicio 5: productos criticos (decision del modelo) pintados de rojo (decision de la vista). */
    static void ej5(File carpeta) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Inventario inventario = new Inventario();
                VistaSwing vista = new VistaSwing("Inventario - criticos en rojo");
                new ControladorSwing(inventario, vista);
                agregar(vista, "Teclado", "40", "350");
                agregar(vista, "Mouse", "12", "199.99");
                agregar(vista, "Cable HDMI", "6", "89.5");
                agregar(vista, "Monitor", "15", "3500");
                escribir(vista, "txtProducto", "Mouse");
                escribir(vista, "txtCantidadVenta", "5");
                presionar(vista, "btnVender");
                capturar(vista, new File(carpeta, "ej5_criticos_en_rojo.png"));
                vista.dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    static void ej3(File carpeta) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Inventario inventario = new Inventario();
                VistaSwing vista = new VistaSwing();
                new ControladorSwing(inventario, vista);
                agregar(vista, "Teclado", "5", "350");
                agregar(vista, "Mouse", "10", "199.99");
                agregar(vista, "Cable HDMI", "25", "89.5");
                escribir(vista, "txtProducto", "Mouse");
                escribir(vista, "txtCantidadVenta", "3");
                presionar(vista, "btnVender");
                presionar(vista, "btnValorTotal");
                capturar(vista, new File(carpeta, "ej3_swing.png"));
                vista.dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Ejercicio 4: dos ventanas Swing y una consola sobre el mismo Inventario.
     * Se vende desde la ventana B y se agrega desde la consola; se captura el estado de ambas ventanas.
     */
    static void dosVistas(File carpeta) throws Exception {
        Inventario inventario = new Inventario();
        VistaSwing[] v = new VistaSwing[2];
        SwingUtilities.invokeAndWait(() -> {
            v[0] = new VistaSwing("Ventana A");
            v[1] = new VistaSwing("Ventana B");
            new ControladorSwing(inventario, v[0]);
            new ControladorSwing(inventario, v[1]);
            agregar(v[0], "Teclado", "5", "350");
            agregar(v[0], "Mouse", "10", "199.99");
        });
        SwingUtilities.invokeAndWait(() -> {
            try {
                capturar(v[0], new File(carpeta, "ej4_1_A_tras_agregar_en_A.png"));
                capturar(v[1], new File(carpeta, "ej4_1_B_tras_agregar_en_A.png"));
                escribir(v[1], "txtProducto", "Mouse");
                escribir(v[1], "txtCantidadVenta", "4");
                presionar(v[1], "btnVender");
                capturar(v[0], new File(carpeta, "ej4_2_A_tras_vender_en_B.png"));
                capturar(v[1], new File(carpeta, "ej4_2_B_tras_vender_en_B.png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // La consola corre en este hilo con un guion de entrada, como si alguien escribiera.
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        java.io.PrintStream out = new java.io.PrintStream(salida, true, "UTF-8");
        String guion = "3\n1\nMonitor\n2\n3500\n0\n";
        vista.VistaConsola consola = new vista.VistaConsola(
                new java.io.ByteArrayInputStream(guion.getBytes("UTF-8")), out);
        controlador.ControladorConsola cc = new controlador.ControladorConsola(
                new controlador.ControladorInventario(inventario, consola), consola);
        cc.activarAvisos();
        cc.iniciar();
        SwingUtilities.invokeAndWait(() -> { });   // espera a que Swing procese los avisos
        SwingUtilities.invokeAndWait(() -> {
            try {
                capturar(v[0], new File(carpeta, "ej4_3_A_tras_agregar_en_consola.png"));
                capturar(v[1], new File(carpeta, "ej4_3_B_tras_agregar_en_consola.png"));
                v[0].dispose();
                v[1].dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        java.nio.file.Files.writeString(new File(carpeta, "ej4_consola.txt").toPath(), salida.toString("UTF-8"));
        System.out.println("Consola: " + new File(carpeta, "ej4_consola.txt").getPath());
    }

    static void agregar(VistaSwing v, String n, String c, String p) {
        escribir(v, "txtNombre", n);
        escribir(v, "txtCantidad", c);
        escribir(v, "txtPrecio", p);
        presionar(v, "btnAgregar");
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

    /** Pinta la ventana completa (con su titulo simulado) en un PNG. */
    static void capturar(JFrame frame, File destino) throws Exception {
        frame.pack();
        frame.setSize(Math.max(frame.getWidth(), 660), 440);
        frame.validate();
        Container contenido = frame.getContentPane();
        int barra = 26;
        BufferedImage img = new BufferedImage(contenido.getWidth(), contenido.getHeight() + barra,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(0x2B, 0x57, 0x9A));
        g.fillRect(0, 0, img.getWidth(), barra);
        g.setColor(java.awt.Color.WHITE);
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 13f));
        g.drawString(frame.getTitle(), 10, 18);
        g.translate(0, barra);
        contenido.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", destino);
        System.out.println("Captura: " + destino.getPath());
    }
}
