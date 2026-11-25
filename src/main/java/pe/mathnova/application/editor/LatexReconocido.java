package pe.mathnova.application.editor;

import java.util.regex.Pattern;

public final class LatexReconocido {

    private static final Pattern ESPACIOS_TEX = Pattern.compile(
            "\\\\(?:,|;|:|!| |quad|qquad|enspace|thinspace|medspace|thickspace)(?![A-Za-z])|~|\\\\hspace\\*?\\s*\\{[^}]*\\}");
    private static final Pattern ESTILOS = Pattern.compile(
            "\\\\(?:displaystyle|textstyle|scriptstyle|scriptscriptstyle|limits|nolimits)(?![A-Za-z])");
    private static final Pattern OPERATORNAME = Pattern.compile("\\\\operatorname\\s*\\*");
    private static final Pattern D_RECTA = Pattern.compile("\\\\(?:mathrm|mathit|text|rm)\\s*\\{\\s*d\\s*\\}");
    private static final Pattern LLAVES_VACIAS = Pattern.compile("(?<![_^}])\\{\\s*\\}");
    private static final Pattern PUNTO_FINAL = Pattern.compile("(?<!\\\\right)(?<!\\\\right )(?:\\s*(?:[.,]|\\\\cdot|\\\\ldots|\\\\dots))+\\s*$");
    private static final Pattern BLANCOS = Pattern.compile("\\s+");
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

    private static boolean parteDeComando(String s, int i) {
        int j = i - 1;
        while (j >= 0 && Character.isLetter(s.charAt(j))) {
            j--;
        }
        return j >= 0 && s.charAt(j) == '\\' && !escapada(s, j);
    }

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

    public static String breve(String latex, int maximo) {
        String s = latex == null ? "" : BLANCOS.matcher(latex.strip()).replaceAll(" ");
        return s.length() <= maximo ? s : s.substring(0, maximo - 1) + "…";
    }
}
