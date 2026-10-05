package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.Articulo;

/**
 * Vista 1: ventana del almacén. Muestra el inventario completo, pinta de rojo lo que el modelo
 * marca como "requiere reabasto" y permite registrar la entrada de mercancía.
 */
public class VentanaAlmacen extends JFrame implements VistaAlmacen {
    private static final Color ROJO = new Color(255, 205, 205);
    private static final Color TEXTO_ROJO = new Color(170, 0, 0);
    private static final Color TEXTO_VERDE = new Color(0, 110, 40);

    private final ModeloTabla modeloTabla = new ModeloTabla();
    private final JTable tabla = new JTable(modeloTabla);
    private final JLabel alertas = new JLabel(" ");
    private final JLabel estado = new JLabel(" ");
    private final DefaultComboBoxModel<String> codigos = new DefaultComboBoxModel<>();
    private final JComboBox<String> comboCodigo = new JComboBox<>(codigos);
    private final JTextField campoCantidad = new JTextField(5);
    private final JButton botonReabastecer = new JButton("Registrar entrada");

    public VentanaAlmacen() {
        super("Almacén - Tienda de instrumentos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabla.setRowHeight(22);
        tabla.setDefaultRenderer(Object.class, new Resaltador());
        int[] anchos = {110, 290, 80, 70, 90, 50, 100};
        for (int c = 0; c < anchos.length; c++) {
            tabla.getColumnModel().getColumn(c).setPreferredWidth(anchos[c]);
        }

        JPanel reabasto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reabasto.setBorder(BorderFactory.createTitledBorder("Entrada de mercancía"));
        reabasto.add(new JLabel("Código:"));
        reabasto.add(comboCodigo);
        reabasto.add(new JLabel("Unidades:"));
        reabasto.add(campoCantidad);
        reabasto.add(botonReabastecer);

        alertas.setForeground(TEXTO_ROJO);
        alertas.setFont(alertas.getFont().deriveFont(Font.BOLD));
        JPanel pie = new JPanel(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        pie.add(alertas, BorderLayout.NORTH);
        pie.add(estado, BorderLayout.SOUTH);

        add(reabasto, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        setSize(860, 360);
        setLocation(40, 60);
    }

    @Override
    public void mostrarInventario(List<Articulo> articulos) {
        enPantalla(() -> {
            Object seleccionado = codigos.getSelectedItem();
            modeloTabla.setArticulos(articulos);
            codigos.removeAllElements();
            articulos.forEach(a -> codigos.addElement(a.getCodigo()));
            if (seleccionado != null) {
                codigos.setSelectedItem(seleccionado);
            }
        });
    }

    @Override
    public void mostrarAlertas(List<Articulo> lista) {
        enPantalla(() -> alertas.setText(lista.isEmpty() ? "Sin alertas de reabastecimiento"
                : "Reabastecer (" + lista.size() + "): "
                  + lista.stream().map(Articulo::getNombre).collect(Collectors.joining(" | "))));
    }

    @Override
    public void mostrarMensaje(String mensaje) {
        enPantalla(() -> {
            estado.setForeground(TEXTO_VERDE);
            estado.setText(mensaje);
        });
    }

    @Override
    public void mostrarError(String mensaje) {
        enPantalla(() -> {
            estado.setForeground(TEXTO_ROJO);
            estado.setText(mensaje);
        });
    }

    @Override
    public String getCodigoReabasto() {
        Object c = comboCodigo.getSelectedItem();
        return c == null ? "" : c.toString();
    }

    @Override
    public String getCantidadReabasto() {
        return campoCantidad.getText();
    }

    /** Fija la captura de reabasto (lo usa el script de evidencias). */
    public void capturarReabasto(String codigo, String cantidad) {
        comboCodigo.setSelectedItem(codigo);
        campoCantidad.setText(cantidad);
    }

    @Override
    public void limpiarReabasto() {
        enPantalla(() -> campoCantidad.setText(""));
    }

    @Override
    public void alReabastecer(Runnable accion) {
        botonReabastecer.addActionListener(e -> accion.run());
    }

    @Override
    public void abrir() {
        enPantalla(() -> setVisible(true));
    }

    /** Swing sólo se toca desde su hilo; los avisos del modelo pueden llegar desde otro. */
    private static void enPantalla(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    // ---------------------------------------------------------------- tabla

    private static final class ModeloTabla extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Código", "Artículo", "Tipo", "Familia", "Precio", "Stock", "Estado"};
        private List<Articulo> filas = new ArrayList<>();

        void setArticulos(List<Articulo> articulos) {
            filas = new ArrayList<>(articulos);
            fireTableDataChanged();
        }

        Articulo get(int fila) { return filas.get(fila); }

        @Override public int getRowCount()           { return filas.size(); }
        @Override public int getColumnCount()        { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            Articulo a = filas.get(fila);
            switch (col) {
                case 0: return a.getCodigo();
                case 1: return a.getNombre();
                case 2: return Formato.tipo(a.getTipo());
                case 3: return Formato.familia(a.getFamilia());
                case 4: return Formato.dinero(a.getPrecio());
                case 5: return a.getStock();
                default: return a.requiereReabasto() ? "REABASTECER" : "OK";
            }
        }
    }

    /** El modelo decide qué está bajo el mínimo; la vista sólo decide que eso se pinta de rojo. */
    private final class Resaltador extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel,
                                                       boolean foco, int fila, int col) {
            Component c = super.getTableCellRendererComponent(t, valor, sel, foco, fila, col);
            setHorizontalAlignment(col >= 4 && col <= 5 ? RIGHT : LEFT);
            if (!sel) {
                c.setBackground(modeloTabla.get(fila).requiereReabasto() ? ROJO : Color.WHITE);
            }
            return c;
        }
    }
}
