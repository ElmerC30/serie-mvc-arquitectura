package modelo;

/** Un ejemplar de la biblioteca, identificado por su ISBN. */
public class Libro {
    private final String isbn;
    private final String titulo;
    private final String autor;

    public Libro(String isbn, String titulo, String autor) {
        this.isbn = isbn;
        this.titulo = titulo;
        this.autor = autor;
    }

    public String getIsbn()   { return isbn; }
    public String getTitulo() { return titulo; }
    public String getAutor()  { return autor; }
}
