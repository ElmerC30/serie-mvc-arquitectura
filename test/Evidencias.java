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
        System.exit(0);
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
