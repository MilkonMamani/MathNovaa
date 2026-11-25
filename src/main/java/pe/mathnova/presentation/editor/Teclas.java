package pe.mathnova.presentation.editor;

import java.util.ArrayList;
import java.util.List;

public final class Teclas {

    public enum Tipo {
        NORMAL,
        ESPECIAL,
        ENTER,
        CONSTANTE,
        DESACTIVADA
    }

    public enum Accion {
        INSERTAR,
        ESCRIBIR,
        BORRAR, IZQUIERDA, DERECHA, ENTER,
        ANS,
        MAYUSCULA,
        CAMBIAR_CAPA,
        NADA
    }

    public record Tecla(String etiqueta, Accion accion, String valor, Tipo tipo, String ayuda) {

        static Tecla escribir(String s) {
            return new Tecla(s, Accion.ESCRIBIR, s, Tipo.NORMAL, null);
        }

        static Tecla escribir(String etiqueta, String s) {
            return new Tecla(etiqueta, Accion.ESCRIBIR, s, Tipo.NORMAL, null);
        }

        static Tecla latex(String etiqueta, String latex) {
            return new Tecla(etiqueta, Accion.INSERTAR, latex, Tipo.NORMAL, null);
        }

        static Tecla latex(String etiqueta, String latex, String ayuda) {
            return new Tecla(etiqueta, Accion.INSERTAR, latex, Tipo.NORMAL, ayuda);
        }

        static Tecla constante(String etiqueta, String latex) {
            return new Tecla(etiqueta, Accion.INSERTAR, latex, Tipo.CONSTANTE, "constante");
        }

        static Tecla desactivada(String etiqueta, String ayuda) {
            return new Tecla(etiqueta, Accion.NADA, null, Tipo.DESACTIVADA, ayuda);
        }

        static Tecla especial(String etiqueta, Accion accion) {
            return new Tecla(etiqueta, accion, null, Tipo.ESPECIAL, null);
        }
    }

    public record Bloque(List<List<Tecla>> filas) {

        public int columnas() {
            return filas.stream().mapToInt(List::size).max().orElse(0);
        }
    }

    public record Pestana(String titulo, Bloque izquierda, Bloque derecha) {
    }

    static final Tecla BORRAR = Tecla.especial("⌫", Accion.BORRAR);
    static final Tecla IZQUIERDA = Tecla.especial("←", Accion.IZQUIERDA);
    static final Tecla DERECHA = Tecla.especial("→", Accion.DERECHA);
    static final Tecla ENTER = new Tecla("Enter", Accion.ENTER, null, Tipo.ENTER, null);
    static final Tecla ANS = new Tecla("ans", Accion.ANS, null, Tipo.NORMAL, "respuesta anterior");
    static final Tecla PI = Tecla.constante("π", "\\pi");
    static final Tecla E = Tecla.constante("e", "e");
    static final Tecla I = Tecla.constante("i", "i");
    static final String FUERA_DE_ALCANCE = "fuera del alcance";

    private static List<Tecla> fila(Tecla... t) {
        return List.of(t);
    }

    @SafeVarargs
    private static Bloque bloque(List<Tecla>... filas) {
        return new Bloque(List.of(filas));
    }

    public static final Pestana NUMEROS = new Pestana("123",
            bloque(
                    fila(Tecla.escribir("x"), Tecla.escribir("y"), PI, E),
                    fila(Tecla.latex("□²", Plantillas.CUADRADO, "al cuadrado"),
                            Tecla.latex("□ⁿ", Plantillas.POTENCIA, "potencia"),
                            Tecla.latex("√□", Plantillas.RAIZ, "raíz cuadrada"),
                            Tecla.latex("□/□", Plantillas.FRACCION, "fracción")),
                    fila(Tecla.escribir("<"), Tecla.escribir(">"), Tecla.escribir("("), Tecla.escribir(")")),
                    fila(ANS, Tecla.escribir(","), Tecla.latex("≤", "\\le"), Tecla.latex("≥", "\\ge"))),
            bloque(
                    fila(Tecla.escribir("7"), Tecla.escribir("8"), Tecla.escribir("9"),
                            Tecla.latex("×", "\\cdot"), Tecla.latex("÷", "\\frac{#@}{#?}", "dividir")),
                    fila(Tecla.escribir("4"), Tecla.escribir("5"), Tecla.escribir("6"),
                            Tecla.escribir("+"), Tecla.escribir("−", "-")),
                    fila(Tecla.escribir("1"), Tecla.escribir("2"), Tecla.escribir("3"),
                            Tecla.escribir("="), BORRAR),
                    fila(Tecla.escribir("0"), Tecla.escribir("."), IZQUIERDA, DERECHA, ENTER)));

    public static final Pestana FUNCIONES = new Pestana("f(x)",
            bloque(
                    fila(Tecla.latex("sen", Plantillas.SEN), Tecla.latex("cos", Plantillas.COS),
                            Tecla.latex("tan", Plantillas.TAN)),
                    fila(Tecla.latex("sen⁻¹", Plantillas.ARCSEN), Tecla.latex("cos⁻¹", Plantillas.ARCCOS),
                            Tecla.latex("tan⁻¹", Plantillas.ARCTAN)),
                    fila(Tecla.latex("ln", Plantillas.LN), Tecla.latex("log₁₀", Plantillas.LOG10),
                            Tecla.latex("log□", Plantillas.LOG_B, "logaritmo en base b")),
                    fila(Tecla.latex("e^□", Plantillas.EXP), Tecla.latex("ⁿ√□", Plantillas.RAIZ_N, "raíz enésima"),
                            Tecla.desactivada("|□|", FUERA_DE_ALCANCE))),
            bloque(
                    fila(Tecla.latex("lím", Plantillas.LIMITE, "límite"),
                            Tecla.latex("d/dx", Plantillas.DERIVADA, "derivada"),
                            Tecla.latex("∂/∂x", Plantillas.PARCIAL, "derivada parcial"),
                            Tecla.latex("∫", Plantillas.INTEGRAL, "integral: los límites son opcionales"),
                            Tecla.latex("∬", Plantillas.INTEGRAL_DOBLE, "integral doble")),
                    fila(Tecla.latex("∭", Plantillas.INTEGRAL_TRIPLE, "integral triple"),
                            Tecla.latex("∞", "\\infty"), I,
                            Tecla.latex("lím(x,y)", Plantillas.LIMITE_DOBLE, "límite doble"),
                            Tecla.latex("extr", Plantillas.EXTREMOS, "puntos críticos y extremos")),
                    fila(Tecla.latex("□₍□₎", Plantillas.SUBINDICE, "subíndice"),
                            Tecla.latex("∇f", Plantillas.GRADIENTE, "gradiente"),
                            Tecla.latex("Dᵤf", Plantillas.DIRECCIONAL, "derivada direccional"),
                            Tecla.latex("∂", Plantillas.PARCIAL_EN, "derivada parcial respecto de la variable que escribas"), BORRAR),
                    fila(Tecla.latex("→", "\\to", "tiende a"), Tecla.escribir(","), IZQUIERDA, DERECHA, ENTER)));

    private static List<Tecla> letras(String s) {
        List<Tecla> r = new ArrayList<>();
        for (char c : s.toCharArray()) {
            String l = String.valueOf(c);
            r.add(switch (l) {
                case "e" -> E;
                case "i" -> I;
                case "ñ" -> Tecla.desactivada("ñ", "no es una variable de MathNova");
                default -> Tecla.escribir(l);
            });
        }
        return r;
    }

    private static List<Tecla> griegas(String s) {
        List<Tecla> r = new ArrayList<>();
        for (char c : s.toCharArray()) {
            String l = String.valueOf(c);
            r.add(switch (l) {
                case "π" -> PI;
                case "ς" -> Tecla.desactivada("ς", "no es una variable de MathNova");
                case "ο" -> Tecla.escribir("ο");
                default -> Tecla.latex(l, LATEX_GRIEGAS.get(l));
            });
        }
        return r;
    }

    private static final java.util.Map<String, String> LATEX_GRIEGAS = java.util.Map.ofEntries(
            java.util.Map.entry("α", "\\alpha"), java.util.Map.entry("β", "\\beta"), java.util.Map.entry("γ", "\\gamma"),
            java.util.Map.entry("δ", "\\delta"), java.util.Map.entry("ε", "\\varepsilon"), java.util.Map.entry("ζ", "\\zeta"),
            java.util.Map.entry("η", "\\eta"), java.util.Map.entry("θ", "\\theta"), java.util.Map.entry("ι", "\\iota"),
            java.util.Map.entry("κ", "\\kappa"), java.util.Map.entry("λ", "\\lambda"), java.util.Map.entry("μ", "\\mu"),
            java.util.Map.entry("ν", "\\nu"), java.util.Map.entry("ξ", "\\xi"), java.util.Map.entry("ο", "\\omicron"),
            java.util.Map.entry("ρ", "\\rho"), java.util.Map.entry("σ", "\\sigma"), java.util.Map.entry("τ", "\\tau"),
            java.util.Map.entry("υ", "\\upsilon"), java.util.Map.entry("φ", "\\varphi"), java.util.Map.entry("χ", "\\chi"),
            java.util.Map.entry("ψ", "\\psi"), java.util.Map.entry("ω", "\\omega"));

    static final Tecla MAYUSCULA = Tecla.especial("⇧", Accion.MAYUSCULA);
    static final Tecla ESPACIO = new Tecla("espacio", Accion.ESCRIBIR, " ", Tipo.NORMAL, null);

    private static List<Tecla> filaInferior(String cambio) {
        return fila(Tecla.especial(cambio, Accion.CAMBIAR_CAPA), Tecla.escribir(","), Tecla.escribir("("),
                Tecla.escribir(")"), ESPACIO, IZQUIERDA, DERECHA, ENTER);
    }

    public static final Pestana LETRAS = new Pestana("ABC",
            new Bloque(List.of(
                    letras("qwertyuiop"),
                    letras("asdfghjklñ"),
                    concat(List.of(MAYUSCULA), letras("zxcvbnm"), List.of(BORRAR)),
                    filaInferior("αβγ"))),
            null);

    public static final Pestana GRIEGAS = new Pestana("αβγ",
            new Bloque(List.of(
                    griegas("αβγδεζηθικ"),
                    griegas("λμνξοπρσςτ"),
                    concat(griegas("υφχψω"), List.of(BORRAR)),
                    filaInferior("ABC"))),
            null);

    public static final Pestana SIMBOLOS = new Pestana("#&¬",
            bloque(
                    fila(Tecla.latex("∞", "\\infty"), Tecla.latex("≠", "\\ne"), Tecla.latex("≈", "\\approx"),
                            Tecla.latex("≤", "\\le"), Tecla.latex("≥", "\\ge"), Tecla.latex("→", "\\to"),
                            Tecla.latex("∂", Plantillas.PARCIAL_EN, "derivada parcial respecto de la variable que escribas"),
                            Tecla.latex("∈", "\\in"),
                            Tecla.latex("[", "\\lbrack"), Tecla.latex("]", "\\rbrack")),
                    fila(Tecla.latex("±", "\\pm"), Tecla.latex("·", "\\cdot"), Tecla.latex("∘", "\\circ"),
                            Tecla.latex("°", "\\degree"), Tecla.latex("′", "\\prime"), Tecla.latex("″", "\\doubleprime"),
                            Tecla.escribir("|"), Tecla.latex("∠", "\\angle"),
                            Tecla.latex("{", "\\lbrace"), Tecla.latex("}", "\\rbrace")),
                    fila(Tecla.escribir("("), Tecla.escribir(")"), Tecla.latex("⟨", "\\langle"),
                            Tecla.latex("⟩", "\\rangle"), Tecla.escribir(";"), Tecla.escribir(":"),
                            Tecla.escribir("'"), Tecla.escribir("\""), Tecla.latex("%", "\\%"), BORRAR),
                    fila(ANS, Tecla.escribir(","), Tecla.escribir("."), Tecla.escribir("="), Tecla.escribir("<"),
                            Tecla.escribir(">"), Tecla.escribir("!"), IZQUIERDA, DERECHA, ENTER)),
            null);

    public static final List<Pestana> PESTANAS = List.of(NUMEROS, FUNCIONES, LETRAS, SIMBOLOS);

    @SafeVarargs
    private static List<Tecla> concat(List<Tecla>... partes) {
        List<Tecla> r = new ArrayList<>();
        for (List<Tecla> p : partes) {
            r.addAll(p);
        }
        return List.copyOf(r);
    }

    private Teclas() {
    }
}
