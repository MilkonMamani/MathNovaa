package pe.mathnova.domain.calculo.parciales;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.Anidados;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.derivadas.FormaFinal;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Direccional;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Gradiente;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;

/**
 * HU28 (RF52, RF53): derivadas parciales, gradiente, derivada direccional y regla de la cadena en varias
 * variables, con el derivador de una variable tratando las demás letras como constantes.
 * <ul>
 *   <li>V01: ∂/∂x(…), ∂2/∂x2(…), ∂2/∂x∂y(…) (de derecha a izquierda: ∂²f/∂x∂y = ∂/∂x(∂f/∂y));</li>
 *   <li>V02: ∇f = (f_x, f_y, …), y su valor en un punto;</li>
 *   <li>V03: D_u f(P) = ∇f(P)·u, normalizando antes el vector si no es unitario;</li>
 *   <li>V04: df/dt = f_x·x' + f_y·y' con x = x(t), y = y(t), con su diagrama de árbol.</li>
 * </ul>
 * También el dominio de f(x, y) (RF52) y la tabla de parciales con la hessiana para Análisis.
 */
public final class MotorParciales {

    public static final String PARCIAL = "V01";
    public static final String GRADIENTE = "V02";
    public static final String DIRECCIONAL = "V03";
    public static final String CADENA = "V04";

    private static final MathContext MC = new MathContext(60);
    private static final BigDecimal H = BigDecimal.ONE.movePointLeft(22);
    private static final List<Racional> COORDENADAS = List.of(Racional.de(7, 5), Racional.de(3, 7), Racional.de(5, 3),
            Racional.de(-4, 9), Racional.de(11, 6));

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    /** Un polinomio en varias letras queda en su orden (2xy + 3y²); lo demás, con la forma final de x. */
    private static Expresion simple(Expresion e, char x) {
        try {
            pe.mathnova.domain.algebra.Polinomio q = pe.mathnova.domain.algebra.Polinomio.desde(Simplificador.simplificar(e));
            if (q.variables().size() >= 2) {
                return Simplificador.simplificar(q.aExpresion());
            }
        } catch (RuntimeException noEsPolinomio) {
            // sigue con la forma final
        }
        try {
            return FormaFinal.de(e, x);
        } catch (RuntimeException no) {
            return Simplificador.simplificar(e);
        }
    }

    /** x, y, z si f usa solo esas letras; si no, las letras de f en orden alfabético. */
    public static List<Character> variables(Expresion f, int minimo) {
        SortedSet<Character> letras = Expresiones.variables(f);
        List<Character> xyz = List.of('x', 'y', 'z');
        if (xyz.containsAll(letras)) {
            int n = Math.max(minimo, letras.isEmpty() ? 0 : xyz.indexOf(letras.last()) + 1);
            return xyz.subList(0, Math.min(3, Math.max(n, letras.size())));
        }
        return List.copyOf(letras);
    }

    // ================================================================== V01

    /** Una parcial respecto de v, como paso: "y se trata como constante". */
    private static PasoLimite parcial(Expresion f, char v, List<Character> letras, Expresion[] resultado) {
        Derivador.Resultado r = Derivador.derivar(f, v);
        Expresion d = simple(r.derivada(), v);
        resultado[0] = d;
        List<Character> otras = letras.stream().filter(c -> c != v).toList();
        String constantes = otras.isEmpty() ? "" : "; " + String.join(", ", otras.stream().map(String::valueOf).toList())
                + (otras.size() == 1 ? " se trata como constante" : " se tratan como constantes");
        List<PasoLimite> reglas = new ArrayList<>();
        r.arbol().forEach(pd -> reglas.add(Anidados.deDerivada(pd)));
        return new PasoLimite(PARCIAL, "Derivada parcial respecto de " + v, "Se deriva respecto de " + v + constantes,
                f, d, new TrabajoIntermedio.Agrupacion(List.of("∂/∂" + v + "(" + p(f) + ") = " + p(d))),
                reglas.isEmpty() ? null : "Reglas de derivación", reglas);
    }

    private ResolucionParcial parcial(DerivadaParcial d) {
        Expresion f = d.cuerpo();
        List<Character> letras = variables(f, 0);
        List<PasoLimite> pasos = new ArrayList<>();
        Expresion actual = f;
        boolean verificada = true;
        List<Character> orden = new ArrayList<>(d.variables());
        java.util.Collections.reverse(orden);
        for (char v : orden) {
            Expresion[] r = new Expresion[1];
            pasos.add(parcial(actual, v, letras, r));
            verificada &= verificar(actual, v, r[0], letras);
            actual = r[0];
        }
        if (orden.size() > 1 && orden.get(0) != orden.get(orden.size() - 1)) {
            pasos.add(new PasoLimite(PARCIAL, "Orden de las derivadas", "En ∂²f/∂" + d.variables().getFirst() + "∂"
                    + d.variables().getLast() + " se deriva primero respecto de " + orden.getFirst() + " y luego de "
                    + orden.getLast() + "; por el teorema de Schwarz el otro orden da lo mismo", null, actual,
                    new TrabajoIntermedio.Agrupacion(List.of(p(actual)))));
        }
        return new ResolucionParcial(d, ResolucionParcial.Tipo.PARCIAL, f, letras, actual, List.of(), pasos, verificada,
                dominio(f));
    }

    // ================================================================== V02

    private record Grad(List<Expresion> componentes, List<PasoLimite> pasos, boolean verificada) {
    }

    private static Grad gradiente(Expresion f, List<Character> letras) {
        List<Expresion> c = new ArrayList<>();
        List<PasoLimite> pasos = new ArrayList<>();
        boolean ok = true;
        for (char v : letras) {
            Expresion[] r = new Expresion[1];
            pasos.add(parcial(f, v, letras, r));
            ok &= verificar(f, v, r[0], letras);
            c.add(r[0]);
        }
        return new Grad(c, pasos, ok);
    }

    private static String vector(List<Expresion> v) {
        return "(" + String.join(", ", v.stream().map(MotorParciales::p).toList()) + ")";
    }

    private static Map<Character, Expresion> punto(List<Character> letras, List<Expresion> punto) {
        if (punto.size() != letras.size()) {
            throw new IllegalArgumentException("El punto tiene " + punto.size() + " coordenadas y f tiene "
                    + letras.size() + " variables (" + String.join(", ", letras.stream().map(String::valueOf).toList())
                    + ")");
        }
        Map<Character, Expresion> r = new LinkedHashMap<>();
        for (int i = 0; i < letras.size(); i++) {
            r.put(letras.get(i), punto.get(i));
        }
        return r;
    }

    private static Expresion evaluar(Expresion e, Map<Character, Expresion> valores) {
        Expresion r = e;
        for (var v : valores.entrySet()) {
            r = Expresiones.reemplazar(r, new Variable(v.getKey()), v.getValue());
        }
        return Simplificador.simplificar(r);
    }

    private static String enPunto(Map<Character, Expresion> pt) {
        return "(" + String.join(", ", pt.values().stream().map(MotorParciales::p).toList()) + ")";
    }

    private ResolucionParcial gradiente(Gradiente g) {
        Expresion f = g.cuerpo();
        List<Character> letras = variables(f, g.punto().size());
        Grad gr = gradiente(f, letras);
        List<PasoLimite> pasos = new ArrayList<>();
        String nombres = String.join(", ", letras.stream().map(v -> "∂f/∂" + v).toList());
        pasos.add(new PasoLimite(GRADIENTE, "Gradiente", "∇f es el vector de las derivadas parciales (" + nombres + ")",
                f, null, new TrabajoIntermedio.Agrupacion(List.of("∇f = " + vector(gr.componentes()))),
                "Una derivada parcial por variable", gr.pasos()));
        List<Expresion> resultado = gr.componentes();
        if (g.enUnPunto()) {
            Map<Character, Expresion> pt = punto(letras, g.punto());
            List<Expresion> valores = resultado.stream().map(c -> evaluar(c, pt)).toList();
            pasos.add(new PasoLimite(GRADIENTE, "Evaluar en el punto", "Se reemplaza " + asignaciones(pt)
                    + " en cada componente", null, null, new TrabajoIntermedio.Agrupacion(List.of(
                            "∇f" + enPunto(pt) + " = " + vector(valores)))));
            resultado = valores;
        }
        return new ResolucionParcial(g, ResolucionParcial.Tipo.GRADIENTE, f, letras, null, resultado, pasos,
                gr.verificada(), dominio(f));
    }

    private static String asignaciones(Map<Character, Expresion> pt) {
        return String.join(", ", pt.entrySet().stream().map(e -> e.getKey() + " = " + p(e.getValue())).toList());
    }

    // ================================================================== V03

    private ResolucionParcial direccional(Direccional d) {
        Expresion f = d.cuerpo();
        List<Character> letras = variables(f, d.punto().size());
        Map<Character, Expresion> pt = punto(letras, d.punto());
        Grad gr = gradiente(f, letras);
        List<PasoLimite> pasos = new ArrayList<>();
        pasos.add(new PasoLimite(GRADIENTE, "Gradiente", "Primero ∇f, el vector de las derivadas parciales", f, null,
                new TrabajoIntermedio.Agrupacion(List.of("∇f = " + vector(gr.componentes()))),
                "Una derivada parcial por variable", gr.pasos()));
        List<Expresion> enP = gr.componentes().stream().map(c -> evaluar(c, pt)).toList();
        pasos.add(new PasoLimite(GRADIENTE, "Evaluar en el punto", "Se reemplaza " + asignaciones(pt), null, null,
                new TrabajoIntermedio.Agrupacion(List.of("∇f" + enPunto(pt) + " = " + vector(enP)))));
        List<Expresion> v = d.vector();
        List<Expresion> cuadrados = v.stream().map(c -> Simplificador.potencia(c, Constante.de(2))).toList();
        Expresion norma2 = Simplificador.simplificar(Simplificador.suma(cuadrados));
        Expresion norma = Simplificador.simplificar(Simplificador.raiz(2, norma2));
        List<Expresion> u = v;
        if (!norma.equals(Constante.de(1))) {
            u = v.stream().map(c -> Simplificador.simplificar(Simplificador.cociente(c, norma))).toList();
            List<String> sumandos = v.stream().map(c -> (c instanceof Constante k && k.valor().signo() < 0
                    || c instanceof Negacion ? "(" + p(c) + ")" : p(c)) + "²").toList();
            pasos.add(new PasoLimite(DIRECCIONAL, "Normalizar el vector", "El vector " + vector(v) + " no es unitario: "
                    + "se divide entre su longitud para que la derivada mida la razón de cambio por unidad", null, null,
                    new TrabajoIntermedio.Agrupacion(List.of(
                            "|v| = √(" + String.join(" + ", sumandos) + ") = " + p(norma),
                            "u = v/|v| = " + vector(u)))));
        }
        List<Expresion> productos = new ArrayList<>();
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < u.size(); i++) {
            productos.add(Simplificador.producto(List.of(enP.get(i), u.get(i))));
            textos.add(paren(enP.get(i)) + "·" + paren(u.get(i)));
        }
        Expresion valor = Simplificador.simplificar(Simplificador.suma(productos));
        pasos.add(new PasoLimite(DIRECCIONAL, "Producto punto", "D_u f(P) = ∇f(P)·u", null, valor,
                new TrabajoIntermedio.Agrupacion(List.of("D_u f" + enPunto(pt) + " = " + String.join(" + ", textos)
                        + " = " + p(valor)))));
        boolean ok = gr.verificada() && verificarDireccional(f, letras, pt, u, valor);
        return new ResolucionParcial(d, ResolucionParcial.Tipo.DIRECCIONAL, f, letras, valor, List.of(), pasos, ok,
                dominio(f));
    }

    private static String paren(Expresion e) {
        return e instanceof Suma || e instanceof Negacion || e instanceof Constante k && k.valor().signo() < 0
                || e instanceof Cociente ? "(" + p(e) + ")" : p(e);
    }

    // ================================================================== V04

    /**
     * Lee "d/dt(x^2 y); x = t^2, y = sen t": la derivada y, después de ";", las sustituciones separadas por
     * comas. Vacío si no tiene esa forma.
     */
    public static Optional<ReglaCadena> leerCadena(String texto) {
        int pc = texto.indexOf(';');
        if (pc < 0) {
            return Optional.empty();
        }
        Expresion e = Parser.parse(texto.substring(0, pc));
        if (!(e instanceof Derivada d) || d.orden() != 1) {
            throw new IllegalArgumentException("Antes de \";\" va la derivada d/dt(f)");
        }
        Map<Character, Expresion> sus = new LinkedHashMap<>();
        for (String parte : partir(texto.substring(pc + 1))) {
            if (parte.isBlank()) {
                continue;
            }
            Expresion s = Parser.parse(parte);
            if (!(s instanceof Ecuacion(Expresion izq, Expresion der)) || !(izq instanceof Variable v)) {
                throw new IllegalArgumentException("Después de \";\" van las sustituciones, como x = t^2, y = sen t");
            }
            sus.put(v.nombre(), der);
        }
        if (sus.isEmpty()) {
            throw new IllegalArgumentException("Faltan las sustituciones después de \";\": x = t^2, y = sen t");
        }
        return Optional.of(new ReglaCadena(d, sus));
    }

    /** Las comas que no están dentro de paréntesis. */
    private static List<String> partir(String s) {
        List<String> r = new ArrayList<>();
        int nivel = 0;
        StringBuilder actual = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[') {
                nivel++;
            } else if (c == ')' || c == ']') {
                nivel--;
            }
            if (c == ',' && nivel == 0) {
                r.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(c);
            }
        }
        r.add(actual.toString());
        return r;
    }

    /** d/dt(f) con x = x(t), y = y(t). */
    public record ReglaCadena(Derivada derivada, Map<Character, Expresion> sustituciones) {
        public ReglaCadena {
            sustituciones = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(sustituciones));
        }
    }

    public ResolucionParcial cadena(ReglaCadena c) {
        Derivada d = c.derivada();
        char t = d.variable();
        Expresion f = d.cuerpo();
        List<Character> intermedias = new ArrayList<>(c.sustituciones().keySet());
        List<Character> letras = variables(f, 0);
        List<PasoLimite> hijos = new ArrayList<>();
        List<String> arriba = new ArrayList<>();
        List<String> abajo = new ArrayList<>();
        List<Expresion> sumandos = new ArrayList<>();
        List<String> formula = new ArrayList<>();
        List<String> conValores = new ArrayList<>();
        boolean ok = true;
        for (char v : intermedias) {
            Expresion[] fv = new Expresion[1];
            hijos.add(parcial(f, v, letras, fv));
            ok &= verificar(f, v, fv[0], letras);
            Expresion xv = c.sustituciones().get(v);
            Derivador.Resultado r = Derivador.derivar(xv, t);
            Expresion dv = simple(r.derivada(), t);
            List<PasoLimite> reglas = new ArrayList<>();
            r.arbol().forEach(pd -> reglas.add(Anidados.deDerivada(pd)));
            hijos.add(new PasoLimite(CADENA, "d" + v + "/d" + t, "Se deriva " + v + " = " + p(xv) + " respecto de " + t,
                    xv, dv, new TrabajoIntermedio.Agrupacion(List.of("d" + v + "/d" + t + " = " + p(dv))),
                    reglas.isEmpty() ? null : "Reglas de derivación", reglas));
            arriba.add("∂f/∂" + v + " = " + p(fv[0]));
            abajo.add("d" + v + "/d" + t + " = " + p(dv));
            sumandos.add(Simplificador.producto(List.of(fv[0], dv)));
            formula.add("f" + subindice(v) + "·" + v + "'");
            conValores.add(paren(fv[0]) + "·" + paren(dv));
        }
        String suma = "df/d" + t + " = " + String.join(" + ", formula) + " = " + String.join(" + ", conValores);
        Expresion enVariables = Simplificador.simplificar(Simplificador.suma(sumandos));
        List<PasoLimite> pasos = new ArrayList<>();
        pasos.add(new PasoLimite(CADENA, "Diagrama de árbol", "f depende de " + nombres(intermedias) + ", y "
                + (intermedias.size() == 1 ? "esa depende" : "cada una depende") + " de " + t + ": se suma un camino "
                + "por cada rama del árbol", null, null, new TrabajoIntermedio.ArbolCadena("f",
                        intermedias.stream().map(String::valueOf).toList(), String.valueOf(t), arriba, abajo, suma),
                "Las derivadas de cada rama", hijos));
        Expresion sustituida = enVariables;
        for (var e : c.sustituciones().entrySet()) {
            sustituida = Expresiones.reemplazar(sustituida, new Variable(e.getKey()), e.getValue());
        }
        Expresion resultado = simple(sustituida, t);
        List<String> asign = new ArrayList<>();
        c.sustituciones().forEach((k, v) -> asign.add(k + " = " + p(v)));
        pasos.add(new PasoLimite(CADENA, "Sustituir y simplificar", "Se reemplaza " + String.join(", ", asign)
                + " para que todo quede en " + t, enVariables, resultado, new TrabajoIntermedio.Agrupacion(List.of(
                        "df/d" + t + " = " + p(enVariables), "= " + p(resultado)))));
        ok &= verificarCadena(c, resultado);
        return new ResolucionParcial(d, ResolucionParcial.Tipo.CADENA, f, letras, resultado, List.of(), pasos, ok,
                dominio(f));
    }

    private static String nombres(List<Character> vs) {
        List<String> r = vs.stream().map(String::valueOf).toList();
        return r.size() == 1 ? r.getFirst() : String.join(", ", r.subList(0, r.size() - 1)) + " y " + r.getLast();
    }

    private static String subindice(char v) {
        return switch (v) {
            case 'x' -> "ₓ";
            case 'y' -> "ᵧ";
            default -> String.valueOf(v);
        };
    }

    // ================================================================== entrada

    /**
     * @throws IllegalArgumentException si no es ∂, grad ni Du, o el punto no tiene las coordenadas de f
     */
    public ResolucionParcial resolver(Expresion e) {
        return switch (e) {
            case DerivadaParcial d -> parcial(d);
            case Gradiente g -> gradiente(g);
            case Direccional d -> direccional(d);
            default -> throw new IllegalArgumentException("No es una derivada parcial, un gradiente ni una "
                    + "derivada direccional: " + p(e));
        };
    }

    // ================================================================== dominio (RF52)

    /** Las restricciones de f: denominador ≠ 0, raíz par ≥ 0, logaritmo > 0, arcsen y arccos entre −1 y 1. */
    public static List<String> dominio(Expresion f) {
        LinkedHashSet<String> r = new LinkedHashSet<>();
        restricciones(f, r);
        return List.copyOf(r);
    }

    private static boolean constante(Expresion e) {
        return Expresiones.variables(e).isEmpty();
    }

    private static void restricciones(Expresion e, LinkedHashSet<String> r) {
        switch (e) {
            case Suma s -> s.terminos().forEach(t -> restricciones(t, r));
            case Producto pr -> pr.factores().forEach(t -> restricciones(t, r));
            case Negacion n -> restricciones(n.argumento(), r);
            case Cociente q -> {
                restricciones(q.numerador(), r);
                restricciones(q.denominador(), r);
                if (!constante(q.denominador())) {
                    r.add(p(q.denominador()) + " ≠ 0");
                }
            }
            case Raiz rz -> {
                restricciones(rz.radicando(), r);
                if (rz.indice() % 2 == 0 && !constante(rz.radicando())) {
                    r.add(p(rz.radicando()) + " ≥ 0");
                }
            }
            case Potencia pw -> {
                restricciones(pw.base(), r);
                restricciones(pw.exponente(), r);
                if (pw.exponente() instanceof Constante k && !constante(pw.base())) {
                    if (k.valor().signo() < 0) {
                        r.add(p(pw.base()) + " ≠ 0");
                    }
                    if (!k.valor().esEntero() && k.valor().den().testBit(0) == false) {
                        r.add(p(pw.base()) + (k.valor().signo() < 0 ? " > 0" : " ≥ 0"));
                    }
                }
            }
            case Funcion fn -> {
                restricciones(fn.argumento(), r);
                if (constante(fn.argumento())) {
                    return;
                }
                String a = p(fn.argumento());
                switch (fn.tipo()) {
                    case LN, LOG -> r.add(a + " > 0");
                    case SQRT -> r.add(a + " ≥ 0");
                    case ARCSEN, ARCCOS -> r.add("−1 ≤ " + a + " ≤ 1");
                    case ARCSEC -> r.add("|" + a + "| ≥ 1");
                    case TAN, SEC -> r.add("cos(" + a + ") ≠ 0");
                    case COT, CSC -> r.add("sen(" + a + ") ≠ 0");
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }

    /** "Todos los (x, y) del plano" o las restricciones unidas con "y". */
    public static String textoDominio(List<String> restricciones, List<Character> letras) {
        String punto = "(" + String.join(", ", letras.stream().map(String::valueOf).toList()) + ")";
        if (restricciones.isEmpty()) {
            return "Todos los " + punto + (letras.size() == 2 ? " del plano" : letras.size() == 3 ? " del espacio" : "");
        }
        return "Los " + punto + " con " + String.join(" y ", restricciones);
    }

    // ================================================================== Análisis: parciales y hessiana

    /**
     * Las parciales de primer y segundo orden y la matriz hessiana (la usará el Sprint 8).
     *
     * @param primeras  "f_x" → 2xy…, en el orden de las letras
     * @param segundas  "f_xx", "f_xy"…, en el orden de la matriz
     * @param hessiana  H[i][j] = ∂²f/∂xᵢ∂xⱼ
     */
    public record TablaParciales(List<Character> variables, Map<String, Expresion> primeras,
                                 Map<String, Expresion> segundas, List<List<Expresion>> hessiana) {
    }

    public static TablaParciales tabla(Expresion f) {
        List<Character> letras = variables(f, 0);
        Map<String, Expresion> primeras = new LinkedHashMap<>();
        Map<String, Expresion> segundas = new LinkedHashMap<>();
        Map<Character, Expresion> d1 = new HashMap<>();
        for (char v : letras) {
            Expresion fv = simple(Derivador.derivar(f, v).derivada(), v);
            d1.put(v, fv);
            primeras.put("f" + v, fv);
        }
        List<List<Expresion>> h = new ArrayList<>();
        for (char a : letras) {
            List<Expresion> fila = new ArrayList<>();
            for (char b : letras) {
                Expresion fab = simple(Derivador.derivar(d1.get(a), b).derivada(), b);
                fila.add(fab);
                segundas.putIfAbsent("f" + a + b, fab);
            }
            h.add(List.copyOf(fila));
        }
        return new TablaParciales(letras, primeras, segundas, List.copyOf(h));
    }

    // ================================================================== verificación numérica

    private static Map<Character, ComplejoDecimal> puntoDePrueba(List<Character> letras, Expresion... es) {
        java.util.SortedSet<Character> todas = new java.util.TreeSet<>(letras);
        for (Expresion e : es) {
            todas.addAll(Expresiones.variables(e));
        }
        Map<Character, ComplejoDecimal> r = new HashMap<>();
        int i = 0;
        for (char c : todas) {
            r.put(c, ComplejoDecimal.de(COORDENADAS.get(i++ % COORDENADAS.size()), MC));
        }
        return r;
    }

    /** ∂f/∂v en un punto, con el cociente incremental centrado, contra el resultado. */
    static boolean verificar(Expresion f, char v, Expresion derivada, List<Character> letras) {
        try {
            Map<Character, ComplejoDecimal> pt = puntoDePrueba(letras, f, derivada);
            pt.putIfAbsent(v, ComplejoDecimal.de(COORDENADAS.getFirst(), MC));
            Map<Character, ComplejoDecimal> mas = new HashMap<>(pt);
            Map<Character, ComplejoDecimal> menos = new HashMap<>(pt);
            mas.put(v, pt.get(v).sumar(ComplejoDecimal.real(H), MC));
            menos.put(v, pt.get(v).restar(ComplejoDecimal.real(H), MC));
            ComplejoDecimal num = EvaluacionNumerica.evaluar(f, mas, MC).restar(EvaluacionNumerica.evaluar(f, menos, MC), MC)
                    .dividir(ComplejoDecimal.real(H.multiply(BigDecimal.valueOf(2))), MC);
            ComplejoDecimal exacto = EvaluacionNumerica.evaluar(derivada, pt, MC);
            return num.cercano(exacto, 15, MC);
        } catch (RuntimeException fuera) {
            return false;
        }
    }

    private static boolean verificarDireccional(Expresion f, List<Character> letras, Map<Character, Expresion> pt,
                                                List<Expresion> u, Expresion valor) {
        try {
            Map<Character, ComplejoDecimal> mas = new HashMap<>();
            Map<Character, ComplejoDecimal> menos = new HashMap<>();
            for (int i = 0; i < letras.size(); i++) {
                ComplejoDecimal xi = EvaluacionNumerica.evaluar(pt.get(letras.get(i)), Map.of(), MC);
                ComplejoDecimal ui = EvaluacionNumerica.evaluar(u.get(i), Map.of(), MC).multiplicar(H, MC);
                mas.put(letras.get(i), xi.sumar(ui, MC));
                menos.put(letras.get(i), xi.restar(ui, MC));
            }
            ComplejoDecimal num = EvaluacionNumerica.evaluar(f, mas, MC).restar(EvaluacionNumerica.evaluar(f, menos, MC), MC)
                    .dividir(ComplejoDecimal.real(H.multiply(BigDecimal.valueOf(2))), MC);
            return num.cercano(EvaluacionNumerica.evaluar(valor, Map.of(), MC), 15, MC);
        } catch (RuntimeException fuera) {
            return false;
        }
    }

    private static boolean verificarCadena(ReglaCadena c, Expresion resultado) {
        Expresion compuesta = c.derivada().cuerpo();
        for (var e : c.sustituciones().entrySet()) {
            compuesta = Expresiones.reemplazar(compuesta, new Variable(e.getKey()), e.getValue());
        }
        return verificar(compuesta, c.derivada().variable(), resultado, List.of(c.derivada().variable()));
    }
}
