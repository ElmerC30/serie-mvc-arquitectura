package modelo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Nucleo del sistema de prestamos: estado y reglas de negocio.
 *
 * Reglas:
 *   R1. Un socio puede tener como maximo 3 prestamos activos.
 *   R2. Un libro con un prestamo activo no se puede prestar otra vez.
 *   R3. Al devolver tarde se genera una multa de $5 por cada dia de retraso (plazo: 7 dias).
 *   R4. Un socio con multa pendiente no puede pedir prestamos hasta pagarla;
 *       el pago debe ser positivo y no mayor a lo que debe.
 *
 * No imprime, no lee teclado, no conoce vistas ni sabe como se guardan los datos.
 * La fecha actual llega por un Clock para que las reglas de fechas se puedan probar.
 */
public class Biblioteca {
    public static final int MAX_PRESTAMOS_ACTIVOS = 3;
    public static final int DIAS_PRESTAMO = 7;
    public static final double MULTA_POR_DIA = 5.0;

    private final RepositorioBiblioteca repositorio;
    private final Clock reloj;
    private final List<ObservadorBiblioteca> observadores = new CopyOnWriteArrayList<>();

    public Biblioteca(RepositorioBiblioteca repositorio, Clock reloj) {
        if (repositorio == null || reloj == null) {
            throw new IllegalArgumentException("La biblioteca necesita un repositorio y un reloj");
        }
        this.repositorio = repositorio;
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ observadores

    public void agregarObservador(ObservadorBiblioteca o)  { observadores.add(o); }
    public void quitarObservador(ObservadorBiblioteca o)   { observadores.remove(o); }

    private void notificar() {
        for (ObservadorBiblioteca o : observadores) {
            o.bibliotecaCambiada();
        }
    }

    public LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    // ------------------------------------------------------------------ altas

    public void registrarLibro(Libro libro) throws DatosInvalidosException {
        synchronized (this) {
            if (libro == null || vacio(libro.getIsbn()) || vacio(libro.getTitulo())) {
                throw new DatosInvalidosException("El libro necesita ISBN y titulo");
            }
            if (buscarLibroOpcional(libro.getIsbn()) != null) {
                throw new DatosInvalidosException("Ya existe un libro con ISBN " + libro.getIsbn());
            }
            repositorio.insertarLibro(libro);
        }
        notificar();
    }

    public void registrarSocio(Socio socio) throws DatosInvalidosException {
        synchronized (this) {
            if (socio == null || vacio(socio.getId()) || vacio(socio.getNombre())) {
                throw new DatosInvalidosException("El socio necesita id y nombre");
            }
            if (buscarSocioOpcional(socio.getId()) != null) {
                throw new DatosInvalidosException("Ya existe un socio con id " + socio.getId());
            }
            repositorio.insertarSocio(socio);
        }
        notificar();
    }

    // ------------------------------------------------------------------ operaciones con reglas

    /** Presta un libro aplicando R1, R2 y R4. */
    public Prestamo prestar(String isbn, String socioId) throws ReglaNegocioException {
        Prestamo prestamo;
        synchronized (this) {
            Libro libro = buscarLibro(isbn);
            Socio socio = buscarSocio(socioId);
            if (socio.tieneMulta()) {
                throw new SocioConMultaException(String.format(java.util.Locale.ROOT,
                        "%s debe $%.2f de multas", socio.getNombre(), socio.getMultaPendiente()));
            }
            if (!estaDisponible(libro.getIsbn())) {
                throw new LibroNoDisponibleException("\"" + libro.getTitulo() + "\" ya esta prestado");
            }
            if (prestamosActivosDe(socioId).size() >= MAX_PRESTAMOS_ACTIVOS) {
                throw new LimitePrestamosException(socio.getNombre() + " ya tiene "
                        + MAX_PRESTAMOS_ACTIVOS + " prestamos activos");
            }
            LocalDate hoy = hoy();
            prestamo = new Prestamo(isbn, socioId, hoy, hoy.plusDays(DIAS_PRESTAMO), null);
            repositorio.insertarPrestamo(prestamo);
        }
        notificar();
        return prestamo;
    }

    /**
     * Registra la devolucion de un libro aplicando R3.
     * @return la multa generada por esta devolucion (0 si fue a tiempo)
     */
    public double devolver(String isbn) throws ReglaNegocioException {
        double multa;
        synchronized (this) {
            Prestamo prestamo = prestamoActivoDe(isbn);
            if (prestamo == null) {
                throw new NoEncontradoException("El libro " + isbn + " no tiene un prestamo activo");
            }
            LocalDate hoy = hoy();
            prestamo.registrarDevolucion(hoy);
            repositorio.actualizarPrestamo(prestamo);
            multa = calcularMulta(prestamo.getFechaLimite(), hoy);
            if (multa > 0) {
                Socio socio = buscarSocio(prestamo.getSocioId());
                socio.setMultaPendiente(socio.getMultaPendiente() + multa);
                repositorio.actualizarSocio(socio);
            }
        }
        notificar();
        return multa;
    }

    /** Abona a la multa de un socio (R4). @return lo que sigue debiendo */
    public double pagarMulta(String socioId, double monto) throws ReglaNegocioException {
        double resta;
        synchronized (this) {
            Socio socio = buscarSocio(socioId);
            if (monto <= 0) {
                throw new DatosInvalidosException("El pago debe ser mayor que cero");
            }
            if (monto > socio.getMultaPendiente() + 1e-9) {
                throw new DatosInvalidosException(String.format(java.util.Locale.ROOT,
                        "El pago excede la multa pendiente ($%.2f)", socio.getMultaPendiente()));
            }
            resta = Math.max(0, socio.getMultaPendiente() - monto);
            socio.setMultaPendiente(resta);
            repositorio.actualizarSocio(socio);
        }
        notificar();
        return resta;
    }

    /** R3: $5 por cada dia completo despues de la fecha limite. */
    public static double calcularMulta(LocalDate fechaLimite, LocalDate fechaDevolucion) {
        long diasRetraso = ChronoUnit.DAYS.between(fechaLimite, fechaDevolucion);
        return diasRetraso > 0 ? diasRetraso * MULTA_POR_DIA : 0.0;
    }

    /**
     * El reloj es externo al modelo; cuando avanza, el estado derivado (prestamos vencidos)
     * cambia aunque no haya habido operaciones, asi que el modelo avisa a sus observadores.
     */
    public void fechaActualizada() {
        notificar();
    }

    // ------------------------------------------------------------------ consultas

    /** Estado de cada libro (disponible / prestado / vencido) para las vistas. */
    public synchronized List<EstadoLibro> getEstadoLibros() {
        LocalDate hoy = hoy();
        List<EstadoLibro> estados = new ArrayList<>();
        for (Libro l : repositorio.listarLibros()) {
            Prestamo p = prestamoActivoDe(l.getIsbn());
            String socio = null;
            if (p != null) {
                Socio s = buscarSocioOpcional(p.getSocioId());
                socio = s == null ? p.getSocioId() : s.getNombre();
            }
            estados.add(new EstadoLibro(l, p, socio, p != null && p.estaVencido(hoy)));
        }
        return Collections.unmodifiableList(estados);
    }

    /** Estado de cada socio (prestamos activos, multa, si puede pedir) para las vistas. */
    public synchronized List<EstadoSocio> getEstadoSocios() {
        List<EstadoSocio> estados = new ArrayList<>();
        for (Socio s : repositorio.listarSocios()) {
            estados.add(new EstadoSocio(s, prestamosActivosDe(s.getId()).size()));
        }
        return Collections.unmodifiableList(estados);
    }

    public synchronized List<Libro> getLibros() {
        return Collections.unmodifiableList(new ArrayList<>(repositorio.listarLibros()));
    }

    public synchronized List<Socio> getSocios() {
        return Collections.unmodifiableList(new ArrayList<>(repositorio.listarSocios()));
    }

    public synchronized List<Prestamo> getPrestamosActivos() {
        List<Prestamo> activos = new ArrayList<>();
        for (Prestamo p : repositorio.listarPrestamos()) {
            if (p.estaActivo()) {
                activos.add(p);
            }
        }
        return Collections.unmodifiableList(activos);
    }

    public synchronized boolean estaDisponible(String isbn) {
        return prestamoActivoDe(isbn) == null;
    }

    public synchronized List<Prestamo> prestamosActivosDe(String socioId) {
        List<Prestamo> activos = new ArrayList<>();
        for (Prestamo p : repositorio.listarPrestamos()) {
            if (p.estaActivo() && p.getSocioId().equals(socioId)) {
                activos.add(p);
            }
        }
        return activos;
    }

    public synchronized Libro buscarLibro(String isbn) throws NoEncontradoException {
        Libro l = buscarLibroOpcional(isbn);
        if (l == null) {
            throw new NoEncontradoException("No existe el libro " + isbn);
        }
        return l;
    }

    public synchronized Socio buscarSocio(String id) throws NoEncontradoException {
        Socio s = buscarSocioOpcional(id);
        if (s == null) {
            throw new NoEncontradoException("No existe el socio " + id);
        }
        return s;
    }

    private Libro buscarLibroOpcional(String isbn) {
        for (Libro l : repositorio.listarLibros()) {
            if (l.getIsbn().equals(isbn)) {
                return l;
            }
        }
        return null;
    }

    private Socio buscarSocioOpcional(String id) {
        for (Socio s : repositorio.listarSocios()) {
            if (s.getId().equals(id)) {
                return s;
            }
        }
        return null;
    }

    private Prestamo prestamoActivoDe(String isbn) {
        for (Prestamo p : repositorio.listarPrestamos()) {
            if (p.estaActivo() && p.getIsbn().equals(isbn)) {
                return p;
            }
        }
        return null;
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
