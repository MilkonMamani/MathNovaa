package pe.mathnova.presentation.editor;

import java.util.List;

/**
 * Plantillas LaTeX que insertan el teclado (RF72) y las sugerencias (RF70). En MathLive, {@code #?}
 * es una casilla vacía y {@code #@} es lo que está antes del cursor (para □², □ⁿ y □₍□₎).
 */
public final class Plantillas {

    public static final String CUADRADO = "#@^{2}";
    public static final String POTENCIA = "#@^{#?}";
    public static final String SUBINDICE = "#@_{#?}";
    public static final String RAIZ = "\\sqrt{#?}";
    public static final String RAIZ_N = "\\sqrt[#?]{#?}";
    public static final String FRACCION = "\\frac{#?}{#?}";
    public static final String EXP = "e^{#?}";

    public static final String SEN = "\\operatorname{sen}\\left(#?\\right)";
    public static final String COS = "\\cos\\left(#?\\right)";
    public static final String TAN = "\\tan\\left(#?\\right)";
    public static final String ARCSEN = "\\operatorname{sen}^{-1}\\left(#?\\right)";
    public static final String ARCCOS = "\\cos^{-1}\\left(#?\\right)";
    public static final String ARCTAN = "\\tan^{-1}\\left(#?\\right)";
    public static final String LN = "\\ln\\left(#?\\right)";
    public static final String LOG10 = "\\log_{10}\\left(#?\\right)";
    public static final String LOG_B = "\\log_{#?}\\left(#?\\right)";

    public static final String LIMITE = "\\lim_{x\\to#?}#?";
    /** Sprint 8: lim((x, y) → (a, b), f). */
    public static final String LIMITE_DOBLE = "\\lim_{(x,y)\\to(#?,#?)}#?";
    /** Sprint 8: extremos(f), o extremos(f; g = c) con Lagrange. */
    public static final String EXTREMOS = "\\operatorname{extremos}\\left(#?\\right)";
    public static final String DERIVADA = "\\frac{d}{dx}\\left(#?\\right)";
    public static final String PARCIAL = "\\frac{\\partial}{\\partial x}\\left(#?\\right)";
    /** Sprint 7: ∇f, que se lee "grad(…)"; el punto es opcional: ∇(f, (1, 2)). */
    public static final String GRADIENTE = "\\nabla\\left(#?\\right)";
    /** Sprint 7: D_u f(P), que se lee "Du(f, (v), (P))". */
    public static final String DIRECCIONAL = "D_{\\vec u}\\left(#?,\\left(#?\\right),\\left(#?\\right)\\right)";
    /** RF69: las casillas de los límites son opcionales; vacías, la integral es indefinida. */
    public static final String INTEGRAL = "\\int_{#?}^{#?}#?\\,dx";
    public static final String INTEGRAL_DOBLE = "\\iint_{[#?,#?]\\times[#?,#?]}#?\\,dx\\,dy";
    public static final String INTEGRAL_TRIPLE = "\\iiint_{[#?,#?]\\times[#?,#?]\\times[#?,#?]}#?\\,dx\\,dy\\,dz";

    /** Una sugerencia: al escribir {@code palabra} se ofrece {@code etiqueta}, que inserta {@code latex}. */
    public record Sugerencia(String palabra, String etiqueta, String latex) {
    }

    /** RF70: lim, der, grad, Du, extr, int, sen, cos, tan, ln, log, raiz y frac. */
    public static final List<Sugerencia> SUGERENCIAS = List.of(
            new Sugerencia("lim", "lím  límite", LIMITE),
            new Sugerencia("lim", "lím₍ₓ,ᵧ₎  límite doble", LIMITE_DOBLE),
            new Sugerencia("extr", "extremos(f)  puntos críticos y extremos", EXTREMOS),
            new Sugerencia("der", "d/dx  derivada", DERIVADA),
            new Sugerencia("der", "∂/∂x  derivada parcial", PARCIAL),
            new Sugerencia("grad", "∇f  gradiente", GRADIENTE),
            new Sugerencia("Du", "Dᵤf  derivada direccional", DIRECCIONAL),
            new Sugerencia("int", "∫  integral (límites opcionales)", INTEGRAL),
            new Sugerencia("int", "∬  integral doble", INTEGRAL_DOBLE),
            new Sugerencia("int", "∭  integral triple", INTEGRAL_TRIPLE),
            new Sugerencia("sen", "sen(□)  seno", SEN),
            new Sugerencia("sen", "sen⁻¹(□)  arcoseno", ARCSEN),
            new Sugerencia("cos", "cos(□)  coseno", COS),
            new Sugerencia("cos", "cos⁻¹(□)  arcocoseno", ARCCOS),
            new Sugerencia("tan", "tan(□)  tangente", TAN),
            new Sugerencia("tan", "tan⁻¹(□)  arcotangente", ARCTAN),
            new Sugerencia("ln", "ln(□)  logaritmo natural", LN),
            new Sugerencia("log", "log₁₀(□)  logaritmo decimal", LOG10),
            new Sugerencia("log", "log□(□)  logaritmo en base b", LOG_B),
            new Sugerencia("raiz", "√□  raíz cuadrada", RAIZ),
            new Sugerencia("raiz", "ⁿ√□  raíz enésima", RAIZ_N),
            new Sugerencia("frac", "□/□  fracción", FRACCION));

    private Plantillas() {
    }

    /** La lista como JSON para editor.html: [{"palabra": …, "latex": …}]. */
    static String sugerenciasJson() {
        StringBuilder sb = new StringBuilder("[");
        for (Sugerencia s : SUGERENCIAS) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append("{\"palabra\":").append(Js.cadena(s.palabra()))
                    .append(",\"latex\":").append(Js.cadena(s.latex())).append('}');
        }
        return sb.append(']').toString();
    }
}
