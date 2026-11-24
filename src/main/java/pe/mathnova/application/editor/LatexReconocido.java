package pe.mathnova.application.editor;

import java.util.regex.Pattern;

/**
 * Adapta el LaTeX que lee el reconocedor de escritura (UniMERNet escribe cada símbolo separado:
 * {@code \int _ { 0 } ^ { 1 } 2 x \, d x}) al LaTeX que se pone en el editor de arriba. Después, el
 * editor y {@link TraductorLatex} siguen el mismo camino que con el teclado.
 *
 * <p>Es a propósito pequeño y no adivina: solo quita lo que no cambia la expresión.
 * <ul>
 *   <li>Espacios repetidos y espacios de composición ({@code \,} {@code \;} {@code \quad} {@code ~}
 *       {@code \hspace{…}}), salvo los que separan un comando de una letra ({@code \int x}).</li>
 *   <li>Estilos de tamaño ({@code \displaystyle}, {@code \limits}) y {@code \operatorname*}.</li>
 *   <li>Un punto o una coma al final, que suele ser un trazo suelto.</li>
 *   <li>{@code \mathrm{d}x} y {@code \mathrm{d} x}: la d del diferencial queda como "d".</li>
 *   <li>Las llaves vacías {@code {}} sueltas.</li>
 *   <li>{@code \times} con exponente o subíndice ({@code 3 \times ^ { 2 }}): un signo × no lleva
 *       exponente, así que es la letra x (la confusión x/× es la más común al escribir a mano).</li>
 *   <li>Letras mayúsculas sueltas a minúscula: MathNova solo admite variables en minúscula (RF57).</li>
 * </ul>
 * Una fórmula de varias líneas ({@code \\}, {@code \begin{array}}) no se arregla: va tal cual al editor y
 * el estudiante ve el aviso para corregirla.
 */
public final class LatexReconocido {

    private static final Pattern ESPACIOS_TEX = Pattern.compile(
            "\\\\(?:,|;|:|!| |quad|qquad|enspace|thinspace|medspace|thickspace)(?![A-Za-z])|~|\\\\hspace\\*?\\s*\\{[^}]*\\}");
    private static final Pattern ESTILOS = Pattern.compile(
            "\\\\(?:displaystyle|textstyle|scriptstyle|scriptscriptstyle|limits|nolimits)(?![A-Za-z])");
    private static final Pattern OPERATORNAME = Pattern.compile("\\\\operatorname\\s*\\*");
    private static final Pattern D_RECTA = Pattern.compile("\\\\(?:mathrm|mathit|text|rm)\\s*\\{\\s*d\\s*\\}");
    private static final Pattern LLAVES_VACIAS = Pattern.compile("(?<![_^}])\\{\\s*\\}");
    /** Un punto, una coma o puntos suspensivos al final (pero no el "." de un {@code \right.}). */
    private static final Pattern PUNTO_FINAL = Pattern.compile("(?<!\\\\right)(?<!\\\\right )(?:\\s*(?:[.,]|\\\\cdot|\\\\ldots|\\\\dots))+\\s*$");
    private static final Pattern BLANCOS = Pattern.compile("\\s+");
    /** Un × con exponente o subíndice no es un producto: es la letra x ("3 \times ^ { 2 }"). */
    private static final Pattern POR_CON_EXPONENTE = Pattern.compile("\\\\times(?=\\s*[\\^_])");

    private LatexReconocido() {
    }

    public static String adaptar(String latex) {
        if (latex == null) {
            return "";
        }
        String s = latex.strip();
        s = ESPACIOS_TEX.matcher(s).replaceAll(" ");
        s = ESTILOS.matcher(s).replaceAll(" ");
        s = OPERATORNAME.matcher(s).replaceAll("\\\\operatorname");
        s = D_RECTA.matcher(s).replaceAll(" d ");
        s = LLAVES_VACIAS.matcher(s).replaceAll(" ");
        s = PUNTO_FINAL.matcher(s).replaceAll("");
        s = POR_CON_EXPONENTE.matcher(s).replaceAll(" x");
        return minusculas(compactar(s));
    }

    /**
     * MathNova solo admite variables en minúscula (RF57, {@code Letras}): una mayúscula latina suelta nunca
     * sería una entrada válida. A mano, x/X, c/C, s/S, o/O… solo se distinguen por el tamaño, así que se pasa
     * a minúscula. Las letras de un comando ({@code \Delta}, {@code \Big}) no se tocan.
     */
    static String minusculas(String s) {
        StringBuilder r = new StringBuilder(s);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z' && !parteDeComando(s, i)) {
                r.setCharAt(i, Character.toLowerCase(c));
            }
        }
        return r.toString();
    }

    /** true si la letra en i es parte del nombre de un comando: "\Delta", "\mathbb". */
    private static boolean parteDeComando(String s, int i) {
        int j = i - 1;
        while (j >= 0 && Character.isLetter(s.charAt(j))) {
            j--;
        }
        return j >= 0 && s.charAt(j) == '\\' && !escapada(s, j);
    }

    /**
     * Quita los espacios que no hacen falta: entre dos símbolos que no son letras de un comando. Se deja
     * uno solo entre un comando con nombre y una letra o número que lo sigue ({@code \int x},
     * {@code \pi 2}) para que no se lean juntos.
     */
    static String compactar(String s) {
        String uno = BLANCOS.matcher(s.strip()).replaceAll(" ");
        StringBuilder r = new StringBuilder(uno.length());
        for (int i = 0; i < uno.length(); i++) {
            char c = uno.charAt(i);
            if (c != ' ') {
                r.append(c);
                continue;
            }
            char siguiente = i + 1 < uno.length() ? uno.charAt(i + 1) : ' ';
            if (terminaEnComando(r) && Character.isLetterOrDigit(siguiente)) {
                r.append(' ');
            }
        }
        return r.toString();
    }

    /** true si lo escrito termina en un comando con nombre: "\int", "\frac" (no "\," ni "x"). */
    private static boolean terminaEnComando(CharSequence s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isLetter(s.charAt(i))) {
            i--;
        }
        return i >= 0 && i < s.length() - 1 && s.charAt(i) == '\\' && !escapada(s, i);
    }

    private static boolean escapada(CharSequence s, int barra) {
        int n = 0;
        for (int i = barra - 1; i >= 0 && s.charAt(i) == '\\'; i--) {
            n++;
        }
        return n % 2 == 1;
    }

    /** Para mensajes: el LaTeX en una línea, recortado. */
    public static String breve(String latex, int maximo) {
        String s = latex == null ? "" : BLANCOS.matcher(latex.strip()).replaceAll(" ");
        return s.length() <= maximo ? s : s.substring(0, maximo - 1) + "…";
    }
}
