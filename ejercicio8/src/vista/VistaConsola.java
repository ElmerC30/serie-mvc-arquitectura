package vista;

import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import modelo.EstadoLibro;
import modelo.EstadoSocio;

/** Vista de texto. Muestra y captura; no interpreta ni decide nada. */
public class VistaConsola implements VistaBiblioteca {
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
        out.println();
        out.println("1) Registrar libro  2) Registrar socio  3) Prestar  4) Devolver  5) Pagar multa");
        out.println("6) Ver libros  7) Ver socios  8) Avanzar dias  0) Salir");
    }

    /** Devuelve la siguiente linea o null si ya no hay entrada. */
    public String pedir(String etiqueta) {
        out.print(etiqueta + ": ");
        out.flush();
        return sc.hasNextLine() ? sc.nextLine() : null;
    }

    public void mostrarAvisoCambio() {
        out.println();
        out.println("[Aviso] La biblioteca cambio (desde esta u otra vista):");
    }

    @Override public void mostrarExito(String mensaje) { out.println("OK: " + mensaje); }
    @Override public void mostrarError(String mensaje) { out.println("ERROR: " + mensaje); }
    @Override public void mostrarFecha(LocalDate hoy)  { out.println("Hoy es " + Formato.fecha(hoy)); }

    @Override
    public void mostrarLibros(List<EstadoLibro> libros) {
        out.printf("%-6s %-28s %-20s %-10s %-16s %s%n", "ISBN", "Titulo", "Autor", "Estado", "Socio", "Vence");
        for (EstadoLibro e : libros) {
            String estado = e.isDisponible() ? "Disponible" : (e.isVencido() ? "VENCIDO" : "Prestado");
            out.printf("%-6s %-28s %-20s %-10s %-16s %s%n", e.getLibro().getIsbn(),
                    recortar(e.getLibro().getTitulo(), 28), recortar(e.getLibro().getAutor(), 20), estado,
                    e.getNombreSocio() == null ? "-" : recortar(e.getNombreSocio(), 16),
                    Formato.fecha(e.getFechaLimite()));
        }
    }

    @Override
    public void mostrarSocios(List<EstadoSocio> socios) {
        out.printf("%-6s %-20s %-9s %-12s %s%n", "Id", "Nombre", "Prestamos", "Multa", "Puede pedir");
        for (EstadoSocio e : socios) {
            out.printf("%-6s %-20s %9d %-12s %s%n", e.getSocio().getId(), recortar(e.getSocio().getNombre(), 20),
                    e.getPrestamosActivos(), Formato.dinero(e.getSocio().getMultaPendiente()),
                    e.puedePedirPrestado() ? "si" : "no");
        }
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
