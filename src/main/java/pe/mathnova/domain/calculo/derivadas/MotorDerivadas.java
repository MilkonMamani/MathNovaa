package pe.mathnova.domain.calculo.derivadas;

import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.Anidados;
import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * HU12 y HU25: resuelve d/dx(f), dⁿ/dxⁿ(f) y la derivación implícita d/dx(F(x, y) = G(x, y)), con un
 * paso por regla (D01…D09, las internas anidadas y plegadas) y al final "Simplificar". Si f es una
 * fracción de polinomios de una variable, busca también los puntos críticos (RF28) con el resolvedor de
 * ecuaciones y los clasifica con f″ o con el cambio de signo de f′.
 */
public final class MotorDerivadas {

    /** El paso final que deja el resultado en su forma de pizarra. */
    public static final String SIMPLIFICAR = "SIMP";
    /** RF28. */
    public static final String PUNTOS_CRITICOS = "PCR";
    /** Exploración: expandir el polinomio y derivar término a término. */
    public static final String EXPANDIR = "EXP";
    /** Exploración: f/g como f·g⁻¹. */
    public static final String COMO_PRODUCTO = "PROD";
    /** Exploración: f^g como e^(g·ln f). */
    public static final String COMO_EXPONENCIAL = "EXPO";

    private final ResolvedorEcuaciones resolvedor;

    public MotorDerivadas(OrquestadorFactorizacion orquestador) {
        this.resolvedor = new ResolvedorEcuaciones(orquestador);
    }

    public ResolucionDerivada resolver(Derivada d) {
        return resolver(d, null);
    }

    /**
     * @param forzado un camino de Exploración ({@link #EXPANDIR}, {@link #COMO_PRODUCTO},
     *                {@link #COMO_EXPONENCIAL}) con que se empieza, o null
     * @throws IllegalArgumentException si no se puede derivar (una implícita sin y, por ejemplo)
     */
    public ResolucionDerivada resolver(Derivada d, String forzado) {
        if (d.cuerpo() instanceof Ecuacion ec) {
            return implicita(d, ec);
        }
        char x = d.variable();
        Expresion f = d.cuerpo();
        List<PasoLimite> pasos = new ArrayList<>();
        Expresion actual = f;
        for (int k = 1; k <= d.orden(); k++) {
            Expresion aDerivar = actual;
            if (k == 1 && forzado != null) {
                Optional<Expresion> otra = reescribir(f, forzado, x);
                if (otra.isPresent()) {
                    aDerivar = otra.get();
                    pasos.add(new PasoLimite(forzado, nombreCamino(forzado), porQueCamino(forzado), f, aDerivar,
                            new TrabajoIntermedio.Agrupacion(List.of(pizarra(f) + " = " + pizarra(aDerivar)))));
                }
            }
            Derivador.Resultado r = Derivador.derivar(aDerivar, x);
            Expresion simple = FormaFinal.de(r.derivada(), x);
            List<PasoLimite> reglas = new ArrayList<>();
            r.arbol().forEach(p -> reglas.add(Anidados.deDerivada(p)));
            if (d.orden() == 1) {
                pasos.addAll(reglas);
                pasos.add(simplificar(r.derivada(), simple));
            } else {
                String codigo = r.arbol().isEmpty() ? "D01" : r.arbol().getFirst().codigo();
                String nombre = ordinal(k) + " derivada";
                pasos.add(new PasoLimite(codigo, nombre, "Se deriva " + pizarra(actual) + " respecto de " + x
                        + (k == 1 ? "" : ": la " + ordinal(k).toLowerCase() + " derivada es la derivada de la "
                                + ordinal(k - 1).toLowerCase()), actual, simple,
                        new TrabajoIntermedio.Agrupacion(List.of(primas(k, x) + " = " + pizarra(simple))),
                        "Reglas de la " + ordinal(k).toLowerCase() + " derivada", reglas));
            }
            actual = simple;
        }
        AnalisisDerivada analisis = d.orden() == 1 ? AnalisisDerivada.de(f, x, resolvedor).orElse(null) : null;
        if (analisis != null) {
            pasos.add(puntosCriticos(analisis));
        }
        return new ResolucionDerivada(d, f, actual, pasos, (char) 0, analisis, verificar(f, actual, x, d.orden()));
    }

    /** RF28 para cualquier f de una variable (también fuera de una derivada, como en una sugerencia). */
    public Optional<AnalisisDerivada> analisis(Expresion f, char x) {
        return AnalisisDerivada.de(f, x, resolvedor);
    }

    private static String pizarra(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static String ordinal(int k) {
        return switch (k) {
            case 1 -> "Primera";
            case 2 -> "Segunda";
            case 3 -> "Tercera";
            case 4 -> "Cuarta";
            case 5 -> "Quinta";
            default -> k + ".ª";
        };
    }

    private static String primas(int k, char x) {
        return "f" + (k <= 3 ? "'".repeat(k) : "⁽" + k + "⁾") + "(" + x + ")";
    }

    private static PasoLimite simplificar(Expresion antes, Expresion despues) {
        String a = pizarra(antes);
        String b = pizarra(despues);
        String porque = a.equals(b) ? "La derivada ya está en su forma más simple"
                : "Se reúnen los términos semejantes, se expande lo que es polinomio y se saca el factor común; "
                        + "el resultado vale lo mismo";
        List<String> lineas = a.equals(b) ? List.of(b) : List.of(a, "= " + b);
        return new PasoLimite(SIMPLIFICAR, "Simplificar", porque, antes, despues, new TrabajoIntermedio.Agrupacion(lineas));
    }

    private static PasoLimite puntosCriticos(AnalisisDerivada a) {
        char x = a.variable();
        List<String> lineas = new ArrayList<>();
        Expresion numerador = a.ecuacion().entrada().izquierda();
        lineas.add("f'(" + x + ") = 0  →  " + pizarra(Simplificador.simplificar(numerador)) + " = 0");
        if (a.criticos().isEmpty()) {
            lineas.add(a.crecimiento().aviso() != null ? a.crecimiento().aviso()
                    : "No hay puntos críticos racionales.");
        }
        for (AnalisisDerivada.PuntoCritico c : a.criticos()) {
            lineas.add(x + " = " + ImpresoraPizarra.numero(c.x()) + ": " + c.criterio() + "  →  " + c.tipo().texto()
                    + " en (" + ImpresoraPizarra.numero(c.x()) + ", " + ImpresoraPizarra.numero(c.y()) + ")");
        }
        return new PasoLimite(PUNTOS_CRITICOS, "Puntos críticos", "Se resuelve f'(" + x + ") = 0 con el resolvedor de "
                + "ecuaciones y cada punto se clasifica con el signo de f'' (o con el cambio de signo de f')", null,
                null, new TrabajoIntermedio.Agrupacion(lineas), "Resolver f'(" + x + ") = 0", Anidados.deEcuacion(a.ecuacion()));
    }

    // ------------------------------------------------------------------ D08

    private ResolucionDerivada implicita(Derivada d, Ecuacion ec) {
        char x = d.variable();
        SortedSet<Character> otras = new TreeSet<>(Expresiones.variables(ec));
        otras.remove(x);
        if (otras.size() != 1) {
            throw new IllegalArgumentException(otras.isEmpty()
                    ? "Para derivar implícitamente la ecuación debe tener otra letra además de " + x
                    : "La derivación implícita es con dos letras (" + x + " y otra); esta tiene " + (otras.size() + 1));
        }
        char y = otras.first();
        Derivador.Resultado izq = Derivador.derivarImplicita(ec.izquierda(), x, y);
        Derivador.Resultado der = Derivador.derivarImplicita(ec.derecha(), x, y);
        Expresion l = FormaFinal.ordenar(izq.derivada(), x);
        Expresion r = FormaFinal.ordenar(der.derivada(), x);
        List<PasoLimite> hijos = new ArrayList<>();
        izq.arbol().forEach(p -> hijos.add(Anidados.deDerivada(p, "lado izquierdo")));
        der.arbol().forEach(p -> hijos.add(Anidados.deDerivada(p, "lado derecho")));
        List<PasoLimite> pasos = new ArrayList<>();
        pasos.add(new PasoLimite("D08", "Derivar ambos lados respecto de " + x, y + " depende de " + x
                + ": se derivan los dos lados y, cada vez que se deriva un término con " + y + ", aparece " + y
                + "' (regla de la cadena)", ec, new Ecuacion(l, r), new TrabajoIntermedio.Agrupacion(List.of(
                        "d/d" + x + "(" + pizarra(ec.izquierda()) + ") = d/d" + x + "(" + pizarra(ec.derecha()) + ")",
                        pizarra(l) + " = " + pizarra(r))), "Derivar cada lado", hijos));

        Expresion prima = Derivador.prima(y, x);
        Expresion e = Simplificador.simplificar(Simplificador.suma(List.of(izq.derivada(),
                Simplificador.negar(der.derivada()))));
        Expresion a = Simplificador.simplificar(Expresiones.reemplazar(e, prima, Constante.de(0)));
        Expresion b = Simplificador.simplificar(Simplificador.suma(List.of(
                Expresiones.reemplazar(e, prima, Constante.de(1)), Simplificador.negar(a))));
        if (b.equals(Constante.de(0)) || Expresiones.contiene(b, prima) || !lineal(e, a, b, prima, x, y)) {
            throw new IllegalArgumentException("No se pudo despejar " + y + "': no aparece de forma lineal");
        }
        Expresion bruto = new Cociente(Simplificador.negar(a), b);
        Expresion despeje = FormaFinal.de(Simplificador.cociente(Simplificador.negar(a), b), x);
        String coef = b instanceof Suma ? "(" + pizarra(FormaFinal.ordenar(b, x)) + ")" : pizarra(FormaFinal.ordenar(b, x));
        String yp = y + "'";
        pasos.add(new PasoLimite("D08", "Despejar " + yp, "Los términos con " + yp + " quedan a un lado y los demás "
                + "pasan al otro; luego se divide entre lo que multiplica a " + yp, new Ecuacion(l, r),
                new Ecuacion(prima, despeje), new TrabajoIntermedio.Agrupacion(List.of(
                        coef + "·" + yp + " = " + pizarra(FormaFinal.ordenar(Simplificador.negar(a), x)),
                        yp + " = " + pizarra(FormaFinal.ordenar(bruto, x)) + " = " + pizarra(despeje)))));
        boolean anula = cero(Expresiones.reemplazar(e, prima, despeje), x, y);
        return new ResolucionDerivada(d, ec, despeje, pasos, y, null, anula);
    }

    // ------------------------------------------------------------------ verificación

    private static final java.math.BigDecimal H = java.math.BigDecimal.ONE.movePointLeft(20);
    private static final MathContext MC90 = new MathContext(90);
    private static final List<Racional> PUNTOS = List.of(Racional.de(1, 3), Racional.de(13, 11), Racional.de(17, 9),
            Racional.de(5, 2), Racional.de(3, 7));

    /**
     * (f(x + h) − f(x − h))/(2h) con h = 10⁻²⁰ y 90 cifras en tres puntos donde f está definida; en orden n
     * se compara la derivada n − 1 con la n. Las otras letras valen 5/7.
     */
    static boolean verificar(Expresion f, Expresion derivada, char x, int orden) {
        try {
            Expresion anterior = f;
            for (int k = 1; k < orden; k++) {
                anterior = Derivador.derivar(anterior, x).derivada();
            }
            int buenos = 0;
            for (Racional p : PUNTOS) {
                java.math.BigDecimal x0 = new java.math.BigDecimal(p.num()).divide(new java.math.BigDecimal(p.den()), MC90);
                try {
                    java.math.BigDecimal mas = valor(anterior, x, x0.add(H));
                    java.math.BigDecimal menos = valor(anterior, x, x0.subtract(H));
                    java.math.BigDecimal incremental = mas.subtract(menos).divide(H.multiply(java.math.BigDecimal.TWO), MC90);
                    java.math.BigDecimal d = valor(derivada, x, x0);
                    java.math.BigDecimal tolerancia = java.math.BigDecimal.ONE.movePointLeft(25)
                            .multiply(java.math.BigDecimal.ONE.max(d.abs()));
                    if (incremental.subtract(d).abs().compareTo(tolerancia) > 0) {
                        return false;
                    }
                    buenos++;
                } catch (ArithmeticException fuera) {
                    // no definida en ese punto
                }
                if (buenos == 3) {
                    return true;
                }
            }
            return buenos > 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static java.math.BigDecimal valor(Expresion e, char x, java.math.BigDecimal v) {
        Map<Character, ComplejoDecimal> valores = new HashMap<>();
        for (char letra : Expresiones.variables(e)) {
            valores.put(letra, ComplejoDecimal.de(Racional.de(5, 7), MC90));
        }
        valores.put(x, ComplejoDecimal.real(v));
        ComplejoDecimal z = EvaluacionNumerica.evaluar(e, valores, MC90);
        if (!z.esReal(80)) {
            throw new ArithmeticException("No es real");
        }
        return z.re();
    }

    /** e vale 0 en tres puntos (x, y). */
    private static boolean cero(Expresion e, char x, char y) {
        MathContext mc = EvaluacionNumerica.MC;
        Racional[][] puntos = {{Racional.de(1, 3), Racional.de(5, 7)}, {Racional.de(13, 11), Racional.de(-3, 4)},
                {Racional.de(-17, 9), Racional.de(7, 5)}};
        for (Racional[] p : puntos) {
            Map<Character, ComplejoDecimal> v = new HashMap<>();
            v.put(x, ComplejoDecimal.de(p[0], mc));
            v.put(y, ComplejoDecimal.de(p[1], mc));
            try {
                if (!EvaluacionNumerica.evaluar(e, v, mc).cercano(ComplejoDecimal.CERO, EvaluacionNumerica.CIFRAS_IGUALES, mc)) {
                    return false;
                }
            } catch (ArithmeticException fuera) {
                // fuera del dominio
            }
        }
        return true;
    }

    /** e(y' = 2) = a + 2b en tres puntos: y' aparece de forma lineal. */
    private static boolean lineal(Expresion e, Expresion a, Expresion b, Expresion prima, char x, char y) {
        Expresion conDos = Expresiones.reemplazar(e, prima, Constante.de(2));
        Expresion esperado = Simplificador.suma(List.of(a, Simplificador.producto(List.of(Constante.de(2), b))));
        MathContext mc = EvaluacionNumerica.MC;
        Racional[][] puntos = {{Racional.de(1, 3), Racional.de(5, 7)}, {Racional.de(13, 11), Racional.de(-3, 4)},
                {Racional.de(-17, 9), Racional.de(7, 5)}};
        for (Racional[] p : puntos) {
            Map<Character, ComplejoDecimal> v = new HashMap<>();
            v.put(x, ComplejoDecimal.de(p[0], mc));
            v.put(y, ComplejoDecimal.de(p[1], mc));
            try {
                ComplejoDecimal u = EvaluacionNumerica.evaluar(conDos, v, mc);
                ComplejoDecimal w = EvaluacionNumerica.evaluar(esperado, v, mc);
                if (!u.cercano(w, EvaluacionNumerica.CIFRAS_IGUALES, mc)) {
                    return false;
                }
            } catch (ArithmeticException fuera) {
                // fuera del dominio en ese punto
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ Exploración

    public static String nombreCamino(String codigo) {
        return switch (codigo) {
            case EXPANDIR -> "Expandir primero y derivar término a término";
            case COMO_PRODUCTO -> "Escribir como producto f·g⁻¹";
            case COMO_EXPONENCIAL -> "Escribir como e^(g·ln f) y usar la cadena";
            default -> PasoDerivada.nombre(codigo);
        };
    }

    private static String porQueCamino(String codigo) {
        return switch (codigo) {
            case EXPANDIR -> "Es un polinomio: se expande y queda una suma de potencias, que se deriva término a término";
            case COMO_PRODUCTO -> "Un cociente f/g es el producto f·g⁻¹: se deriva con la regla del producto y la cadena";
            case COMO_EXPONENCIAL -> "f^g = e^(g·ln f): se deriva con la regla de la cadena";
            default -> "Se reescribe la función";
        };
    }

    /** f escrita de otra forma para el camino forzado; vacío si ese camino no aplica. */
    private static Optional<Expresion> reescribir(Expresion f, String camino, char x) {
        switch (camino) {
            case EXPANDIR -> {
                if (f instanceof Suma && sinProductos(f)) {
                    return Optional.empty();
                }
                try {
                    Polinomio p = Polinomio.desde(f);
                    return Optional.of(Simplificador.simplificar(p.aExpresion()));
                } catch (RuntimeException noEs) {
                    return Optional.empty();
                }
            }
            case COMO_PRODUCTO -> {
                if (f instanceof Cociente c && Expresiones.variables(c.denominador()).contains(x)) {
                    return Optional.of(new Producto(List.of(c.numerador(),
                            new Potencia(c.denominador(), Constante.de(-1)))));
                }
                return Optional.empty();
            }
            case COMO_EXPONENCIAL -> {
                if (f instanceof Potencia p && Expresiones.variables(p.base()).contains(x)
                        && Expresiones.variables(p.exponente()).contains(x)) {
                    return Optional.of(new Potencia(new ConstanteEspecial(ConstanteEspecial.Tipo.E),
                            new Producto(List.of(p.exponente(), new Funcion(TipoFuncion.LN, p.base())))));
                }
                return Optional.empty();
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /** Una suma de términos c·xⁿ: ya está expandida. */
    private static boolean sinProductos(Expresion f) {
        return ((Suma) f).terminos().stream().noneMatch(MotorDerivadas::tieneSuma);
    }

    private static boolean tieneSuma(Expresion e) {
        return switch (e) {
            case Suma s -> true;
            case Producto p -> p.factores().stream().anyMatch(MotorDerivadas::tieneSuma);
            case Potencia p -> tieneSuma(p.base());
            case pe.mathnova.domain.expresion.Negacion n -> tieneSuma(n.argumento());
            default -> false;
        };
    }

    /**
     * Exploración: el camino usado (la regla de afuera, o el camino forzado), los otros que también llegan
     * a la misma derivada (con lo que dan) y los que no aplican con su motivo.
     */
    public List<CaminoCalculo> caminos(ResolucionDerivada r) {
        List<CaminoCalculo> caminos = new ArrayList<>();
        if (r.esImplicita()) {
            caminos.add(new CaminoCalculo("D08", PasoDerivada.nombre("D08"), CaminoCalculo.Estado.USADO, null));
            caminos.add(new CaminoCalculo(EXPANDIR, "Despejar " + r.implicita() + " y derivar",
                    CaminoCalculo.Estado.NO_APLICA, "No siempre se puede despejar " + r.implicita()
                    + "; la derivación implícita no lo necesita"));
            return caminos;
        }
        char x = r.variable();
        Expresion f = r.funcion();
        String usado = r.pasos().isEmpty() ? "D01" : r.pasos().getFirst().codigo();
        caminos.add(new CaminoCalculo(usado, nombreCamino(usado), CaminoCalculo.Estado.USADO, null));
        for (String c : List.of(EXPANDIR, COMO_PRODUCTO, COMO_EXPONENCIAL)) {
            if (c.equals(usado)) {
                continue;
            }
            Optional<Expresion> otra = reescribir(f, c, x);
            if (otra.isPresent()) {
                try {
                    ResolucionDerivada con = resolver(r.entrada(), c);
                    caminos.add(new CaminoCalculo(c, nombreCamino(c), CaminoCalculo.Estado.SIRVE,
                            pizarra(otra.get()) + "  →  " + r.nombre() + " = " + pizarra(con.resultado())));
                } catch (RuntimeException e) {
                    caminos.add(new CaminoCalculo(c, nombreCamino(c), CaminoCalculo.Estado.NO_APLICA, e.getMessage()));
                }
            } else {
                String motivo = motivo(f, c, x);
                if (motivo != null) {
                    caminos.add(new CaminoCalculo(c, nombreCamino(c), CaminoCalculo.Estado.NO_APLICA, motivo));
                }
            }
        }
        return caminos;
    }

    /** Por qué no aplica; null si no vale la pena mostrarlo. */
    private static String motivo(Expresion f, String camino, char x) {
        return switch (camino) {
            case EXPANDIR -> f instanceof Suma ? "Ya es una suma: se deriva término a término"
                    : Expresiones.esTrascendente(f) || f instanceof Cociente
                    ? "No es un polinomio: hay funciones o divisiones que no se expanden" : null;
            case COMO_PRODUCTO -> f instanceof Producto ? "Ya es un producto" : null;
            case COMO_EXPONENCIAL -> f instanceof Potencia ? "El exponente no depende de " + x
                    + ": basta la regla de la potencia" : null;
            default -> null;
        };
    }
}
