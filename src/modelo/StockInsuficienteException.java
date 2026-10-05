package modelo;

/** Se intenta vender mas unidades de las que hay en stock. */
public class StockInsuficienteException extends Exception {
    private final int disponible;
    private final int solicitado;

    public StockInsuficienteException(String producto, int disponible, int solicitado) {
        super("Stock insuficiente de " + producto + ": hay " + disponible + ", se pidieron " + solicitado);
        this.disponible = disponible;
        this.solicitado = solicitado;
    }

    public int getDisponible() { return disponible; }
    public int getSolicitado() { return solicitado; }
}
