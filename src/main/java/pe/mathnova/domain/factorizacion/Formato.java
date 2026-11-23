package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.stream.Collectors;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Texto de las justificaciones, como en la pizarra: exponentes con superíndice y signo "−"
 * (U+2212). "4x² = (2x)² y 25 = 5²", "−2 + (−3) = −5".
 */
final class Formato {

    static final String MENOS = "−";
    private static final String SUPERINDICES = "⁰¹²³⁴⁵⁶⁷⁸⁹";

    private Formato() {
    }

    static String superindice(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) {
            sb.append(c == '-' ? '⁻' : SUPERINDICES.charAt(c - '0'));
        }
        return sb.toString();
    }

    /** "3", "−2", "3/4", "−3/4". */
    static String numero(Racional r) {
        return r.signo() < 0 ? MENOS + r.negar() : r.toString();
    }

    /** Número entre paréntesis si es negativo: "(−3)". */
    static String numeroEnParentesis(Racional r) {
        return r.signo() < 0 ? "(" + numero(r) + ")" : numero(r);
    }

    /** "x²y", "" para UNO. */
    static String monomio(Monomio m) {
        StringBuilder sb = new StringBuilder();
        m.exponentes().forEach((letra, e) -> {
            sb.append(letra);
            if (e > 1) {
                sb.append(superindice(e));
            }
        });
        return sb.toString();
    }

    /** "4x²", "−x", "12", "(3/4)x". */
    static String termino(Termino t) {
        Racional c = t.coeficiente();
        String m = monomio(t.monomio());
        if (m.isEmpty()) {
            return numero(c);
        }
        String signo = c.signo() < 0 ? MENOS : "";
        Racional abs = c.signo() < 0 ? c.negar() : c;
        if (abs.equals(Racional.UNO)) {
            return signo + m;
        }
        return signo + (abs.esEntero() ? abs.toString() : "(" + abs + ")") + m;
    }

    /** Base de una potencia: "5", "(2x)", "(x)", "(−2)". */
    static String base(Termino t) {
        String s = termino(t);
        boolean numeroPositivo = t.monomio().equals(Monomio.UNO) && t.positivo() && t.coeficiente().esEntero();
        return numeroPositivo ? s : "(" + s + ")";
    }

    /** "x³ − 2x² − x + 2". */
    static String polinomio(Polinomio p) {
        return suma(Termino.de(p));
    }

    static String suma(List<Termino> terminos) {
        if (terminos.isEmpty()) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(termino(terminos.getFirst()));
        for (Termino t : terminos.subList(1, terminos.size())) {
            sb.append(t.positivo() ? " + " : " " + MENOS + " ").append(termino(t.abs()));
        }
        return sb.toString();
    }

    /** "(x − 2)". */
    static String parentesis(Polinomio p) {
        return "(" + polinomio(p) + ")";
    }

    /**
     * Producto c·m·(q): "x²(x − 2)", "−(x − 2)", "−8(2x − 1)", "3(x + 1)".
     */
    static String productoComun(Racional c, Monomio m, Polinomio q) {
        String pre;
        if (m.equals(Monomio.UNO)) {
            pre = c.equals(Racional.UNO) ? "" : c.equals(Racional.UNO.negar()) ? MENOS : numero(c);
        } else {
            pre = termino(new Termino(c, m));
        }
        return pre + parentesis(q);
    }

    /** "1, 2, 1, 2". */
    static String lista(List<?> valores) {
        return valores.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /** Pasa un texto del parser ("x^2(x - 3)") a la forma de pizarra ("x²(x − 3)"). */
    static String desdeTexto(String texto) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '^' && i + 1 < texto.length() && Character.isDigit(texto.charAt(i + 1))) {
                int j = i + 1;
                while (j < texto.length() && Character.isDigit(texto.charAt(j))) {
                    j++;
                }
                sb.append(superindice(Integer.parseInt(texto.substring(i + 1, j))));
                i = j - 1;
            } else if (c == '-') {
                sb.append(MENOS);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
