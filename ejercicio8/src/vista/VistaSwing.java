package vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import modelo.EstadoLibro;
import modelo.EstadoSocio;

/** Vista grafica. Expone lo escrito y los clics; el controlador decide que hacer. */
public class VistaSwing extends JFrame implements VistaBiblioteca {
    private static final long serialVersionUID = 1L;
    public static final Color COLOR_ALERTA = new Color(0xC6, 0x28, 0x28);

    private final JTextField txtIsbn = campo("txtIsbn", 6);
    private final JTextField txtTitulo = campo("txtTitulo", 11);
    private final JTextField txtAutor = campo("txtAutor", 9);
    private final JTextField txtSocioId = campo("txtSocioId", 5);
    private final JTextField txtSocioNombre = campo("txtSocioNombre", 12);
    private final JTextField txtPrestamoIsbn = campo("txtPrestamoIsbn", 6);
    private final JTextField txtPrestamoSocio = campo("txtPrestamoSocio", 5);
    private final JTextField txtPagoSocio = campo("txtPagoSocio", 5);
    private final JTextField txtPagoMonto = campo("txtPagoMonto", 6);

    private final JButton btnRegistrarLibro = boton("btnRegistrarLibro", "Registrar libro");
    private final JButton btnRegistrarSocio = boton("btnRegistrarSocio", "Registrar socio");
    private final JButton btnPrestar = boton("btnPrestar", "Prestar");
    private final JButton btnDevolver = boton("btnDevolver", "Devolver");
    private final JButton btnPagar = boton("btnPagar", "Pagar multa");
    private final JButton btnAvanzarDia = boton("btnAvanzarDia", "Avanzar 1 dia");

    private final DefaultTableModel modeloLibros = modelo("ISBN", "Titulo", "Autor", "Estado", "Socio", "Vence");
    private final DefaultTableModel modeloSocios = modelo("Id", "Nombre", "Prestamos", "Multa", "Puede pedir");
    private final JTable tablaLibros = new JTable(modeloLibros);
    private final JTable tablaSocios = new JTable(modeloSocios);
    private final List<Boolean> librosEnAlerta = new ArrayList<>();
    private final List<Boolean> sociosEnAlerta = new ArrayList<>();
    private final JLabel lblEstado = new JLabel(" ");
    private final JLabel lblFecha = new JLabel(" ");

    public VistaSwing(String titulo) {
        super(titulo);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        tablaLibros.setName("tablaLibros");
        tablaSocios.setName("tablaSocios");
        lblEstado.setName("lblEstado");
        lblFecha.setName("lblFecha");
        tablaLibros.setDefaultRenderer(Object.class, new Resaltador(librosEnAlerta));
        tablaSocios.setDefaultRenderer(Object.class, new Resaltador(sociosEnAlerta));

        JPanel filaAltas = new JPanel(new BorderLayout());
        filaAltas.add(panel("Libro", new JLabel("ISBN"), txtIsbn, new JLabel("Titulo"), txtTitulo,
                new JLabel("Autor"), txtAutor, btnRegistrarLibro), BorderLayout.CENTER);
        filaAltas.add(panel("Socio", new JLabel("Id"), txtSocioId, new JLabel("Nombre"), txtSocioNombre,
                btnRegistrarSocio), BorderLayout.EAST);
        JPanel filaOperaciones = new JPanel(new BorderLayout());
        filaOperaciones.add(panel("Prestamo", new JLabel("ISBN"), txtPrestamoIsbn, new JLabel("Socio"),
                txtPrestamoSocio, btnPrestar, btnDevolver), BorderLayout.CENTER);
        filaOperaciones.add(panel("Multas", new JLabel("Socio"), txtPagoSocio, new JLabel("Monto"), txtPagoMonto,
                btnPagar, btnAvanzarDia), BorderLayout.EAST);
        JPanel formularios = new JPanel(new GridLayout(2, 1));
        formularios.add(filaAltas);
        formularios.add(filaOperaciones);

        JSplitPane tablas = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                envolver("Libros", tablaLibros), envolver("Socios", tablaSocios));
        tablas.setResizeWeight(0.55);

        JPanel sur = new JPanel(new BorderLayout());
        lblEstado.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        lblFecha.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        sur.add(lblEstado, BorderLayout.CENTER);
        sur.add(lblFecha, BorderLayout.EAST);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formularios, BorderLayout.NORTH);
        getContentPane().add(tablas, BorderLayout.CENTER);
        getContentPane().add(sur, BorderLayout.SOUTH);
        setSize(1100, 600);
        setLocationByPlatform(true);
    }

    // ---- construccion ----
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

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) { return false; }
        };
    }

    private static JPanel panel(String titulo, Component... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        for (Component c : componentes) {
            p.add(c);
        }
        return p;
    }

    private static JPanel envolver(String titulo, JTable tabla) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titulo));
        p.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return p;
    }

    /** Pinta en rojo las filas marcadas en alerta (libro vencido, socio con multa). */
    private static final class Resaltador extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final List<Boolean> alertas;

        Resaltador(List<Boolean> alertas) {
            this.alertas = alertas;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco,
                                                       int fila, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foco, fila, col);
            boolean alerta = fila < alertas.size() && alertas.get(fila);
            c.setForeground(alerta ? COLOR_ALERTA : t.getForeground());
            return c;
        }
    }

    // ---- entrada ----
    public String getIsbn()            { return txtIsbn.getText(); }
    public String getTitulo()          { return txtTitulo.getText(); }
    public String getAutor()           { return txtAutor.getText(); }
    public String getSocioId()         { return txtSocioId.getText(); }
    public String getSocioNombre()     { return txtSocioNombre.getText(); }
    public String getPrestamoIsbn()    { return txtPrestamoIsbn.getText(); }
    public String getPrestamoSocio()   { return txtPrestamoSocio.getText(); }
    public String getPagoSocio()       { return txtPagoSocio.getText(); }
    public String getPagoMonto()       { return txtPagoMonto.getText(); }

    public void limpiarLibro()  { txtIsbn.setText(""); txtTitulo.setText(""); txtAutor.setText(""); }
    public void limpiarSocio()  { txtSocioId.setText(""); txtSocioNombre.setText(""); }

    public void alRegistrarLibro(ActionListener l)  { btnRegistrarLibro.addActionListener(l); }
    public void alRegistrarSocio(ActionListener l)  { btnRegistrarSocio.addActionListener(l); }
    public void alPrestar(ActionListener l)         { btnPrestar.addActionListener(l); }
    public void alDevolver(ActionListener l)        { btnDevolver.addActionListener(l); }
    public void alPagar(ActionListener l)           { btnPagar.addActionListener(l); }
    public void alAvanzarDia(ActionListener l)      { btnAvanzarDia.addActionListener(l); }

    // ---- salida ----
    @Override
    public void mostrarExito(String mensaje) {
        lblEstado.setForeground(new Color(0, 120, 0));
        lblEstado.setText(mensaje);
    }

    @Override
    public void mostrarError(String mensaje) {
        lblEstado.setForeground(COLOR_ALERTA);
        lblEstado.setText(mensaje);
    }

    @Override
    public void mostrarFecha(LocalDate hoy) {
        lblFecha.setText("Hoy: " + Formato.fecha(hoy));
    }

    @Override
    public void mostrarLibros(List<EstadoLibro> libros) {
        modeloLibros.setRowCount(0);
        librosEnAlerta.clear();
        for (EstadoLibro e : libros) {
            librosEnAlerta.add(e.isVencido());
            modeloLibros.addRow(new Object[] {
                e.getLibro().getIsbn(), e.getLibro().getTitulo(), e.getLibro().getAutor(),
                e.isDisponible() ? "Disponible" : (e.isVencido() ? "Vencido" : "Prestado"),
                e.getNombreSocio() == null ? "" : e.getNombreSocio(), Formato.fecha(e.getFechaLimite())
            });
        }
    }

    @Override
    public void mostrarSocios(List<EstadoSocio> socios) {
        modeloSocios.setRowCount(0);
        sociosEnAlerta.clear();
        for (EstadoSocio e : socios) {
            sociosEnAlerta.add(e.getSocio().tieneMulta());
            modeloSocios.addRow(new Object[] {
                e.getSocio().getId(), e.getSocio().getNombre(), e.getPrestamosActivos(),
                Formato.dinero(e.getSocio().getMultaPendiente()), e.puedePedirPrestado() ? "Si" : "No"
            });
        }
    }
}
