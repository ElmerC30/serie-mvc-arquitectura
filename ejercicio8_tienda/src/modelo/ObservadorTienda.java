package modelo;

/**
 * Quien quiera enterarse de que el inventario cambió implementa esta interfaz.
 * La tienda no sabe quién la escucha ni qué hará con el aviso.
 */
@FunctionalInterface
public interface ObservadorTienda {
    void tiendaCambiada();
}
