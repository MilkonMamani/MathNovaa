package pe.mathnova.presentation.latex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pasa a LaTeX los renglones de pizarra que el motor entrega como texto ("x⁶ − 6x⁴ + 9x² = (x³ − 3x)²",
 * "(x + 1)² = x² + 2x + 1", "a² + 2ab + b² = (a + b)² con a = x y b = 1") para dibujarlos con la misma
 * tipografía matemática que el resto de la pizarra. No cambia el contenido: solo la escritura.
 *
 * <p>Las palabras ("con", "Integrando") van en {@code \text{}}. Si el renglón es sobre todo prosa (cuatro
 * palabras o más) o tiene un símbolo que no se conoce, devuelve null y quien llama lo deja como texto.
 */
public final class TextoLatex {

    private static final String SUPER = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final String SUB = "₀₁₂₃₄₅₆₇₈₉";
    /** Más palabras que esto y el renglón es prosa. */
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

    /** El renglón en LaTeX, o null si no se puede (prosa o un símbolo desconocido). */
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

    /** El "=" de afuera (fuera de llaves y paréntesis); -1 si no hay. */
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

    // ------------------------------------------------------------------ lectura

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
                piezas.add(palabra(palabra, s, i, j));
                i = j;
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < n && Character.isDigit(s.charAt(j))) {
                    j++;
                }
                // 1835/729 como fracción
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
                // ³√x: el índice de la raíz
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

    /** "xyz", "abc": tres letras de variable seguidas se leen como producto, no como palabra. */
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

    /** Lo que sigue a ^ o _: (…) sin los paréntesis, un número o una letra. */
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

    /** Lo que va bajo √: (…) sin los paréntesis, o un número, o una letra. */
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

    private static String unir(List<Pieza> piezas) {
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
