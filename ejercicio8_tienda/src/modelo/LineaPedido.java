package modelo;

/** Lo que el cliente pide: un código de artículo y cuántas unidades. */
public record LineaPedido(String codigo, int cantidad) {
}
