package pe.mathnova.domain.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Direccional;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Extremos;
import pe.mathnova.domain.expresion.LimiteDoble;
import pe.mathnova.domain.expresion.Gradiente;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Letras;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Rango;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

/**
 * Parser de descenso recursivo: un método por regla de la gramática BNF de MathNova.
 * Sigue el prototipo banco-de-prueba-casos-uso/parser_bnf.py.
 *
 * <p>Precedencia de mayor a menor: llamada, raíz y paréntesis; ^ (derecha); signo unario;
 * *, /, multiplicación implícita (izquierda); + y - (izquierda).
 *
 * <p>{@code <entrada> ::= <operacion> | <expresion>}: un límite, una derivada o una integral va
 * sola, nunca dentro de otra expresión (HU34). En este sprint solo se reconoce; no se resuelve.
 *
 * <p>HU38: {@code <ecuacion> ::= <expresion> "=" <expresion>}, con un solo "=" en el nivel superior.
 */
public final class Parser {

    private static final Map<String, TipoFuncion> FUNCIONES = Map.ofEntries(
            Map.entry("sen", TipoFuncion.SEN), Map.entry("cos", TipoFuncion.COS),
            Map.entry("tan", TipoFuncion.TAN), Map.entry("cot", TipoFuncion.COT),
            Map.entry("sec", TipoFuncion.SEC), Map.entry("csc", TipoFuncion.CSC),
            Map.entry("arcsen", TipoFuncion.ARCSEN), Map.entry("arccos", TipoFuncion.ARCCOS),
            Map.entry("arctan", TipoFuncion.ARCTAN), Map.entry("arcsec", TipoFuncion.ARCSEC),
            Map.entry("ln", TipoFuncion.LN),
            Map.entry("log", TipoFuncion.LOG), Map.entry("exp", TipoFuncion.EXP),
            Map.entry("sqrt", TipoFuncion.SQRT));

    private final List<Token> tokens;
    private int k;

    private Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Lee una entrada completa: una operación (lim, d/dx, ∂/∂x, ∫, ∬, ∭) o una expresión.
     *
     * @throws ErrorSintaxis si está mal escrita, con mensaje en español y posición base 1
     */
    public static Expresion parse(String texto) {
        Parser p = new Parser(Lexer.tokenizar(texto));
        Expresion e;
        if (p.actual().esOperacion()) {
            e = p.operacion();
        } else if (p.coeficienteDeOperacion()) {
            // "3∫(3x, x, 3, 6)" o "3·∫(…)": un número delante de la operación (Sprint 6)
            Constante c = new Constante(Racional.parse(p.avanzar().texto()));
            if (p.esOperador('*', '·')) {
                p.avanzar();
            }
            e = new Producto(c, p.operacion());
        } else {
            e = p.expresion();
        }
        if (!(e instanceof Limite || e instanceof Derivada || e instanceof DerivadaParcial || e instanceof Integral
                || e instanceof IntegralMultiple || e instanceof Gradiente || e instanceof Direccional
                || e instanceof LimiteDoble || e instanceof Extremos)
                && p.esOperador('=')) {
            e = p.ecuacion(e);
        }
        Token sobra = p.actual();
        if (sobra.tipo() != Token.Tipo.FIN) {
            throw new ErrorSintaxis("Sobra \"" + sobra.texto() + "\" al final", sobra.posicion());
        }
        return e;
    }

    /** Después de "=": el lado derecho; un segundo "=" no se acepta. */
    private Expresion ecuacion(Expresion izquierda) {
        Token igual = avanzar();
        if (es(Token.Tipo.FIN) || esOperador('=')) {
            throw new ErrorSintaxis("Falta el lado derecho de la ecuación", actual().posicion());
        }
        if (actual().esOperacion()) {
            throw new ErrorSintaxis("\"" + actual().texto()
                    + "\" va sola: un límite, una derivada o una integral no se combina con otra expresión",
                    actual().posicion());
        }
        Expresion derecha = expresion();
        if (esOperador('=')) {
            throw new ErrorSintaxis("Una ecuación lleva un solo \"=\"", actual().posicion());
        }
        return new pe.mathnova.domain.expresion.Ecuacion(izquierda, derecha);
    }

    /** Un número seguido de una derivada o una integral, con o sin "·" entre los dos. */
    private boolean coeficienteDeOperacion() {
        if (!es(Token.Tipo.NUMERO) || k + 1 >= tokens.size()) {
            return false;
        }
        Token sig = tokens.get(k + 1);
        if ((sig.esOperador('*') || sig.esOperador('·')) && k + 2 < tokens.size()) {
            sig = tokens.get(k + 2);
        }
        return sig.tipo() == Token.Tipo.DERIVADA || sig.tipo() == Token.Tipo.INTEGRAL && sig.texto().equals("∫");
    }

    // ------------------------------------------------------------------ utilidades

    private Token actual() {
        return tokens.get(k);
    }

    private Token avanzar() {
        return tokens.get(k++);
    }

    private boolean esOperador(char... ops) {
        for (char op : ops) {
            if (actual().esOperador(op)) {
                return true;
            }
        }
        return false;
    }

    private boolean es(Token.Tipo tipo) {
        return actual().tipo() == tipo;
    }

    private ErrorSintaxis esperaba(String que) {
        Token t = actual();
        return new ErrorSintaxis("Se esperaba " + que + " y llegó " + t.describir(), t.posicion());
    }

    private void esperarCierre() {
        esperar(')');
    }

    private void esperar(char op) {
        if (!esOperador(op)) {
            throw esperaba("\"" + op + "\"");
        }
        avanzar();
    }

    private int entero() {
        if (!es(Token.Tipo.NUMERO) || actual().texto().contains(".")) {
            throw esperaba("un número entero");
        }
        Token t = avanzar();
        try {
            return Integer.parseInt(t.texto());
        } catch (NumberFormatException e) {
            throw new ErrorSintaxis("El número " + t.texto() + " es demasiado grande", t.posicion());
        }
    }

    // ------------------------------------------------------------------ operaciones (HU34)

    /**
     * {@code <operacion> ::= <limite> | <derivada> | <parcial> | <integral> | <integral_multiple> | <gradiente>
     * | <direccional>}
     */
    private Expresion operacion() {
        Token t = actual();
        return switch (t.tipo()) {
            case LIMITE -> limite();
            case DERIVADA -> derivada();
            case PARCIAL -> parcial();
            case GRADIENTE -> gradiente();
            case DIRECCIONAL -> direccional();
            case EXTREMOS -> extremos();
            default -> t.texto().equals("∫") ? integral() : integralMultiple();
        };
    }

    /** {@code <limite> ::= "lim" "(" <variable> <flecha> <punto> [ "+" | "-" ] "," <expresion> ")"} */
    private Expresion limite() {
        avanzar();
        esperar('(');
        if (esOperador('(')) {
            return limiteDoble();
        }
        char variable = variableDeOperacion();
        esperar('→');
        Limite.Punto punto = punto();
        Limite.Lado lado = Limite.Lado.AMBOS;
        if (esOperador('+', '-')) {
            Token signo = avanzar();
            if (punto instanceof Limite.Infinito) {
                throw new ErrorSintaxis("Un límite al infinito no lleva lado (+ o -)", signo.posicion());
            }
            lado = signo.esOperador('+') ? Limite.Lado.DERECHA : Limite.Lado.IZQUIERDA;
        }
        esperar(',');
        Expresion cuerpo = cuerpo("del límite");
        esperarCierre();
        return new Limite(variable, punto, lado, cuerpo);
    }

    /**
     * {@code <limite_doble> ::= "lim" "(" "(" <variable> "," <variable> ")" <flecha> "(" <punto_lim> "," <punto_lim> ")"
     * "," <expresion> ")"} (HU29, RF54). Ya se leyeron "lim" y el primer "(".
     */
    private Expresion limiteDoble() {
        avanzar();
        Token tx = actual();
        char x = variableDeOperacion();
        esperar(',');
        Token ty = actual();
        char y = variableDeOperacion();
        if (x == y) {
            throw new ErrorSintaxis("Las dos variables del límite doble deben ser distintas", ty.posicion());
        }
        esperarCierre();
        esperar('→');
        if (!esOperador('(')) {
            throw esperaba("el punto entre paréntesis, como (0, 0)");
        }
        avanzar();
        Expresion a = puntoFinito(tx);
        esperar(',');
        Expresion b = puntoFinito(ty);
        esperarCierre();
        esperar(',');
        Expresion cuerpo = cuerpo("del límite");
        esperarCierre();
        return new LimiteDoble(x, y, a, b, cuerpo);
    }

    /** Un {@code <punto_lim>} del límite doble: como el del límite simple, pero sin ∞. */
    private Expresion puntoFinito(Token variable) {
        Token t = actual();
        Limite.Punto punto = punto();
        if (punto instanceof Limite.Finito f) {
            return f.valor();
        }
        throw new ErrorSintaxis("En el límite doble " + variable.texto() + " debe tender a un número, no a ∞",
                t.posicion());
    }

    /** {@code <extremos> ::= "extremos" "(" <expresion> [ ";" <ecuacion> ] ")"} (HU30, RF55). */
    private Expresion extremos() {
        avanzar();
        esperar('(');
        Expresion f = cuerpo("de extremos");
        Ecuacion g = null;
        if (esOperador(';')) {
            avanzar();
            Expresion izq = cuerpo("de la restricción");
            if (!esOperador('=')) {
                throw esperaba("\"=\" en la restricción, como x + y = 10");
            }
            avanzar();
            g = new Ecuacion(izq, cuerpo("del lado derecho de la restricción"));
        }
        esperarCierre();
        return new Extremos(f, g);
    }

    /**
     * {@code <punto> ::= [ "-" ] ( "∞" | "inf" | <numero> [ "π" ] [ "/" <numero> ] | "π" [ "/" <numero> ] )}
     *
     * <p>RN46: un número, una fracción, un múltiplo de π o ±∞; nunca una letra.
     */
    private Limite.Punto punto() {
        boolean negativo = false;
        if (esOperador('-')) {
            avanzar();
            negativo = true;
        }
        if (es(Token.Tipo.INFINITO)) {
            avanzar();
            return new Limite.Infinito(!negativo);
        }
        if (!es(Token.Tipo.NUMERO) && !esPi()) {
            rechazarLetraEnPunto();
            throw esperaba("el punto del límite (un número, una fracción, un múltiplo de π o ∞)");
        }
        Expresion valor = null;
        if (es(Token.Tipo.NUMERO)) {
            valor = new Constante(Racional.parse(avanzar().texto()));
        }
        if (esPi()) {
            avanzar();
            Expresion pi = new ConstanteEspecial(ConstanteEspecial.Tipo.PI);
            valor = valor == null ? pi : new Producto(valor, pi);
        }
        if (esOperador('/')) {
            avanzar();
            if (!es(Token.Tipo.NUMERO)) {
                throw esperaba("un número después de \"/\" en el punto del límite");
            }
            valor = new Cociente(valor, new Constante(Racional.parse(avanzar().texto())));
        }
        rechazarLetraEnPunto();
        return new Limite.Finito(negativo ? new Negacion(valor) : valor);
    }

    private boolean esPi() {
        return es(Token.Tipo.CONSTANTE) && actual().texto().equals("π");
    }

    /** RN46. */
    private void rechazarLetraEnPunto() {
        if (es(Token.Tipo.VARIABLE) || es(Token.Tipo.CONSTANTE) || es(Token.Tipo.FUNCION)) {
            Token t = actual();
            throw new ErrorSintaxis("El punto del límite debe ser un número, una fracción, un múltiplo de π "
                    + "o ±∞, no la letra '" + t.texto() + "'", t.posicion());
        }
    }

    /** {@code <derivada> ::= "d" [ <entero> ] "/d" <variable> [ <entero> ] "(" <expresion> ")"} (RN47) */
    private Expresion derivada() {
        Token t = avanzar();
        String texto = t.texto(); // "d2/dx2"
        int barra = texto.indexOf('/');
        int ordenArriba = ordenDe(texto.substring(1, barra), t);
        char variable = texto.charAt(barra + 2);
        int ordenAbajo = ordenDe(texto.substring(barra + 3), t);
        validarVariable(variable, t.posicion() + barra + 2);
        if (ordenArriba != ordenAbajo) {
            throw new ErrorSintaxis("El orden de arriba (" + ordenArriba + ") y el de abajo (" + ordenAbajo
                    + ") de la derivada no coinciden", t.posicion());
        }
        esperar('(');
        Expresion cuerpo = cuerpo("de la derivada");
        if (ordenArriba == 1 && esOperador('=')) {
            // D08: d/dx(x^2 + y^2 = 25), derivación implícita
            avanzar();
            cuerpo = new pe.mathnova.domain.expresion.Ecuacion(cuerpo, cuerpo("del lado derecho"));
        }
        esperarCierre();
        return new Derivada(ordenArriba, variable, cuerpo);
    }

    /**
     * {@code <parcial> ::= "∂" [ <entero> ] "/" "∂" <variable> { "∂" <variable> } "(" <expresion> ")"}.
     * También acepta ∂2/∂x2, que es ∂x∂x.
     */
    private Expresion parcial() {
        Token t = avanzar();
        String texto = t.texto(); // "∂2/∂x∂y"
        int barra = texto.indexOf('/');
        int orden = ordenDe(texto.substring(1, barra), t);
        List<Character> variables = new ArrayList<>();
        int j = barra + 1;
        while (j < texto.length()) {
            char v = texto.charAt(j + 1); // en j está '∂'
            validarVariable(v, t.posicion() + j + 1);
            int fin = j + 2;
            while (fin < texto.length() && Character.isDigit(texto.charAt(fin))) {
                fin++;
            }
            int veces = ordenDe(texto.substring(j + 2, fin), t);
            for (int k = 0; k < veces; k++) {
                variables.add(v);
            }
            j = fin;
        }
        if (orden != variables.size()) {
            throw new ErrorSintaxis("El orden de la derivada parcial (" + orden + ") no coincide con las "
                    + variables.size() + " variables de abajo", t.posicion());
        }
        return new DerivadaParcial(variables, cuerpoEntreParentesis("de la derivada parcial"));
    }

    /** {@code <gradiente> ::= "grad" "(" <expresion> [ "," <punto> ] ")"} (HU28, RF53). */
    private Expresion gradiente() {
        avanzar();
        esperar('(');
        Expresion f = cuerpo("del gradiente");
        List<Expresion> punto = List.of();
        if (esOperador(',')) {
            avanzar();
            punto = tupla("el punto");
        }
        esperarCierre();
        return new Gradiente(f, punto);
    }

    /** {@code <direccional> ::= "Du" "(" <expresion> "," <vector> "," <punto> ")"} (HU28, RF53). */
    private Expresion direccional() {
        avanzar();
        esperar('(');
        Expresion f = cuerpo("de la derivada direccional");
        esperar(',');
        List<Expresion> vector = tupla("el vector");
        esperar(',');
        List<Expresion> punto = tupla("el punto");
        if (vector.size() != punto.size()) {
            throw new ErrorSintaxis("El vector tiene " + vector.size() + " componentes y el punto " + punto.size()
                    + ": deben tener las mismas", actual().posicion());
        }
        esperarCierre();
        return new Direccional(f, vector, punto);
    }

    /** {@code <vector> ::= "(" <expresion> { "," <expresion> } ")"}, igual que {@code <punto>}. */
    private List<Expresion> tupla(String que) {
        if (!esOperador('(')) {
            throw esperaba(que + " entre paréntesis, como (1, 2)");
        }
        avanzar();
        List<Expresion> r = new ArrayList<>();
        r.add(cuerpo("de " + que));
        while (esOperador(',')) {
            avanzar();
            r.add(cuerpo("de " + que));
        }
        esperarCierre();
        return r;
    }

    /** Orden escrito junto a d o ∂; "" es orden 1. */
    private static int ordenDe(String digitos, Token t) {
        if (digitos.isEmpty()) {
            return 1;
        }
        int orden;
        try {
            orden = Integer.parseInt(digitos);
        } catch (NumberFormatException e) {
            throw new ErrorSintaxis("El orden " + digitos + " es demasiado grande", t.posicion());
        }
        if (orden < 1) {
            throw new ErrorSintaxis("El orden de la derivada debe ser al menos 1", t.posicion());
        }
        return orden;
    }

    /** {@code <integral> ::= ("∫" | "int") "(" <expresion> "," <variable> [ "," <expresion> "," <expresion> ] ")"} */
    private Expresion integral() {
        avanzar();
        esperar('(');
        Expresion cuerpo = cuerpo("de la integral");
        esperar(',');
        char variable = variableDeOperacion();
        if (esOperador(',')) {
            avanzar();
            Expresion inferior = cuerpo("del límite inferior");
            esperar(',');
            Expresion superior = cuerpo("del límite superior");
            esperarCierre();
            return new Integral(cuerpo, variable, inferior, superior);
        }
        esperarCierre();
        return new Integral(cuerpo, variable);
    }

    /**
     * {@code <integral_multiple> ::= ("∬" | "∭") "(" <expresion> { "," <variable> "," <rango> } [ ";" <coordenadas> ] ")"},
     * {@code <rango> ::= <expresion> "," <expresion> | "[" <expresion> "," <expresion> "]"},
     * con 2 rangos para ∬ y 3 para ∭.
     */
    private Expresion integralMultiple() {
        Token simbolo = avanzar();
        int rangosEsperados = simbolo.texto().equals("∬") ? 2 : 3;
        esperar('(');
        Expresion cuerpo = cuerpo("de la integral");
        List<Rango> rangos = new ArrayList<>();
        while (esOperador(',')) {
            avanzar();
            Token tokenVariable = actual();
            char variable = variableDeOperacion();
            for (Rango r : rangos) {
                if (r.variable() == variable) {
                    throw new ErrorSintaxis("La variable " + variable + " se repite en la integral",
                            tokenVariable.posicion());
                }
            }
            esperar(',');
            // Gramática v2: "x, a, b". También "x, [a, b]", la forma de antes: dan el mismo árbol.
            boolean corchete = esOperador('[');
            if (corchete) {
                avanzar();
            }
            Expresion inferior = cuerpo("del límite inferior");
            esperar(',');
            Expresion superior = cuerpo("del límite superior");
            if (corchete) {
                esperar(']');
            }
            rangos.add(new Rango(variable, inferior, superior));
        }
        if (rangos.size() != rangosEsperados) {
            String forma = rangosEsperados == 2 ? "∬(f, x, a, b, y, c, d)" : "∭(f, x, a, b, y, c, d, z, g, h)";
            throw new ErrorSintaxis(simbolo.texto() + " necesita " + rangosEsperados
                    + " variables con sus límites, como " + forma, simbolo.posicion());
        }
        IntegralMultiple.Coordenadas coordenadas = IntegralMultiple.Coordenadas.CARTESIANAS;
        if (esOperador(';')) {
            avanzar();
            coordenadas = coordenadas(rangosEsperados);
        }
        esperarCierre();
        return new IntegralMultiple(cuerpo, rangos, coordenadas);
    }

    /** {@code "coord" "=" ( "polares" | "cilindricas" | "esfericas" )} (Sprint 8, V10). */
    private IntegralMultiple.Coordenadas coordenadas(int rangos) {
        if (!es(Token.Tipo.COORDENADAS) || !actual().texto().equals("coord")) {
            throw esperaba("coord=polares, coord=cilindricas o coord=esfericas");
        }
        avanzar();
        esperar('=');
        Token t = actual();
        if (!es(Token.Tipo.COORDENADAS) || t.texto().equals("coord")) {
            throw esperaba("polares, cilindricas o esfericas");
        }
        avanzar();
        IntegralMultiple.Coordenadas c = switch (t.texto()) {
            case "polares" -> IntegralMultiple.Coordenadas.POLARES;
            case "cilindricas" -> IntegralMultiple.Coordenadas.CILINDRICAS;
            default -> IntegralMultiple.Coordenadas.ESFERICAS;
        };
        if (c == IntegralMultiple.Coordenadas.POLARES && rangos != 2) {
            throw new ErrorSintaxis("Las coordenadas polares son para ∬; en ∭ usa cilindricas o esfericas",
                    t.posicion());
        }
        if (c != IntegralMultiple.Coordenadas.POLARES && rangos != 3) {
            throw new ErrorSintaxis("Las coordenadas " + c.nombre() + " son para ∭; en ∬ usa polares", t.posicion());
        }
        return c;
    }

    /** {@code <variable>} de una operación: una letra que no sea e, i ni π (RN42). */
    private char variableDeOperacion() {
        Token t = actual();
        if (es(Token.Tipo.CONSTANTE)) {
            throw new ErrorSintaxis("'" + t.texto() + "' es una constante; no puede ser la variable de la operación",
                    t.posicion());
        }
        if (!es(Token.Tipo.VARIABLE)) {
            throw esperaba("una variable");
        }
        avanzar();
        return t.texto().charAt(0);
    }

    private static void validarVariable(char v, int posicion) {
        if (Letras.esConstante(v)) {
            throw new ErrorSintaxis("'" + v + "' es una constante; no puede ser la variable de la operación",
                    posicion);
        }
        if (!Letras.esVariable(v)) {
            throw new ErrorSintaxis("'" + v + "' en la posición " + posicion + " no es una letra permitida", posicion);
        }
    }

    private Expresion cuerpoEntreParentesis(String de) {
        esperar('(');
        Expresion e = cuerpo(de);
        esperarCierre();
        return e;
    }

    /** Una expresión obligatoria dentro de una operación; si no está, lo dice claro. */
    private Expresion cuerpo(String de) {
        if (esOperador(')', ',') || es(Token.Tipo.FIN)) {
            throw new ErrorSintaxis("Falta la expresión " + de, actual().posicion());
        }
        return expresion();
    }

    // ------------------------------------------------------------------ gramática

    /** {@code <expresion> ::= <termino> { ("+" | "-") <termino> }} */
    private Expresion expresion() {
        List<Expresion> terminos = new ArrayList<>();
        terminos.add(termino());
        while (esOperador('+', '-')) {
            boolean resta = avanzar().esOperador('-');
            Expresion t = termino();
            terminos.add(resta ? new Negacion(t) : t);
        }
        return terminos.size() == 1 ? terminos.getFirst() : new Suma(terminos);
    }

    /**
     * {@code <termino> ::= <unario> { <op_mult> <unario> | <factor_implicito> }}
     *
     * <p>Si el término empieza con signo menos y sigue con más factores ("-2x"), el signo
     * se sube a todo el término: queda -(2x) y no (-2)·x. El valor es el mismo.
     */
    private Expresion termino() {
        Expresion primero = unario();
        boolean negado = false;
        if (primero instanceof Negacion(Expresion arg) && sigueFactor()) {
            negado = true;
            primero = arg;
        }
        List<Expresion> factores = new ArrayList<>();
        factores.add(primero);
        while (true) {
            if (esOperador('*', '·')) {
                avanzar();
                factores.add(unario());
            } else if (esOperador('/')) {
                avanzar();
                Expresion divisor = unario();
                Expresion dividendo = producto(factores);
                factores.clear();
                factores.add(new Cociente(dividendo, divisor));
            } else if (iniciaImplicito()) {
                factores.add(potencia());
            } else {
                break;
            }
        }
        Expresion t = producto(factores);
        return negado ? new Negacion(t) : t;
    }

    private static Expresion producto(List<Expresion> factores) {
        return factores.size() == 1 ? factores.getFirst() : new Producto(factores);
    }

    private boolean sigueFactor() {
        return esOperador('*', '·', '/') || iniciaImplicito();
    }

    /** {@code <factor_implicito>}: empieza con variable, constante, función, "(", "√" o "∛"; nunca con número. */
    private boolean iniciaImplicito() {
        return es(Token.Tipo.VARIABLE) || es(Token.Tipo.CONSTANTE) || es(Token.Tipo.FUNCION)
                || actual().esOperacion() || es(Token.Tipo.INFINITO) || esOperador('(', '√', '∛');
    }

    /** {@code <unario> ::= ("+" | "-") <unario> | <potencia>} */
    private Expresion unario() {
        if (esOperador('-')) {
            avanzar();
            return new Negacion(unario());
        }
        if (esOperador('+')) {
            avanzar();
            return unario();
        }
        return potencia();
    }

    /** {@code <potencia> ::= <primario> [ "^" <exponente> ]} */
    private Expresion potencia() {
        Expresion base = primario();
        if (esOperador('^')) {
            avanzar();
            return new Potencia(base, exponente());
        }
        return base;
    }

    /** {@code <exponente> ::= [ "+" | "-" ] <primario> [ "^" <exponente> ]} */
    private Expresion exponente() {
        boolean negativo = false;
        if (esOperador('+', '-')) {
            negativo = avanzar().esOperador('-');
        }
        Expresion e = primario();
        if (esOperador('^')) {
            avanzar();
            e = new Potencia(e, exponente());
        }
        return negativo ? new Negacion(e) : e;
    }

    /** {@code <primario> ::= <numero> | <variable> | <constante> | "(" <expresion> ")" | <llamada> | <raiz>} */
    private Expresion primario() {
        Token t = actual();
        switch (t.tipo()) {
            case NUMERO -> {
                avanzar();
                return new Constante(Racional.parse(t.texto()));
            }
            case VARIABLE -> {
                avanzar();
                return new Variable(t.texto().charAt(0));
            }
            case CONSTANTE -> {
                avanzar();
                return new ConstanteEspecial(switch (t.texto()) {
                    case "e" -> ConstanteEspecial.Tipo.E;
                    case "i" -> ConstanteEspecial.Tipo.I;
                    default -> ConstanteEspecial.Tipo.PI;
                });
            }
            case FUNCION -> {
                return llamada();
            }
            case COORDENADAS -> throw new ErrorSintaxis("\"" + t.texto() + "\" solo va al final de una integral "
                    + "múltiple, como ∬(f, r, 0, 1, θ, 0, 2π; coord=polares)", t.posicion());
            case LIMITE, DERIVADA, PARCIAL, INTEGRAL, EXTREMOS -> throw new ErrorSintaxis("\"" + t.texto()
                    + "\" va sola: un límite, una derivada o una integral no se combina con otra expresión",
                    t.posicion());
            case INFINITO -> throw new ErrorSintaxis("∞ solo puede ir como punto de un límite", t.posicion());
            default -> {
                if (esOperador('(')) {
                    avanzar();
                    Expresion e = expresion();
                    esperarCierre();
                    return e;
                }
                if (esOperador('√', '∛')) {
                    return raiz();
                }
                throw esperaba("un número, una variable o \"(\"");
            }
        }
    }

    /** {@code <raiz> ::= ("√" | "∛") <primario> | "√[" <entero> "]" <primario>} */
    private Expresion raiz() {
        int indice = avanzar().esOperador('√') ? 2 : 3;
        if (indice == 2 && esOperador('[')) {
            avanzar();
            Token tokenIndice = actual();
            indice = entero();
            if (indice < 2) {
                throw new ErrorSintaxis("El índice de la raíz debe ser un entero mayor que 1", tokenIndice.posicion());
            }
            esperar(']');
        }
        return new Raiz(indice, primario());
    }

    /**
     * {@code <llamada> ::= <funcion> [ "^" <entero> ] <argumento>}
     * con {@code <funcion> ::= ... | "log" [ "_" <entero> ]}
     */
    private Expresion llamada() {
        Token nombre = avanzar();
        TipoFuncion tipo = FUNCIONES.get(nombre.texto());
        int base = 0;
        if (tipo == TipoFuncion.LOG) {
            base = 10;
            if (esOperador('_')) {
                avanzar();
                Token tokenBase = actual();
                base = entero();
                if (base < 2) {
                    throw new ErrorSintaxis("La base del logaritmo debe ser un entero mayor que 1",
                            tokenBase.posicion());
                }
            }
        }
        Integer potencia = null;
        if (esOperador('^')) {
            avanzar();
            potencia = entero();
        }
        Expresion f = new Funcion(tipo, argumento(), base);
        return potencia == null ? f : new Potencia(f, Constante.de(potencia));
    }

    /**
     * {@code <argumento> ::= "(" <expresion> ")" | <argumento_simple>}
     * con {@code <argumento_simple> ::= [ <numero> ] <variable> [ "^" <entero> ]}
     */
    private Expresion argumento() {
        if (esOperador('(')) {
            avanzar();
            Expresion e = expresion();
            esperarCierre();
            return e;
        }
        Constante coeficiente = null;
        if (es(Token.Tipo.NUMERO)) {
            coeficiente = new Constante(Racional.parse(avanzar().texto()));
        }
        if (!es(Token.Tipo.VARIABLE)) {
            throw esperaba("una variable");
        }
        Expresion v = new Variable(avanzar().texto().charAt(0));
        if (esOperador('^')) {
            avanzar();
            v = new Potencia(v, Constante.de(entero()));
        }
        return coeficiente == null ? v : new Producto(coeficiente, v);
    }
}
