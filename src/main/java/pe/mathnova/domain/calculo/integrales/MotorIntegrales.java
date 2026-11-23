package pe.mathnova.domain.calculo.integrales;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.Anidados;
import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.FuncionRacional;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.derivadas.FormaFinal;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * HU13: resuelve ∫(f, x) y ∫(f, x, a, b) con los métodos en este orden (RF29–RF32):
 * <ol>
 *   <li>I01: inmediata o término a término, con la tabla de integrales básicas;</li>
 *   <li>I02: sustitución u = g(x) si el integrando es f(g(x))·g′(x) salvo una constante;</li>
 *   <li>un polinomio escrito como producto o potencia se expande y se integra término a término;</li>
 *   <li>P02 + I06: si es racional impropia se divide primero (RN17) y después fracciones parciales, con
 *       el denominador factorizado por el orquestador;</li>
 *   <li>I04: integrales trigonométricas (senᵐ·cosⁿ, tanⁿ, secⁿ) y I05: sustitución trigonométrica, con el
 *       triángulo para regresar a x (Sprint 7);</li>
 *   <li>I03: por partes con ILATE, repetida hasta cuatro veces y con la integral cíclica (Sprint 7);</li>
 *   <li>I07: en una definida, la regla de Barrow F(b) − F(a).</li>
 * </ol>
 * Si ninguno aplica no se inventa nada: se dice que hace falta otro método. El resultado se verifica
 * derivándolo (exacto si se puede, si no en cinco puntos).
 */
public final class MotorIntegrales {

    public static final String SIN_METODO = "Ninguno de los métodos de MathNova (inmediata, sustitución, fracciones "
            + "parciales, trigonométricas, sustitución trigonométrica y por partes) resuelve esta integral.";
    public static final String INMEDIATA = "I01";
    public static final String SUSTITUCION = "I02";
    public static final String EXPANDIR = "EXP";
    public static final String FRACCIONES = "I06";
    public static final String DIVISION = "P02";
    public static final String BARROW = "I07";
    public static final String PARTES = IntegracionAvanzada.PARTES;
    public static final String TRIGONOMETRICA = IntegracionAvanzada.TRIGONOMETRICA;
    public static final String SUSTITUCION_TRIG = IntegracionAvanzada.SUSTITUCION_TRIG;
    /** El paso que multiplica por el número de delante: k∫f = k·F. */
    public static final String CONSTANTE = "K";

    private static final List<String> ORDEN = List.of(INMEDIATA, SUSTITUCION, EXPANDIR, FRACCIONES, TRIGONOMETRICA,
            SUSTITUCION_TRIG, PARTES);
    private static final MathContext MC = new MathContext(40);

    private final OrquestadorFactorizacion orquestador;

    public MotorIntegrales(OrquestadorFactorizacion orquestador) {
        this.orquestador = orquestador;
    }

    /** Lo que da un método: F sin + C, sus pasos y el código. */
    record Integracion(Expresion antiderivada, List<PasoLimite> pasos, String metodo) {
    }

    public ResolucionIntegral resolver(Expresion entrada) {
        return resolver(entrada, null);
    }

    /**
     * @param entrada ∫(…) o k·∫(…)
     * @param forzado el método que se prueba primero ("Probar" en Exploración), o null
     */
    public ResolucionIntegral resolver(Expresion entrada, String forzado) {
        Racional k = Racional.UNO;
        Integral in;
        if (entrada instanceof Integral i) {
            in = i;
        } else if (entrada instanceof Producto(List<Expresion> fs) && fs.size() == 2
                && fs.get(0) instanceof Constante c && fs.get(1) instanceof Integral i) {
            k = c.valor();
            in = i;
        } else {
            throw new IllegalArgumentException("No es una integral: " + ImpresoraPizarra.imprimir(entrada));
        }
        char x = in.variable();
        Expresion f = in.cuerpo();
        Optional<Integracion> r = integrar(f, x, forzado);
        if (r.isEmpty()) {
            return new ResolucionIntegral(entrada, in, k, null, null, null, List.of(), null, null, SIN_METODO);
        }
        List<PasoLimite> pasos = new ArrayList<>(r.get().pasos());
        Expresion antiderivada = FormaFinal.ordenar(Simplificador.simplificar(r.get().antiderivada()), x);
        ResolucionIntegral.Verificacion verificacion = verificar(antiderivada, f, x);
        if (!in.esDefinida()) {
            Expresion conK = antiderivada;
            if (!k.equals(Racional.UNO)) {
                conK = FormaFinal.ordenar(Simplificador.simplificar(Simplificador.producto(List.of(new Constante(k),
                        antiderivada))), x);
                pasos.add(constante(k, antiderivada, conK, false));
            }
            return new ResolucionIntegral(entrada, in, k, conK, null, null, pasos, r.get().metodo(), verificacion, null);
        }
        Optional<String> impropia = impropia(f, x, in.inferior(), in.superior());
        if (impropia.isPresent()) {
            return new ResolucionIntegral(entrada, in, k, antiderivada, null, null, pasos, r.get().metodo(),
                    verificacion, impropia.get());
        }
        Expresion valor = barrow(antiderivada, in, pasos);
        if (!k.equals(Racional.UNO)) {
            Expresion conK = Simplificador.simplificar(Simplificador.producto(List.of(new Constante(k), valor)));
            pasos.add(constante(k, valor, conK, true));
            valor = conK;
        }
        Expresion conKF = Simplificador.simplificar(Simplificador.producto(List.of(new Constante(k), antiderivada)));
        return new ResolucionIntegral(entrada, in, k, conKF, valor, decimal(valor), pasos, r.get().metodo(),
                verificacion, null);
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    /** "(x² + 1)" si es una suma, para escribir ∫(…) dx. */
    private static String integrando(Expresion f) {
        return f instanceof Suma ? "(" + p(f) + ")" : p(f);
    }

    private static PasoLimite constante(Racional k, Expresion antes, Expresion despues, boolean definida) {
        String kt = ImpresoraPizarra.numero(k);
        String antesT = antes instanceof Suma || antes instanceof Negacion ? "(" + p(antes) + ")" : p(antes);
        return new PasoLimite(CONSTANTE, "Multiplicar por " + kt, "El número " + kt + " delante de la integral sale como "
                + "factor: k∫f = k·" + (definida ? "(F(b) − F(a))" : "F"), antes, despues,
                new TrabajoIntermedio.Agrupacion(List.of(kt + "·" + antesT + " = " + p(despues))));
    }

    // ------------------------------------------------------------------ la cascada

    private Optional<Integracion> integrar(Expresion f, char x, String forzado) {
        return integrar(f, x, forzado, false);
    }

    /** @param sinPartes true dentro de por partes: para v = ∫dv y ∫v du (por partes se repite por su cuenta) */
    private Optional<Integracion> integrar(Expresion f, char x, String forzado, boolean sinPartes) {
        List<String> orden = new ArrayList<>(ORDEN);
        if (sinPartes) {
            orden.remove(PARTES);
        }
        if (forzado != null && orden.remove(forzado)) {
            orden.addFirst(forzado);
        }
        for (String m : orden) {
            Optional<Integracion> r;
            try {
                r = metodo(m, f, x);
            } catch (RuntimeException e) {
                r = Optional.empty();
            }
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    private Optional<Integracion> metodo(String codigo, Expresion f, char x) {
        return switch (codigo) {
            case INMEDIATA -> inmediata(f, x).map(p -> new Integracion(p.despues(), List.of(p), INMEDIATA));
            case SUSTITUCION -> sustitucion(f, x);
            case EXPANDIR -> expandir(f, x);
            case FRACCIONES -> racional(f, x);
            case TRIGONOMETRICA -> IntegracionAvanzada.trigonometrica(f, x);
            case SUSTITUCION_TRIG -> IntegracionAvanzada.sustitucionTrigonometrica(f, x);
            case PARTES -> IntegracionAvanzada.partes(f, x, (g, y) -> integrar(g, y, null, true));
            default -> Optional.empty();
        };
    }

    // ------------------------------------------------------------------ I01

    private static Optional<PasoLimite> inmediata(Expresion f, char x) {
        return TablaIntegrales.integrar(f, x).map(ts -> {
            Expresion integral = TablaIntegrales.suma(ts);
            List<String> lineas = new ArrayList<>(TablaIntegrales.formulas(ts, x));
            for (TablaIntegrales.Termino t : ts) {
                lineas.add("∫" + integrando(Simplificador.simplificar(t.termino())) + " d" + x + " = " + p(t.integral()));
            }
            boolean varios = ts.size() > 1;
            return new PasoLimite(INMEDIATA, varios ? "Integral término a término" : "Integral inmediata",
                    varios ? "Es una suma: cada término se integra por separado con la tabla de integrales básicas"
                            : "Está en la tabla de integrales básicas (una constante sale como factor)",
                    new Integral(f, x), integral, new TrabajoIntermedio.Agrupacion(lineas));
        });
    }

    // ------------------------------------------------------------------ I02

    private static Optional<Integracion> sustitucion(Expresion f, char x) {
        Expresion s = Simplificador.simplificar(f);
        char w = letraLibre(s);
        Variable u = new Variable(w);
        for (Expresion g : candidatos(s, x)) {
            Expresion dg = Derivador.derivar(g, x).derivada();
            if (dg.equals(Constante.de(0))) {
                continue;
            }
            Expresion q = Simplificador.simplificar(new Cociente(s, dg));
            Expresion enU = Simplificador.simplificar(Expresiones.reemplazar(q, g, u));
            if (Expresiones.variables(enU).contains(x)) {
                continue;
            }
            Optional<PasoLimite> enLaU = inmediata(enU, w);
            if (enLaU.isEmpty()) {
                continue;
            }
            Expresion gu = enLaU.get().despues();
            Expresion antiderivada = TablaIntegrales.sinAbsolutoInnecesario(
                    Simplificador.simplificar(Expresiones.reemplazar(gu, u, g)));
            antiderivada = FormaFinal.ordenar(antiderivada, x);
            List<String> lineas = List.of(
                    w + " = " + p(g),
                    "d" + w + " = " + p(FormaFinal.ordenar(dg, x)) + " d" + x,
                    "∫" + integrando(s) + " d" + x + " = ∫" + integrando(enU) + " d" + w,
                    "∫" + integrando(enU) + " d" + w + " = " + p(gu),
                    "Se regresa a " + x + ": " + p(antiderivada));
            PasoLimite paso = new PasoLimite(SUSTITUCION, "Sustitución " + w + " = " + p(g), "El integrando es "
                    + "f(g(" + x + "))·g'(" + x + ") salvo una constante: con " + w + " = " + p(g) + " queda una integral "
                    + "inmediata en " + w, new Integral(f, x), antiderivada, new TrabajoIntermedio.Agrupacion(lineas),
                    "Integral en " + w, List.of(enLaU.get()));
            return Optional.of(new Integracion(antiderivada, List.of(paso), SUSTITUCION));
        }
        return Optional.empty();
    }

    private static char letraLibre(Expresion f) {
        var letras = Expresiones.variables(f);
        for (char c : new char[] {'u', 'w', 't', 'v', 'z'}) {
            if (!letras.contains(c)) {
                return c;
            }
        }
        return 'u';
    }

    /** Lo que puede ser u: bases de potencias, exponentes, argumentos, radicandos y denominadores. */
    private static List<Expresion> candidatos(Expresion f, char x) {
        List<Expresion> r = new ArrayList<>();
        recolectar(f, x, r);
        return r;
    }

    private static void recolectar(Expresion e, char x, List<Expresion> r) {
        switch (e) {
            case Suma s -> s.terminos().forEach(t -> recolectar(t, x, r));
            case Producto p -> p.factores().forEach(t -> recolectar(t, x, r));
            case Negacion n -> recolectar(n.argumento(), x, r);
            case Cociente c -> {
                agregar(c.denominador(), x, r);
                recolectar(c.numerador(), x, r);
                recolectar(c.denominador(), x, r);
            }
            case Potencia p -> {
                if (p.exponente() instanceof Constante) {
                    agregar(p.base(), x, r);
                } else {
                    agregar(p.exponente(), x, r);
                }
                recolectar(p.base(), x, r);
                recolectar(p.exponente(), x, r);
            }
            case Raiz rz -> {
                agregar(rz.radicando(), x, r);
                recolectar(rz.radicando(), x, r);
            }
            case Funcion fn -> {
                agregar(fn.argumento(), x, r);
                recolectar(fn.argumento(), x, r);
            }
            default -> {
            }
        }
    }

    private static void agregar(Expresion g, char x, List<Expresion> r) {
        boolean esX = g instanceof Variable v && v.nombre() == x;
        if (!esX && Expresiones.variables(g).contains(x) && !r.contains(g)) {
            r.add(g);
        }
    }

    // ------------------------------------------------------------------ expandir

    private static Optional<Integracion> expandir(Expresion f, char x) {
        if (TablaIntegrales.integrar(f, x).isPresent()) {
            return Optional.empty(); // ya es término a término
        }
        Polinomio p;
        try {
            p = Polinomio.desde(f);
        } catch (RuntimeException noEs) {
            return Optional.empty();
        }
        if (p.grado() > 40) {
            return Optional.empty();
        }
        Expresion expandido = Simplificador.simplificar(p.aExpresion());
        Optional<PasoLimite> i = inmediata(expandido, x);
        if (i.isEmpty()) {
            return Optional.empty();
        }
        PasoLimite paso = new PasoLimite(EXPANDIR, "Expandir", "Es un polinomio escrito como producto o potencia: se "
                + "expande para integrar término a término", f, expandido,
                new TrabajoIntermedio.Agrupacion(List.of(p(f) + " = " + p(expandido))));
        return Optional.of(new Integracion(i.get().despues(), List.of(paso, i.get()), EXPANDIR));
    }

    // ------------------------------------------------------------------ P02 + I06

    private Optional<Integracion> racional(Expresion f, char x) {
        var letras = Expresiones.variables(f);
        if (letras.size() != 1 || !letras.contains(x)) {
            return Optional.empty();
        }
        Optional<FuncionRacional> fr = FuncionRacional.de(f);
        if (fr.isEmpty() || fr.get().denominador().gradoEn(x) < 1) {
            return Optional.empty();
        }
        Polinomio n = fr.get().numerador();
        Polinomio d = fr.get().denominador();
        List<PasoLimite> pasos = new ArrayList<>();
        List<Expresion> partes = new ArrayList<>();
        Polinomio resto = n;
        boolean dividio = false;
        if (n.gradoEn(x) >= d.gradoEn(x)) {
            FraccionesParciales.Division div = FraccionesParciales.dividir(n, d, x);
            resto = div.resto();
            dividio = true;
            Expresion q = Simplificador.simplificar(div.cociente().aExpresion());
            Expresion propia = resto.esCero() ? null : fraccion(resto, d);
            pasos.add(new PasoLimite(DIVISION, "División de polinomios", "La fracción es impropia (grado "
                    + n.gradoEn(x) + " ≥ " + d.gradoEn(x) + "): antes de integrar se dividen los polinomios (RN17)",
                    f, propia == null ? q : Simplificador.suma(List.of(q, propia)), new TrabajoIntermedio.Agrupacion(
                            List.of("(" + texto(n) + ") ÷ (" + texto(d) + ") = " + p(q) + ", resto " + texto(resto),
                                    p(fraccion(n, d)) + " = " + p(q) + (propia == null ? "" : " + " + p(propia))))));
            PasoLimite iq = inmediata(q, x).orElseThrow();
            pasos.add(iq);
            partes.add(iq.despues());
        }
        if (!resto.esCero()) {
            Resolucion fact = orquestador.resolver(d.aExpresion(), Campo.Q);
            FormaFactorizada forma = fact.resultado();
            List<Factor> conX = forma.factores().stream().filter(fa -> fa.polinomio().gradoEn(x) > 0).toList();
            if (conX.size() == 1 && conX.getFirst().polinomio().gradoEn(x) == 1 && conX.getFirst().multiplicidad() == 1
                    && resto.esConstante()) {
                // c/(αx + β): inmediata, ln|αx + β|
                Racional c = resto.constante().dividir(forma.constante());
                Expresion i = FraccionesParciales.lineal(c, conX.getFirst().polinomio(), 1, x);
                Expresion propia = fraccion(resto, d);
                pasos.add(new PasoLimite(INMEDIATA, "Integral inmediata", "El denominador es lineal: la integral es un "
                        + "logaritmo (con valor absoluto, RN23)", new Integral(propia, x), i,
                        new TrabajoIntermedio.Agrupacion(List.of(TablaIntegrales.Fila.LOGARITMO.formula(x),
                                "∫" + p(propia) + " d" + x + " = " + p(i)))));
                partes.add(i);
            } else {
                Optional<FraccionesParciales.Descomposicion> desc = FraccionesParciales.descomponer(resto, forma, x);
                if (desc.isEmpty()) {
                    return Optional.empty();
                }
                pasos.addAll(fraccionesParciales(resto, d, fact, desc.get(), x));
                partes.add(desc.get().integral());
            }
        }
        if (partes.isEmpty()) {
            return Optional.empty();
        }
        Expresion antiderivada = Simplificador.suma(partes);
        String metodo = pasos.stream().anyMatch(ps -> ps.codigo().equals(FRACCIONES)) ? FRACCIONES
                : dividio ? DIVISION : INMEDIATA;
        return Optional.of(new Integracion(antiderivada, pasos, metodo));
    }

    private static String texto(Polinomio q) {
        return p(Simplificador.simplificar(q.aExpresion()));
    }

    private static Expresion fraccion(Polinomio n, Polinomio d) {
        return new Cociente(Simplificador.simplificar(n.aExpresion()), Simplificador.simplificar(d.aExpresion()));
    }

    private static List<PasoLimite> fraccionesParciales(Polinomio resto, Polinomio d, Resolucion fact,
                                                        FraccionesParciales.Descomposicion desc, char x) {
        Expresion propia = fraccion(resto, d);
        String factorizado = p(fact.resultado().aExpresion());
        List<PasoLimite> pasos = new ArrayList<>();
        pasos.add(new PasoLimite(FRACCIONES, "Factorizar el denominador", "Para separar la fracción en fracciones "
                + "parciales hace falta el denominador factorizado", Simplificador.simplificar(d.aExpresion()),
                fact.resultado().aExpresion(), new TrabajoIntermedio.Agrupacion(List.of(texto(d) + " = " + factorizado)),
                "Métodos de factorización", Anidados.deFactorizacion(fact, "denominador")));
        pasos.add(new PasoLimite(FRACCIONES, "Plantear las fracciones parciales", "Un factor lineal da A/(x − a); uno "
                + "repetido, A/(x − a) + B/(x − a)²; un cuadrático irreducible, (Ax + B)/(x² + bx + c) (RN22)", propia,
                null, new TrabajoIntermedio.Agrupacion(List.of(p(propia) + " = " + desc.planteo(),
                        "Se multiplica por el denominador:", desc.identidad()))));
        List<TrabajoIntermedio.SistemaCoeficientes.Ecuacion> ecs = new ArrayList<>();
        for (String[] e : desc.ecuaciones()) {
            ecs.add(new TrabajoIntermedio.SistemaCoeficientes.Ecuacion(e[0].equals("1") ? "término independiente"
                    : "coeficiente de " + e[0], e[1]));
        }
        List<String> solucion = new ArrayList<>();
        desc.valores().forEach((letra, v) -> solucion.add(letra + " = " + ImpresoraPizarra.numero(v)));
        String conValores = desc.planteo();
        for (var e : desc.valores().entrySet()) {
            String v = ImpresoraPizarra.numero(e.getValue());
            conValores = conValores.replace(e.getKey(), e.getValue().esEntero() && e.getValue().signo() >= 0 ? v
                    : "(" + v + ")");
        }
        pasos.add(new PasoLimite(FRACCIONES, "Resolver el sistema", "Se igualan los coeficientes de cada potencia de "
                + x + " en los dos lados y se resuelve el sistema", null, null,
                new TrabajoIntermedio.SistemaCoeficientes(desc.identidad(), ecs, solucion, p(propia) + " = " + conValores)));
        pasos.add(new PasoLimite(FRACCIONES, "Integrar cada fracción", "Cada fracción es inmediata: ln|…| para los "
                + "factores lineales (RN23), una potencia para los repetidos, y ln(…) con arctan para los cuadráticos",
                new Integral(propia, x), desc.integral(), new TrabajoIntermedio.Agrupacion(desc.integrales())));
        return pasos;
    }

    // ------------------------------------------------------------------ I07

    private static Expresion barrow(Expresion antiderivada, Integral in, List<PasoLimite> pasos) {
        Variable x = new Variable(in.variable());
        Expresion a = in.inferior();
        Expresion b = in.superior();
        Expresion fbCrudo = Expresiones.reemplazar(antiderivada, x, b);
        Expresion faCrudo = Expresiones.reemplazar(antiderivada, x, a);
        Expresion fb = Simplificador.simplificar(fbCrudo);
        Expresion fa = Simplificador.simplificar(faCrudo);
        Expresion valor = Simplificador.simplificar(Simplificador.suma(List.of(fb, Simplificador.negar(fa))));
        String bt = p(b);
        String at = p(a);
        String faT = fa instanceof Negacion || fa instanceof Suma || fa instanceof Constante c && c.valor().signo() < 0
                ? "(" + p(fa) + ")" : p(fa);
        List<String> lineas = List.of(
                "F(" + x.nombre() + ") = " + p(antiderivada),
                "F(" + bt + ") = " + p(fbCrudo) + igualA(fbCrudo, fb),
                "F(" + at + ") = " + p(faCrudo) + igualA(faCrudo, fa),
                "F(" + bt + ") − F(" + at + ") = " + p(fb) + " − " + faT + " = " + p(valor));
        pasos.add(new PasoLimite(BARROW, "Regla de Barrow", "La integral definida es F(b) − F(a), con F una "
                + "antiderivada; la constante C se cancela", in, valor, new TrabajoIntermedio.Agrupacion(lineas)));
        return valor;
    }

    private static String igualA(Expresion crudo, Expresion simple) {
        String a = p(crudo);
        String b = p(simple);
        return a.equals(b) ? "" : " = " + b;
    }

    /** El integrando no está definido en algún punto de [a, b]: es una impropia. */
    private Optional<String> impropia(Expresion f, char x, Expresion a, Expresion b) {
        BigDecimal ia;
        BigDecimal ib;
        try {
            ia = EvaluacionNumerica.evaluar(a, Map.of(), MC).re();
            ib = EvaluacionNumerica.evaluar(b, Map.of(), MC).re();
        } catch (RuntimeException e) {
            return Optional.of("Los límites de la integral deben ser números.");
        }
        BigDecimal desde = ia.min(ib);
        BigDecimal hasta = ia.max(ib);
        Optional<FuncionRacional> fr = FuncionRacional.de(f);
        if (fr.isPresent() && !fr.get().esPolinomio()) {
            Resolucion fact = orquestador.resolver(fr.get().denominador().aExpresion(), Campo.Q);
            for (Factor fa : fact.resultado().factores()) {
                Polinomio q = fa.polinomio();
                if (q.gradoEn(x) == 1) {
                    Racional raiz = q.constante().negar().dividir(q.coeficiente(
                            pe.mathnova.domain.algebra.Monomio.de(x, 1)));
                    BigDecimal r = new BigDecimal(raiz.num()).divide(new BigDecimal(raiz.den()), MC);
                    if (r.compareTo(desde) >= 0 && r.compareTo(hasta) <= 0) {
                        return Optional.of(noDefinida(x, ImpresoraPizarra.numero(raiz)));
                    }
                }
            }
        }
        // en lo demás (y en raíces no racionales), se mira el integrando en 200 puntos
        BigDecimal paso = hasta.subtract(desde).divide(BigDecimal.valueOf(200), MC);
        for (int i = 0; i <= 200; i++) {
            BigDecimal v = desde.add(paso.multiply(BigDecimal.valueOf(i)));
            try {
                ComplejoDecimal y = EvaluacionNumerica.evaluar(f, Map.of(x, ComplejoDecimal.real(v)), MC);
                if (!y.esReal(30)) {
                    return Optional.of(noDefinida(x, "≈ " + v.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
                            .toPlainString()));
                }
            } catch (ArithmeticException e) {
                return Optional.of(noDefinida(x, "≈ " + v.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros()
                        .toPlainString()));
            }
        }
        return Optional.empty();
    }

    private static String noDefinida(char x, String en) {
        return "El integrando no está definido en " + x + " = " + en + ", dentro del intervalo: es una integral "
                + "impropia; llega en la próxima versión.";
    }

    private static BigDecimal decimal(Expresion valor) {
        try {
            return EvaluacionNumerica.evaluar(valor, Map.of(), MC).re();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ verificación

    private static final List<Racional> PUNTOS = List.of(Racional.de(1, 3), Racional.de(13, 11), Racional.de(17, 9),
            Racional.de(5, 2), Racional.de(-5, 7), Racional.de(-29, 13), Racional.de(3, 5), Racional.de(11, 4),
            Racional.de(-7, 3), Racional.de(1, 7), Racional.de(-2, 9), Racional.de(4, 5), Racional.de(-1, 2),
            Racional.de(7, 2), Racional.de(-10, 3), Racional.de(4), Racional.de(-5), Racional.de(13, 3));

    /** La derivada de F debe dar el integrando: exacto si se puede, si no en cinco puntos. */
    static ResolucionIntegral.Verificacion verificar(Expresion antiderivada, Expresion f, char x) {
        Expresion derivada;
        try {
            derivada = FormaFinal.de(Derivador.derivar(antiderivada, x).derivada(), x);
        } catch (RuntimeException e) {
            return new ResolucionIntegral.Verificacion(false, false, antiderivada);
        }
        Optional<FuncionRacional> a = FuncionRacional.de(derivada);
        Optional<FuncionRacional> b = FuncionRacional.de(f);
        if (a.isPresent() && b.isPresent()) {
            return new ResolucionIntegral.Verificacion(a.get().igual(b.get()), true, derivada);
        }
        Expresion resta = Simplificador.simplificar(Simplificador.suma(List.of(derivada,
                Simplificador.negar(Simplificador.simplificar(f)))));
        if (resta.equals(Constante.de(0))) {
            return new ResolucionIntegral.Verificacion(true, true, derivada);
        }
        int buenos = 0;
        var letras = Expresiones.variables(f);
        letras.addAll(Expresiones.variables(antiderivada));
        for (Racional punto : PUNTOS) {
            java.util.Map<Character, ComplejoDecimal> v = new java.util.HashMap<>();
            for (char letra : letras) {
                v.put(letra, ComplejoDecimal.de(Racional.de(7, 5), EvaluacionNumerica.MC));
            }
            v.put(x, ComplejoDecimal.de(punto, EvaluacionNumerica.MC));
            try {
                ComplejoDecimal y1 = EvaluacionNumerica.evaluar(derivada, v, EvaluacionNumerica.MC);
                ComplejoDecimal y2 = EvaluacionNumerica.evaluar(f, v, EvaluacionNumerica.MC);
                if (!y1.cercano(y2, EvaluacionNumerica.CIFRAS_IGUALES, EvaluacionNumerica.MC)) {
                    return new ResolucionIntegral.Verificacion(false, false, derivada);
                }
                buenos++;
            } catch (ArithmeticException fuera) {
                // fuera del dominio
            }
            if (buenos == 5) {
                break;
            }
        }
        return new ResolucionIntegral.Verificacion(buenos == 5, false, derivada);
    }

    // ------------------------------------------------------------------ Simpson

    /** Regla de Simpson con n subintervalos (n par), para comparar con el valor exacto en Análisis. */
    public static Optional<BigDecimal> simpson(ResolucionIntegral r, int n) {
        Integral in = r.integral();
        if (!in.esDefinida()) {
            return Optional.empty();
        }
        try {
            BigDecimal a = EvaluacionNumerica.evaluar(in.inferior(), Map.of(), MC).re();
            BigDecimal b = EvaluacionNumerica.evaluar(in.superior(), Map.of(), MC).re();
            BigDecimal h = b.subtract(a).divide(BigDecimal.valueOf(n), MC);
            BigDecimal suma = BigDecimal.ZERO;
            for (int i = 0; i <= n; i++) {
                BigDecimal xi = a.add(h.multiply(BigDecimal.valueOf(i)));
                BigDecimal y = EvaluacionNumerica.evaluar(in.cuerpo(), Map.of(in.variable(), ComplejoDecimal.real(xi)),
                        MC).re();
                int peso = i == 0 || i == n ? 1 : i % 2 == 1 ? 4 : 2;
                suma = suma.add(y.multiply(BigDecimal.valueOf(peso)));
            }
            BigDecimal valor = suma.multiply(h).divide(BigDecimal.valueOf(3), MC)
                    .multiply(new BigDecimal(r.coeficiente().num())).divide(new BigDecimal(r.coeficiente().den()), MC);
            return Optional.of(valor);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ Exploración

    public static String nombre(String codigo) {
        return switch (codigo) {
            case INMEDIATA -> "Inmediata o término a término";
            case SUSTITUCION -> "Sustitución";
            case EXPANDIR -> "Expandir y término a término";
            case FRACCIONES -> "Fracciones parciales";
            case DIVISION -> "División y término a término";
            case PARTES -> "Por partes (ILATE)";
            case TRIGONOMETRICA -> "Integral trigonométrica";
            case SUSTITUCION_TRIG -> "Sustitución trigonométrica";
            default -> codigo;
        };
    }

    /**
     * El método usado, los otros que también llegan a una antiderivada correcta (con lo que dan) y los que
     * no aplican, con su motivo. En por partes se ve la elección de u por ILATE.
     */
    public List<CaminoCalculo> caminos(ResolucionIntegral r) {
        char x = r.variable();
        Expresion f = r.integral().cuerpo();
        String usado = r.metodo() == null ? null : r.metodo().equals(DIVISION) ? FRACCIONES : r.metodo();
        List<CaminoCalculo> caminos = new ArrayList<>();
        if (usado != null) {
            caminos.add(new CaminoCalculo(usado, nombre(r.metodo()), CaminoCalculo.Estado.USADO,
                    usado.equals(PARTES) ? eleccionIlate(f, x) : null));
        }
        for (String m : ORDEN) {
            if (m.equals(usado)) {
                continue;
            }
            Optional<Integracion> otra;
            try {
                otra = metodo(m, f, x);
            } catch (RuntimeException e) {
                otra = Optional.empty();
            }
            if (otra.isPresent() && verificar(otra.get().antiderivada(), f, x).coincide()) {
                Expresion g = FormaFinal.ordenar(Simplificador.simplificar(otra.get().antiderivada()), x);
                String ilate = m.equals(PARTES) ? eleccionIlate(f, x) : null;
                caminos.add(new CaminoCalculo(m, nombre(m), CaminoCalculo.Estado.SIRVE, p(g) + " + C"
                        + (ilate == null ? "" : " · " + ilate)));
            } else {
                caminos.add(new CaminoCalculo(m, nombre(m), CaminoCalculo.Estado.NO_APLICA, motivo(m, f, x)));
            }
        }
        return caminos;
    }

    /** "ILATE: u = ln x es logarítmica (L) y va antes que x, algebraica (A)", o null si no hay elección. */
    private String eleccionIlate(Expresion f, char x) {
        try {
            return IntegracionAvanzada.elegir(Simplificador.simplificar(f), x, (g, y) -> integrar(g, y, null, true))
                    .map(IntegracionAvanzada.Eleccion::ilate).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String motivo(String m, Expresion f, char x) {
        boolean polinomio;
        try {
            Polinomio.desde(f);
            polinomio = true;
        } catch (RuntimeException e) {
            polinomio = false;
        }
        return switch (m) {
            case INMEDIATA -> "No todos los términos están en la tabla de integrales básicas";
            case SUSTITUCION -> "No hay una u = g(" + x + ") cuya derivada g'(" + x + ") esté en el integrando "
                    + "(salvo una constante)";
            case EXPANDIR -> polinomio ? "Ya está expandido: se integra término a término" : "No es un polinomio";
            case FRACCIONES -> FuncionRacional.de(f).map(fr -> fr.esPolinomio()
                    ? "No hay denominador con " + x : "El denominador tiene factores de grado 3 o más, o cuadráticos "
                    + "con raíces reales que no son racionales").orElse("No es una fracción de polinomios");
            case TRIGONOMETRICA -> "No es un producto de potencias de sen, cos, tan, sec, csc o cot de un mismo "
                    + "argumento k·" + x;
            case SUSTITUCION_TRIG -> "No tiene una raíz √(a² − " + x + "²), √(a² + " + x + "²) ni √(" + x + "² − a²) "
                    + "con potencias de " + x;
            case PARTES -> "No es un producto que ILATE pueda repartir en u y dv (ni ln, ni una inversa sola), o la "
                    + "integral que queda no se resuelve en cuatro vueltas";
            default -> null;
        };
    }
}
