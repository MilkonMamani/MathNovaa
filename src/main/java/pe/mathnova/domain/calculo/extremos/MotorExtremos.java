package pe.mathnova.domain.calculo.extremos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.Polinomios;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Clase;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Punto;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Tipo;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.parciales.MotorParciales;
import pe.mathnova.domain.ecuacion.SistemaPolinomico;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Extremos;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

public final class MotorExtremos {

    public static final String HESSIANO = "V07";
    public static final String LAGRANGE = "V08";
    public static final String PARCIAL = "V01";
    public static final char LAMBDA = 'λ';

    private final SistemaPolinomico sistema;

    public MotorExtremos(OrquestadorFactorizacion orquestador) {
        this.sistema = new SistemaPolinomico(orquestador);
    }

    private static String p(Polinomio q) {
        return ImpresoraPizarra.imprimir(q.aExpresion());
    }

    private static String n(Racional q) {
        return ImpresoraPizarra.numero(q);
    }

    private static String np(Racional q) {
        return q.signo() < 0 || !q.esEntero() ? "(" + n(q) + ")" : n(q);
    }

    private static Polinomio polinomio(Expresion e, String que) {
        try {
            return Polinomio.desde(e);
        } catch (RuntimeException no) {
            throw new IllegalArgumentException("Por ahora los puntos críticos se buscan cuando " + que
                    + " es un polinomio");
        }
    }

    public ResolucionExtremos resolver(Extremos e) {
        return e.conRestriccion() ? lagrange(e) : hessiano(e);
    }

    private ResolucionExtremos hessiano(Extremos e) {
        Expresion fe = e.funcion();
        Polinomio f = polinomio(fe, "f");
        List<Character> letras = MotorParciales.variables(fe, 0);
        if (letras.size() == 1) {
            throw new IllegalArgumentException("f tiene una sola variable: sus puntos críticos se buscan con "
                    + "d/d" + letras.getFirst() + "(f)");
        }
        if (letras.size() != 2) {
            throw new IllegalArgumentException("El criterio del Hessiano se aplica a funciones de dos variables, "
                    + "f(x, y)");
        }
        char x = letras.get(0);
        char y = letras.get(1);
        Polinomio fx = Polinomios.derivar(f, x);
        Polinomio fy = Polinomios.derivar(f, y);
        Polinomio fxx = Polinomios.derivar(fx, x);
        Polinomio fxy = Polinomios.derivar(fx, y);
        Polinomio fyy = Polinomios.derivar(fy, y);
        List<PasoLimite> pasos = new ArrayList<>();
        pasos.add(new PasoLimite(PARCIAL, "Derivadas parciales", "Se deriva f respecto de cada variable, tratando la "
                + "otra como constante", fe, null, new TrabajoIntermedio.Agrupacion(List.of(
                        "∂f/∂" + x + " = " + p(fx), "∂f/∂" + y + " = " + p(fy)))));

        SistemaPolinomico.Resultado s = sistema.resolver(List.of(fx, fy), letras);
        List<String> lineas = new ArrayList<>();
        lineas.add("∂f/∂" + x + " = 0:  " + p(fx) + " = 0");
        lineas.add("∂f/∂" + y + " = 0:  " + p(fy) + " = 0");
        lineas.add(s.soluciones().isEmpty() ? "No hay puntos críticos con coordenadas exactas"
                : "Puntos críticos: " + String.join(", ", s.soluciones().stream()
                        .map(m -> "(" + n(m.get(x)) + ", " + n(m.get(y)) + ")").toList()));
        lineas.addAll(s.avisos());
        pasos.add(new PasoLimite(HESSIANO, "Puntos críticos: ∇f = 0", "Un extremo de f solo puede estar donde las dos "
                + "derivadas parciales valen cero: se resuelve el sistema por sustitución", null, null,
                new TrabajoIntermedio.Agrupacion(lineas), "Resolver el sistema", s.pasos()));

        List<Punto> puntos = new ArrayList<>();
        List<PasoLimite> porPunto = new ArrayList<>();
        for (Map<Character, Racional> m : s.soluciones()) {
            Racional a = fxx.evaluar(m);
            Racional b = fxy.evaluar(m);
            Racional c = fyy.evaluar(m);
            Racional d = a.multiplicar(c).restar(b.multiplicar(b));
            Clase clase = d.signo() > 0 ? (a.signo() > 0 ? Clase.MINIMO : Clase.MAXIMO)
                    : d.signo() < 0 ? Clase.SILLA : Clase.NO_DECIDE;
            Punto pt = new Punto(List.of(m.get(x), m.get(y)), null, f.evaluar(m), a, b, c, d, clase);
            puntos.add(pt);
            String porque = switch (clase) {
                case MINIMO -> "D = " + n(d) + " > 0 y fxx = " + n(a) + " > 0: mínimo local";
                case MAXIMO -> "D = " + n(d) + " > 0 y fxx = " + n(a) + " < 0: máximo local";
                case SILLA -> "D = " + n(d) + " < 0: punto de silla";
                default -> "D = 0: el criterio no decide (RN39); hace falta mirar f cerca del punto";
            };
            porPunto.add(new PasoLimite(HESSIANO, "En " + pt.texto(), "Se evalúa la matriz hessiana en el punto", null,
                    null, new TrabajoIntermedio.Agrupacion(List.of(
                            "H" + pt.texto() + " = [ " + n(a) + "   " + n(b) + " ;  " + n(b) + "   " + n(c) + " ]",
                            "D = " + np(a) + "·" + np(c) + " − " + np(b) + "² = " + n(d),
                            porque))));
        }
        if (!puntos.isEmpty()) {
            pasos.add(new PasoLimite(HESSIANO, "Criterio del Hessiano", "En cada punto crítico D = fxx·fyy − fxy²: "
                    + "D > 0 y fxx > 0 es mínimo, D > 0 y fxx < 0 es máximo, D < 0 es silla y D = 0 no decide", null,
                    null, new TrabajoIntermedio.Agrupacion(List.of(
                            "f" + x + x + " = " + p(fxx) + ",  f" + y + y + " = " + p(fyy) + ",  f" + x + y + " = "
                                    + p(fxy),
                            "H = [ " + p(fxx) + "   " + p(fxy) + " ;  " + p(fxy) + "   " + p(fyy) + " ]",
                            "D = (" + p(fxx) + ")(" + p(fyy) + ") − (" + p(fxy) + ")²")),
                    "Un punto a la vez", porPunto));
        }
        List<List<Expresion>> h = List.of(List.of(fxx.aExpresion(), fxy.aExpresion()),
                List.of(fxy.aExpresion(), fyy.aExpresion()));
        return new ResolucionExtremos(e, Tipo.HESSIANO, fe, letras, List.of(fx.aExpresion(), fy.aExpresion()), h,
                null, puntos, pasos, s.avisos(), s.completo());
    }

    private ResolucionExtremos lagrange(Extremos e) {
        Expresion fe = e.funcion();
        Polinomio f = polinomio(fe, "f");
        Polinomio g = polinomio(e.restriccion().izquierda(), "la restricción")
                .restar(polinomio(e.restriccion().derecha(), "la restricción"));
        TreeSet<Character> todas = new TreeSet<>(f.variables());
        todas.addAll(g.variables());
        if (todas.contains(LAMBDA)) {
            throw new IllegalArgumentException("λ es el multiplicador de Lagrange: usa otras letras en f y en g");
        }
        List<Character> letras = MotorParciales.variables(new pe.mathnova.domain.expresion.Suma(List.of(fe,
                e.restriccion().izquierda(), e.restriccion().derecha())), 0);
        if (letras.size() < 2) {
            throw new IllegalArgumentException("Con una restricción, f y g deben tener al menos dos variables");
        }
        if (g.esConstante()) {
            throw new IllegalArgumentException("La restricción no tiene variables");
        }
        List<PasoLimite> pasos = new ArrayList<>();
        List<String> planteo = new ArrayList<>();
        List<Polinomio> ecuaciones = new ArrayList<>();
        List<String> gradF = new ArrayList<>();
        List<String> gradG = new ArrayList<>();
        List<Expresion> grad = new ArrayList<>();
        for (char v : letras) {
            Polinomio fv = Polinomios.derivar(f, v);
            Polinomio gv = Polinomios.derivar(g, v);
            gradF.add(p(fv));
            gradG.add(p(gv));
            grad.add(fv.aExpresion());
            ecuaciones.add(fv.restar(gv.multiplicar(Polinomio.variable(LAMBDA))));
            String lg = gv.esConstante() && gv.constante().equals(Racional.UNO) ? "λ"
                    : gv.esConstante() ? n(gv.constante()) + "λ" : "λ·(" + p(gv) + ")";
            planteo.add("∂f/∂" + v + " = λ·∂g/∂" + v + ":  " + p(fv) + " = " + lg);
        }
        ecuaciones.add(g);
        planteo.add("g = 0:  " + ImpresoraPizarra.imprimir(e.restriccion()));
        planteo.addFirst("∇g = (" + String.join(", ", gradG) + ")");
        planteo.addFirst("∇f = (" + String.join(", ", gradF) + ")");
        pasos.add(new PasoLimite(LAGRANGE, "Planteo: ∇f = λ∇g y g = 0", "En un extremo sobre la restricción los "
                + "gradientes de f y g son paralelos: ∇f = λ∇g, con λ el multiplicador", fe, null,
                new TrabajoIntermedio.Agrupacion(planteo)));

        List<Character> incognitas = new ArrayList<>(letras);
        incognitas.add(LAMBDA);
        SistemaPolinomico.Resultado s = sistema.resolver(ecuaciones, incognitas);
        List<String> lineas = new ArrayList<>();
        ecuaciones.forEach(q -> lineas.add(p(q) + " = 0"));
        for (Map<Character, Racional> m : s.soluciones()) {
            lineas.add(punto(m, letras) + " con λ = " + n(m.get(LAMBDA)));
        }
        if (s.soluciones().isEmpty()) {
            lineas.add("No hay puntos con coordenadas exactas");
        }
        lineas.addAll(s.avisos());
        pasos.add(new PasoLimite(LAGRANGE, "Resolver el sistema", "Son " + ecuaciones.size() + " ecuaciones con "
                + incognitas.size() + " incógnitas (" + String.join(", ", incognitas.stream().map(String::valueOf)
                .toList()) + "): se resuelven por sustitución", null, null, new TrabajoIntermedio.Agrupacion(lineas),
                "Sustituciones", s.pasos()));

        List<Punto> puntos = clasificar(f, g, letras, s.soluciones(), pasos);
        return new ResolucionExtremos(e, Tipo.LAGRANGE, fe, letras, grad, List.of(), g.aExpresion(), puntos, pasos,
                s.avisos(), s.completo());
    }

    private static String punto(Map<Character, Racional> m, List<Character> letras) {
        return "(" + String.join(", ", letras.stream().map(v -> n(m.get(v))).toList()) + ")";
    }

    private static List<Punto> clasificar(Polinomio f, Polinomio g, List<Character> letras,
                                          List<Map<Character, Racional>> soluciones, List<PasoLimite> pasos) {
        List<Racional> valores = soluciones.stream().map(f::evaluar).toList();
        List<String> lineas = new ArrayList<>();
        for (int i = 0; i < soluciones.size(); i++) {
            lineas.add("f" + punto(soluciones.get(i), letras) + " = " + n(valores.get(i)));
        }
        List<Clase> clases = new ArrayList<>();
        if (soluciones.size() >= 2) {
            Racional max = valores.stream().max(Racional::compareTo).orElseThrow();
            Racional min = valores.stream().min(Racional::compareTo).orElseThrow();
            for (Racional v : valores) {
                clases.add(max.equals(min) ? Clase.SIN_CLASIFICAR : v.equals(max) ? Clase.MAXIMO_RESTRINGIDO
                        : v.equals(min) ? Clase.MINIMO_RESTRINGIDO : Clase.SIN_CLASIFICAR);
            }
            lineas.add(max.equals(min) ? "f vale lo mismo en todos: la comparación no decide"
                    : "El mayor valor, " + n(max) + ", es el máximo; el menor, " + n(min) + ", es el mínimo");
        } else if (soluciones.size() == 1) {
            Map<Character, Racional> p0 = soluciones.getFirst();
            List<Map<Character, Racional>> vecinos = vecinos(g, letras, p0);
            Racional v0 = valores.getFirst();
            int mayores = 0;
            int menores = 0;
            for (Map<Character, Racional> q : vecinos) {
                Racional v = f.evaluar(q);
                lineas.add("f" + punto(q, letras) + " = " + n(v) + " (otro punto de la restricción)");
                int c = v.compareTo(v0);
                if (c > 0) {
                    mayores++;
                } else if (c < 0) {
                    menores++;
                }
            }
            Clase clase = vecinos.isEmpty() ? Clase.SIN_CLASIFICAR : menores == vecinos.size()
                    ? Clase.MAXIMO_RESTRINGIDO : mayores == vecinos.size() ? Clase.MINIMO_RESTRINGIDO
                    : Clase.SIN_CLASIFICAR;
            clases.add(clase);
            lineas.add(switch (clase) {
                case MAXIMO_RESTRINGIDO -> "En los otros puntos de la restricción f es menor que " + n(v0)
                        + ": " + punto(p0, letras) + " es el máximo";
                case MINIMO_RESTRINGIDO -> "En los otros puntos de la restricción f es mayor que " + n(v0)
                        + ": " + punto(p0, letras) + " es el mínimo";
                default -> "La comparación no decide si es máximo o mínimo";
            });
        }
        List<Punto> puntos = new ArrayList<>();
        for (int i = 0; i < soluciones.size(); i++) {
            Map<Character, Racional> m = soluciones.get(i);
            puntos.add(new Punto(letras.stream().map(m::get).toList(), m.get(LAMBDA), valores.get(i), null, null,
                    null, null, clases.get(i)));
        }
        if (!soluciones.isEmpty()) {
            pasos.add(new PasoLimite(LAGRANGE, "Comparar f", "Se evalúa f en los puntos hallados para decir cuál es el "
                    + "máximo y cuál el mínimo sobre la restricción", null, null,
                    new TrabajoIntermedio.Agrupacion(lineas)));
        }
        return puntos;
    }

    private static List<Map<Character, Racional>> vecinos(Polinomio g, List<Character> letras,
                                                           Map<Character, Racional> p0) {
        for (char v : letras) {
            if (g.gradoEn(v) != 1) {
                continue;
            }
            TreeMap<Integer, Polinomio> pv = Polinomios.enPotenciasDe(g, v);
            if (!pv.get(1).esConstante()) {
                continue;
            }
            Polinomio despeje = pv.getOrDefault(0, Polinomio.CERO).negar().multiplicar(pv.get(1).constante().inverso());
            List<Map<Character, Racional>> r = new ArrayList<>();
            for (char u : letras) {
                if (u == v) {
                    continue;
                }
                for (int delta : new int[] {-1, 1}) {
                    Map<Character, Racional> q = new LinkedHashMap<>();
                    for (char w : letras) {
                        if (w != v) {
                            q.put(w, w == u ? p0.get(w).sumar(Racional.de(delta)) : p0.get(w));
                        }
                    }
                    Racional vv = despeje.evaluar(q);
                    Map<Character, Racional> ordenado = new LinkedHashMap<>();
                    for (char w : letras) {
                        ordenado.put(w, w == v ? vv : q.get(w));
                    }
                    r.add(ordenado);
                }
            }
            return r;
        }
        return List.of();
    }
}
