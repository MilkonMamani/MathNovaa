package pe.mathnova.domain.calculo.multiples;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.Polinomios;
import pe.mathnova.domain.calculo.Trigonometricas;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.integrales.MotorIntegrales;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.IntegralMultiple.Coordenadas;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Rango;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * HU31 (RF56): integrales dobles y triples.
 * <ul>
 *   <li>V09, integral iterada: de adentro hacia afuera, cada integral es un paso que usa el motor de una variable
 *       (sus pasos van anidados) y Barrow con los límites, que pueden depender de las variables de afuera.</li>
 *   <li>Cambio de orden: si la integral de adentro no es elemental (e^(x²)), se describe la región con el otro
 *       orden y se resuelve así. También se puede pedir desde Exploración.</li>
 *   <li>V10, cambio de coordenadas (polares, cilíndricas, esféricas), con el jacobiano como determinante y
 *       simplificado (RN40). Con "coord=" los rangos ya vienen en r, θ, z, ρ, φ; desde Exploración se reconoce un
 *       disco, un cilindro o una esfera centrados en el origen.</li>
 * </ul>
 */
public final class MotorIntegralesMultiples {

    public static final String ITERADA = "V09";
    public static final String COORDENADAS = "V10";
    /** Los caminos que se pueden forzar desde Exploración. */
    public static final String ORDEN = "ORDEN";
    public static final String POLARES = "POLARES";
    public static final String CILINDRICAS = "CILINDRICAS";
    public static final String ESFERICAS = "ESFERICAS";

    static final char R = 'r';
    static final char RHO = 'ρ';
    static final char THETA = Trigonometricas.THETA;
    static final char PHI = Trigonometricas.PHI;
    private static final MathContext MC = new MathContext(24);

    private final MotorIntegrales integrales;

    public MotorIntegralesMultiples(OrquestadorFactorizacion orquestador) {
        this.integrales = new MotorIntegrales(orquestador);
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static String paren(Expresion e) {
        return e instanceof Suma || e instanceof Negacion || e instanceof Constante k && k.valor().signo() < 0
                ? "(" + p(e) + ")" : p(e);
    }

    public ResolucionIntegralMultiple resolver(IntegralMultiple in) {
        return resolver(in, null);
    }

    /**
     * @param forzado ORDEN, POLARES, CILINDRICAS o ESFERICAS ("Probar" en Exploración), o null
     * @throws IllegalArgumentException si los límites o las variables no son válidos
     */
    public ResolucionIntegralMultiple resolver(IntegralMultiple in, String forzado) {
        validar(in);
        List<PasoLimite> pasos = new ArrayList<>();
        IntegralMultiple efectiva = in;
        boolean invertida = false;
        if (in.coordenadas() == Coordenadas.CARTESIANAS && forzado != null) {
            if (forzado.equals(ORDEN)) {
                Optional<Inversion> inv = invertir(in);
                if (inv.isEmpty()) {
                    throw new IllegalArgumentException(motivoOrden(in));
                }
                pasos.add(inv.get().paso(false));
                efectiva = inv.get().nueva();
                invertida = true;
            } else {
                Optional<IntegralMultiple> c = enCoordenadas(in, coordenadas(forzado));
                if (c.isEmpty()) {
                    throw new IllegalArgumentException(motivoCoordenadas(in, coordenadas(forzado)));
                }
                pasos.add(pasoRegion(in, c.get()));
                efectiva = c.get();
            }
        }
        Expresion integrando = efectiva.cuerpo();
        Expresion jacobiano = null;
        if (efectiva.coordenadas() != Coordenadas.CARTESIANAS) {
            Cambio cambio = cambio(efectiva);
            pasos.add(cambio.paso());
            integrando = cambio.integrando();
            jacobiano = cambio.jacobiano();
        }
        Iterada it = iterar(integrando, efectiva.rangos());
        if (it.valor() == null && efectiva.coordenadas() == Coordenadas.CARTESIANAS && !invertida
                && efectiva.esDoble() && it.fallo() == 0) {
            Optional<Inversion> inv = invertir(efectiva);
            if (inv.isPresent()) {
                Iterada otra = iterar(efectiva.cuerpo(), inv.get().nueva().rangos());
                if (otra.valor() != null) {
                    pasos.add(inv.get().paso(true));
                    pasos.addAll(otra.pasos());
                    return new ResolucionIntegralMultiple(in, inv.get().nueva(), efectiva.cuerpo(), null,
                            otra.valor(), true, pasos, null);
                }
            }
        }
        pasos.addAll(it.pasos());
        return new ResolucionIntegralMultiple(in, efectiva, integrando, jacobiano, it.valor(), invertida, pasos,
                it.motivo());
    }

    private static Coordenadas coordenadas(String forzado) {
        return switch (forzado) {
            case POLARES -> Coordenadas.POLARES;
            case CILINDRICAS -> Coordenadas.CILINDRICAS;
            case ESFERICAS -> Coordenadas.ESFERICAS;
            default -> throw new IllegalArgumentException("No es un camino de una integral múltiple: " + forzado);
        };
    }

    // ================================================================== validación

    private static void validar(IntegralMultiple in) {
        List<Rango> rs = in.rangos();
        for (int i = 0; i < rs.size(); i++) {
            Set<Character> afuera = new HashSet<>();
            for (int j = i + 1; j < rs.size(); j++) {
                afuera.add(rs.get(j).variable());
            }
            Set<Character> usadas = Expresiones.variables(rs.get(i).inferior());
            usadas.addAll(Expresiones.variables(rs.get(i).superior()));
            for (int j = 0; j <= i; j++) {
                if (usadas.contains(rs.get(j).variable())) {
                    char v = rs.get(i).variable();
                    throw new IllegalArgumentException("Los límites de " + v + " solo pueden depender de las variables "
                            + "de afuera" + (afuera.isEmpty() ? " (" + v + " es la de afuera: sus límites son números)"
                            : " (" + nombres(afuera) + ")"));
                }
            }
        }
        if (in.coordenadas() == Coordenadas.CARTESIANAS) {
            return;
        }
        Set<Character> esperadas = switch (in.coordenadas()) {
            case POLARES -> Set.of(R, THETA);
            case CILINDRICAS -> Set.of('z', R, THETA);
            default -> Set.of(RHO, PHI, THETA);
        };
        Set<Character> dadas = new HashSet<>();
        rs.forEach(r -> dadas.add(r.variable()));
        if (!dadas.equals(esperadas)) {
            throw new IllegalArgumentException("En coordenadas " + in.coordenadas().nombre() + " los rangos van en "
                    + nombres(esperadas));
        }
        Set<Character> xyz = in.coordenadas() == Coordenadas.POLARES ? Set.of('x', 'y') : Set.of('x', 'y', 'z');
        Set<Character> cuerpo = Expresiones.variables(in.cuerpo());
        if (!xyz.containsAll(cuerpo)) {
            throw new IllegalArgumentException("En coordenadas " + in.coordenadas().nombre() + " la función se escribe "
                    + "en " + nombres(xyz) + "; el cambio lo hace MathNova");
        }
    }

    private static String nombres(Set<Character> cs) {
        return String.join(", ", new java.util.TreeSet<>(cs).stream().map(String::valueOf).toList());
    }

    // ================================================================== V09

    /** @param fallo el índice del rango que no se pudo integrar (0 la de adentro), o −1 */
    private record Iterada(Expresion valor, List<PasoLimite> pasos, String motivo, int fallo) {
    }

    private Iterada iterar(Expresion integrando, List<Rango> rangos) {
        List<PasoLimite> pasos = new ArrayList<>();
        Expresion actual = normalizar(integrando);
        for (int i = 0; i < rangos.size(); i++) {
            Rango r = rangos.get(i);
            char v = r.variable();
            ResolucionIntegral res = integrales.resolver(new Integral(actual, v));
            String integral = "∫ " + paren(actual) + " d" + v + "  de " + v + " = " + p(r.inferior()) + " a " + v
                    + " = " + p(r.superior());
            if (!res.resuelta()) {
                String motivo = "La integral " + (i == 0 ? "de adentro" : "respecto de " + v) + " ∫ " + paren(actual)
                        + " d" + v + " no es elemental: ningún método de una variable la resuelve";
                pasos.add(new PasoLimite(ITERADA, "Integral respecto de " + v, motivo, actual, null,
                        new TrabajoIntermedio.Agrupacion(List.of(integral))));
                return new Iterada(null, pasos, motivo, i);
            }
            Expresion f = res.antiderivada();
            Variable x = new Variable(v);
            Expresion fb = Expresiones.reemplazar(f, x, r.superior());
            Expresion fa = Expresiones.reemplazar(f, x, r.inferior());
            Expresion fbS = normalizar(fb);
            Expresion faS = normalizar(fa);
            Expresion valor = normalizar(Simplificador.suma(List.of(fbS, Simplificador.negar(faS))));
            List<Character> constantes = new ArrayList<>(Expresiones.variables(actual));
            constantes.remove((Character) v);
            String porque = v + " va de " + p(r.inferior()) + " a " + p(r.superior())
                    + (constantes.isEmpty() ? "" : "; " + String.join(", ", constantes.stream().map(String::valueOf)
                    .toList()) + (constantes.size() == 1 ? " se trata como constante" : " se tratan como constantes"));
            List<String> lineas = new ArrayList<>();
            lineas.add(integral);
            lineas.add("F(" + v + ") = " + p(f));
            String resta = p(fbS) + " − " + paren(faS);
            lineas.add("F(" + p(r.superior()) + ") − F(" + p(r.inferior()) + ") = " + resta
                    + (resta.equals(p(valor)) ? "" : " = " + p(valor)));
            pasos.add(new PasoLimite(ITERADA, "Integral respecto de " + v, porque, actual, valor,
                    new TrabajoIntermedio.Agrupacion(lineas), "Integral en " + v, res.pasos()));
            actual = valor;
        }
        return new Iterada(actual, pasos, null, -1);
    }

    /** Un polinomio en orden; lo demás, simplificado. */
    static Expresion normalizar(Expresion e) {
        Expresion s = Simplificador.simplificar(e);
        try {
            Polinomio q = Polinomio.desde(s);
            return Simplificador.simplificar(q.aExpresion());
        } catch (RuntimeException noEsPolinomio) {
            return conResta(s);
        }
    }

    /** e/2 + (−1/2) como e/2 − 1/2, para que el LaTeX no diga "+ −". */
    private static Expresion conResta(Expresion e) {
        if (!(e instanceof Suma su)) {
            return e;
        }
        List<Expresion> ts = new ArrayList<>();
        for (Expresion t : su.terminos()) {
            ts.add(t instanceof Constante k && k.valor().signo() < 0 ? new Negacion(new Constante(k.valor().negar())) : t);
        }
        return new Suma(ts);
    }

    // ================================================================== cambio de orden

    /** Una función de borde: constante, creciente o decreciente, con su inversa. */
    private record Borde(Expresion e, int sentido, java.util.function.UnaryOperator<Expresion> inversa) {
    }

    private record Inversion(IntegralMultiple original, IntegralMultiple nueva) {
        PasoLimite paso(boolean porqueNoSale) {
            Rango ri = original.rangos().get(0);
            Rango re = original.rangos().get(1);
            Rango ni = nueva.rangos().get(0);
            Rango ne = nueva.rangos().get(1);
            String antes = p(re.inferior()) + " ≤ " + re.variable() + " ≤ " + p(re.superior()) + ",  " + p(ri.inferior())
                    + " ≤ " + ri.variable() + " ≤ " + p(ri.superior());
            String despues = p(ne.inferior()) + " ≤ " + ne.variable() + " ≤ " + p(ne.superior()) + ",  "
                    + p(ni.inferior()) + " ≤ " + ni.variable() + " ≤ " + p(ni.superior());
            String porque = (porqueNoSale ? "La integral de adentro respecto de " + ri.variable() + " no es "
                    + "elemental: se invierte el orden. " : "")
                    + "La misma región se describe con " + ni.variable() + " adentro y " + ne.variable() + " afuera";
            return new PasoLimite(ITERADA, "Cambiar el orden de integración", porque, null, null,
                    new TrabajoIntermedio.Agrupacion(List.of("Región: " + antes, "Es la misma región: " + despues,
                            "Queda " + ImpresoraPizarra.imprimir(nueva))));
        }
    }

    private static Optional<Racional> racional(Expresion e) {
        try {
            Expresion s = Simplificador.simplificar(e);
            return s instanceof Constante c ? Optional.of(c.valor()) : Optional.empty();
        } catch (RuntimeException no) {
            return Optional.empty();
        }
    }

    private static Optional<Racional> en(Expresion e, char v, Racional t) {
        return racional(Expresiones.reemplazar(e, new Variable(v), new Constante(t)));
    }

    private static Optional<Borde> borde(Expresion g, char v, Racional a) {
        Set<Character> letras = Expresiones.variables(g);
        if (letras.isEmpty()) {
            return Optional.of(new Borde(g, 0, null));
        }
        if (!letras.equals(Set.of(v))) {
            return Optional.empty();
        }
        Expresion base = g instanceof Funcion f && f.tipo() == TipoFuncion.SQRT ? f.argumento()
                : g instanceof Raiz rz && rz.indice() == 2 ? rz.radicando() : null;
        if (base instanceof Variable && a.signo() >= 0) {
            return Optional.of(new Borde(g, 1, w -> Simplificador.potencia(w, Constante.de(2))));
        }
        Polinomio q;
        try {
            q = Polinomio.desde(g);
        } catch (RuntimeException no) {
            return Optional.empty();
        }
        if (q.grado() == 1) {
            Racional k = q.coeficiente(Monomio.de(v, 1));
            Racional m = q.constante();
            return Optional.of(new Borde(g, k.signo(), w -> normalizar(Simplificador.cociente(Simplificador.suma(
                    List.of(w, new Constante(m.negar()))), new Constante(k)))));
        }
        if (q.terminos().size() == 1 && a.signo() >= 0) {
            Racional c = q.terminos().values().iterator().next();
            int n = q.grado();
            if (c.signo() > 0) {
                return Optional.of(new Borde(g, 1, w -> Simplificador.simplificar(Simplificador.raiz(n,
                        Simplificador.cociente(w, new Constante(c))))));
            }
        }
        return Optional.empty();
    }

    /**
     * ∬ con v₁ entre g₁(v₂) y g₂(v₂), v₂ entre a y b → v₂ entre h₁(v₁) y h₂(v₁), v₁ entre c y d. Se exige que cada
     * borde sea constante o monótono con inversa, y que la región no haya que partirla.
     */
    private static Optional<Inversion> invertir(IntegralMultiple in) {
        if (!in.esDoble() || in.coordenadas() != Coordenadas.CARTESIANAS) {
            return Optional.empty();
        }
        Rango ri = in.rangos().get(0);
        Rango re = in.rangos().get(1);
        char v1 = ri.variable();
        char v2 = re.variable();
        Optional<Racional> oa = racional(re.inferior());
        Optional<Racional> ob = racional(re.superior());
        if (oa.isEmpty() || ob.isEmpty()) {
            return Optional.empty();
        }
        Racional a = oa.get();
        Racional b = ob.get();
        Optional<Borde> g1 = borde(ri.inferior(), v2, a);
        Optional<Borde> g2 = borde(ri.superior(), v2, a);
        if (g1.isEmpty() || g2.isEmpty()) {
            return Optional.empty();
        }
        Optional<Racional> g1a = en(ri.inferior(), v2, a);
        Optional<Racional> g1b = en(ri.inferior(), v2, b);
        Optional<Racional> g2a = en(ri.superior(), v2, a);
        Optional<Racional> g2b = en(ri.superior(), v2, b);
        if (g1a.isEmpty() || g1b.isEmpty() || g2a.isEmpty() || g2b.isEmpty()) {
            return Optional.empty();
        }
        Racional c = min(g1a.get(), g1b.get());
        Racional d = max(g2a.get(), g2b.get());
        Variable w = new Variable(v1);
        List<Expresion> abajo = new ArrayList<>(List.of(new Constante(a)));
        List<Expresion> arriba = new ArrayList<>(List.of(new Constante(b)));
        // g₁(v₂) ≤ v₁: creciente da v₂ ≤ g₁⁻¹(v₁); decreciente, v₂ ≥ g₁⁻¹(v₁)
        if (g1.get().sentido() > 0) {
            arriba.add(g1.get().inversa().apply(w));
        } else if (g1.get().sentido() < 0) {
            abajo.add(g1.get().inversa().apply(w));
        }
        // v₁ ≤ g₂(v₂): creciente da v₂ ≥ g₂⁻¹(v₁); decreciente, v₂ ≤ g₂⁻¹(v₁)
        if (g2.get().sentido() > 0) {
            abajo.add(g2.get().inversa().apply(w));
        } else if (g2.get().sentido() < 0) {
            arriba.add(g2.get().inversa().apply(w));
        }
        Optional<Expresion> h1 = dominante(abajo, v1, c, d, true);
        Optional<Expresion> h2 = dominante(arriba, v1, c, d, false);
        if (h1.isEmpty() || h2.isEmpty()) {
            return Optional.empty();
        }
        IntegralMultiple nueva = new IntegralMultiple(in.cuerpo(), List.of(new Rango(v2, h1.get(), h2.get()),
                new Rango(v1, new Constante(c), new Constante(d))));
        return Optional.of(new Inversion(in, nueva));
    }

    private static Racional min(Racional a, Racional b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private static Racional max(Racional a, Racional b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    /**
     * El candidato que manda en todo [c, d] (el mayor de los de abajo o el menor de los de arriba): se compara en c,
     * en d y en el punto medio. Vacío si ninguno manda en todo el intervalo (habría que partir la región).
     */
    private static Optional<Expresion> dominante(List<Expresion> cs, char v, Racional c, Racional d, boolean mayor) {
        List<Racional> puntos = List.of(c, c.sumar(d).dividir(Racional.de(2)), d);
        for (Expresion h : cs) {
            boolean manda = true;
            for (Expresion o : cs) {
                for (Racional t : puntos) {
                    Optional<Racional> vh = en(h, v, t);
                    Optional<Racional> vo = en(o, v, t);
                    if (vh.isEmpty() || vo.isEmpty()) {
                        if (h != o) {
                            manda = false;
                        }
                        continue;
                    }
                    int cmp = vh.get().compareTo(vo.get());
                    if (mayor ? cmp < 0 : cmp > 0) {
                        manda = false;
                    }
                }
            }
            if (manda) {
                return Optional.of(h);
            }
        }
        return Optional.empty();
    }

    private static String motivoOrden(IntegralMultiple in) {
        if (!in.esDoble()) {
            return "El cambio de orden se hace en integrales dobles";
        }
        if (in.coordenadas() != Coordenadas.CARTESIANAS) {
            return "El cambio de orden se hace en coordenadas cartesianas";
        }
        return "Los bordes de la región no son constantes ni funciones monótonas con inversa (o la región habría que "
                + "partirla en dos)";
    }

    // ================================================================== V10: el cambio y el jacobiano

    private record Cambio(Expresion integrando, Expresion jacobiano, PasoLimite paso) {
    }

    /** x, y, z en las letras auxiliares (α = cos θ, β = sen θ, γ = cos φ, δ = sen φ). */
    private static Map<Character, Polinomio> sustitucion(Coordenadas c) {
        Polinomio ct = Polinomio.variable(Trigonometricas.COS_T);
        Polinomio st = Polinomio.variable(Trigonometricas.SEN_T);
        return switch (c) {
            case POLARES -> Map.of('x', Polinomio.variable(R).multiplicar(ct), 'y',
                    Polinomio.variable(R).multiplicar(st));
            case CILINDRICAS -> Map.of('x', Polinomio.variable(R).multiplicar(ct), 'y',
                    Polinomio.variable(R).multiplicar(st), 'z', Polinomio.variable('z'));
            case ESFERICAS -> {
                Polinomio rs = Polinomio.variable(RHO).multiplicar(Polinomio.variable(Trigonometricas.SEN_F));
                yield Map.of('x', rs.multiplicar(ct), 'y', rs.multiplicar(st), 'z',
                        Polinomio.variable(RHO).multiplicar(Polinomio.variable(Trigonometricas.COS_F)));
            }
            case CARTESIANAS -> throw new IllegalArgumentException("cartesianas");
        };
    }

    /** Las variables nuevas, en el orden de las columnas del jacobiano. */
    private static List<Character> nuevas(Coordenadas c) {
        return switch (c) {
            case POLARES -> List.of(R, THETA);
            case CILINDRICAS -> List.of(R, THETA, 'z');
            default -> List.of(RHO, PHI, THETA);
        };
    }

    private static Polinomio derivar(Polinomio p, char v) {
        return v == THETA || v == PHI ? Trigonometricas.derivarAngulo(p, v) : Polinomios.derivar(p, v);
    }

    /** f(x, y, z) en las coordenadas nuevas, sin jacobiano: polinomios por la sustitución, lo demás por partes. */
    static Expresion aCoordenadas(Expresion f, Coordenadas c) {
        Map<Character, Polinomio> s = sustitucion(c);
        try {
            Polinomio q = Polinomio.desde(f);
            Polinomio r = q;
            for (var e : s.entrySet()) {
                r = Polinomios.sustituir(r, e.getKey(), e.getValue());
            }
            return Trigonometricas.aExpresion(Trigonometricas.reducir(r));
        } catch (RuntimeException noEsPolinomio) {
            // sigue por partes
        }
        return switch (f) {
            case Suma su -> Simplificador.simplificar(new Suma(su.terminos().stream().map(t -> aCoordenadas(t, c))
                    .toList()));
            case Producto pr -> Simplificador.simplificar(new Producto(pr.factores().stream()
                    .map(t -> aCoordenadas(t, c)).toList()));
            case pe.mathnova.domain.expresion.Cociente q -> Simplificador.simplificar(
                    new pe.mathnova.domain.expresion.Cociente(aCoordenadas(q.numerador(), c),
                            aCoordenadas(q.denominador(), c)));
            case Potencia pw -> Simplificador.simplificar(new Potencia(aCoordenadas(pw.base(), c),
                    aCoordenadas(pw.exponente(), c)));
            case Negacion ng -> Simplificador.negar(aCoordenadas(ng.argumento(), c));
            case Raiz rz -> Simplificador.simplificar(new Raiz(rz.indice(), aCoordenadas(rz.radicando(), c)));
            case Funcion fn -> Simplificador.simplificar(new Funcion(fn.tipo(), aCoordenadas(fn.argumento(), c),
                    fn.base()));
            default -> f;
        };
    }

    private static Cambio cambio(IntegralMultiple in) {
        Coordenadas c = in.coordenadas();
        Map<Character, Polinomio> s = sustitucion(c);
        List<Character> cartesianas = c == Coordenadas.POLARES ? List.of('x', 'y') : List.of('x', 'y', 'z');
        List<Character> nuevas = nuevas(c);
        // el jacobiano como determinante
        Polinomio[][] m = new Polinomio[cartesianas.size()][nuevas.size()];
        for (int i = 0; i < cartesianas.size(); i++) {
            for (int j = 0; j < nuevas.size(); j++) {
                m[i][j] = derivar(s.get(cartesianas.get(i)), nuevas.get(j));
            }
        }
        Polinomio det = determinante(m);
        Polinomio jac = Trigonometricas.reducir(det);
        Expresion jacobiano = Trigonometricas.aExpresion(jac);
        List<String> lineas = new ArrayList<>();
        List<String> cambios = new ArrayList<>();
        for (char x : cartesianas) {
            cambios.add(x + " = " + p(Trigonometricas.aExpresion(s.get(x))));
        }
        lineas.add(String.join(",  ", cambios));
        String columnas = String.join(", ", nuevas.stream().map(String::valueOf).toList());
        StringBuilder matriz = new StringBuilder("J = ∂(" + String.join(", ", cartesianas.stream().map(String::valueOf)
                .toList()) + ")/∂(" + columnas + ") = | ");
        for (int i = 0; i < m.length; i++) {
            if (i > 0) {
                matriz.append(" ;  ");
            }
            List<String> fila = new ArrayList<>();
            for (Polinomio q : m[i]) {
                fila.add(p(Trigonometricas.aExpresion(q)));
            }
            matriz.append(String.join("   ", fila));
        }
        lineas.add(matriz.append(" |").toString());
        String expandido = p(Trigonometricas.aExpresion(det));
        String simple = p(jacobiano);
        lineas.add("det J = " + (expandido.equals(simple) ? simple : expandido + " = " + simple)
                + (c == Coordenadas.ESFERICAS ? "  (≥ 0 porque 0 ≤ φ ≤ π)" : ""));
        String diferencial = switch (c) {
            case POLARES -> "dA = r dr dθ";
            case CILINDRICAS -> "dV = r dz dr dθ";
            default -> "dV = ρ² sen φ dρ dφ dθ";
        };
        lineas.add(diferencial);
        Expresion fNueva = aCoordenadas(in.cuerpo(), c);
        Expresion integrando;
        try {
            Polinomio q = Polinomio.desde(in.cuerpo());
            for (var e : s.entrySet()) {
                q = Polinomios.sustituir(q, e.getKey(), e.getValue());
            }
            integrando = Trigonometricas.aExpresion(Trigonometricas.reducir(q.multiplicar(jac)));
        } catch (RuntimeException noEsPolinomio) {
            integrando = Simplificador.simplificar(Simplificador.producto(List.of(fNueva, jacobiano)));
        }
        lineas.add(p(in.cuerpo()).equals(p(fNueva)) ? "f = " + p(fNueva) : "f = " + p(in.cuerpo()) + " = " + p(fNueva));
        lineas.add("f·|J| = " + paren(fNueva) + "·" + paren(jacobiano) + " = " + p(integrando));
        PasoLimite paso = new PasoLimite(COORDENADAS, "Cambio a coordenadas " + c.nombre(), "Se cambian las variables y "
                + "el diferencial se multiplica por el valor absoluto del jacobiano (RN40): " + diferencial, in.cuerpo(),
                integrando, new TrabajoIntermedio.Agrupacion(lineas));
        return new Cambio(integrando, jacobiano, paso);
    }

    private static Polinomio determinante(Polinomio[][] m) {
        if (m.length == 2) {
            return m[0][0].multiplicar(m[1][1]).restar(m[0][1].multiplicar(m[1][0]));
        }
        Polinomio r = Polinomio.CERO;
        for (int j = 0; j < 3; j++) {
            Polinomio[][] menor = new Polinomio[2][2];
            for (int i = 1; i < 3; i++) {
                int k = 0;
                for (int jj = 0; jj < 3; jj++) {
                    if (jj != j) {
                        menor[i - 1][k++] = m[i][jj];
                    }
                }
            }
            Polinomio t = m[0][j].multiplicar(determinante(menor));
            r = j % 2 == 0 ? r.sumar(t) : r.restar(t);
        }
        return r;
    }

    // ================================================================== V10 desde Exploración: reconocer la región

    /** R² si e es √(R² − v²) (o √(R² − v² − w²) con {@code otra}); vacío si no. */
    private static Optional<Racional> raizCirculo(Expresion e, char v, char otra) {
        Expresion base = e instanceof Funcion f && f.tipo() == TipoFuncion.SQRT ? f.argumento()
                : e instanceof Raiz rz && rz.indice() == 2 ? rz.radicando() : null;
        if (base == null) {
            return Optional.empty();
        }
        try {
            Polinomio q = Polinomio.desde(base);
            Polinomio esperado = Polinomio.constante(q.constante()).restar(Polinomio.variable(v).potencia(2));
            if (otra != 0) {
                esperado = esperado.restar(Polinomio.variable(otra).potencia(2));
            }
            return q.equals(esperado) && q.constante().signo() > 0 ? Optional.of(q.constante()) : Optional.empty();
        } catch (RuntimeException no) {
            return Optional.empty();
        }
    }

    private static boolean igual(Expresion e, Expresion v) {
        return Simplificador.simplificar(Simplificador.suma(List.of(e, Simplificador.negar(v))))
                .equals(Constante.de(0));
    }

    private static boolean cero(Expresion e) {
        return Simplificador.simplificar(e).equals(Constante.de(0));
    }

    /** El borde de abajo: −√(R² − …) (true) o 0 (false); vacío si es otra cosa. */
    private static Optional<Boolean> abajoSimetrico(Expresion inferior, Expresion superior) {
        if (cero(inferior)) {
            return Optional.of(false);
        }
        return igual(inferior, Simplificador.negar(superior)) ? Optional.of(true) : Optional.empty();
    }

    private record Disco(Expresion radio, Expresion desde, Expresion hasta) {
    }

    /** Un disco, medio disco o cuarto de disco centrado en el origen, con v₁ adentro y v₂ afuera. */
    private static Optional<Disco> disco(Rango adentro, Rango afuera) {
        char v1 = adentro.variable();
        char v2 = afuera.variable();
        if (!(v1 == 'y' && v2 == 'x' || v1 == 'x' && v2 == 'y')) {
            return Optional.empty();
        }
        Optional<Racional> r2 = raizCirculo(adentro.superior(), v2, (char) 0);
        Optional<Boolean> simetrico = abajoSimetrico(adentro.inferior(), adentro.superior());
        if (r2.isEmpty() || simetrico.isEmpty()) {
            return Optional.empty();
        }
        Expresion radio = Simplificador.simplificar(Simplificador.raiz(2, new Constante(r2.get())));
        if (!igual(afuera.superior(), radio)) {
            return Optional.empty();
        }
        boolean afueraSimetrico;
        if (cero(afuera.inferior())) {
            afueraSimetrico = false;
        } else if (igual(afuera.inferior(), Simplificador.negar(radio))) {
            afueraSimetrico = true;
        } else {
            return Optional.empty();
        }
        Expresion pi = new ConstanteEspecial(ConstanteEspecial.Tipo.PI);
        Expresion medioPi = Simplificador.cociente(pi, Constante.de(2));
        // con y adentro: abajo simétrico es "todo y", afuera simétrico es "todo x"
        boolean todoY = v1 == 'y' ? simetrico.get() : afueraSimetrico;
        boolean todoX = v1 == 'y' ? afueraSimetrico : simetrico.get();
        if (todoX && todoY) {
            return Optional.of(new Disco(radio, Constante.de(0), Simplificador.producto(List.of(Constante.de(2), pi))));
        }
        if (todoX) {
            return Optional.of(new Disco(radio, Constante.de(0), pi));
        }
        if (todoY) {
            return Optional.of(new Disco(radio, Simplificador.negar(medioPi), medioPi));
        }
        return Optional.of(new Disco(radio, Constante.de(0), medioPi));
    }

    /** La misma integral en coordenadas, si la región es un disco, un cilindro o una esfera centrados en el origen. */
    static Optional<IntegralMultiple> enCoordenadas(IntegralMultiple in, Coordenadas c) {
        List<Rango> rs = in.rangos();
        switch (c) {
            case POLARES -> {
                if (!in.esDoble()) {
                    return Optional.empty();
                }
                return disco(rs.get(0), rs.get(1)).map(d -> new IntegralMultiple(in.cuerpo(), List.of(
                        new Rango(R, Constante.de(0), d.radio()), new Rango(THETA, d.desde(), d.hasta())),
                        Coordenadas.POLARES));
            }
            case CILINDRICAS -> {
                if (in.esDoble() || rs.get(0).variable() != 'z') {
                    return Optional.empty();
                }
                Optional<Disco> d = disco(rs.get(1), rs.get(2));
                if (d.isEmpty()) {
                    return Optional.empty();
                }
                Expresion z1 = aCoordenadas(rs.get(0).inferior(), Coordenadas.CILINDRICAS);
                Expresion z2 = aCoordenadas(rs.get(0).superior(), Coordenadas.CILINDRICAS);
                Set<Character> enZ = Expresiones.variables(z1);
                enZ.addAll(Expresiones.variables(z2));
                if (enZ.contains('x') || enZ.contains('y')) {
                    return Optional.empty();
                }
                return Optional.of(new IntegralMultiple(in.cuerpo(), List.of(new Rango('z', z1, z2),
                        new Rango(R, Constante.de(0), d.get().radio()), new Rango(THETA, d.get().desde(),
                                d.get().hasta())), Coordenadas.CILINDRICAS));
            }
            case ESFERICAS -> {
                if (in.esDoble() || rs.get(0).variable() != 'z' || rs.get(1).variable() != 'y'
                        || rs.get(2).variable() != 'x') {
                    return Optional.empty();
                }
                Optional<Racional> r2 = raizCirculo(rs.get(0).superior(), 'x', 'y');
                Optional<Boolean> sim = abajoSimetrico(rs.get(0).inferior(), rs.get(0).superior());
                Optional<Disco> d = disco(rs.get(1), rs.get(2));
                if (r2.isEmpty() || sim.isEmpty() || d.isEmpty()
                        || !igual(d.get().radio(), Simplificador.simplificar(Simplificador.raiz(2,
                        new Constante(r2.get()))))) {
                    return Optional.empty();
                }
                Expresion pi = new ConstanteEspecial(ConstanteEspecial.Tipo.PI);
                Expresion hastaPhi = sim.get() ? pi : Simplificador.cociente(pi, Constante.de(2));
                return Optional.of(new IntegralMultiple(in.cuerpo(), List.of(new Rango(RHO, Constante.de(0),
                        d.get().radio()), new Rango(PHI, Constante.de(0), hastaPhi), new Rango(THETA,
                        d.get().desde(), d.get().hasta())), Coordenadas.ESFERICAS));
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    private static String motivoCoordenadas(IntegralMultiple in, Coordenadas c) {
        return switch (c) {
            case POLARES -> in.esDoble() ? "La región no es un disco, medio disco o cuarto de disco centrado en el "
                    + "origen (y entre ±√(R² − x²))" : "Las coordenadas polares son para integrales dobles";
            case CILINDRICAS -> in.esDoble() ? "Las coordenadas cilíndricas son para integrales triples"
                    : "La región no es z entre dos funciones sobre un disco centrado en el origen";
            default -> in.esDoble() ? "Las coordenadas esféricas son para integrales triples"
                    : "La región no es una esfera (o media esfera) centrada en el origen";
        };
    }

    private static PasoLimite pasoRegion(IntegralMultiple antes, IntegralMultiple despues) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Región: " + region(antes));
        lineas.add("En coordenadas " + despues.coordenadas().nombre() + ": " + region(despues));
        lineas.add("Queda " + ImpresoraPizarra.imprimir(despues));
        return new PasoLimite(COORDENADAS, "Describir la región en coordenadas " + despues.coordenadas().nombre(),
                "La región tiene simetría alrededor del origen: en " + despues.coordenadas().nombre()
                        + " sus límites son constantes", null, null, new TrabajoIntermedio.Agrupacion(lineas));
    }

    private static String region(IntegralMultiple in) {
        List<String> r = new ArrayList<>();
        for (int i = in.rangos().size() - 1; i >= 0; i--) {
            Rango g = in.rangos().get(i);
            r.add(p(g.inferior()) + " ≤ " + g.variable() + " ≤ " + p(g.superior()));
        }
        return String.join(",  ", r);
    }

    // ================================================================== Exploración

    public static String nombre(String codigo) {
        return switch (codigo) {
            case ITERADA -> "Integral iterada";
            case ORDEN -> "Invertir el orden";
            case POLARES -> "Coordenadas polares";
            case CILINDRICAS -> "Coordenadas cilíndricas";
            case ESFERICAS -> "Coordenadas esféricas";
            default -> codigo;
        };
    }

    /** La iterada (usada), el cambio de orden y los cambios de coordenadas que sirven (con su valor) o no aplican. */
    public List<CaminoCalculo> caminos(ResolucionIntegralMultiple r) {
        IntegralMultiple in = r.entrada();
        List<CaminoCalculo> caminos = new ArrayList<>();
        String usado = r.coordenadas() == Coordenadas.CARTESIANAS ? r.invertida() ? ORDEN : ITERADA
                : r.coordenadas().name();
        caminos.add(new CaminoCalculo(usado, nombre(usado), CaminoCalculo.Estado.USADO, r.resuelta()
                ? "= " + r.texto() : null));
        if (in.coordenadas() != Coordenadas.CARTESIANAS) {
            return caminos;
        }
        List<String> otros = new ArrayList<>();
        if (!usado.equals(ORDEN)) {
            otros.add(ORDEN);
        }
        if (in.esDoble()) {
            otros.add(POLARES);
        } else {
            otros.add(CILINDRICAS);
            otros.add(ESFERICAS);
        }
        for (String c : otros) {
            if (c.equals(usado)) {
                continue;
            }
            try {
                ResolucionIntegralMultiple otra = resolver(in, c);
                caminos.add(new CaminoCalculo(c, nombre(c), otra.resuelta() ? CaminoCalculo.Estado.SIRVE
                        : CaminoCalculo.Estado.NO_APLICA, otra.resuelta() ? "= " + otra.texto() : otra.motivo()));
            } catch (IllegalArgumentException e) {
                caminos.add(new CaminoCalculo(c, nombre(c), CaminoCalculo.Estado.NO_APLICA, e.getMessage()));
            }
        }
        return caminos;
    }

    // ================================================================== comprobación numérica (Simpson)

    /**
     * La regla de Simpson en 2D o 3D sobre la integral efectiva (con el jacobiano), para comparar en Análisis.
     *
     * @param n subintervalos por variable (par)
     */
    public static Optional<BigDecimal> simpson(ResolucionIntegralMultiple r, int n) {
        try {
            return Optional.of(simpson(r.integrando(), r.efectiva().rangos(), r.efectiva().rangos().size() - 1,
                    new HashMap<>(), n));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private static BigDecimal simpson(Expresion f, List<Rango> rs, int k, Map<Character, ComplejoDecimal> afuera, int n) {
        Rango g = rs.get(k);
        BigDecimal a = EvaluacionNumerica.evaluar(g.inferior(), afuera, MC).re();
        BigDecimal b = EvaluacionNumerica.evaluar(g.superior(), afuera, MC).re();
        BigDecimal h = b.subtract(a).divide(BigDecimal.valueOf(n), MC);
        BigDecimal suma = BigDecimal.ZERO;
        for (int i = 0; i <= n; i++) {
            Map<Character, ComplejoDecimal> v = new HashMap<>(afuera);
            v.put(g.variable(), ComplejoDecimal.real(a.add(h.multiply(BigDecimal.valueOf(i)))));
            BigDecimal y = k == 0 ? EvaluacionNumerica.evaluar(f, v, MC).re() : simpson(f, rs, k - 1, v, n);
            int peso = i == 0 || i == n ? 1 : i % 2 == 1 ? 4 : 2;
            suma = suma.add(y.multiply(BigDecimal.valueOf(peso)));
        }
        return suma.multiply(h).divide(BigDecimal.valueOf(3), MC);
    }

    /** El valor exacto en decimal, o vacío si no se pudo. */
    public static Optional<BigDecimal> decimal(ResolucionIntegralMultiple r) {
        if (!r.resuelta()) {
            return Optional.empty();
        }
        try {
            return Optional.of(EvaluacionNumerica.evaluar(r.valor(), Map.of(), MC).re());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
