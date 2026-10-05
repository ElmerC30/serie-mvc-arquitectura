package controlador;

import java.util.ArrayList;
import java.util.List;

import modelo.Articulo;
import modelo.Ticket;
import vista.VistaAlmacen;
import vista.VistaMostrador;

/** Vistas sin interfaz gráfica: registran lo que el controlador les pide mostrar. */
final class VistasFalsas {
    private VistasFalsas() {
    }

    static final class Almacen implements VistaAlmacen {
        List<Articulo> inventario = List.of();
        List<Articulo> alertas = List.of();
        String ultimoError;
        String ultimoMensaje;
        int refrescos;
        String codigo = "";
        String cantidad = "";
        Runnable reabastecer;

        @Override public void mostrarInventario(List<Articulo> a) { inventario = a; refrescos++; }
        @Override public void mostrarAlertas(List<Articulo> a)    { alertas = a; }
        @Override public void mostrarMensaje(String m)            { ultimoMensaje = m; }
        @Override public void mostrarError(String m)              { ultimoError = m; }
        @Override public String getCodigoReabasto()               { return codigo; }
        @Override public String getCantidadReabasto()             { return cantidad; }
        @Override public void limpiarReabasto()                   { cantidad = ""; }
        @Override public void alReabastecer(Runnable r)           { reabastecer = r; }
        @Override public void abrir()                             { }
    }

    static final class Mostrador implements VistaMostrador {
        List<Articulo> catalogo = List.of();
        Ticket pedido = Ticket.vacio();
        Ticket vendido;
        String ultimoError;
        String ultimoMensaje;
        String codigo = "";
        String cantidad = "1";
        final List<Runnable> acciones = new ArrayList<>();

        @Override public void mostrarCatalogo(List<Articulo> a)   { catalogo = a; }
        @Override public void mostrarPedido(Ticket t)             { pedido = t; }
        @Override public void mostrarVentaRealizada(Ticket t)     { vendido = t; }
        @Override public void mostrarMensaje(String m)            { ultimoMensaje = m; }
        @Override public void mostrarError(String m)              { ultimoError = m; }
        @Override public String getCodigoSeleccionado()           { return codigo; }
        @Override public String getCantidad()                     { return cantidad; }
        @Override public void limpiarCantidad()                   { cantidad = "1"; }
        @Override public void alAgregar(Runnable r)               { acciones.add(r); }
        @Override public void alCobrar(Runnable r)                { acciones.add(r); }
        @Override public void alCancelar(Runnable r)              { acciones.add(r); }
        @Override public void abrir()                             { }

        void capturar(String codigo, String cantidad) {
            this.codigo = codigo;
            this.cantidad = cantidad;
        }

        int stockEnCatalogo(String codigo) {
            return catalogo.stream().filter(a -> a.getCodigo().equals(codigo)).findFirst().orElseThrow().getStock();
        }
    }
}
