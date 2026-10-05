package modelo;

/** El pedido solicita más unidades de las que hay en almacén. */
public class StockInsuficienteException extends ReglaNegocioException {
    private final int disponible;
    private final int solicitado;

    public StockInsuficienteException(String nombre, int disponible, int solicitado) {
        super("Stock insuficiente de " + nombre + ": hay " + disponible + ", se pidieron " + solicitado);
        this.disponible = disponible;
        this.solicitado = solicitado;
    }

    public int getDisponible() { return disponible; }
    public int getSolicitado() { return solicitado; }
}
