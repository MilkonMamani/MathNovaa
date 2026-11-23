package pe.mathnova.application.editor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Convierte el LaTeX del editor visual (MathLive, HU36) al texto que lee el parser (sección 9):
 * {@code \frac{x+1}{x-1}} es "(x + 1)/(x - 1)" y {@code 3\int_{3}^{6}3x\,dx} es "3·∫(3x, x, 3, 6)".
 * El parser no cambia: este traductor solo escribe texto.
 *
 * <p>Pone paréntesis y "·" donde hace falta para que el parser arme el mismo árbol que se ve en el
 * editor: una fracción junto a otro factor va entre paréntesis ("(1/2)x"), un exponente o un
 * denominador compuesto también ("x^(n + 1)", "1/(2x)"), y antes de un número, de una raíz cerrada,
 * de una función o de una operación va "·" ("2·3", "√2·x", "x·sen(x)", "3·∫(…)").
 *
 * <p>RN45: las letras sueltas siguen siendo variables. Si dos letras seguidas forman un nombre que el
 * parser leería como función ("l" "n"), se separan con un espacio ("l n").
 *
 * <p>Una casilla vacía ({@code \placeholder{}}) se escribe "□", que el parser rechaza con "Completa
 * la casilla vacía". En una integral simple las dos casillas de los límites son opcionales (RF69): si
 * las dos están vacías, la integral es indefinida.
 */
public final class TraductorLatex {

    /** El LaTeX tiene algo que MathNova no sabe escribir como texto. */
    public static final class LatexNoValido extends RuntimeException {
        public LatexNoValido(String mensaje) {
            super(mensaje);
        }
    }

    public static final String CASILLA = "□";

    private TraductorLatex() {
    }

    public static String traducir(String latex) {
        if (latex == null || latex.isBlank()) {
            return "";
        }
        Lectura l = new Lectura(normalizar(Tok.leer(latex)));
        List<Pieza> piezas = l.secuencia(k -> false);
        return emitir(piezas).strip();
    }

    /**
     * false si queda una casilla obligatoria vacía. Las dos casillas de los límites de una integral
     * simple son opcionales cuando están vacías las dos (RF69): la integral queda indefinida.
     */
    public static boolean completo(String latex) {
        if (latex == null || latex.isBlank()) {
            return true;
        }
        List<Tok> t = normalizar(Tok.leer(latex));
        boolean[] opcional = new boolean[t.size()];
        for (int i = 0; i < t.size(); i++) {
            if (!t.get(i).esCmd("int")) {
                continue;
            }
            int j = i + 1;
            int[] inferior = null;
            int[] superior = null;
            while (t.get(j).tipo() == Tipo.SUB || t.get(j).tipo() == Tipo.SUP) {
                boolean sub = t.get(j).tipo() == Tipo.SUB;
                int fin = finDeArgumento(t, j + 1);
                if (sub) {
                    inferior = new int[] {j + 1, fin};
                } else {
                    superior = new int[] {j + 1, fin};
                }
                j = fin;
            }
            if (inferior != null && superior != null && soloCasilla(t, inferior) && soloCasilla(t, superior)) {
                for (int[] r : List.of(inferior, superior)) {
                    for (int x = r[0]; x < r[1]; x++) {
                        opcional[x] = true;
                    }
                }
            }
        }
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i).esCmd("placeholder") && !opcional[i]) {
                return false;
            }
        }
        return true;
    }

    /** Índice justo después del argumento que empieza en i: {…}, \placeholder{} o un token. */
    private static int finDeArgumento(List<Tok> t, int i) {
        Tok x = t.get(i);
        if (x.tipo() == Tipo.ABRE) {
            int nivel = 0;
            for (int j = i; j < t.size(); j++) {
                if (t.get(j).tipo() == Tipo.ABRE) {
                    nivel++;
                } else if (t.get(j).tipo() == Tipo.CIERRA && --nivel == 0) {
                    return j + 1;
                }
            }
            return t.size() - 1;
        }
        if (x.esCmd("placeholder") && t.get(i + 1).tipo() == Tipo.ABRE) {
            return finDeArgumento(t, i + 1);
        }
        return x.tipo() == Tipo.FIN ? i : i + 1;
    }

    private static boolean soloCasilla(List<Tok> t, int[] r) {
        List<Tok> dentro = new ArrayList<>(t.subList(r[0], r[1]));
        if (!dentro.isEmpty() && dentro.getFirst().tipo() == Tipo.ABRE && dentro.getLast().tipo() == Tipo.CIERRA) {
            dentro = dentro.subList(1, dentro.size() - 1);
        }
        if (dentro.isEmpty()) {
            return true;
        }
        return dentro.getFirst().esCmd("placeholder")
                && dentro.stream().skip(1).allMatch(x -> x.tipo() == Tipo.ABRE || x.tipo() == Tipo.CIERRA);
    }

    // ================================================================== tokens

    private enum Tipo { CMD, CHAR, ABRE, CIERRA, SUP, SUB, FIN }

    private record Tok(Tipo tipo, String v) {

        boolean esChar(char c) {
            return tipo == Tipo.CHAR && v.charAt(0) == c;
        }

        boolean esCmd(String... nombres) {
            if (tipo != Tipo.CMD) {
                return false;
            }
            for (String n : nombres) {
                if (v.equals(n)) {
                    return true;
                }
            }
            return false;
        }

        /** MathLive guarda \operatorname{sen} como \operatorname{\mathrm{sen}}. */
        static final java.util.regex.Pattern ESTILO_DOBLE =
                java.util.regex.Pattern.compile("\\\\(?:operatorname|mathrm)\\{\\\\(?:mathrm|text|textrm)\\{([A-Za-z]+)\\}\\}");

        static List<Tok> leer(String original) {
            String s = ESTILO_DOBLE.matcher(original).replaceAll("\\\\operatorname{$1}");
            List<Tok> t = new ArrayList<>();
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '\\') {
                    int j = i + 1;
                    while (j < s.length() && Character.isLetter(s.charAt(j)) && s.charAt(j) < 128) {
                        j++;
                    }
                    if (j == i + 1) {
                        // \, \; \{ \} \! y compañía: un solo carácter.
                        String nombre = i + 1 < s.length() ? String.valueOf(s.charAt(i + 1)) : " ";
                        t.add(new Tok(Tipo.CMD, nombre));
                        i += 2;
                    } else {
                        t.add(new Tok(Tipo.CMD, s.substring(i + 1, j)));
                        i = j;
                    }
                    continue;
                }
                switch (c) {
                    case '{' -> t.add(new Tok(Tipo.ABRE, "{"));
                    case '}' -> t.add(new Tok(Tipo.CIERRA, "}"));
                    case '^' -> t.add(new Tok(Tipo.SUP, "^"));
                    case '_' -> t.add(new Tok(Tipo.SUB, "_"));
                    case '~', '&' -> { }
                    default -> {
                        if (!Character.isWhitespace(c)) {
                            t.add(new Tok(Tipo.CHAR, String.valueOf(c)));
                        }
                    }
                }
                i++;
            }
            return t;
        }
    }

    private static final Set<String> ESPACIOS = Set.of(",", ";", ":", "!", " ", ">", "quad", "qquad", "enspace",
            "thinspace", "medspace", "thickspace", "negthinspace", "negmedspace", "negthickspace",
            "displaystyle", "textstyle", "limits", "nolimits", "mathstrut", "strut");

    private static final Set<String> ESTILOS = Set.of("mathrm", "operatorname", "text", "textrm", "mathit",
            "mathsf", "mathbf", "mathnormal", "textit", "boldsymbol", "bm");

    /** Nombre del comando → nombre canónico de la función en el parser. */
    private static final Map<String, String> FUNCIONES = Map.ofEntries(
            Map.entry("sin", "sen"), Map.entry("sen", "sen"), Map.entry("cos", "cos"),
            Map.entry("tan", "tan"), Map.entry("tg", "tan"), Map.entry("cot", "cot"), Map.entry("cotg", "cot"),
            Map.entry("sec", "sec"), Map.entry("csc", "csc"), Map.entry("cosec", "csc"),
            Map.entry("arcsin", "arcsen"), Map.entry("arcsen", "arcsen"), Map.entry("arccos", "arccos"),
            Map.entry("arctan", "arctan"), Map.entry("arctg", "arctan"),
            Map.entry("ln", "ln"), Map.entry("log", "log"), Map.entry("exp", "exp"));

    private static final Map<String, String> INVERSAS = Map.of("sen", "arcsen", "cos", "arccos", "tan", "arctan");

    private static final Map<String, Character> GRIEGAS = Map.ofEntries(
            Map.entry("alpha", 'α'), Map.entry("beta", 'β'), Map.entry("gamma", 'γ'), Map.entry("delta", 'δ'),
            Map.entry("epsilon", 'ε'), Map.entry("varepsilon", 'ε'), Map.entry("zeta", 'ζ'), Map.entry("eta", 'η'),
            Map.entry("theta", 'θ'), Map.entry("vartheta", 'θ'), Map.entry("iota", 'ι'), Map.entry("kappa", 'κ'),
            Map.entry("lambda", 'λ'), Map.entry("mu", 'μ'), Map.entry("nu", 'ν'), Map.entry("xi", 'ξ'),
            Map.entry("omicron", 'ο'), Map.entry("pi", 'π'), Map.entry("rho", 'ρ'), Map.entry("varrho", 'ρ'),
            Map.entry("sigma", 'σ'), Map.entry("varsigma", 'ς'), Map.entry("tau", 'τ'), Map.entry("upsilon", 'υ'),
            Map.entry("phi", 'φ'), Map.entry("varphi", 'φ'), Map.entry("chi", 'χ'), Map.entry("psi", 'ψ'),
            Map.entry("omega", 'ω'));

    private static final Map<String, String> SIMBOLOS = Map.ofEntries(
            Map.entry("cdot", "·"), Map.entry("times", "·"), Map.entry("ast", "·"), Map.entry("div", "/"),
            Map.entry("le", "≤"), Map.entry("leq", "≤"), Map.entry("leqslant", "≤"),
            Map.entry("ge", "≥"), Map.entry("geq", "≥"), Map.entry("geqslant", "≥"),
            Map.entry("ne", "≠"), Map.entry("neq", "≠"), Map.entry("lt", "<"), Map.entry("gt", ">"),
            Map.entry("approx", "≈"), Map.entry("pm", "±"), Map.entry("mp", "∓"),
            Map.entry("to", "→"), Map.entry("rightarrow", "→"), Map.entry("longrightarrow", "→"),
            Map.entry("in", "∈"), Map.entry("circ", "∘"), Map.entry("degree", "°"), Map.entry("prime", "′"),
            Map.entry("doubleprime", "″"), Map.entry("angle", "∠"), Map.entry("%", "%"), Map.entry("colon", ":"),
            Map.entry("vert", "|"), Map.entry("mid", "|"), Map.entry("ldots", "…"), Map.entry("cdots", "…"),
            Map.entry("lbrace", "{"), Map.entry("rbrace", "}"), Map.entry("langle", "⟨"), Map.entry("rangle", "⟩"),
            Map.entry("#", "#"), Map.entry("&", "&"), Map.entry("_", "_"), Map.entry("lnot", "¬"), Map.entry("neg", "¬"),
            // RF62: el parser los rechaza con su mensaje de "fuera del alcance".
            Map.entry("sum", "Σ"), Map.entry("prod", "∏"), Map.entry("oint", "∮"));

    /**
     * Quita espacios y estilos, y deja cada nombre en una forma: {@code \operatorname{sen}} es
     * {@code \sen}, {@code \mathrm{d}} y {@code \differentialD} son la letra d, {@code \exponentialE}
     * es e, {@code \mleft} es {@code \left} y {@code \dfrac} es {@code \frac}.
     */
    private static List<Tok> normalizar(List<Tok> t) {
        List<Tok> r = new ArrayList<>();
        for (int i = 0; i < t.size(); i++) {
            Tok x = t.get(i);
            if (x.tipo() == Tipo.CMD) {
                String n = x.v();
                if (ESPACIOS.contains(n)) {
                    continue;
                }
                if (ESTILOS.contains(n) && i + 1 < t.size() && t.get(i + 1).tipo() == Tipo.ABRE) {
                    int j = i + 2;
                    StringBuilder palabra = new StringBuilder();
                    while (j < t.size() && t.get(j).tipo() == Tipo.CHAR && Character.isLetter(t.get(j).v().charAt(0))) {
                        palabra.append(t.get(j).v());
                        j++;
                    }
                    if (j < t.size() && t.get(j).tipo() == Tipo.CIERRA && !palabra.isEmpty()) {
                        String p = palabra.toString();
                        if (FUNCIONES.containsKey(p)) {
                            r.add(new Tok(Tipo.CMD, p));
                        } else {
                            for (char c : p.toCharArray()) {
                                r.add(new Tok(Tipo.CHAR, String.valueOf(c)));
                            }
                        }
                        i = j;
                        continue;
                    }
                    // Estilo sobre algo que no es una palabra: se ignora el estilo y queda el grupo.
                    continue;
                }
                if (ESTILOS.contains(n)) {
                    continue;
                }
                switch (n) {
                    case "differentialD", "dd" -> r.add(new Tok(Tipo.CHAR, "d"));
                    case "exponentialE" -> r.add(new Tok(Tipo.CHAR, "e"));
                    case "imaginaryI" -> r.add(new Tok(Tipo.CHAR, "i"));
                    case "mleft", "bigl", "Bigl", "big", "Big" -> r.add(new Tok(Tipo.CMD, "left"));
                    case "mright", "bigr", "Bigr" -> r.add(new Tok(Tipo.CMD, "right"));
                    case "dfrac", "tfrac", "cfrac" -> r.add(new Tok(Tipo.CMD, "frac"));
                    case "lbrack" -> r.add(new Tok(Tipo.CHAR, "["));
                    case "rbrack" -> r.add(new Tok(Tipo.CHAR, "]"));
                    case "{" -> r.add(new Tok(Tipo.CHAR, "{"));
                    case "}" -> r.add(new Tok(Tipo.CHAR, "}"));
                    default -> r.add(x);
                }
            } else if (x.tipo() == Tipo.CHAR) {
                r.add(switch (x.v()) {
                    case "−" -> new Tok(Tipo.CHAR, "-");
                    case "×", "*", "∙", "⋅" -> new Tok(Tipo.CMD, "cdot");
                    case "÷" -> new Tok(Tipo.CHAR, "/");
                    default -> x;
                });
            } else {
                r.add(x);
            }
        }
        r.add(new Tok(Tipo.FIN, ""));
        return r;
    }

    // ================================================================== piezas

    private enum Clase {
        /** Número, grupo entre paréntesis, potencia, ∞, □. */
        VALOR,
        /** Una letra sola: variable o constante. */
        LETRA,
        /** sen(x), log_2(x): siempre con paréntesis. */
        LLAMADA,
        FRACCION,
        RAIZ,
        /** lim, d/dx, ∂/∂x, ∫, ∬, ∭. */
        OPERACION,
        OPERADOR
    }

    private record Pieza(String texto, Clase clase, boolean grupo) {

        Pieza(String texto, Clase clase) {
            this(texto, clase, false);
        }

        boolean esValor() {
            return clase != Clase.OPERADOR;
        }

        boolean esOperador(String s) {
            return clase == Clase.OPERADOR && texto.equals(s);
        }

        boolean esNumero() {
            return clase == Clase.VALOR && texto.matches("\\d+(\\.\\d+)?");
        }
    }

    // ================================================================== lectura

    private static final class Lectura {

        private final List<Tok> t;
        private int k;

        Lectura(List<Tok> t) {
            this.t = t;
        }

        private Tok actual() {
            return t.get(k);
        }

        private Tok en(int i) {
            return i < t.size() ? t.get(i) : t.getLast();
        }

        private Tok avanzar() {
            Tok x = t.get(k);
            if (x.tipo() != Tipo.FIN) {
                k++;
            }
            return x;
        }

        private boolean fin() {
            return actual().tipo() == Tipo.FIN;
        }

        /** Lee piezas hasta el final, hasta un cierre que no le toca o hasta donde diga {@code para}. */
        List<Pieza> secuencia(IntPredicate para) {
            List<Pieza> p = new ArrayList<>();
            while (!fin() && !para.test(k)) {
                Tok x = actual();
                if (x.tipo() == Tipo.CIERRA) {
                    // Una llave que no abrió nadie: se ignora.
                    avanzar();
                    continue;
                }
                item(p, para);
            }
            return p;
        }

        private void item(List<Pieza> p, IntPredicate para) {
            Tok x = actual();
            switch (x.tipo()) {
                case CHAR -> caracter(p, para);
                case ABRE -> {
                    avanzar();
                    p.addAll(secuencia(i -> en(i).tipo() == Tipo.CIERRA));
                    if (actual().tipo() == Tipo.CIERRA) {
                        avanzar();
                    }
                }
                case SUP -> {
                    avanzar();
                    Pieza base = p.isEmpty() ? new Pieza(CASILLA, Clase.VALOR) : p.removeLast();
                    p.add(potencia(base, piezas(argumento())));
                }
                case SUB -> {
                    avanzar();
                    Pieza base = p.isEmpty() ? new Pieza(CASILLA, Clase.VALOR) : p.removeLast();
                    p.add(new Pieza(base.texto() + "_" + exponente(piezas(argumento())), Clase.VALOR));
                }
                case CMD -> comando(p, para);
                default -> avanzar();
            }
        }

        private void caracter(List<Pieza> p, IntPredicate para) {
            char c = actual().v().charAt(0);
            if (Character.isDigit(c) || c == '.') {
                p.add(numero());
            } else if (c == '(' || c == '[' || c == '{') {
                avanzar();
                p.add(grupo());
            } else if (c == 'D' && en(k + 1).tipo() == Tipo.SUB) {
                // Sprint 7: D_{\vec u}(f, (v), (P)) es Du(…)
                avanzar();
                avanzar();
                argumento();
                p.add(new Pieza("Du" + argumentoDeFuncion(), Clase.LLAMADA));
            } else if (Character.isLetter(c)) {
                avanzar();
                p.add(new Pieza(String.valueOf(c), Clase.LETRA));
            } else if (c == '∞') {
                avanzar();
                p.add(new Pieza("∞", Clase.VALOR));
            } else if (c == '□') {
                avanzar();
                p.add(new Pieza(CASILLA, Clase.VALOR));
            } else if (c == '∂') {
                avanzar();
                p.add(new Pieza("∂", Clase.VALOR));
            } else {
                avanzar();
                p.add(new Pieza(String.valueOf(c), Clase.OPERADOR));
            }
        }

        /** Dígitos con punto decimal; MathLive escribe la coma decimal como {,}. */
        private Pieza numero() {
            StringBuilder sb = new StringBuilder();
            while (true) {
                Tok x = actual();
                if (x.tipo() == Tipo.CHAR && (Character.isDigit(x.v().charAt(0)) || x.esChar('.'))) {
                    sb.append(x.v());
                    avanzar();
                } else if (x.tipo() == Tipo.ABRE && en(k + 1).esChar(',') && en(k + 2).tipo() == Tipo.CIERRA
                        && en(k + 3).tipo() == Tipo.CHAR && Character.isDigit(en(k + 3).v().charAt(0))) {
                    sb.append('.');
                    k += 3;
                } else {
                    break;
                }
            }
            String s = sb.toString();
            if (s.startsWith(".")) {
                s = "0" + s;
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
            return new Pieza(s, Clase.VALOR);
        }

        private static boolean esCierre(Tok x) {
            return x.esChar(')') || x.esChar(']') || x.esChar('}') || x.esCmd("right");
        }

        /** Después de "(", "[", "{" o \left: lee hasta el cierre. Los corchetes y llaves agrupan igual que el paréntesis. */
        private Pieza grupo() {
            List<Pieza> dentro = secuencia(i -> esCierre(en(i)));
            Tok cierre = actual();
            if (cierre.esCmd("right")) {
                avanzar();
                avanzar(); // el delimitador
            } else if (esCierre(cierre)) {
                avanzar();
            }
            return new Pieza("(" + emitir(dentro).strip() + ")", Clase.VALOR, true);
        }

        /** {...} o un solo token; un dígito solo es un dígito ({@code \frac12}). */
        private List<Tok> argumento() {
            List<Tok> r = new ArrayList<>();
            Tok x = actual();
            if (x.tipo() == Tipo.ABRE) {
                int nivel = 0;
                avanzar();
                while (!fin()) {
                    Tok y = actual();
                    if (y.tipo() == Tipo.ABRE) {
                        nivel++;
                    } else if (y.tipo() == Tipo.CIERRA) {
                        if (nivel == 0) {
                            avanzar();
                            break;
                        }
                        nivel--;
                    }
                    r.add(avanzar());
                }
                return r;
            }
            if (x.tipo() == Tipo.FIN) {
                return r;
            }
            if (x.tipo() == Tipo.CMD) {
                // Un comando con sus propios argumentos (\frac12, \placeholder{}): se copia entero.
                int desde = k;
                List<Pieza> descartadas = new ArrayList<>();
                item(descartadas, i -> false);
                return new ArrayList<>(t.subList(desde, k));
            }
            r.add(avanzar());
            return r;
        }

        private static List<Pieza> piezas(List<Tok> tokens) {
            List<Tok> copia = new ArrayList<>(tokens);
            copia.add(new Tok(Tipo.FIN, ""));
            return new Lectura(copia).secuencia(i -> false);
        }

        private static String texto(List<Tok> tokens) {
            return emitir(piezas(tokens)).strip();
        }

        private static boolean vacio(List<Tok> tokens) {
            return tokens.isEmpty() || (tokens.size() <= 3 && tokens.getFirst().esCmd("placeholder")
                    && tokens.stream().skip(1).allMatch(x -> x.tipo() != Tipo.CHAR && x.tipo() != Tipo.CMD));
        }

        // -------------------------------------------------------------- comandos

        private void comando(List<Pieza> p, IntPredicate para) {
            String n = avanzar().v();
            if (FUNCIONES.containsKey(n)) {
                p.add(funcion(FUNCIONES.get(n)));
                return;
            }
            Character griega = GRIEGAS.get(n);
            if (griega != null) {
                p.add(new Pieza(String.valueOf(griega), Clase.LETRA));
                return;
            }
            String simbolo = SIMBOLOS.get(n);
            if (simbolo != null) {
                p.add(new Pieza(simbolo, Clase.OPERADOR));
                return;
            }
            switch (n) {
                case "frac" -> p.add(fraccion(para));
                case "sqrt" -> p.add(raiz());
                case "int" -> p.add(integral(para));
                case "iint" -> p.add(integralMultiple(para, 2));
                case "iiint" -> p.add(integralMultiple(para, 3));
                case "lim" -> p.add(limite(para));
                case "left" -> {
                    Tok delim = avanzar();
                    if (delim.esChar('|') || delim.esCmd("vert", "lvert")) {
                        p.add(new Pieza("|", Clase.OPERADOR));
                    } else {
                        p.add(grupo());
                    }
                }
                case "right" -> avanzar();
                case "infty" -> p.add(new Pieza("∞", Clase.VALOR));
                case "partial" -> p.add(new Pieza("∂", Clase.VALOR));
                // Sprint 7: \nabla(f) y \nabla(f, (1, 2)) son grad(…)
                case "nabla" -> p.add(new Pieza("grad" + argumentoDeFuncion(), Clase.LLAMADA));
                case "placeholder" -> {
                    if (actual().esChar('[')) {
                        while (!fin() && !avanzar().esChar(']')) {
                            // id del placeholder
                        }
                    }
                    if (actual().tipo() == Tipo.ABRE) {
                        argumento();
                    }
                    p.add(new Pieza(CASILLA, Clase.VALOR));
                }
                default -> throw new LatexNoValido("MathNova no reconoce \\" + n);
            }
        }

        /** sen, cos, log_b, ln...: siempre con paréntesis; sen^{-1} es arcsen. */
        private Pieza funcion(String nombre) {
            String base = "";
            String potencia = "";
            while (actual().tipo() == Tipo.SUB || actual().tipo() == Tipo.SUP) {
                boolean sub = avanzar().tipo() == Tipo.SUB;
                List<Tok> arg = argumento();
                if (sub) {
                    String b = texto(arg);
                    base = nombre.equals("log") && !b.equals("10") ? "_" + (b.isEmpty() ? CASILLA : b) : "";
                } else {
                    String e = texto(arg);
                    if (e.equals("-1") && INVERSAS.containsKey(nombre)) {
                        nombre = INVERSAS.get(nombre);
                    } else {
                        potencia = "^" + exponente(piezas(arg));
                    }
                }
            }
            return new Pieza(nombre + base + potencia + argumentoDeFuncion(), Clase.LLAMADA);
        }

        /** "(…)" o, sin paréntesis, [número] letra [^n] como en el parser: \sin 2x es sen(2x). */
        private String argumentoDeFuncion() {
            Tok x = actual();
            if (x.esChar('(') || x.esChar('[')) {
                avanzar();
                return grupo().texto();
            }
            if (x.esCmd("left")) {
                avanzar();
                avanzar();
                return grupo().texto();
            }
            if (x.esCmd("placeholder")) {
                List<Pieza> p = new ArrayList<>();
                item(p, i -> false);
                return "(" + CASILLA + ")";
            }
            List<Pieza> p = new ArrayList<>();
            if (x.tipo() == Tipo.CHAR && Character.isDigit(x.v().charAt(0))) {
                p.add(numero());
            }
            Tok y = actual();
            boolean letra = (y.tipo() == Tipo.CHAR && Character.isLetter(y.v().charAt(0)))
                    || (y.tipo() == Tipo.CMD && GRIEGAS.containsKey(y.v()));
            if (letra) {
                item(p, i -> false);
                if (actual().tipo() == Tipo.SUP) {
                    item(p, i -> false);
                }
            }
            return "(" + (p.isEmpty() ? CASILLA : emitir(p).strip()) + ")";
        }

        private Pieza raiz() {
            String indice = "";
            if (actual().esChar('[')) {
                avanzar();
                List<Tok> idx = new ArrayList<>();
                while (!fin() && !actual().esChar(']')) {
                    idx.add(avanzar());
                }
                avanzar();
                indice = vacio(idx) ? CASILLA : texto(idx);
            }
            List<Pieza> rad = piezas(argumento());
            String r = rad.isEmpty() ? CASILLA : envolverSalvoAtomo(rad);
            String simbolo = switch (indice) {
                case "", "2" -> "√";
                case "3" -> "∛";
                default -> "√[" + indice + "]";
            };
            return new Pieza(simbolo + r, Clase.RAIZ);
        }

        // -------------------------------------------------------------- fracción y derivadas

        private Pieza fraccion(IntPredicate para) {
            List<Tok> num = argumento();
            List<Tok> den = argumento();
            Integer orden = ordenDeD(num);
            if (orden != null) {
                Character v = variableDeDx(den);
                if (v != null) {
                    int ordenAbajo = ordenAbajo(den);
                    String o = orden == 1 ? "" : String.valueOf(orden);
                    String oa = ordenAbajo == 1 ? "" : String.valueOf(ordenAbajo);
                    return new Pieza("d" + o + "/d" + v + oa + "(" + cuerpo(para) + ")", Clase.OPERACION);
                }
            }
            Integer ordenParcial = ordenDeParcial(num);
            if (ordenParcial != null) {
                List<Character> vars = variablesParciales(den);
                if (vars != null) {
                    StringBuilder sb = new StringBuilder("∂");
                    if (ordenParcial > 1) {
                        sb.append(ordenParcial);
                    }
                    sb.append('/');
                    vars.forEach(v -> sb.append('∂').append(v));
                    return new Pieza(sb + "(" + cuerpo(para) + ")", Clase.OPERACION);
                }
            }
            List<Pieza> n = piezas(num);
            List<Pieza> d = piezas(den);
            String tn = n.isEmpty() ? CASILLA : emitir(n).strip();
            String td = d.isEmpty() ? CASILLA : emitir(d).strip();
            if (numeradorCompuesto(n)) {
                tn = "(" + tn + ")";
            }
            if (!(d.size() == 1 && (d.getFirst().clase() == Clase.VALOR || d.getFirst().clase() == Clase.LETRA
                    || d.getFirst().clase() == Clase.LLAMADA || d.getFirst().clase() == Clase.RAIZ))) {
                td = d.isEmpty() ? td : "(" + td + ")";
            }
            return new Pieza(tn + "/" + td, Clase.FRACCION);
        }

        private static boolean numeradorCompuesto(List<Pieza> n) {
            if (n.isEmpty()) {
                return false;
            }
            if (n.getFirst().clase() == Clase.OPERADOR) {
                return true;
            }
            for (Pieza x : n) {
                if (x.clase() == Clase.OPERACION || x.clase() == Clase.FRACCION || (x.clase() == Clase.OPERADOR && !x.esOperador("·") && !x.esOperador("/"))) {
                    return true;
                }
            }
            return false;
        }

        /** d, d^2, d^{2} → orden; si no es eso, null. */
        private static Integer ordenDeD(List<Tok> num) {
            if (num.isEmpty() || !num.getFirst().esChar('d')) {
                return null;
            }
            if (num.size() == 1) {
                return 1;
            }
            Integer o = ordenTras(num, 1);
            return o != null && finDeOrden(num, 1) == num.size() ? o : null;
        }

        private static Integer ordenDeParcial(List<Tok> num) {
            if (num.isEmpty() || !num.getFirst().esCmd("partial")) {
                return null;
            }
            if (num.size() == 1) {
                return 1;
            }
            Integer o = ordenTras(num, 1);
            return o != null && finDeOrden(num, 1) == num.size() ? o : null;
        }

        /** En i hay ^n o ^{n}: devuelve n. */
        private static Integer ordenTras(List<Tok> x, int i) {
            if (i >= x.size() || x.get(i).tipo() != Tipo.SUP) {
                return null;
            }
            StringBuilder dig = new StringBuilder();
            if (i + 1 < x.size() && x.get(i + 1).tipo() == Tipo.ABRE) {
                int j = i + 2;
                while (j < x.size() && x.get(j).tipo() == Tipo.CHAR && Character.isDigit(x.get(j).v().charAt(0))) {
                    dig.append(x.get(j).v());
                    j++;
                }
                if (j >= x.size() || x.get(j).tipo() != Tipo.CIERRA) {
                    return null;
                }
            } else if (i + 1 < x.size() && x.get(i + 1).tipo() == Tipo.CHAR && Character.isDigit(x.get(i + 1).v().charAt(0))) {
                dig.append(x.get(i + 1).v());
            }
            return dig.isEmpty() ? null : Integer.valueOf(dig.toString());
        }

        private static int finDeOrden(List<Tok> x, int i) {
            if (i + 1 < x.size() && x.get(i + 1).tipo() == Tipo.ABRE) {
                int j = i + 2;
                while (x.get(j).tipo() != Tipo.CIERRA) {
                    j++;
                }
                return j + 1;
            }
            return i + 2;
        }

        /** dx o dx^2: la letra; si no es eso, null. */
        private static Character variableDeDx(List<Tok> den) {
            if (den.size() < 2 || !den.getFirst().esChar('d')) {
                return null;
            }
            Character v = letra(den.get(1));
            if (v == null) {
                return null;
            }
            if (den.size() == 2) {
                return v;
            }
            return ordenTras(den, 2) != null && finDeOrden(den, 2) == den.size() ? v : null;
        }

        private static int ordenAbajo(List<Tok> den) {
            Integer o = den.size() > 2 ? ordenTras(den, 2) : null;
            return o == null ? 1 : o;
        }

        /** ∂x ∂y^2 … → [x, y, y]; si no es eso, null. */
        private static List<Character> variablesParciales(List<Tok> den) {
            List<Character> vars = new ArrayList<>();
            int i = 0;
            while (i < den.size()) {
                if (!den.get(i).esCmd("partial") || i + 1 >= den.size()) {
                    return null;
                }
                Character v = letra(den.get(i + 1));
                if (v == null) {
                    return null;
                }
                i += 2;
                int veces = 1;
                Integer o = ordenTras(den, i);
                if (o != null) {
                    veces = o;
                    i = finDeOrden(den, i);
                }
                for (int j = 0; j < veces; j++) {
                    vars.add(v);
                }
            }
            return vars.isEmpty() ? null : vars;
        }

        private static Character letra(Tok x) {
            if (x.tipo() == Tipo.CHAR && Character.isLetter(x.v().charAt(0))) {
                return x.v().charAt(0);
            }
            if (x.tipo() == Tipo.CMD && GRIEGAS.containsKey(x.v())) {
                return GRIEGAS.get(x.v());
            }
            return null;
        }

        /** El cuerpo de lim, d/dx o ∂/∂x: lo que sigue, hasta "=", "," o el fin del grupo. */
        private String cuerpo(IntPredicate para) {
            List<Pieza> c = secuencia(para.or(this::esSeparador));
            return textoDeCuerpo(c);
        }

        private static String textoDeCuerpo(List<Pieza> c) {
            if (c.isEmpty()) {
                return CASILLA;
            }
            if (c.size() == 1 && c.getFirst().grupo()) {
                String s = c.getFirst().texto();
                return s.substring(1, s.length() - 1);
            }
            return emitir(c).strip();
        }

        private boolean esSeparador(int i) {
            Tok x = en(i);
            return x.esChar('=') || x.esChar(',') || x.esChar('<') || x.esChar('>')
                    || x.esCmd("le", "leq", "ge", "geq", "ne", "neq", "lt", "gt");
        }

        // -------------------------------------------------------------- límite

        private Pieza limite(IntPredicate para) {
            if (actual().tipo() != Tipo.SUB) {
                throw new LatexNoValido("Al límite le falta la variable y el punto: lim_{x→a}");
            }
            avanzar();
            List<Tok> sub = argumento();
            int flecha = -1;
            for (int i = 0; i < sub.size(); i++) {
                Tok x = sub.get(i);
                if (x.esCmd("to", "rightarrow", "longrightarrow") || x.esChar('→')) {
                    flecha = i;
                    break;
                }
            }
            String variable;
            List<Tok> punto;
            if (flecha < 0) {
                variable = vacio(sub) ? CASILLA : texto(sub);
                punto = List.of();
            } else {
                List<Tok> izq = sub.subList(0, flecha);
                variable = vacio(izq) ? CASILLA : texto(izq);
                punto = new ArrayList<>(sub.subList(flecha + 1, sub.size()));
            }
            String lado = "";
            int n = punto.size();
            if (n >= 2 && punto.get(n - 2).tipo() == Tipo.SUP && (punto.get(n - 1).esChar('+') || punto.get(n - 1).esChar('-'))) {
                lado = punto.get(n - 1).v();
                punto = punto.subList(0, n - 2);
            } else if (n >= 4 && punto.get(n - 4).tipo() == Tipo.SUP && punto.get(n - 3).tipo() == Tipo.ABRE
                    && (punto.get(n - 2).esChar('+') || punto.get(n - 2).esChar('-')) && punto.get(n - 1).tipo() == Tipo.CIERRA) {
                lado = punto.get(n - 2).v();
                punto = punto.subList(0, n - 4);
            }
            String p = vacio(punto) ? CASILLA : texto(punto);
            return new Pieza("lim(" + variable + "→" + p + lado + ", " + cuerpo(para) + ")", Clase.OPERACION);
        }

        // -------------------------------------------------------------- integrales

        /** En i empieza un diferencial "d x" que cierra el cuerpo de la integral. */
        private boolean esDiferencial(int i) {
            if (!en(i).esChar('d') || letra(en(i + 1)) == null) {
                return false;
            }
            Tok sig = en(i + 2);
            return sig.tipo() == Tipo.FIN || sig.tipo() == Tipo.CIERRA || esCierre(sig) || esDiferencial(i + 2)
                    || sig.esChar('=') || sig.esChar(',') || sig.esChar('+') || sig.esChar('-')
                    || sig.esChar('<') || sig.esChar('>') || sig.esCmd("le", "leq", "ge", "geq", "ne", "neq", "cdot");
        }

        private Pieza integral(IntPredicate para) {
            List<Tok> inferior = null;
            List<Tok> superior = null;
            while (actual().tipo() == Tipo.SUB || actual().tipo() == Tipo.SUP) {
                boolean sub = avanzar().tipo() == Tipo.SUB;
                if (sub) {
                    inferior = argumento();
                } else {
                    superior = argumento();
                }
            }
            List<Pieza> c = secuencia(para.or(this::esDiferencial).or(this::esSeparador));
            if (!esDiferencial(k)) {
                throw new LatexNoValido("A la integral le falta el diferencial, por ejemplo dx");
            }
            avanzar();
            char v = letra(avanzar());
            String cuerpo = c.isEmpty() ? "1" : textoDeCuerpo(c);
            boolean sinInferior = inferior == null || vacio(inferior);
            boolean sinSuperior = superior == null || vacio(superior);
            if (sinInferior && sinSuperior) {
                return new Pieza("∫(" + cuerpo + ", " + v + ")", Clase.OPERACION);
            }
            String a = sinInferior ? CASILLA : texto(inferior);
            String b = sinSuperior ? CASILLA : texto(superior);
            return new Pieza("∫(" + cuerpo + ", " + v + ", " + a + ", " + b + ")", Clase.OPERACION);
        }

        /** {@code \iint_{[a, b] \times [c, d]} f \, dx \, dy}, como lo dibuja ImpresoraLatex. */
        private Pieza integralMultiple(IntPredicate para, int n) {
            String simbolo = n == 2 ? "∬" : "∭";
            List<Tok> region = null;
            while (actual().tipo() == Tipo.SUB || actual().tipo() == Tipo.SUP) {
                boolean sub = avanzar().tipo() == Tipo.SUB;
                List<Tok> arg = argumento();
                if (sub) {
                    region = arg;
                }
            }
            if (region == null) {
                throw new LatexNoValido(simbolo + " necesita la región, por ejemplo [0, 1] × [0, 2]");
            }
            List<String[]> intervalos = intervalos(region);
            List<Pieza> c = secuencia(para.or(this::esDiferencial).or(this::esSeparador));
            List<Character> vars = new ArrayList<>();
            while (esDiferencial(k)) {
                avanzar();
                vars.add(letra(avanzar()));
            }
            if (vars.size() != n || intervalos.size() != n) {
                throw new LatexNoValido(simbolo + " necesita " + n + " intervalos y " + n + " diferenciales, como "
                        + (n == 2 ? "dx dy" : "dx dy dz"));
            }
            StringBuilder sb = new StringBuilder(simbolo).append('(').append(c.isEmpty() ? "1" : textoDeCuerpo(c));
            for (int i = 0; i < n; i++) {
                sb.append(", ").append(vars.get(i)).append(", ").append(intervalos.get(i)[0])
                        .append(", ").append(intervalos.get(i)[1]);
            }
            return new Pieza(sb.append(')').toString(), Clase.OPERACION);
        }

        /** [a, b] \times [c, d] → {a, b}, {c, d}. */
        private static List<String[]> intervalos(List<Tok> region) {
            List<List<Tok>> partes = new ArrayList<>();
            List<Tok> actual = new ArrayList<>();
            int nivel = 0;
            for (Tok x : region) {
                if (x.tipo() == Tipo.ABRE) {
                    nivel++;
                } else if (x.tipo() == Tipo.CIERRA) {
                    nivel--;
                }
                if (nivel == 0 && x.esCmd("cdot", "times")) {
                    partes.add(actual);
                    actual = new ArrayList<>();
                } else {
                    actual.add(x);
                }
            }
            partes.add(actual);
            List<String[]> r = new ArrayList<>();
            for (List<Tok> parte : partes) {
                List<Tok> p = new ArrayList<>(parte);
                p.removeIf(x -> x.esCmd("left", "right"));
                if (!p.isEmpty() && (p.getFirst().esChar('[') || p.getFirst().esChar('('))) {
                    p.removeFirst();
                }
                if (!p.isEmpty() && (p.getLast().esChar(']') || p.getLast().esChar(')'))) {
                    p.removeLast();
                }
                int coma = -1;
                int nv = 0;
                for (int i = 0; i < p.size(); i++) {
                    Tok x = p.get(i);
                    if (x.tipo() == Tipo.ABRE) {
                        nv++;
                    } else if (x.tipo() == Tipo.CIERRA) {
                        nv--;
                    } else if (nv == 0 && x.esChar(',')) {
                        coma = i;
                        break;
                    }
                }
                if (coma < 0) {
                    throw new LatexNoValido("Cada intervalo de la región va como [a, b]");
                }
                List<Tok> a = p.subList(0, coma);
                List<Tok> b = p.subList(coma + 1, p.size());
                r.add(new String[] {vacio(a) ? CASILLA : texto(a), vacio(b) ? CASILLA : texto(b)});
            }
            return r;
        }

        // -------------------------------------------------------------- potencias

        private static Pieza potencia(Pieza base, List<Pieza> exp) {
            String b = switch (base.clase()) {
                case VALOR, LETRA, LLAMADA -> base.texto();
                default -> "(" + base.texto() + ")";
            };
            return new Pieza(b + "^" + exponente(exp), Clase.VALOR);
        }

        /** Un solo factor va tal cual ("x^2", "e^x", "x^-2"); lo demás, entre paréntesis ("x^(n + 1)"). */
        private static String exponente(List<Pieza> exp) {
            if (exp.isEmpty()) {
                return CASILLA;
            }
            if (exp.size() == 1 && simple(exp.getFirst())) {
                return exp.getFirst().texto();
            }
            if (exp.size() == 2 && (exp.getFirst().esOperador("-") || exp.getFirst().esOperador("+"))
                    && simple(exp.get(1))) {
                return exp.getFirst().texto() + exp.get(1).texto();
            }
            return "(" + emitir(exp).strip() + ")";
        }

        private static boolean simple(Pieza p) {
            return p.clase() == Clase.VALOR || p.clase() == Clase.LETRA || p.clase() == Clase.LLAMADA
                    || p.clase() == Clase.RAIZ;
        }

        /** Radicando: un número, una letra o un grupo van solos; lo demás, entre paréntesis. */
        private static String envolverSalvoAtomo(List<Pieza> p) {
            if (p.size() == 1) {
                Pieza x = p.getFirst();
                if (x.clase() == Clase.LETRA || x.grupo() || x.esNumero() || x.texto().equals(CASILLA)) {
                    return x.texto();
                }
            }
            return "(" + emitir(p).strip() + ")";
        }
    }

    // ================================================================== escritura

    /** Nombres que el lexer lee como una palabra: si dos letras sueltas los forman, se separan (RN45). */
    private static final List<String> PALABRAS = List.of("arcsen", "arcsin", "arccos", "arctan", "arctg",
                    "cosec", "sqrt", "sen", "sin", "cos", "tan", "cot", "ctg", "sec", "csc", "log", "exp", "lim",
                    "int", "inf", "ln", "tg", "pi").stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .toList();

    private static final Set<String> CON_ESPACIOS = Set.of("=", "≠", "≤", "≥", "<", ">", "≈");

    /** Operadores tras los que una fracción puede ir sin paréntesis: empieza un término. */
    private static final Set<String> INICIAN_TERMINO = Set.of("+", "-", "=", "≠", "≤", "≥", "<", ">", "≈", ",",
            ";", "→", "±", "∓");

    private static String emitir(List<Pieza> ps) {
        StringBuilder sb = new StringBuilder();
        StringBuilder corrida = new StringBuilder();
        for (int i = 0; i < ps.size(); i++) {
            Pieza p = ps.get(i);
            Pieza prev = i > 0 ? ps.get(i - 1) : null;
            Pieza next = i + 1 < ps.size() ? ps.get(i + 1) : null;
            if (p.clase() == Clase.OPERADOR) {
                corrida.setLength(0);
                String o = p.texto();
                if (o.equals("+") || o.equals("-")) {
                    boolean unario = prev == null || prev.clase() == Clase.OPERADOR;
                    sb.append(unario ? o : " " + o + " ");
                } else if (CON_ESPACIOS.contains(o)) {
                    sb.append(' ').append(o).append(' ');
                } else if (o.equals(",") || o.equals(";")) {
                    sb.append(o).append(' ');
                } else {
                    sb.append(o);
                }
                continue;
            }
            String texto = p.texto();
            if (p.clase() == Clase.FRACCION) {
                boolean antes = prev != null && (prev.esValor() || !INICIAN_TERMINO.contains(prev.texto()));
                boolean despues = next != null && next.esValor();
                if (antes || despues) {
                    texto = "(" + texto + ")";
                }
            }
            String sep = prev != null && prev.esValor() ? separador(prev, p, texto, corrida) : "";
            if (p.clase() == Clase.LETRA) {
                if (!sep.isEmpty() || prev == null || prev.clase() != Clase.LETRA) {
                    corrida.setLength(0);
                }
                corrida.append(texto);
            } else {
                corrida.setLength(0);
            }
            sb.append(sep).append(texto);
        }
        return sb.toString();
    }

    private static String separador(Pieza prev, Pieza p, String texto, StringBuilder corrida) {
        char inicio = texto.charAt(0);
        if (p.clase() == Clase.OPERACION || prev.clase() == Clase.OPERACION) {
            return "·";
        }
        if (Character.isDigit(inicio) || inicio == '.') {
            return "·";
        }
        if (prev.clase() == Clase.RAIZ || prev.clase() == Clase.LLAMADA) {
            return "·";
        }
        if (p.clase() == Clase.LLAMADA) {
            return prev.esNumero() ? "" : "·";
        }
        String t = prev.texto();
        if (t.contains("^") && Character.isLetter(t.charAt(t.length() - 1)) && Character.isLetter(inicio)) {
            // "e^x·x" y no "e^xx"
            return "·";
        }
        if (p.clase() == Clase.LETRA && prev.clase() == Clase.LETRA) {
            String candidata = corrida + texto;
            for (String w : PALABRAS) {
                if (candidata.endsWith(w)) {
                    return " ";
                }
            }
        }
        return "";
    }
}
