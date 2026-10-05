package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import modelo.Producto;

/**
 * Vista grafica Swing. Igual que la de consola, no toma decisiones:
 * expone lo que el usuario escribio y avisa cuando presiona un boton;
 * el controlador decide que hacer.
 */
public class VistaSwing extends JFrame implements VistaInventario {
    private final JTextField txtNombre = campo("txtNombre", 10);
    private final JTextField txtCantidad = campo("txtCantidad", 5);
    private final JTextField txtPrecio = campo("txtPrecio", 7);
    private final JTextField txtProducto = campo("txtProducto", 10);
    private final JTextField txtCantidadVenta = campo("txtCantidadVenta", 5);

    private final JButton btnAgregar = boton("btnAgregar", "Agregar");
    private final JButton btnVender = boton("btnVender", "Vender");
    private final JButton btnValorTotal = boton("btnValorTotal", "Valor total");
    private final JButton btnExportar = boton("btnExportar", "Exportar CSV");

    private final DefaultTableModel modeloTabla =
            new DefaultTableModel(new Object[] {"Nombre", "Cantidad", "Precio", "Valor"}, 0) {
                @Override
                public boolean isCellEditable(int fila, int columna) { return false; }
            };
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblValorTotal = new JLabel("Valor del inventario: -");

    public VistaSwing() {
        this("Inventario - Swing");
    }

    public VistaSwing(String titulo) {
        super(titulo);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        tabla.setName("tabla");
        lblEstado.setName("lblEstado");
        lblValorTotal.setName("lblValorTotal");

        JPanel alta = new JPanel(new FlowLayout(FlowLayout.LEFT));
        alta.setBorder(BorderFactory.createTitledBorder("Agregar producto"));
        alta.add(new JLabel("Nombre")); alta.add(txtNombre);
        alta.add(new JLabel("Cantidad")); alta.add(txtCantidad);
        alta.add(new JLabel("Precio")); alta.add(txtPrecio);
        alta.add(btnAgregar);

        JPanel venta = new JPanel(new FlowLayout(FlowLayout.LEFT));
        venta.setBorder(BorderFactory.createTitledBorder("Vender"));
        venta.add(new JLabel("Producto")); venta.add(txtProducto);
        venta.add(new JLabel("Cantidad a vender")); venta.add(txtCantidadVenta);
        venta.add(btnVender);

        JPanel formularios = new JPanel(new GridLayout(2, 1));
        formularios.add(alta);
        formularios.add(venta);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acciones.add(btnValorTotal);
        acciones.add(btnExportar);
        acciones.add(lblValorTotal);

        JPanel sur = new JPanel(new GridLayout(2, 1));
        sur.add(acciones);
        lblEstado.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        sur.add(lblEstado);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formularios, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(tabla), BorderLayout.CENTER);
        getContentPane().add(sur, BorderLayout.SOUTH);
        setSize(640, 420);
        setLocationByPlatform(true);
    }

    private static JTextField campo(String nombre, int columnas) {
        JTextField t = new JTextField(columnas);
        t.setName(nombre);
        return t;
    }

    private static JButton boton(String nombre, String texto) {
        JButton b = new JButton(texto);
        b.setName(nombre);
        return b;
    }

    // ---- Entrada: la vista solo entrega lo escrito, sin interpretarlo ----
    public String getNombre()          { return txtNombre.getText(); }
    public String getCantidad()        { return txtCantidad.getText(); }
    public String getPrecio()          { return txtPrecio.getText(); }
    public String getProducto()        { return txtProducto.getText(); }
    public String getCantidadVenta()   { return txtCantidadVenta.getText(); }

    public void limpiarAlta() {
        txtNombre.setText(""); txtCantidad.setText(""); txtPrecio.setText("");
    }

    /** Pide al usuario un archivo de destino; devuelve null si cancela. */
    public Path pedirArchivoDestino() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("inventario.csv"));
        return fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile().toPath() : null;
    }

    public void alAgregar(ActionListener l)     { btnAgregar.addActionListener(l); }
    public void alVender(ActionListener l)      { btnVender.addActionListener(l); }
    public void alValorTotal(ActionListener l)  { btnValorTotal.addActionListener(l); }
    public void alExportar(ActionListener l)    { btnExportar.addActionListener(l); }

    // ---- Salida ----
    private void estado(String texto, Color color) {
        lblEstado.setForeground(color);
        lblEstado.setText(texto);
    }

    @Override public void mostrarMensaje(String mensaje)  { estado(mensaje, Color.DARK_GRAY); }
    @Override public void mostrarDatosInvalidos()         { estado("Datos invalidos", Color.RED.darker()); }
    @Override public void mostrarAgregado()               { estado("Agregado.", new Color(0, 120, 0)); }
    @Override public void mostrarNoExiste()               { estado("No existe", Color.RED.darker()); }
    @Override public void mostrarStockInsuficiente()      { estado("Stock insuficiente", Color.RED.darker()); }
    @Override public void mostrarVentaOk(int quedan)      { estado("Venta ok. Quedan " + quedan, new Color(0, 120, 0)); }

    @Override
    public void mostrarListado(List<Producto> productos) {
        modeloTabla.setRowCount(0);
        for (Producto p : productos) {
            modeloTabla.addRow(new Object[] {
                p.getNombre(), p.getCantidad(),
                String.format("$%,.2f", p.getPrecio()), String.format("$%,.2f", p.getValor())
            });
        }
    }

    @Override
    public void mostrarValorTotal(double total) {
        lblValorTotal.setText(String.format("Valor del inventario: $%,.2f", total));
    }
}
