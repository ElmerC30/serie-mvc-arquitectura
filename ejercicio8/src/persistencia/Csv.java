package persistencia;

import java.util.ArrayList;
import java.util.List;

/** Utilidades minimas para leer y escribir lineas CSV con comillas. */
final class Csv {
    private Csv() { }

    static String linea(String... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String c = campos[i] == null ? "" : campos[i];
            if (c.contains(",") || c.contains("\"")) {
                c = "\"" + c.replace("\"", "\"\"") + "\"";
            }
            sb.append(c);
        }
        return sb.toString();
    }

    static List<String> separar(String linea) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (comillas) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (c == '"') {
                    comillas = false;
                } else {
                    actual.append(c);
                }
            } else if (c == '"') {
                comillas = true;
            } else if (c == ',') {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        campos.add(actual.toString());
        return campos;
    }
}
