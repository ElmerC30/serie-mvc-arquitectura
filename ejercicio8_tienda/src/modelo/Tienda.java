package modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Núcleo del sistema de la tienda de instrumentos: estado del inventario y reglas de negocio.
 *
 * Regla 1 (compatibilidad): si un pedido incluye saxofones, cada boquilla y cada caña del pedido
 *         debe ser de la familia de alguno de esos saxofones.
 * Regla 2 (reabastecimiento): un artículo con stock menor al mínimo de su tipo genera una alerta;
 *         las cañas, por ser consumibles, tienen el umbral más alto.
 * Regla 3 (kit de inicio): por cada saxofón vendido, una boquilla y hasta diez cañas de su misma
 *         familia, en el mismo pedido, llevan 15 % de descuento.
 *
 * Además, toda venta es atómica: si una línea viola alguna regla, no se descuenta nada.
 * Esta clase no imprime, no lee teclado y no sabe dónde se guardan los datos.
 */
public class Tienda {
    public static final BigDecimal DESCUENTO_KIT = new BigDecimal("0.15");
    public static final int BOQUILLAS_CON_DESCUENTO_POR_SAXOFON = 1;
    public static final int CANAS_CON_DESCUENTO_POR_SAXOFON = 10;

    private final ArticuloDAO dao;
    private final List<ObservadorTienda> observadores = new CopyOnWriteArrayList<>();

    public Tienda(ArticuloDAO dao) {
        if (dao == null) {
            throw new IllegalArgumentException("La tienda necesita un ArticuloDAO");
        }
        this.dao = dao;
    }

    // ------------------------------------------------------------------ observadores

    public void agregarObservador(ObservadorTienda o) {
        observadores.add(o);
    }

    public void quitarObservador(ObservadorTienda o) {
        observadores.remove(o);
    }

    private void notificar() {
        for (ObservadorTienda o : observadores) {
            o.tiendaCambiada();
        }
    }

    // ------------------------------------------------------------------ altas y reabasto

    public void registrar(Articulo a) throws DatosInvalidosException {
        synchronized (this) {
            if (a == null) {
                throw new DatosInvalidosException("El artículo no puede ser nulo");
            }
            if (vacio(a.getCodigo()) || vacio(a.getNombre())) {
                throw new DatosInvalidosException("El código y el nombre son obligatorios");
            }
            if (a.getTipo() == null || a.getFamilia() == null) {
                throw new DatosInvalidosException("El tipo y la familia son obligatorios");
            }
            if (a.getPrecio() == null || a.getPrecio().signum() <= 0) {
                throw new DatosInvalidosException("El precio debe ser mayor que cero");
            }
            if (a.getStock() < 0) {
                throw new DatosInvalidosException("El stock no puede ser negativo");
            }
            if (dao.buscar(a.getCodigo()).isPresent()) {
                throw new DatosInvalidosException("Ya existe un artículo con el código " + a.getCodigo());
            }
            dao.insertar(a);
        }
        notificar();
    }

    /** Suma unidades que llegaron del proveedor. */
    public int reabastecer(String codigo, int cantidad)
            throws ArticuloNoEncontradoException, DatosInvalidosException {
        int quedan;
        synchronized (this) {
            if (cantidad <= 0) {
                throw new DatosInvalidosException("La cantidad a reabastecer debe ser mayor que cero");
            }
            Articulo a = buscar(codigo);
            a.setStock(a.getStock() + cantidad);
            dao.actualizar(a);
            quedan = a.getStock();
        }
        notificar();
        return quedan;
    }

    // ------------------------------------------------------------------ ventas

    /**
     * Valida el pedido contra todas las reglas y calcula el ticket SIN tocar el inventario.
     * Sirve para mostrar al cliente cuánto pagará antes de cobrar.
     */
    public synchronized Ticket cotizar(List<LineaPedido> pedido) throws ReglaNegocioException {
        if (pedido == null || pedido.isEmpty()) {
            throw new DatosInvalidosException("El pedido está vacío");
        }
        List<Articulo> articulos = new ArrayList<>();
        Map<String, Integer> unidadesPorCodigo = new LinkedHashMap<>();
        for (LineaPedido linea : pedido) {
            if (linea.cantidad() <= 0) {
                throw new DatosInvalidosException("La cantidad debe ser mayor que cero");
            }
            articulos.add(buscar(linea.codigo()));
            unidadesPorCodigo.merge(linea.codigo(), linea.cantidad(), Integer::sum);
        }
        validarStock(unidadesPorCodigo);
        validarCompatibilidad(articulos);
        return calcularTicket(pedido, articulos);
    }

    /** Cobra el pedido: valida, descuenta stock de forma atómica y avisa a los observadores. */
    public Ticket vender(List<LineaPedido> pedido) throws ReglaNegocioException {
        Ticket ticket;
        synchronized (this) {
            ticket = cotizar(pedido);
            for (LineaPedido linea : pedido) {
                Articulo a = buscar(linea.codigo());
                a.setStock(a.getStock() - linea.cantidad());
                dao.actualizar(a);
            }
        }
        notificar();
        return ticket;
    }

    private void validarStock(Map<String, Integer> unidadesPorCodigo) throws ReglaNegocioException {
        for (Map.Entry<String, Integer> e : unidadesPorCodigo.entrySet()) {
            Articulo a = buscar(e.getKey());
            if (e.getValue() > a.getStock()) {
                throw new StockInsuficienteException(a.getNombre(), a.getStock(), e.getValue());
            }
        }
    }

    /** Regla 1. Sin saxofones en el pedido, los accesorios se venden sueltos como refacción. */
    private void validarCompatibilidad(List<Articulo> articulos) throws AccesorioIncompatibleException {
        Set<Familia> familias = familiasDeSaxofones(articulos);
        if (familias.isEmpty()) {
            return;
        }
        for (Articulo a : articulos) {
            if (a.getTipo().esAccesorio() && !familias.contains(a.getFamilia())) {
                throw new AccesorioIncompatibleException(a, familias);
            }
        }
    }

    /** Regla 3. El cupo de unidades con descuento se reparte en el orden de las líneas. */
    private Ticket calcularTicket(List<LineaPedido> pedido, List<Articulo> articulos) {
        Map<Familia, Integer> saxofones = new EnumMap<>(Familia.class);
        for (int i = 0; i < pedido.size(); i++) {
            if (articulos.get(i).getTipo() == TipoArticulo.SAXOFON) {
                saxofones.merge(articulos.get(i).getFamilia(), pedido.get(i).cantidad(), Integer::sum);
            }
        }
        Map<Familia, Integer> cupoBoquillas = new EnumMap<>(Familia.class);
        Map<Familia, Integer> cupoCanas = new EnumMap<>(Familia.class);
        saxofones.forEach((familia, n) -> {
            cupoBoquillas.put(familia, n * BOQUILLAS_CON_DESCUENTO_POR_SAXOFON);
            cupoCanas.put(familia, n * CANAS_CON_DESCUENTO_POR_SAXOFON);
        });

        List<RenglonTicket> renglones = new ArrayList<>();
        for (int i = 0; i < pedido.size(); i++) {
            Articulo a = articulos.get(i);
            int cantidad = pedido.get(i).cantidad();
            Map<Familia, Integer> cupo = a.getTipo() == TipoArticulo.BOQUILLA ? cupoBoquillas
                                       : a.getTipo() == TipoArticulo.CANA ? cupoCanas
                                       : null;
            int conDescuento = 0;
            if (cupo != null) {
                conDescuento = Math.min(cantidad, cupo.getOrDefault(a.getFamilia(), 0));
                cupo.merge(a.getFamilia(), -conDescuento, Integer::sum);
            }
            BigDecimal bruto = a.getPrecio().multiply(BigDecimal.valueOf(cantidad));
            BigDecimal descuento = a.getPrecio().multiply(BigDecimal.valueOf(conDescuento))
                    .multiply(DESCUENTO_KIT).setScale(2, RoundingMode.HALF_UP);
            renglones.add(new RenglonTicket(a.getCodigo(), a.getNombre(), cantidad, a.getPrecio(),
                    conDescuento, descuento, bruto.subtract(descuento)));
        }
        return new Ticket(renglones);
    }

    private static Set<Familia> familiasDeSaxofones(List<Articulo> articulos) {
        Set<Familia> familias = EnumSet.noneOf(Familia.class);
        for (Articulo a : articulos) {
            if (a.getTipo() == TipoArticulo.SAXOFON) {
                familias.add(a.getFamilia());
            }
        }
        return familias;
    }

    // ------------------------------------------------------------------ consultas

    public synchronized Articulo buscar(String codigo) throws ArticuloNoEncontradoException {
        return dao.buscar(codigo).orElseThrow(() -> new ArticuloNoEncontradoException(codigo));
    }

    /** Copia de solo lectura de los artículos, en orden de alta. */
    public synchronized List<Articulo> getArticulos() {
        return Collections.unmodifiableList(new ArrayList<>(dao.listar()));
    }

    /** Regla 2: artículos cuyo stock está por debajo del mínimo de su tipo. */
    public synchronized List<Articulo> getAlertasReabasto() {
        List<Articulo> alertas = new ArrayList<>();
        for (Articulo a : dao.listar()) {
            if (a.requiereReabasto()) {
                alertas.add(a);
            }
        }
        return Collections.unmodifiableList(alertas);
    }

    public synchronized boolean estaVacia() {
        return dao.listar().isEmpty();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
