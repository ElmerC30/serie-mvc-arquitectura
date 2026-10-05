import java.awt.Component;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import controlador.ControladorAlmacen;
import controlador.ControladorMostrador;
import modelo.Tienda;
import persistencia.ArticuloDAOMemoria;
import vista.VentanaAlmacen;
import vista.VentanaMostrador;

/**
 * Abre las dos ventanas reales, opera los botones como lo haría un usuario y guarda capturas
 * en evidencias/. Uso: java -cp out\app;out\test EvidenciasTienda
 */
public class EvidenciasTienda {
    private static VentanaAlmacen almacen;
    private static VentanaMostrador mostrador;

    public static void main(String[] args) throws Exception {
        Tienda tienda = new Tienda(new ArticuloDAOMemoria());
        Method carga = Class.forName("Main").getDeclaredMethod("cargarInventarioInicial", Tienda.class);
        carga.setAccessible(true);
        carga.invoke(null, tienda);

        SwingUtilities.invokeAndWait(() -> {
            almacen = new VentanaAlmacen();
            mostrador = new VentanaMostrador();
            new ControladorAlmacen(tienda, almacen).iniciar();
            new ControladorMostrador(tienda, mostrador).iniciar();
        });
        esperar();
        capturar("ej8_0_almacen_inicial", almacen);

        // 1. Kit de inicio en el mostrador: saxofón alto + boquilla alto + 12 cañas de alto.
        enUI(() -> {
            mostrador.capturar("SAX-AS400GP", "1");
            clic(mostrador, "Agregar al pedido");
            mostrador.capturar("BOQ-BL105-AL", "1");
            clic(mostrador, "Agregar al pedido");
            mostrador.capturar("CAN-PCO-AL25", "12");
            clic(mostrador, "Agregar al pedido");
        });
        capturar("ej8_1_mostrador_cotizacion_kit", mostrador);

        // 2. Intento de agregar una boquilla de tenor a un pedido con saxofón alto (Regla 1).
        enUI(() -> {
            mostrador.capturar("BOQ-BL105-TN", "1");
            clic(mostrador, "Agregar al pedido");
        });
        capturar("ej8_2_mostrador_rechazo_incompatible", mostrador);

        // 3. Cobro: el almacén se actualiza solo (Observer) y la caña de alto entra en alerta.
        enUI(() -> clic(mostrador, "Cobrar"));
        capturar("ej8_3_mostrador_tras_cobrar", mostrador);
        enUI(() -> {
            mostrador.capturar("CAN-PCO-AL25", "9");
            clic(mostrador, "Agregar al pedido");
            clic(mostrador, "Cobrar");
        });
        capturar("ej8_4_almacen_tras_ventas_en_mostrador", almacen);

        // 4. Reabasto en el almacén: el catálogo del mostrador refleja el nuevo stock.
        enUI(() -> {
            almacen.capturarReabasto("CAN-PCO-TN25", "30");
            clic(almacen, "Registrar entrada");
            mostrador.capturar("CAN-PCO-TN25", "1");
        });
        capturar("ej8_5_almacen_tras_reabasto", almacen);
        capturar("ej8_6_mostrador_ve_stock_reabastecido", mostrador);
        System.out.println("Capturas guardadas en evidencias/");
        System.exit(0);
    }

    private static void enUI(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
        esperar();
    }

    private static void esperar() throws Exception {
        Thread.sleep(400);
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void clic(Container raiz, String texto) {
        for (Component c : raiz.getComponents()) {
            if (c instanceof AbstractButton && texto.equals(((AbstractButton) c).getText())) {
                ((AbstractButton) c).doClick();
                return;
            }
            if (c instanceof Container) {
                clic((Container) c, texto);
            }
        }
    }

    private static void capturar(String nombre, JFrame ventana) throws Exception {
        BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            ventana.toFront();
            javax.swing.JRootPane raiz = ventana.getRootPane();
            img[0] = new BufferedImage(raiz.getWidth(), raiz.getHeight(), BufferedImage.TYPE_INT_RGB);
            raiz.paint(img[0].getGraphics());
        });
        ImageIO.write(img[0], "png", new File("evidencias", nombre + ".png"));
    }
}
