package pe.mathnova.domain.expresion;

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

    public static boolean esConstante(char c) {
        return c == 'e' || c == 'i' || c == 'π';
    }

    public static boolean esVariable(char c) {
        return (esLatina(c) || esGriega(c)) && !esConstante(c);
    }

    public static String latex(char c) {
        if (!esGriega(c)) {
            return String.valueOf(c);
        }
        String nombre = GRIEGAS_LATEX[c - 'α'];
        return nombre.equals("omicron") ? "o" : "\\" + nombre + " ";
    }
}
