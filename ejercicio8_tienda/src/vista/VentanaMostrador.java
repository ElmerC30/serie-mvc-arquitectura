package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import modelo.Articulo;
import modelo.RenglonTicket;
import modelo.Ticket;

/**
 * Vista 2: ventana del mostrador (punto de venta). Arma el pedido del cliente, muestra la
 * cotización que calcula el modelo y cobra. El catálogo muestra el stock en vivo.
 */
public class VentanaMostrador extends JFrame implements VistaMostrador {
    private static final Color TEXTO_ROJO = new Color(170, 0, 0);
    private static final Color TEXTO_VERDE = new Color(0, 110, 40);

    private final DefaultComboBoxModel<Articulo> catalogo = new DefaultComboBoxModel<>();
    private final JComboBox<Articulo> comboArticulo = new JComboBox<>(catalogo);
    private final JTextField campoCantidad = new JTextField("1", 4);
    private final JButton botonAgregar = new JButton("Agregar al pedido");
    private final JButton botonCobrar = new JButton("Cobrar");
    private final JButton botonCancelar = new JButton("Cancelar pedido");
    private final ModeloPedido modeloPedido = new ModeloPedido();
    private final JLabel subtotal = new JLabel();
    private final JLabel descuento = new JLabel();
    private final JLabel total = new JLabel();
    private final JLabel estado = new JLabel(" ");

    public VentanaMostrador() {
        super("Mostrador - Tienda de instrumentos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        comboArticulo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof Articulo) {
                    Articulo a = (Articulo) v;
                    setText(a.getNombre() + "  -  " + Formato.dinero(a.getPrecio()) + "  (stock: " + a.getStock() + ")");
                }
                return this;
            }
        });

        JPanel captura = new JPanel(new FlowLayout(FlowLayout.LEFT));
        captura.setBorder(BorderFactory.createTitledBorder("Pedido del cliente"));
        captura.add(comboArticulo);
        captura.add(new JLabel("Cant.:"));
        captura.add(campoCantidad);
        captura.add(botonAgregar);

        JTable tabla = new JTable(modeloPedido);
        tabla.setRowHeight(22);
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c = 1; c < modeloPedido.getColumnCount(); c++) {
            tabla.getColumnModel().getColumn(c).setCellRenderer(derecha);
        }
        tabla.getColumnModel().getColumn(0).setPreferredWidth(300);

        JPanel totales = new JPanel(new GridLayout(3, 1));
        for (JLabel l : new JLabel[] {subtotal, descuento, total}) {
            l.setHorizontalAlignment(SwingConstants.RIGHT);
            totales.add(l);
        }
        total.setFont(total.getFont().deriveFont(Font.BOLD, 14f));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(botonCancelar);
        botones.add(botonCobrar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        pie.add(totales, BorderLayout.NORTH);
        pie.add(estado, BorderLayout.CENTER);
        pie.add(botones, BorderLayout.SOUTH);

        add(captura, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        setSize(860, 420);
        setLocation(40, 440);
        mostrarTotales(Ticket.vacio());
    }

    @Override
    public void mostrarCatalogo(List<Articulo> articulos) {
        enPantalla(() -> {
            String seleccionado = getCodigoSeleccionado();
            catalogo.removeAllElements();
            for (Articulo a : articulos) {
                catalogo.addElement(a);
                if (a.getCodigo().equals(seleccionado)) {
                    catalogo.setSelectedItem(a);
                }
            }
        });
    }

    @Override
    public void mostrarPedido(Ticket cotizacion) {
        enPantalla(() -> {
            modeloPedido.setRenglones(cotizacion.getRenglones());
            mostrarTotales(cotizacion);
        });
    }

    @Override
    public void mostrarVentaRealizada(Ticket ticket) {
        mostrarMensaje("Venta cobrada: " + ticket.getRenglones().size() + " renglón(es), total "
                + Formato.dinero(ticket.getTotal()) + ", ahorro por kit " + Formato.dinero(ticket.getDescuentoTotal()));
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
    public String getCodigoSeleccionado() {
        Object a = comboArticulo.getSelectedItem();
        return a instanceof Articulo ? ((Articulo) a).getCodigo() : "";
    }

    /** Fija la captura de una línea del pedido (lo usa el script de evidencias). */
    public void capturar(String codigo, String cantidad) {
        for (int i = 0; i < catalogo.getSize(); i++) {
            if (catalogo.getElementAt(i).getCodigo().equals(codigo)) {
                comboArticulo.setSelectedIndex(i);
            }
        }
        campoCantidad.setText(cantidad);
    }

    @Override public String getCantidad()     { return campoCantidad.getText(); }
    @Override public void limpiarCantidad()   { enPantalla(() -> campoCantidad.setText("1")); }

    @Override public void alAgregar(Runnable accion)  { botonAgregar.addActionListener(e -> accion.run()); }
    @Override public void alCobrar(Runnable accion)   { botonCobrar.addActionListener(e -> accion.run()); }
    @Override public void alCancelar(Runnable accion) { botonCancelar.addActionListener(e -> accion.run()); }
    @Override public void abrir()                     { enPantalla(() -> setVisible(true)); }

    private void mostrarTotales(Ticket t) {
        subtotal.setText("Subtotal: " + Formato.dinero(t.getSubtotal()));
        descuento.setText("Descuento kit de inicio: -" + Formato.dinero(t.getDescuentoTotal()));
        total.setText("Total: " + Formato.dinero(t.getTotal()));
    }

    private static void enPantalla(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private static final class ModeloPedido extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Artículo", "Cant.", "P. unitario", "Desc. kit", "Importe"};
        private List<RenglonTicket> renglones = new ArrayList<>();

        void setRenglones(List<RenglonTicket> nuevos) {
            renglones = new ArrayList<>(nuevos);
            fireTableDataChanged();
        }

        @Override public int getRowCount()           { return renglones.size(); }
        @Override public int getColumnCount()        { return COLUMNAS.length; }
        @Override public String getColumnName(int c) { return COLUMNAS[c]; }

        @Override
        public Object getValueAt(int fila, int col) {
            RenglonTicket r = renglones.get(fila);
            switch (col) {
                case 0: return r.nombre();
                case 1: return r.cantidad();
                case 2: return Formato.dinero(r.precioUnitario());
                case 3: return r.unidadesConDescuento() == 0 ? "-"
                        : "-" + Formato.dinero(r.descuento()) + " (" + r.unidadesConDescuento() + " u.)";
                default: return Formato.dinero(r.importe());
            }
        }
    }
}
