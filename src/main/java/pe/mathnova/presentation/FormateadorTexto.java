package pe.mathnova.presentation;

import pe.mathnova.domain.algebra.Racional;

public final class FormateadorTexto {

    private static final String SUPERINDICES = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    public static final String MENOS = "−";

    private FormateadorTexto() {
    }

    public static String pizarra(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        int n = texto.length();
        for (int i = 0; i < n; i++) {
            char c = texto.charAt(i);
            char sig = i + 1 < n ? texto.charAt(i + 1) : '\0';
            if (c == '^' && i + 1 < n && (Character.isDigit(sig) || sig == '-' && i + 2 < n
                    && Character.isDigit(texto.charAt(i + 2)))) {
                int j = i + 1;
                if (texto.charAt(j) == '-') {
                    sb.append('⁻');
                    j++;
                }
                while (j < n && Character.isDigit(texto.charAt(j))) {
                    sb.append(SUPERINDICES.charAt(texto.charAt(j) - '0'));
                    j++;
                }
                i = j - 1;
            } else if (c == '>' && sig == '=') {
                sb.append('≥');
                i++;
            } else if (c == '<' && sig == '=') {
                sb.append('≤');
                i++;
            } else if (c == '!' && sig == '=') {
                sb.append('≠');
                i++;
            } else if (c == '*') {
                sb.append('·');
            } else if (c == '-' && esBinario(sb)) {
                sb.append(MENOS);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String pizarraConNegativos(String texto) {
        String p = pizarra(texto);
        return p == null ? null : p.replaceAll("(^|[=≈(,→:]\\s*)-", "$1" + MENOS);
    }

    private static boolean esBinario(CharSequence antes) {
        for (int i = antes.length() - 1; i >= 0; i--) {
            char c = antes.charAt(i);
            if (c == ' ') {
                continue;
            }
            return Character.isLetterOrDigit(c) || c == ')' || c == ']' || SUPERINDICES.indexOf(c) >= 0;
        }
        return false;
    }

    public static String numero(Racional r) {
        return r.signo() < 0 ? MENOS + r.negar() : r.toString();
    }

    public static String numeroEnParentesis(Racional r) {
        return r.signo() < 0 ? "(" + numero(r) + ")" : numero(r);
    }

    public static String potencia(char variable, int exponente) {
        if (exponente == 0) {
            return "";
        }
        if (exponente == 1) {
            return String.valueOf(variable);
        }
        StringBuilder sb = new StringBuilder().append(variable);
        for (char d : String.valueOf(exponente).toCharArray()) {
            sb.append(SUPERINDICES.charAt(d - '0'));
        }
        return sb.toString();
    }

    public static String termino(Racional c, char variable, int exponente) {
        String m = potencia(variable, exponente);
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

    public static String terminoEnParentesis(Racional c, char variable, int exponente) {
        String t = termino(c, variable, exponente);
        return c.signo() < 0 ? "(" + t + ")" : t;
    }

    public static String suma(char variable, Racional... coeficientesDeMayorAMenor) {
        StringBuilder sb = new StringBuilder();
        int grado = coeficientesDeMayorAMenor.length - 1;
        for (int i = 0; i <= grado; i++) {
            Racional c = coeficientesDeMayorAMenor[i];
            if (c.esCero()) {
                continue;
            }
            int e = grado - i;
            if (sb.isEmpty()) {
                sb.append(termino(c, variable, e));
            } else {
                sb.append(c.signo() < 0 ? " " + MENOS + " " : " + ").append(termino(c.signo() < 0 ? c.negar() : c,
                        variable, e));
            }
        }
        return sb.isEmpty() ? "0" : sb.toString();
    }

    private static final java.util.regex.Pattern CODIGO =
            java.util.regex.Pattern.compile("\\b(F[0-9][0-9]|M[0-9][0-9]|A[0-9]|P[0-9][0-9])\\b");

    public static String sinCodigos(String texto) {
        if (texto == null) {
            return null;
        }
        return CODIGO.matcher(texto).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(
                pe.mathnova.domain.factorizacion.Tecnicas.nombreCorto(m.group(1))));
    }
}
