package pe.mathnova.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BancoCsv {

    public record Fila(String id, String metodo, String pasos, String campo, String curso,
                       String dificultad, String entrada, String resultadoEsperado, String nota) {

        public String resultadoSinNota() {
            int pc = resultadoEsperado.indexOf(';');
            return (pc < 0 ? resultadoEsperado : resultadoEsperado.substring(0, pc)).trim();
        }
    }

    private static List<Fila> filas;

    private BancoCsv() {
    }

    public static synchronized List<Fila> filas() {
        if (filas == null) {
            filas = List.copyOf(leer());
        }
        return filas;
    }

    private static List<Fila> leer() {
        InputStream in = Objects.requireNonNull(BancoCsv.class.getResourceAsStream("/banco_factorizacion.csv"),
                "No está banco_factorizacion.csv");
        List<Fila> resultado = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            r.readLine();
            String linea;
            while ((linea = r.readLine()) != null) {
                if (linea.isBlank()) {
                    continue;
                }
                List<String> c = campos(linea);
                while (c.size() < 9) {
                    c.add("");
                }
                resultado.add(new Fila(c.get(0), c.get(1), c.get(2), c.get(3), c.get(4),
                        c.get(5), c.get(6), c.get(7), c.get(8)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return resultado;
    }

    private static List<String> campos(String linea) {
        List<String> c = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean comillas = false;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (comillas) {
                if (ch == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    actual.append('"');
                    i++;
                } else if (ch == '"') {
                    comillas = false;
                } else {
                    actual.append(ch);
                }
            } else if (ch == '"') {
                comillas = true;
            } else if (ch == ',') {
                c.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(ch);
            }
        }
        c.add(actual.toString());
        return c;
    }
}
