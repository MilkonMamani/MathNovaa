package pe.mathnova.presentation.latex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TextoLatex {

    private static final String SUPER = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final String SUB = "₀₁₂₃₄₅₆₇₈₉";
    private static final int MAX_PALABRAS = 3;

    private static final Set<String> FUNCIONES = Set.of("sin", "cos", "tan", "cot", "sec", "csc", "ln", "log",
            "exp", "lim", "max", "min", "sinh", "cosh", "tanh", "arcsin", "arccos", "arctan", "det");
    private static final Set<String> FUNCIONES_ES = Set.of("sen", "tg", "ctg", "cosec", "arcsen", "arctg", "senh",
            "arccot");
    private static final Set<String> PALABRAS_CORTAS = Set.of("de", "en", "es", "si", "se", "la", "el", "al", "un",
            "lo");
    private static final Set<String> CONJUNCIONES = Set.of("y", "o", "e", "u");
    private static final Map<Character, String> SIMBOLOS = Map.ofEntries(
            Map.entry('+', "+"), Map.entry('−', "-"), Map.entry('-', "-"), Map.entry('=', "="),
            Map.entry('·', "\\cdot "), Map.entry('*', "\\cdot "), Map.entry('×', "\\times "),
            Map.entry('÷', "\\div "), Map.entry('≈', "\\approx "), Map.entry('≠', "\\neq "),
            Map.entry('≤', "\\leq "), Map.entry('≥', "\\geq "), Map.entry('<', "<"), Map.entry('>', ">"),
            Map.entry('→', "\\to "), Map.entry('⇒', "\\Rightarrow "), Map.entry('∞', "\\infty "),
            Map.entry('∪', "\\cup "), Map.entry('∩', "\\cap "), Map.entry('∅', "\\varnothing "),
            Map.entry('π', "\\pi "), Map.entry('∫', "\\int "), Map.entry('∂', "\\partial "),
            Map.entry('±', "\\pm "), Map.entry('∓', "\\mp "), Map.entry('α', "\\alpha "),
            Map.entry('β', "\\beta "), Map.entry('γ', "\\gamma "), Map.entry('δ', "\\delta "),
            Map.entry('Δ', "\\Delta "), Map.entry('θ', "\\theta "), Map.entry('λ', "\\lambda "),
            Map.entry('μ', "\\mu "), Map.entry('σ', "\\sigma "), Map.entry('φ', "\\varphi "),
            Map.entry('ω', "\\omega "), Map.entry('ε', "\\varepsilon "), Map.entry('′', "'"),
            Map.entry('\'', "'"), Map.entry('|', "|"), Map.entry('(', "("), Map.entry(')', ")"),
            Map.entry('[', "["), Map.entry(']', "]"), Map.entry('{', "\\{"), Map.entry('}', "\\}"),
            Map.entry(':', ":"), Map.entry(';', ";"), Map.entry('/', "/"), Map.entry('!', "!"),
            Map.entry(',', ","), Map.entry('.', "."), Map.entry('%', "\\%"), Map.entry('°', "^{\\circ}"),
            Map.entry('∈', "\\in "), Map.entry('ℝ', "\\mathbb{R}"), Map.entry('ℚ', "\\mathbb{Q}"),
            Map.entry('ℂ', "\\mathbb{C}"), Map.entry('ℤ', "\\mathbb{Z}"), Map.entry('ℕ', "\\mathbb{N}"));

    private TextoLatex() {
    }

    private enum Tipo { MATE, TEXTO, ESPACIO }

    private record Pieza(Tipo tipo, String latex) {
    }

    public static String convertir(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        List<Pieza> piezas = new ArrayList<>();
        if (!leer(texto.strip(), piezas)) {
            return null;
        }
        long palabras = piezas.stream().filter(p -> p.tipo() == Tipo.TEXTO).count();
        if (palabras > MAX_PALABRAS) {
            return null;
        }
        return unir(piezas);
    }

    public static String conDiferenciales(String latex) {
        return latex == null ? null
                : latex.replaceAll("(?<![a-zA-Z\\\\{,])d([a-z]|\\\\theta)(?![a-zA-Z])", "\\\\,d$1");
    }

    public static int igualDeAfuera(String latex) {
        int nivel = 0;
        for (int i = 0; i < latex.length(); i++) {
            char c = latex.charAt(i);
            if (c == '\\' && i + 1 < latex.length()) {
                i++;
                continue;
            }
            if (c == '{' || c == '(' || c == '[') {
                nivel++;
            } else if (c == '}' || c == ')' || c == ']') {
                nivel--;
            } else if (c == '=' && nivel == 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean leer(String s, List<Pieza> piezas) {
        int n = s.length();
        int i = 0;
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                piezas.add(new Pieza(Tipo.ESPACIO, " "));
                while (i < n && Character.isWhitespace(s.charAt(i))) {
                    i++;
                }
                continue;
            }
            if (esLetraLatina(c)) {
                int j = i;
                while (j < n && esLetraLatina(s.charAt(j))) {
                    j++;
                }
                String palabra = s.substring(i, j);
                if (palabra.equals("sqrt") && j < n && s.charAt(j) == '(') {
                    int[] fin = new int[1];
                    String radicando = radicando(s, j, fin);
                    if (radicando == null) {
                        return false;
                    }
                    piezas.add(new Pieza(Tipo.MATE, "\\sqrt{" + radicando + "}"));
                    i = fin[0];
                    continue;
                }
                piezas.add(palabra(palabra, s, i, j));
                i = j;
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < n && Character.isDigit(s.charAt(j))) {
                    j++;
                }
                if (j + 1 < n && s.charAt(j) == '/' && Character.isDigit(s.charAt(j + 1))
                        && (i == 0 || s.charAt(i - 1) != '^')) {
                    int k = j + 1;
                    while (k < n && Character.isDigit(s.charAt(k))) {
                        k++;
                    }
                    if (k >= n || !Character.isLetter(s.charAt(k)) && s.charAt(k) != '(' && s.charAt(k) != '√') {
                        piezas.add(new Pieza(Tipo.MATE, "\\frac{" + s.substring(i, j) + "}{" + s.substring(j + 1, k) + "}"));
                        i = k;
                        continue;
                    }
                }
                piezas.add(new Pieza(Tipo.MATE, s.substring(i, j)));
                i = j;
                continue;
            }
            if (SUPER.indexOf(c) >= 0 || c == '⁻' || c == '⁺' || c == 'ⁿ') {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < n && (SUPER.indexOf(s.charAt(j)) >= 0 || s.charAt(j) == '⁻' || s.charAt(j) == '⁺'
                        || s.charAt(j) == 'ⁿ')) {
                    char d = s.charAt(j);
                    sb.append(d == '⁻' ? "-" : d == '⁺' ? "+" : d == 'ⁿ' ? "n"
                            : String.valueOf((char) ('0' + SUPER.indexOf(d))));
                    j++;
                }
                if (j < n && s.charAt(j) == '√') {
                    int[] fin = new int[1];
                    String radicando = radicando(s, j + 1, fin);
                    if (radicando == null) {
                        return false;
                    }
                    piezas.add(new Pieza(Tipo.MATE, "\\sqrt[" + sb + "]{" + radicando + "}"));
                    i = fin[0];
                    continue;
                }
                piezas.add(new Pieza(Tipo.MATE, "^{" + sb + "}"));
                i = j;
                continue;
            }
            if (SUB.indexOf(c) >= 0) {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < n && SUB.indexOf(s.charAt(j)) >= 0) {
                    sb.append((char) ('0' + SUB.indexOf(s.charAt(j))));
                    j++;
                }
                piezas.add(new Pieza(Tipo.MATE, "_{" + sb + "}"));
                i = j;
                continue;
            }
            if (c == '^' || c == '_') {
                int[] fin = new int[1];
                String arg = argumento(s, i + 1, fin);
                if (arg == null) {
                    return false;
                }
                piezas.add(new Pieza(Tipo.MATE, c + "{" + arg + "}"));
                i = fin[0];
                continue;
            }
            if (c == '√' || c == '∛' || c == '∜') {
                int[] fin = new int[1];
                String radicando = radicando(s, i + 1, fin);
                if (radicando == null) {
                    return false;
                }
                String indice = c == '∛' ? "[3]" : c == '∜' ? "[4]" : "";
                piezas.add(new Pieza(Tipo.MATE, "\\sqrt" + indice + "{" + radicando + "}"));
                i = fin[0];
                continue;
            }
            String simbolo = SIMBOLOS.get(c);
            if (simbolo == null) {
                return false;
            }
            piezas.add(new Pieza(Tipo.MATE, simbolo));
            i++;
        }
        return true;
    }

    private static boolean esLetraLatina(char c) {
        return Character.isLetter(c) && (c < 0x370 || c > 0x3FF) && "ⁿℝℚℂℤℕ".indexOf(c) < 0;
    }

    private static Pieza palabra(String p, String s, int desde, int hasta) {
        if (FUNCIONES.contains(p)) {
            return new Pieza(Tipo.MATE, "\\" + p + " ");
        }
        if (FUNCIONES_ES.contains(p)) {
            return new Pieza(Tipo.MATE, "\\mathrm{" + p + "}" + (siguiente(s, hasta) == '(' ? "" : "\\,"));
        }
        boolean entreEspacios = (desde == 0 || s.charAt(desde - 1) == ' ')
                && (hasta == s.length() || s.charAt(hasta) == ' ' || s.charAt(hasta) == ':' || s.charAt(hasta) == ',');
        boolean acentos = p.chars().anyMatch(ch -> ch > 127);
        if (acentos || p.length() >= 3 && !esVariables(p)) {
            return new Pieza(Tipo.TEXTO, "\\text{" + p + "}");
        }
        if (entreEspacios && PALABRAS_CORTAS.contains(p)) {
            return new Pieza(Tipo.TEXTO, "\\text{" + p + "}");
        }
        if (entreEspacios && CONJUNCIONES.contains(p)) {
            char antes = anterior(s, desde);
            char despues = siguiente(s, hasta);
            boolean operandoAntes = Character.isLetterOrDigit(antes) || antes == ')' || SUPER.indexOf(antes) >= 0;
            boolean operandoDespues = Character.isLetterOrDigit(despues) || despues == '(' || despues == '√';
            if (operandoAntes && operandoDespues) {
                return new Pieza(Tipo.TEXTO, "\\text{" + p + "}");
            }
        }
        return new Pieza(Tipo.MATE, p);
    }

    private static boolean esVariables(String p) {
        return p.length() <= 3 && p.chars().allMatch(ch -> "abcdxyzhkmnpqrstuvw".indexOf(ch) >= 0)
                && p.chars().distinct().count() == p.length() && !Set.of("vez", "sus", "son", "par", "cam").contains(p);
    }

    private static char anterior(String s, int i) {
        for (int k = i - 1; k >= 0; k--) {
            if (s.charAt(k) != ' ') {
                return s.charAt(k);
            }
        }
        return '\0';
    }

    private static char siguiente(String s, int i) {
        for (int k = i; k < s.length(); k++) {
            if (s.charAt(k) != ' ') {
                return s.charAt(k);
            }
        }
        return '\0';
    }

    private static String argumento(String s, int i, int[] fin) {
        if (i >= s.length()) {
            return null;
        }
        char c = s.charAt(i);
        if (c == '(' || c == '{') {
            int cierre = cierre(s, i);
            if (cierre < 0) {
                return null;
            }
            fin[0] = cierre + 1;
            return convertirInterno(s.substring(i + 1, cierre));
        }
        int j = i;
        if (c == '-' || c == '−') {
            j++;
        }
        if (j < s.length() && Character.isDigit(s.charAt(j))) {
            while (j < s.length() && Character.isDigit(s.charAt(j))) {
                j++;
            }
        } else if (j < s.length() && Character.isLetter(s.charAt(j))) {
            j++;
        } else {
            return null;
        }
        fin[0] = j;
        return convertirInterno(s.substring(i, j));
    }

    private static String radicando(String s, int i, int[] fin) {
        if (i < s.length() && s.charAt(i) == '(') {
            int cierre = cierre(s, i);
            if (cierre < 0) {
                return null;
            }
            fin[0] = cierre + 1;
            return convertirInterno(s.substring(i + 1, cierre));
        }
        int j = i;
        while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
            j++;
        }
        if (j == i && j < s.length() && Character.isLetter(s.charAt(j))) {
            j++;
        }
        if (j == i) {
            return null;
        }
        fin[0] = j;
        return convertirInterno(s.substring(i, j));
    }

    private static int cierre(String s, int abre) {
        char a = s.charAt(abre);
        char c = a == '(' ? ')' : '}';
        int nivel = 0;
        for (int k = abre; k < s.length(); k++) {
            if (s.charAt(k) == a) {
                nivel++;
            } else if (s.charAt(k) == c && --nivel == 0) {
                return k;
            }
        }
        return -1;
    }

    private static String convertirInterno(String s) {
        List<Pieza> piezas = new ArrayList<>();
        return leer(s.strip(), piezas) ? unir(piezas) : null;
    }

    private static List<Pieza> fracciones(List<Pieza> piezas) {
        List<Pieza> ps = new ArrayList<>(piezas);
        boolean cambio = true;
        while (cambio) {
            cambio = false;
            for (int k = 1; k + 1 < ps.size(); k++) {
                Pieza p = ps.get(k);
                if (p.tipo() != Tipo.MATE || !"/".equals(p.latex())) {
                    continue;
                }
                int ini = inicioNumerador(ps, k - 1);
                int fin = finDenominador(ps, k + 1);
                if (ini < 0 || fin < 0) {
                    continue;
                }
                String num = unir(sinParentesis(ps.subList(ini, k)));
                String den = unir(sinParentesis(ps.subList(k + 1, fin + 1)));
                if (num.isEmpty() || den.isEmpty()) {
                    continue;
                }
                List<Pieza> nuevo = new ArrayList<>(ps.subList(0, ini));
                nuevo.add(new Pieza(Tipo.MATE, "\\frac{" + num + "}{" + den + "}"));
                nuevo.addAll(ps.subList(fin + 1, ps.size()));
                ps = nuevo;
                cambio = true;
                break;
            }
        }
        return ps;
    }

    private static boolean esExponente(Pieza p) {
        return p.tipo() == Tipo.MATE && (p.latex().startsWith("^{") || p.latex().startsWith("_{")
                || p.latex().equals("'"));
    }

    private static boolean esFuncion(Pieza p) {
        String l = p.latex();
        return p.tipo() == Tipo.MATE && (l.startsWith("\\mathrm{") || l.matches("\\\\[a-z]+ ")
                && FUNCIONES.contains(l.substring(1).strip()));
    }

    private static boolean esAtomo(Pieza p) {
        if (p.tipo() != Tipo.MATE || esFuncion(p) || esExponente(p)) {
            return false;
        }
        String l = p.latex();
        if (l.isEmpty()) {
            return false;
        }
        char c = l.charAt(0);
        if (Character.isLetterOrDigit(c)) {
            return true;
        }
        return l.startsWith("\\sqrt") || l.startsWith("\\frac") || l.startsWith("\\pi") || l.startsWith("\\infty")
                || l.startsWith("\\alpha") || l.startsWith("\\beta") || l.startsWith("\\theta")
                || l.startsWith("\\lambda") || l.startsWith("\\mathbb");
    }

    private static boolean es(Pieza p, String latex) {
        return p.tipo() == Tipo.MATE && latex.equals(p.latex());
    }

    private static int inicioNumerador(List<Pieza> ps, int fin) {
        int i = fin;
        int inicio = -1;
        while (i >= 0) {
            int j = i;
            while (j >= 0 && esExponente(ps.get(j))) {
                j--;
            }
            if (j < 0) {
                return -1;
            }
            Pieza p = ps.get(j);
            int comienzo;
            if (es(p, ")")) {
                comienzo = abre(ps, j);
                if (comienzo < 0) {
                    return -1;
                }
                if (comienzo > 0 && esFuncion(ps.get(comienzo - 1))) {
                    comienzo--;
                }
            } else if (esAtomo(p)) {
                comienzo = j;
            } else {
                break;
            }
            inicio = comienzo;
            i = comienzo - 1;
            if (i < 0 || !(es(ps.get(i), ")") || esAtomo(ps.get(i)) || esExponente(ps.get(i)))) {
                break;
            }
        }
        if (inicio > 0) {
            int k = inicio - 1;
            if (k >= 0 && ps.get(k).tipo() == Tipo.ESPACIO) {
                k--;
            }
            while (k >= 0 && esExponente(ps.get(k))) {
                k--;
            }
            if (k >= 0 && esFuncion(ps.get(k))) {
                inicio = k;
            }
        }
        return inicio;
    }

    private static int finDenominador(List<Pieza> ps, int ini) {
        Pieza p = ps.get(ini);
        int fin;
        if (es(p, "(")) {
            fin = cierra(ps, ini);
        } else if (esFuncion(p) && ini + 1 < ps.size() && es(ps.get(ini + 1), "(")) {
            fin = cierra(ps, ini + 1);
        } else if (esAtomo(p)) {
            fin = ini;
        } else {
            return -1;
        }
        if (fin < 0) {
            return -1;
        }
        while (fin + 1 < ps.size() && esExponente(ps.get(fin + 1))) {
            fin++;
        }
        return fin;
    }

    private static int abre(List<Pieza> ps, int cierre) {
        int nivel = 0;
        for (int k = cierre; k >= 0; k--) {
            if (es(ps.get(k), ")")) {
                nivel++;
            } else if (es(ps.get(k), "(") && --nivel == 0) {
                return k;
            }
        }
        return -1;
    }

    private static int cierra(List<Pieza> ps, int abre) {
        int nivel = 0;
        for (int k = abre; k < ps.size(); k++) {
            if (es(ps.get(k), "(")) {
                nivel++;
            } else if (es(ps.get(k), ")") && --nivel == 0) {
                return k;
            }
        }
        return -1;
    }

    private static List<Pieza> sinParentesis(List<Pieza> ps) {
        if (ps.size() >= 2 && es(ps.getFirst(), "(") && cierra(ps, 0) == ps.size() - 1) {
            return ps.subList(1, ps.size() - 1);
        }
        return ps;
    }

    private static String unir(List<Pieza> entrada) {
        List<Pieza> piezas = entrada.stream().anyMatch(p -> es(p, "/")) ? fracciones(entrada) : entrada;
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < piezas.size(); k++) {
            Pieza p = piezas.get(k);
            if (p.tipo() == Tipo.ESPACIO) {
                Tipo antes = k > 0 ? piezas.get(k - 1).tipo() : null;
                Tipo despues = k + 1 < piezas.size() ? piezas.get(k + 1).tipo() : null;
                if (antes == Tipo.TEXTO || despues == Tipo.TEXTO) {
                    sb.append("\\ ");
                } else if (!sb.isEmpty() && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                continue;
            }
            sb.append(p.latex());
        }
        return sb.toString().strip();
    }
}
