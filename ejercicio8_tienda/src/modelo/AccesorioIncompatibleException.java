package modelo;

import java.util.Set;

/** Regla 1: el pedido mezcla un accesorio con saxofones de otra familia. */
public class AccesorioIncompatibleException extends ReglaNegocioException {
    public AccesorioIncompatibleException(Articulo accesorio, Set<Familia> familiasDelPedido) {
        super(accesorio.getNombre() + " es para saxofón " + accesorio.getFamilia().name().toLowerCase()
                + " y el pedido sólo incluye saxofón " + familiasDelPedido.toString().toLowerCase());
    }
}
