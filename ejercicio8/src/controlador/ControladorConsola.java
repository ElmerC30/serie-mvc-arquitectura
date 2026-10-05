package controlador;

import vista.VistaConsola;

/** Coordinacion propia de la consola: el menu y el orden de las preguntas. */
public class ControladorConsola {
    private final ControladorBiblioteca casos;
    private final VistaConsola vista;

    public ControladorConsola(ControladorBiblioteca casos, VistaConsola vista) {
        this.casos = casos;
        this.vista = vista;
    }

    /** Cada cambio del modelo, venga de donde venga, se refleja en la consola. */
    public void activarAvisos() {
        casos.suscribir(() -> {
            vista.mostrarAvisoCambio();
            casos.refrescar();
        });
    }

    public void iniciar() {
        casos.refrescar();
        while (true) {
            vista.mostrarMenu();
            String op = vista.pedir("Opcion");
            if (op == null || op.trim().equals("0")) {
                return;
            }
            switch (op.trim()) {
                case "1": casos.registrarLibro(leer("ISBN"), leer("Titulo"), leer("Autor")); break;
                case "2": casos.registrarSocio(leer("Id"), leer("Nombre")); break;
                case "3": casos.prestar(leer("ISBN"), leer("Id de socio")); break;
                case "4": casos.devolver(leer("ISBN")); break;
                case "5": casos.pagarMulta(leer("Id de socio"), leer("Monto")); break;
                case "6": casos.verLibros(); break;
                case "7": casos.verSocios(); break;
                case "8": casos.avanzarDias(leer("Dias")); break;
                default: vista.mostrarError("Opcion no valida: " + op);
            }
        }
    }

    private String leer(String etiqueta) {
        String s = vista.pedir(etiqueta);
        return s == null ? "" : s;
    }
}
