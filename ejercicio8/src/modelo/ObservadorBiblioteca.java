package modelo;

/** Recibe un aviso cada vez que el estado de la biblioteca cambia. El modelo no sabe quien escucha. */
@FunctionalInterface
public interface ObservadorBiblioteca {
    void bibliotecaCambiada();
}
