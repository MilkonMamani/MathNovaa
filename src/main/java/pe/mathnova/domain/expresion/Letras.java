package pe.mathnova.domain.expresion;

/**
 * Qué letras pueden ser variables (RF57) y cuáles son siempre constantes (RN42).
 * <ul>
 *   <li>Variable: una letra latina minúscula a-z salvo e e i, o una griega minúscula α-ω salvo π.</li>
 *   <li>Constante: e (Euler), i (unidad imaginaria) y π.</li>
 * </ul>
 * Las griegas son caracteres del plano básico, así que caben en un {@code char}.
 */
public final class Letras {

    private static final String[] GRIEGAS_LATEX = {
            "alpha", "beta", "gamma", "delta", "varepsilon", "zeta", "eta", "theta", "iota", "kappa",
            "lambda", "mu", "nu", "xi", "omicron", "pi", "rho", "varsigma", "sigma", "tau", "upsilon",
            "varphi", "chi", "psi", "omega"};

    private Letras() {
    }

    public static boolean esLatina(char c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean esGriega(char c) {
        return c >= 'α' && c <= 'ω';
    }

    /** RN42: e, i y π. */
    public static boolean esConstante(char c) {
        return c == 'e' || c == 'i' || c == 'π';
    }

    /** RF57. */
    public static boolean esVariable(char c) {
        return (esLatina(c) || esGriega(c)) && !esConstante(c);
    }

    /** "x" para las latinas; "\alpha " (con espacio, para que no se pegue a la letra siguiente) para las griegas. */
    public static String latex(char c) {
        if (!esGriega(c)) {
            return String.valueOf(c);
        }
        String nombre = GRIEGAS_LATEX[c - 'α'];
        // ο no tiene comando en LaTeX: se escribe como la o latina.
        return nombre.equals("omicron") ? "o" : "\\" + nombre + " ";
    }
}
