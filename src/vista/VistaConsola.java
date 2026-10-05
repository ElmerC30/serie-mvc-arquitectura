package vista;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import modelo.Producto;

/**
 * Vista de consola: muestra menus, mensajes y datos, y captura la entrada del usuario.
 * No toma decisiones: lo que se lee se entrega tal cual al controlador.
 */
public class VistaConsola implements VistaInventario {
    private final Scanner sc;
    private final PrintStream out;

    public VistaConsola() {
        this(System.in, System.out);
    }

    public VistaConsola(InputStream in, PrintStream out) {
        this.sc = new Scanner(in);
        this.out = out;
    }

    public void mostrarMenu() {
        out.println("\n1) Agregar  2) Vender  3) Listar  4) Valor total  0) Salir");
    }

    /** Muestra la etiqueta y devuelve lo que el usuario escribio, sin interpretarlo. */
    public String pedir(String etiqueta) {
        out.print(etiqueta + ": ");
        return sc.nextLine();
    }

    public String pedirOpcion()            { return pedir("Opcion"); }
    public String pedirNombre()            { return pedir("Nombre"); }
    public String pedirCantidad()          { return pedir("Cantidad"); }
    public String pedirPrecio()            { return pedir("Precio"); }
    public String pedirProducto()          { return pedir("Producto"); }
    public String pedirCantidadAVender()   { return pedir("Cantidad a vender"); }

    @Override
    public void mostrarMensaje(String mensaje) {
        out.println(mensaje);
    }

    @Override
    public void mostrarDatosInvalidos()    { mostrarMensaje("Datos invalidos"); }
    @Override
    public void mostrarAgregado()          { mostrarMensaje("Agregado."); }
    @Override
    public void mostrarNoExiste()          { mostrarMensaje("No existe"); }
    @Override
    public void mostrarStockInsuficiente() { mostrarMensaje("Stock insuficiente"); }

    @Override
    public void mostrarVentaOk(int quedan) {
        mostrarMensaje("Venta ok. Quedan " + quedan);
    }

    /** Decision de formato (dos decimales, columnas) que pertenece a la vista. */
    @Override
    public void mostrarListado(List<Producto> productos) {
        for (Producto p : productos) {
            out.printf("%-15s %4d  $%.2f%n", p.getNombre(), p.getCantidad(), p.getPrecio());
        }
    }

    @Override
    public void mostrarValorTotal(double total) {
        out.println("Valor del inventario: $" + total);
    }
}
