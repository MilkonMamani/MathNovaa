package pe.mathnova.domain.calculo.integrales;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
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
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * HU26, RF49: los métodos de integración del Sprint 7, que el motor prueba después de sustitución y de
 * fracciones parciales:
 * <ul>
 *   <li>I04: integrales trigonométricas: senᵐ·cosⁿ con un exponente impar (se separa un factor y
 *       sen² + cos² = 1), con los dos pares (ángulo mitad), tanⁿ (tan² = sec² − 1), secⁿ (sec³ por
 *       partes), cscⁿ y cotⁿ;</li>
 *   <li>I05: sustitución trigonométrica para √(a² − x²), √(a² + x²) y √(x² − a²), con el triángulo
 *       rectángulo para regresar a x;</li>
 *   <li>I03: por partes, ∫u dv = uv − ∫v du, con u elegida por ILATE (RN36); se repite hasta cuatro
 *       veces y, si reaparece la integral original (eˣ·sen x), se despeja como una ecuación.</li>
 * </ul>
 */
final class IntegracionAvanzada {

    static final String PARTES = "I03";
    static final String TRIGONOMETRICA = "I04";
    static final String SUSTITUCION_TRIG = "I05";
    /** Cuántas veces seguidas se aplica por partes (RN36). */
    static final int MAXIMO_PARTES = 4;
    private static final int MAXIMA_POTENCIA = 12;
    private static final char THETA = 'θ';

    /** El resto del motor, sin por partes: lo usa por partes para v = ∫dv y para ∫v du. */
    interface Integrador {
        Optional<MotorIntegrales.Integracion> integrar(Expresion g, char x);
    }

    private IntegracionAvanzada() {
    }

    // ================================================================== utilidades

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static String integrando(Expresion f) {
        return f instanceof Suma || f instanceof Negacion ? "(" + p(f) + ")" : p(f);
    }

    private static Expresion s(Expresion e) {
        return Simplificador.simplificar(e);
    }

    private static Expresion c(Racional r) {
        return new Constante(r);
    }

    private static Expresion por(Expresion... fs) {
        return Simplificador.producto(List.of(fs));
    }

    private static Expresion mas(Expresion... ts) {
        return Simplificador.suma(List.of(ts));
    }

    private static Expresion menos(Expresion e) {
        return Simplificador.negar(e);
    }

    /** k·e repartido en los términos de una suma: 3(tan θ − θ) queda 3tan θ − 3θ. */
    private static Expresion escalar(Racional k, Expresion e) {
        Expresion t = s(e);
        if (t instanceof Suma sm) {
            return s(Simplificador.suma(sm.terminos().stream().map(u -> escalar(k, u)).toList()));
        }
        return s(por(c(k), t));
    }

    private static Expresion pot(Expresion b, int n) {
        return Simplificador.potencia(b, Constante.de(n));
    }

    private static boolean depende(Expresion e, char x) {
        return Expresiones.variables(e).contains(x);
    }

    /** El número de delante y el resto: −3·eˣ·sen x da (−3, eˣ·sen x). */
    private record ConCoeficiente(Racional coeficiente, Expresion resto) {
    }

    private static ConCoeficiente separar(Expresion e) {
        Expresion t = s(e);
        if (t instanceof Constante k) {
            return new ConCoeficiente(k.valor(), Constante.de(1));
        }
        if (t instanceof Negacion n) {
            ConCoeficiente a = separar(n.argumento());
            return new ConCoeficiente(a.coeficiente().negar(), a.resto());
        }
        if (t instanceof Producto(List<Expresion> fs) && fs.getFirst() instanceof Constante k) {
            List<Expresion> resto = fs.subList(1, fs.size());
            return new ConCoeficiente(k.valor(), resto.size() == 1 ? resto.getFirst() : new Producto(resto));
        }
        if (t instanceof Cociente(Expresion n, Expresion d) && d instanceof Constante k && !k.valor().esCero()) {
            ConCoeficiente a = separar(n);
            return new ConCoeficiente(a.coeficiente().dividir(k.valor()), a.resto());
        }
        return new ConCoeficiente(Racional.UNO, t);
    }

    /** k si a = k·b con k un número; null si no. */
    static Racional proporcional(Expresion a, Expresion b) {
        ConCoeficiente ca = separar(a);
        ConCoeficiente cb = separar(b);
        if (ca.resto().equals(cb.resto()) && !cb.coeficiente().esCero()) {
            return ca.coeficiente().dividir(cb.coeficiente());
        }
        Expresion q = s(new Cociente(a, b));
        return q instanceof Constante k ? k.valor() : null;
    }

    // ================================================================== I04: trigonométricas

    /** Antiderivada y renglones de una integral trigonométrica. */
    record Trig(Expresion integral, List<String> lineas, String titulo) {
    }

    /** coef·Π fᵢ(k·x)^nᵢ con fᵢ trigonométricas, todas del mismo argumento k·x. */
    private static final class Monomial {
        Racional coeficiente = Racional.UNO;
        Racional k;
        final Map<TipoFuncion, Integer> exponentes = new EnumMap<>(TipoFuncion.class);
    }

    private static boolean esTrig(TipoFuncion t) {
        return switch (t) {
            case SEN, COS, TAN, COT, SEC, CSC -> true;
            default -> false;
        };
    }

    private static Optional<Monomial> monomio(Expresion g, char x) {
        Monomial m = new Monomial();
        if (!acumular(s(g), x, 1, m) || m.exponentes.isEmpty() || m.k == null) {
            return Optional.empty();
        }
        m.exponentes.values().removeIf(n -> n == 0);
        return m.exponentes.isEmpty() ? Optional.empty() : Optional.of(m);
    }

    private static boolean acumular(Expresion e, char x, int signo, Monomial m) {
        switch (e) {
            case Constante k -> {
                if (k.valor().esCero()) {
                    return false;
                }
                m.coeficiente = signo > 0 ? m.coeficiente.multiplicar(k.valor()) : m.coeficiente.dividir(k.valor());
                return true;
            }
            case Negacion n -> {
                m.coeficiente = m.coeficiente.negar();
                return acumular(n.argumento(), x, signo, m);
            }
            case Producto pr -> {
                for (Expresion f : pr.factores()) {
                    if (!acumular(f, x, signo, m)) {
                        return false;
                    }
                }
                return true;
            }
            case Cociente q -> {
                return acumular(q.numerador(), x, signo, m) && acumular(q.denominador(), x, -signo, m);
            }
            case Potencia pw when pw.exponente() instanceof Constante k && k.valor().esEntero()
                    && pw.base() instanceof Funcion f && esTrig(f.tipo()) -> {
                return funcion(f, x, signo * k.valor().num().intValueExact(), m);
            }
            case Funcion f when esTrig(f.tipo()) -> {
                return funcion(f, x, signo, m);
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean funcion(Funcion f, char x, int n, Monomial m) {
        Racional k = lineal(f.argumento(), x);
        if (k == null || m.k != null && !m.k.equals(k)) {
            return false;
        }
        m.k = k;
        m.exponentes.merge(f.tipo(), n, Integer::sum);
        return true;
    }

    /** k si e es k·x; null si no. */
    private static Racional lineal(Expresion e, char x) {
        if (e instanceof Variable v && v.nombre() == x) {
            return Racional.UNO;
        }
        try {
            Polinomio q = Polinomio.desde(e);
            if (q.terminos().size() == 1 && q.variables().size() == 1 && q.gradoEn(x) == 1) {
                return q.coeficiente(Monomio.de(x, 1));
            }
        } catch (RuntimeException noEs) {
            // no es k·x
        }
        return null;
    }

    static Optional<MotorIntegrales.Integracion> trigonometrica(Expresion f, char x) {
        Optional<Monomial> om = monomio(f, x);
        if (om.isEmpty()) {
            return Optional.empty();
        }
        Monomial m = om.get();
        Optional<Trig> t = integrarMonomio(m, x);
        if (t.isEmpty()) {
            return Optional.empty();
        }
        Expresion integral = s(por(c(m.coeficiente), t.get().integral()));
        List<String> lineas = new ArrayList<>(t.get().lineas());
        lineas.add("∫" + integrando(s(f)) + " d" + x + " = " + p(integral));
        PasoLimite paso = new PasoLimite(TRIGONOMETRICA, t.get().titulo(), justificacion(t.get().titulo()),
                new Integral(f, x), integral, new TrabajoIntermedio.Agrupacion(lineas));
        return Optional.of(new MotorIntegrales.Integracion(integral, List.of(paso), TRIGONOMETRICA));
    }

    private static String justificacion(String titulo) {
        if (titulo.startsWith("Potencia impar")) {
            return "Un exponente es impar: se separa un factor, el resto se escribe con sen² + cos² = 1 y queda una "
                    + "sustitución u";
        }
        if (titulo.startsWith("Potencias pares")) {
            return "Los dos exponentes son pares: se bajan con las identidades de ángulo mitad "
                    + "sen² = (1 − cos 2x)/2 y cos² = (1 + cos 2x)/2";
        }
        if (titulo.startsWith("Potencia de tangente") || titulo.startsWith("Potencia de cotangente")) {
            return "Se usa tan² = sec² − 1 (o cot² = csc² − 1) para bajar el exponente de dos en dos";
        }
        return "Se usa la fórmula de reducción, que sale de integrar por partes (sec³ x: u = sec x, dv = sec² x dx)";
    }

    /** Si es una sola función (tanⁿ, secⁿ, cscⁿ, cotⁿ) usa su regla; si no, lo pasa a senᵖ·cos^q. */
    private static Optional<Trig> integrarMonomio(Monomial m, char x) {
        Racional k = m.k;
        if (m.exponentes.size() == 1) {
            var e = m.exponentes.entrySet().iterator().next();
            int n = e.getValue();
            if (n > 0 && n <= MAXIMA_POTENCIA) {
                List<String> l = new ArrayList<>();
                switch (e.getKey()) {
                    case TAN -> {
                        return Optional.of(new Trig(tanN(n, k, x, l), l, "Potencia de tangente"));
                    }
                    case SEC -> {
                        return Optional.of(new Trig(secN(n, k, x, l), l, "Potencia de secante"));
                    }
                    case CSC -> {
                        return Optional.of(new Trig(cscN(n, k, x, l), l, "Potencia de cosecante"));
                    }
                    case COT -> {
                        return Optional.of(new Trig(cotN(n, k, x, l), l, "Potencia de cotangente"));
                    }
                    default -> {
                    }
                }
            }
        }
        int pSen = 0;
        int qCos = 0;
        for (var e : m.exponentes.entrySet()) {
            int n = e.getValue();
            switch (e.getKey()) {
                case SEN -> pSen += n;
                case COS -> qCos += n;
                case TAN -> {
                    pSen += n;
                    qCos -= n;
                }
                case COT -> {
                    pSen -= n;
                    qCos += n;
                }
                case SEC -> qCos -= n;
                case CSC -> pSen -= n;
                default -> {
                    return Optional.empty();
                }
            }
        }
        return senCos(pSen, qCos, k, x);
    }

    /** ∫senᵖ(kx)·cos^q(kx) dx con p y q enteros. */
    static Optional<Trig> senCos(int pSen, int qCos, Racional k, char x) {
        if (Math.abs(pSen) > MAXIMA_POTENCIA || Math.abs(qCos) > MAXIMA_POTENCIA) {
            return Optional.empty();
        }
        List<String> l = new ArrayList<>();
        if (pSen == 0 && qCos == 0) {
            return Optional.empty();
        }
        if (pSen > 0 && pSen % 2 == 1) {
            return Optional.of(new Trig(impar(pSen, qCos, k, x, true, l), l, "Potencia impar de seno"));
        }
        if (qCos > 0 && qCos % 2 == 1) {
            return Optional.of(new Trig(impar(qCos, pSen, k, x, false, l), l, "Potencia impar de coseno"));
        }
        if (pSen >= 0 && qCos >= 0) {
            return Optional.of(new Trig(pares(pSen, qCos, k, x, l), l, "Potencias pares: ángulo mitad"));
        }
        if (pSen == 0) {
            return Optional.of(new Trig(secN(-qCos, k, x, l), l, "Potencia de secante"));
        }
        if (qCos == 0) {
            return Optional.of(new Trig(cscN(-pSen, k, x, l), l, "Potencia de cosecante"));
        }
        if (pSen == -qCos && pSen > 0) {
            return Optional.of(new Trig(tanN(pSen, k, x, l), l, "Potencia de tangente"));
        }
        if (pSen == -qCos) {
            return Optional.of(new Trig(cotN(qCos, k, x, l), l, "Potencia de cotangente"));
        }
        return Optional.empty();
    }

    private static Expresion arg(Racional k, char x) {
        return k.equals(Racional.UNO) ? new Variable(x) : por(c(k), new Variable(x));
    }

    private static Expresion f(TipoFuncion t, Racional k, char x) {
        return new Funcion(t, arg(k, x));
    }

    /** "sen x", "cos(2x)". */
    private static String nombre(TipoFuncion t, Racional k, char x) {
        return p(f(t, k, x));
    }

    /**
     * senᵐ·cosⁿ con m impar (o cosᵐ·senⁿ): se separa un factor y el resto pasa con sen² + cos² = 1.
     *
     * @param impar el exponente impar m; {@code otro} el de la otra función
     * @param seno  true si el impar es el del seno (u = cos), false si es el del coseno (u = sen)
     */
    private static Expresion impar(int impar, int otro, Racional k, char x, boolean seno, List<String> l) {
        TipoFuncion separada = seno ? TipoFuncion.SEN : TipoFuncion.COS;
        TipoFuncion uFun = seno ? TipoFuncion.COS : TipoFuncion.SEN;
        int mitad = (impar - 1) / 2;
        Expresion u = f(uFun, k, x);
        char w = 'u';
        // (1 − u²)^mitad · u^otro, desarrollado
        Polinomio base = Polinomio.UNO.restar(Polinomio.variable(w).potencia(2)).potencia(mitad);
        Racional signo = seno ? Racional.UNO.negar() : Racional.UNO; // du = −k·sen dx o du = k·cos dx
        Racional factor = signo.dividir(k);
        List<Expresion> terminos = new ArrayList<>();
        List<Expresion> enU = new ArrayList<>();
        for (var t : base.terminos().entrySet()) {
            int e = t.getKey().exponente(w) + otro;
            Racional coef = t.getValue();
            enU.add(por(c(coef), pot(new Variable(w), e)));
            if (e == -1) {
                terminos.add(por(c(coef.multiplicar(factor)), TablaIntegrales.ln(u)));
            } else {
                terminos.add(por(c(coef.multiplicar(factor).dividir(Racional.de(e + 1))), pot(u, e + 1)));
            }
        }
        String sep = nombre(separada, k, x);
        String un = nombre(uFun, k, x);
        if (impar > 1) {
            l.add(sep + "^" + impar + " = " + sep + "^" + (impar - 1) + "·" + sep + " = (1 − " + un + "²)"
                    + (mitad > 1 ? "^" + mitad : "") + "·" + sep + ", porque sen² + cos² = 1");
        }
        l.add("u = " + un + ", du = " + (seno ? "−" : "") + (k.equals(Racional.UNO) ? "" : ImpresoraPizarra.numero(k) + "·")
                + sep + " d" + x);
        Expresion integrandoU = s(mas(enU.toArray(Expresion[]::new)));
        l.add("Queda ∫" + (factor.equals(Racional.UNO) ? "" : "(" + ImpresoraPizarra.numero(factor) + ")")
                + integrando(integrandoU) + " du, un polinomio en u");
        Expresion r = s(mas(terminos.toArray(Expresion[]::new)));
        l.add("Se regresa a " + x + " con u = " + un + ": " + p(r));
        return r;
    }

    /** senᵖ·cos^q con p y q pares (≥ 0): ángulo mitad, y otra vez sobre cada potencia de cos(2kx). */
    private static Expresion pares(int pSen, int qCos, Racional k, char x, List<String> l) {
        char w = 'c';
        Racional medio = Racional.de(1, 2);
        Polinomio cc = Polinomio.variable(w);
        Polinomio senos = Polinomio.constante(medio).restar(cc.multiplicar(medio)).potencia(pSen / 2);
        Polinomio cosenos = Polinomio.constante(medio).sumar(cc.multiplicar(medio)).potencia(qCos / 2);
        Polinomio producto = senos.multiplicar(cosenos);
        Racional k2 = k.multiplicar(Racional.de(2));
        String c2 = nombre(TipoFuncion.COS, k2, x);
        if (pSen > 0) {
            l.add(nombre(TipoFuncion.SEN, k, x) + "² = (1 − " + c2 + ")/2");
        }
        if (qCos > 0) {
            l.add(nombre(TipoFuncion.COS, k, x) + "² = (1 + " + c2 + ")/2");
        }
        List<Expresion> desarrollo = new ArrayList<>();
        List<Expresion> integrales = new ArrayList<>();
        for (var t : producto.terminos().entrySet()) {
            int j = t.getKey().exponente(w);
            Racional coef = t.getValue();
            desarrollo.add(por(c(coef), pot(f(TipoFuncion.COS, k2, x), j)));
            Expresion i;
            if (j == 0) {
                i = new Variable(x);
            } else {
                List<String> sub = new ArrayList<>();
                i = j % 2 == 1 ? impar(j, 0, k2, x, false, sub) : pares(0, j, k2, x, sub);
            }
            integrales.add(escalar(coef, i));
        }
        l.add("El integrando queda " + p(s(mas(desarrollo.toArray(Expresion[]::new)))));
        Expresion r = s(mas(integrales.toArray(Expresion[]::new)));
        l.add("Término a término: " + p(r));
        return r;
    }

    /** ∫tanⁿ(kx) dx con tan² = sec² − 1. */
    private static Expresion tanN(int n, Racional k, char x, List<String> l) {
        String t = nombre(TipoFuncion.TAN, k, x);
        if (n == 0) {
            return new Variable(x);
        }
        if (n == 1) {
            Expresion r = por(c(k.inverso()), TablaIntegrales.ln(f(TipoFuncion.SEC, k, x)));
            l.add("∫" + t + " d" + x + " = " + p(r));
            return r;
        }
        l.add(t + "^" + n + " = " + (n > 2 ? t + "^" + (n - 2) + "·" : "") + "(sec² − 1) = "
                + (n > 2 ? t + "^" + (n - 2) + "·" : "") + "sec² − " + (n > 2 ? t + "^" + (n - 2) : "1")
                + ", porque tan² = sec² − 1");
        Expresion primero = por(c(Racional.UNO.dividir(k.multiplicar(Racional.de(n - 1)))),
                pot(f(TipoFuncion.TAN, k, x), n - 1));
        l.add("∫" + (n > 2 ? t + "^" + (n - 2) + "·" : "") + "sec² d" + x + " = " + p(primero) + " (con u = " + t + ")");
        Expresion resto = tanN(n - 2, k, x, l);
        return s(mas(primero, menos(resto)));
    }

    /** ∫cotⁿ(kx) dx con cot² = csc² − 1. */
    private static Expresion cotN(int n, Racional k, char x, List<String> l) {
        String t = nombre(TipoFuncion.COT, k, x);
        if (n == 0) {
            return new Variable(x);
        }
        if (n == 1) {
            Expresion r = por(c(k.inverso()), TablaIntegrales.ln(f(TipoFuncion.SEN, k, x)));
            l.add("∫" + t + " d" + x + " = " + p(r));
            return r;
        }
        l.add(t + "^" + n + " = " + (n > 2 ? t + "^" + (n - 2) + "·" : "") + "(csc² − 1), porque cot² = csc² − 1");
        Expresion primero = menos(por(c(Racional.UNO.dividir(k.multiplicar(Racional.de(n - 1)))),
                pot(f(TipoFuncion.COT, k, x), n - 1)));
        Expresion resto = cotN(n - 2, k, x, l);
        return s(mas(primero, menos(resto)));
    }

    /** ∫secⁿ(kx) dx: sec x → ln|sec x + tan x|, sec² → tan, y desde sec³ la reducción (por partes). */
    private static Expresion secN(int n, Racional k, char x, List<String> l) {
        Expresion sec = f(TipoFuncion.SEC, k, x);
        Expresion tan = f(TipoFuncion.TAN, k, x);
        Racional ik = k.inverso();
        if (n == 0) {
            return new Variable(x);
        }
        if (n == 1) {
            Expresion r = por(c(ik), TablaIntegrales.ln(new Suma(List.of(sec, tan))));
            l.add("∫" + p(sec) + " d" + x + " = " + p(r));
            return r;
        }
        if (n == 2) {
            Expresion r = por(c(ik), tan);
            l.add("∫" + p(sec) + "² d" + x + " = " + p(r));
            return r;
        }
        String se = p(sec);
        String ta = p(tan);
        String sn2 = n - 2 == 1 ? se : se + "^" + (n - 2);
        l.add("Por partes con u = " + sn2 + " y dv = " + se + "² d" + x + ": v = " + ta + ", y tan² = sec² − 1");
        l.add("∫" + se + "^" + n + " d" + x + " = " + sn2 + "·" + ta + " − " + (n - 2 == 1 ? "" : (n - 2) + "")
                + "∫" + sn2 + "·" + ta + "² d" + x + " = " + sn2 + "·" + ta + " − " + (n - 2 == 1 ? "" : (n - 2) + "")
                + "∫" + se + "^" + n + " d" + x + " + " + (n - 2 == 1 ? "" : (n - 2) + "") + "∫" + sn2 + " d" + x);
        l.add("Reaparece la integral original: I = " + sn2 + "·" + ta + " − " + (n - 2 == 1 ? "" : (n - 2) + "") + "I + "
                + (n - 2 == 1 ? "" : (n - 2) + "") + "∫" + sn2 + " d" + x + ", entonces " + (n - 1) + "I = " + sn2 + "·"
                + ta + " + " + (n - 2 == 1 ? "" : (n - 2) + "") + "∫" + sn2 + " d" + x);
        Expresion primero = por(c(ik.dividir(Racional.de(n - 1))), pot(sec, n - 2), tan);
        Expresion resto = secN(n - 2, k, x, l);
        return s(mas(primero, por(c(Racional.de(n - 2, n - 1)), resto)));
    }

    /** ∫cscⁿ(kx) dx: csc x → −ln|csc x + cot x|, csc² → −cot, y desde csc³ la reducción. */
    private static Expresion cscN(int n, Racional k, char x, List<String> l) {
        Expresion csc = f(TipoFuncion.CSC, k, x);
        Expresion cot = f(TipoFuncion.COT, k, x);
        Racional ik = k.inverso();
        if (n == 0) {
            return new Variable(x);
        }
        if (n == 1) {
            Expresion r = menos(por(c(ik), TablaIntegrales.ln(new Suma(List.of(csc, cot)))));
            l.add("∫" + p(csc) + " d" + x + " = " + p(r));
            return r;
        }
        if (n == 2) {
            Expresion r = menos(por(c(ik), cot));
            l.add("∫" + p(csc) + "² d" + x + " = " + p(r) + " (tabla: ∫csc² x dx = −cot x)");
            return r;
        }
        l.add("Fórmula de reducción (por partes con dv = csc² d" + x + "): ∫cscⁿ = −cscⁿ⁻²·cot/(n − 1) + "
                + "(n − 2)/(n − 1)·∫cscⁿ⁻²");
        Expresion primero = menos(por(c(ik.dividir(Racional.de(n - 1))), pot(csc, n - 2), cot));
        Expresion resto = cscN(n - 2, k, x, l);
        return s(mas(primero, por(c(Racional.de(n - 2, n - 1)), resto)));
    }

    // ================================================================== I05: sustitución trigonométrica

    private enum Forma {
        /** √(a² − x²): x = a·sen θ. */
        SEN,
        /** √(a² + x²): x = a·tan θ. */
        TAN,
        /** √(x² − a²): x = a·sec θ. */
        SEC
    }

    /** coef·xᵏ·(radical)ʳ con r impar. */
    private static final class Radical {
        Racional coeficiente = Racional.UNO;
        int k;
        int r;
        Polinomio radicando;
    }

    private static boolean acumularRadical(Expresion e, char x, int signo, Radical m) {
        switch (e) {
            case Constante k -> {
                if (k.valor().esCero()) {
                    return false;
                }
                m.coeficiente = signo > 0 ? m.coeficiente.multiplicar(k.valor()) : m.coeficiente.dividir(k.valor());
                return true;
            }
            case Negacion n -> {
                m.coeficiente = m.coeficiente.negar();
                return acumularRadical(n.argumento(), x, signo, m);
            }
            case Producto pr -> {
                for (Expresion f : pr.factores()) {
                    if (!acumularRadical(f, x, signo, m)) {
                        return false;
                    }
                }
                return true;
            }
            case Cociente q -> {
                return acumularRadical(q.numerador(), x, signo, m) && acumularRadical(q.denominador(), x, -signo, m);
            }
            case Variable v when v.nombre() == x -> {
                m.k += signo;
                return true;
            }
            case Raiz rz when rz.indice() == 2 -> {
                return radicando(rz.radicando(), x, signo, m);
            }
            case Funcion fn when fn.tipo() == TipoFuncion.SQRT -> {
                return radicando(fn.argumento(), x, signo, m);
            }
            case Potencia pw when pw.exponente() instanceof Constante k -> {
                Racional n = k.valor();
                Expresion b = pw.base();
                if (b instanceof Variable v && v.nombre() == x && n.esEntero()) {
                    m.k += signo * n.num().intValueExact();
                    return true;
                }
                if (n.esEntero() && (b instanceof Raiz rz && rz.indice() == 2
                        || b instanceof Funcion fn && fn.tipo() == TipoFuncion.SQRT)) {
                    Expresion rad = b instanceof Raiz rz ? rz.radicando() : ((Funcion) b).argumento();
                    return radicando(rad, x, signo * n.num().intValueExact(), m);
                }
                if (n.den().intValue() == 2) {
                    return radicando(b, x, signo * n.num().intValueExact(), m);
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private static boolean radicando(Expresion e, char x, int veces, Radical m) {
        Polinomio q;
        try {
            q = Polinomio.desde(e);
        } catch (RuntimeException noEs) {
            return false;
        }
        if (m.radicando != null && !m.radicando.equals(q)) {
            return false;
        }
        m.radicando = q;
        m.r += veces;
        return true;
    }

    static Optional<MotorIntegrales.Integracion> sustitucionTrigonometrica(Expresion f, char x) {
        Radical m = new Radical();
        if (!acumularRadical(s(f), x, 1, m) || m.radicando == null || m.r % 2 == 0) {
            return Optional.empty();
        }
        Polinomio q = m.radicando;
        if (q.variables().size() != 1 || q.gradoEn(x) != 2 || !q.coeficiente(Monomio.de(x, 1)).esCero()) {
            return Optional.empty();
        }
        Racional beta = q.coeficiente(Monomio.de(x, 2));
        Racional alfa = q.constante();
        if (alfa.esCero() || !(beta.equals(Racional.UNO) || beta.equals(Racional.UNO.negar()))) {
            return Optional.empty();
        }
        Forma forma = beta.signo() < 0 ? (alfa.signo() > 0 ? Forma.SEN : null)
                : alfa.signo() > 0 ? Forma.TAN : Forma.SEC;
        if (forma == null) {
            return Optional.empty();
        }
        Racional a2 = alfa.signo() > 0 ? alfa : alfa.negar();
        Expresion a = Simplificador.raiz(2, c(a2));
        int k = m.k;
        int r = m.r;
        int pSen;
        int qCos;
        switch (forma) {
            case SEN -> {
                pSen = k;
                qCos = r + 1;
            }
            case TAN -> {
                pSen = k;
                qCos = -k - r - 2;
            }
            default -> {
                pSen = r + 1;
                qCos = -k - r - 2;
            }
        }
        Optional<Trig> enTheta = senCos(pSen, qCos, Racional.UNO, THETA);
        if (enTheta.isEmpty()) {
            return Optional.empty();
        }
        Expresion constante = s(por(c(m.coeficiente), Simplificador.potencia(a, Constante.de(k + r + 1))));
        Expresion integralTheta = constante instanceof Constante kc ? escalar(kc.valor(), enTheta.get().integral())
                : s(por(constante, enTheta.get().integral()));

        Variable xv = new Variable(x);
        Expresion x2 = pot(xv, 2);
        Expresion raiz = new Raiz(2, switch (forma) {
            case SEN -> new Suma(List.of(c(a2), new Negacion(x2)));
            case TAN -> new Suma(List.of(x2, c(a2)));
            case SEC -> new Suma(List.of(x2, new Negacion(c(a2))));
        });
        Expresion opuesto = forma == Forma.SEC ? raiz : xv;
        Expresion adyacente = switch (forma) {
            case SEN -> raiz;
            case TAN, SEC -> a;
        };
        Expresion hipotenusa = switch (forma) {
            case SEN -> a;
            case TAN -> raiz;
            case SEC -> xv;
        };
        Expresion xSobreA = s(new Cociente(xv, a));
        Expresion angulo = new Funcion(switch (forma) {
            case SEN -> TipoFuncion.ARCSEN;
            case TAN -> TipoFuncion.ARCTAN;
            case SEC -> TipoFuncion.ARCSEC;
        }, xSobreA);
        Triangulo tri = new Triangulo(opuesto, adyacente, hipotenusa, angulo);
        Optional<Expresion> enX = tri.regresar(integralTheta);
        if (enX.isEmpty()) {
            return Optional.empty();
        }
        Expresion antiderivada = TablaIntegrales.sinAbsolutoInnecesario(s(enX.get()));

        String fun = switch (forma) {
            case SEN -> "sen";
            case TAN -> "tan";
            case SEC -> "sec";
        };
        String at = p(a);
        String aPor = a2.equals(Racional.UNO) ? "" : at + "·";
        String sustitucion = x + " = " + aPor + fun + " θ";
        String dx = switch (forma) {
            case SEN -> aPor + "cos θ dθ";
            case TAN -> aPor + "sec² θ dθ";
            case SEC -> aPor + "sec θ·tan θ dθ";
        };
        String rad = switch (forma) {
            case SEN -> aPor + "cos θ";
            case TAN -> aPor + "sec θ";
            case SEC -> aPor + "tan θ";
        };
        Expresion integrandoTheta = s(por(constante, monomioTrig(pSen, qCos)));
        List<String> lineas = new ArrayList<>();
        lineas.add(sustitucion + ", d" + x + " = " + dx);
        lineas.add(p(raiz) + " = " + rad + " (por la identidad "
                + switch (forma) {
                    case SEN -> "1 − sen² θ = cos² θ";
                    case TAN -> "1 + tan² θ = sec² θ";
                    case SEC -> "sec² θ − 1 = tan² θ";
                } + ")");
        lineas.add("∫" + integrando(s(f)) + " d" + x + " = ∫" + integrando(integrandoTheta) + " dθ");
        lineas.add("∫" + integrando(integrandoTheta) + " dθ = " + p(integralTheta));
        lineas.add("Se regresa a " + x + " con el triángulo: " + p(antiderivada));
        List<String> razones = tri.razonesUsadas(integralTheta);
        TrabajoIntermedio.TrianguloSustitucion trabajo = new TrabajoIntermedio.TrianguloSustitucion(sustitucion,
                p(opuesto), p(adyacente), p(hipotenusa), "θ = " + p(angulo), razones, lineas);
        List<String> lineasTheta = new ArrayList<>(enTheta.get().lineas());
        lineasTheta.add("∫" + integrando(integrandoTheta) + " dθ = " + p(integralTheta));
        PasoLimite hijo = new PasoLimite(TRIGONOMETRICA, enTheta.get().titulo() + " en θ",
                justificacion(enTheta.get().titulo()), new Integral(integrandoTheta, THETA), integralTheta,
                new TrabajoIntermedio.Agrupacion(lineasTheta));
        String radTexto = switch (forma) {
            case SEN -> "√(a² − x²)";
            case TAN -> "√(a² + x²)";
            case SEC -> "√(x² − a²)";
        };
        PasoLimite paso = new PasoLimite(SUSTITUCION_TRIG, "Sustitución " + sustitucion, "El integrando tiene "
                + radTexto.replace("a²", ImpresoraPizarra.numero(a2)).replace('x', x) + ": con " + sustitucion
                + " la raíz sale exacta y queda una integral trigonométrica en θ", new Integral(f, x), antiderivada,
                trabajo, "Integral en θ", List.of(hijo));
        return Optional.of(new MotorIntegrales.Integracion(antiderivada, List.of(paso), SUSTITUCION_TRIG));
    }

    /** senᵖ θ·cos^q θ escrita con su nombre: cos² θ, sec θ, tan² θ, csc² θ, … */
    private static Expresion monomioTrig(int pSen, int qCos) {
        Variable t = new Variable(THETA);
        if (pSen == -qCos && pSen != 0) {
            return pSen > 0 ? pot(new Funcion(TipoFuncion.TAN, t), pSen) : pot(new Funcion(TipoFuncion.COT, t), -pSen);
        }
        if (pSen == 0 && qCos < 0) {
            return pot(new Funcion(TipoFuncion.SEC, t), -qCos);
        }
        if (qCos == 0 && pSen < 0) {
            return pot(new Funcion(TipoFuncion.CSC, t), -pSen);
        }
        return por(pot(new Funcion(TipoFuncion.SEN, t), pSen), pot(new Funcion(TipoFuncion.COS, t), qCos));
    }

    /** El triángulo rectángulo: lados en x y θ en función de x. */
    private record Triangulo(Expresion opuesto, Expresion adyacente, Expresion hipotenusa, Expresion angulo) {

        /** La razón como numerador/denominador. */
        Expresion[] razon(TipoFuncion t) {
            return switch (t) {
                case SEN -> new Expresion[] {opuesto, hipotenusa};
                case COS -> new Expresion[] {adyacente, hipotenusa};
                case TAN -> new Expresion[] {opuesto, adyacente};
                case COT -> new Expresion[] {adyacente, opuesto};
                case SEC -> new Expresion[] {hipotenusa, adyacente};
                case CSC -> new Expresion[] {hipotenusa, opuesto};
                default -> null;
            };
        }

        Expresion valor(TipoFuncion t) {
            Expresion[] r = razon(t);
            return r == null ? null : s(new Cociente(r[0], r[1]));
        }

        List<String> razonesUsadas(Expresion e) {
            List<String> r = new ArrayList<>();
            for (TipoFuncion t : List.of(TipoFuncion.SEN, TipoFuncion.COS, TipoFuncion.TAN, TipoFuncion.COT,
                    TipoFuncion.SEC, TipoFuncion.CSC)) {
                if (Expresiones.contiene(e, new Funcion(t, new Variable(THETA)))
                        || (t == TipoFuncion.SEN || t == TipoFuncion.COS)
                        && Expresiones.contiene(e, new Funcion(TipoFuncion.SEN, por(Constante.de(2), new Variable(THETA))))) {
                    Expresion[] q = razon(t);
                    r.add(t.nombre() + " θ = " + p(q[0]) + "/" + (q[1] instanceof Suma || q[1] instanceof Raiz ? p(q[1])
                            : p(q[1])));
                }
            }
            return r;
        }

        /** Cambia θ y sus razones por lo que dice el triángulo; vacío si queda algo que no se sabe regresar. */
        Optional<Expresion> regresar(Expresion e) {
            try {
                return Optional.of(cambiar(e));
            } catch (IllegalStateException no) {
                return Optional.empty();
            }
        }

        private Expresion cambiar(Expresion e) {
            return switch (e) {
                case Variable v when v.nombre() == THETA -> angulo;
                case Funcion fn when fn.tipo() == TipoFuncion.LN && fn.argumento() instanceof Funcion abs
                        && abs.tipo() == TipoFuncion.ABS -> logaritmo(abs.argumento());
                case Funcion fn when fn.argumento() instanceof Variable v && v.nombre() == THETA -> {
                    Expresion val = valor(fn.tipo());
                    if (val == null) {
                        throw new IllegalStateException();
                    }
                    yield val;
                }
                case Funcion fn when esDoble(fn.argumento()) -> switch (fn.tipo()) {
                    case SEN -> por(Constante.de(2), valor(TipoFuncion.SEN), valor(TipoFuncion.COS));
                    case COS -> mas(pot(valor(TipoFuncion.COS), 2), menos(pot(valor(TipoFuncion.SEN), 2)));
                    default -> throw new IllegalStateException();
                };
                case Funcion fn -> {
                    if (Expresiones.variables(fn.argumento()).contains(THETA)) {
                        throw new IllegalStateException();
                    }
                    yield fn;
                }
                case Suma sm -> new Suma(sm.terminos().stream().map(this::cambiar).toList());
                case Producto pr -> new Producto(pr.factores().stream().map(this::cambiar).toList());
                case Cociente q -> new Cociente(cambiar(q.numerador()), cambiar(q.denominador()));
                case Negacion n -> new Negacion(cambiar(n.argumento()));
                case Potencia pw -> new Potencia(cambiar(pw.base()), cambiar(pw.exponente()));
                case Raiz rz -> new Raiz(rz.indice(), cambiar(rz.radicando()));
                default -> e;
            };
        }

        private static boolean esDoble(Expresion a) {
            ConCoeficiente cc = separar(a);
            return cc.coeficiente().equals(Racional.de(2)) && cc.resto() instanceof Variable v && v.nombre() == THETA;
        }

        /**
         * ln|…| de razones de θ: todas tienen el mismo denominador en el triángulo, así que queda
         * ln|numerador| − ln|denominador|, y si el denominador es un número se va con la constante C.
         */
        private Expresion logaritmo(Expresion dentro) {
            List<Expresion> terminos = dentro instanceof Suma sm ? sm.terminos() : List.of(dentro);
            Expresion den = null;
            List<Expresion> nums = new ArrayList<>();
            for (Expresion t : terminos) {
                boolean negativo = t instanceof Negacion;
                Expresion base = negativo ? ((Negacion) t).argumento() : t;
                if (!(base instanceof Funcion fn && fn.argumento() instanceof Variable v && v.nombre() == THETA)) {
                    return new Funcion(TipoFuncion.LN, new Funcion(TipoFuncion.ABS, cambiar(dentro)));
                }
                Expresion[] q = razon(fn.tipo());
                if (q == null || den != null && !den.equals(q[1])) {
                    return new Funcion(TipoFuncion.LN, new Funcion(TipoFuncion.ABS, cambiar(dentro)));
                }
                den = q[1];
                nums.add(negativo ? new Negacion(q[0]) : q[0]);
            }
            Expresion num = nums.size() == 1 ? nums.getFirst() : new Suma(nums);
            List<Expresion> r = new ArrayList<>();
            if (depende(num, 'x') || !Expresiones.variables(num).isEmpty()) {
                r.add(TablaIntegrales.ln(num));
            }
            if (!Expresiones.variables(den).isEmpty()) {
                r.add(new Negacion(TablaIntegrales.ln(den)));
            }
            return r.isEmpty() ? Constante.de(0) : r.size() == 1 ? r.getFirst() : new Suma(r);
        }
    }

    // ================================================================== I03: por partes

    /** Las letras de ILATE, de la que se elige primero como u a la última. */
    enum Ilate {
        I("inversa"), L("logarítmica"), A("algebraica"), T("trigonométrica"), E("exponencial");

        private final String nombre;

        Ilate(String nombre) {
            this.nombre = nombre;
        }

        String nombre() {
            return nombre;
        }
    }

    static Optional<Ilate> categoria(Expresion f, char x) {
        if (!depende(f, x)) {
            return Optional.empty();
        }
        switch (f) {
            case Funcion fn -> {
                return switch (fn.tipo()) {
                    case ARCSEN, ARCCOS, ARCTAN, ARCSEC -> Optional.of(Ilate.I);
                    case LN, LOG -> Optional.of(Ilate.L);
                    case SEN, COS, TAN, COT, SEC, CSC -> Optional.of(Ilate.T);
                    case EXP -> Optional.of(Ilate.E);
                    case SQRT -> Optional.of(Ilate.A);
                    default -> Optional.empty();
                };
            }
            case Potencia pw -> {
                if (pw.base() instanceof ConstanteEspecial || pw.base() instanceof Constante) {
                    return depende(pw.exponente(), x) && !depende(pw.base(), x) ? Optional.of(Ilate.E) : Optional.empty();
                }
                if (pw.exponente() instanceof Constante && pw.base() instanceof Funcion) {
                    return categoria(pw.base(), x);
                }
                break;
            }
            default -> {
            }
        }
        try {
            Polinomio.desde(f);
            return Optional.of(Ilate.A);
        } catch (RuntimeException noEs) {
            if (f instanceof Raiz || f instanceof Potencia pw && pw.exponente() instanceof Constante) {
                return Optional.of(Ilate.A);
            }
            return Optional.empty();
        }
    }

    /** La elección de u y dv, con v ya integrada. */
    record Eleccion(Expresion u, Ilate letraU, Expresion dv, Expresion v, String ilate) {
    }

    /** Elige u por ILATE (la primera que deja integrar dv) y calcula v. */
    static Optional<Eleccion> elegir(Expresion g, char x, Integrador resto) {
        ConCoeficiente cc = separar(g);
        Expresion h = cc.resto();
        List<Expresion> factores = new ArrayList<>();
        switch (h) {
            case Producto pr -> factores.addAll(pr.factores());
            case Cociente q -> {
                factores.add(q.numerador());
                factores.add(Simplificador.potencia(q.denominador(), Constante.de(-1)));
            }
            default -> factores.add(h);
        }
        factores.removeIf(fa -> fa.equals(Constante.de(1)));
        List<Ilate> letras = new ArrayList<>();
        for (Expresion fa : factores) {
            Optional<Ilate> l = categoria(fa, x);
            if (l.isEmpty()) {
                return Optional.empty();
            }
            letras.add(l.get());
        }
        if (factores.isEmpty()) {
            return Optional.empty();
        }
        if (factores.size() == 1) {
            Ilate l = letras.getFirst();
            if (l != Ilate.I && l != Ilate.L) {
                return Optional.empty();
            }
            Expresion u = factores.getFirst();
            Expresion v = s(por(c(cc.coeficiente()), new Variable(x)));
            return Optional.of(new Eleccion(u, l, c(cc.coeficiente()), v, "Un solo factor: u = " + p(u) + " ("
                    + l.nombre() + ", " + l + ") y dv = " + (cc.coeficiente().equals(Racional.UNO) ? ""
                    : ImpresoraPizarra.numero(cc.coeficiente()) + " ") + "d" + x));
        }
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < factores.size(); i++) {
            orden.add(i);
        }
        orden.sort((i, j) -> letras.get(i).compareTo(letras.get(j)));
        for (int i : orden) {
            Expresion u = factores.get(i);
            List<Expresion> otros = new ArrayList<>();
            otros.add(c(cc.coeficiente()));
            for (int j = 0; j < factores.size(); j++) {
                if (j != i) {
                    otros.add(factores.get(j));
                }
            }
            Expresion dv = s(Simplificador.producto(otros));
            Optional<MotorIntegrales.Integracion> v = resto.integrar(dv, x);
            if (v.isEmpty()) {
                continue;
            }
            Ilate l = letras.get(i);
            StringBuilder texto = new StringBuilder("ILATE: u = " + p(u) + " es " + l.nombre() + " (" + l + ")");
            List<String> despues = new ArrayList<>();
            for (int j = 0; j < factores.size(); j++) {
                if (j != i) {
                    despues.add(p(factores.get(j)) + ", " + letras.get(j).nombre() + " (" + letras.get(j) + ")");
                }
            }
            texto.append(letras.get(i) == letrasMinimas(letras) ? " y va antes que " : "; la primera opción no deja "
                    + "integrar dv, así que va antes que ").append(String.join(" y ", despues));
            return Optional.of(new Eleccion(u, l, dv, FormaFinalSegura.ordenar(s(v.get().antiderivada()), x),
                    texto.toString()));
        }
        return Optional.empty();
    }

    private static Ilate letrasMinimas(List<Ilate> letras) {
        return letras.stream().min(Ilate::compareTo).orElseThrow();
    }

    /** ∫g = a + c·I, con I la integral original (para la integral cíclica). */
    private record Lineal(Expresion a, Racional c, List<PasoLimite> pasos) {
    }

    static Optional<MotorIntegrales.Integracion> partes(Expresion f, char x, Integrador resto) {
        Expresion original = s(f);
        Optional<Lineal> l = partes(original, original, x, 1, resto);
        if (l.isEmpty()) {
            return Optional.empty();
        }
        Lineal r = l.get();
        List<PasoLimite> pasos = new ArrayList<>(r.pasos());
        Expresion antiderivada;
        if (r.c().esCero()) {
            antiderivada = r.a();
        } else {
            Racional unoMenos = Racional.UNO.restar(r.c());
            if (unoMenos.esCero()) {
                return Optional.empty();
            }
            antiderivada = s(por(c(unoMenos.inverso()), r.a()));
            String n = ImpresoraPizarra.numero(unoMenos);
            String cI = r.c().equals(Racional.UNO.negar()) ? " − I" : (r.c().signo() < 0 ? " − " : " + ")
                    + ImpresoraPizarra.numero(r.c().signo() < 0 ? r.c().negar() : r.c()) + "I";
            List<String> lineas = List.of(
                    "Reaparece la integral original: I = " + p(r.a()) + cI + ", entonces " + n + "I = " + p(r.a()),
                    "I = " + p(antiderivada));
            pasos.add(new PasoLimite(PARTES, "Despejar la integral original", "Al integrar por partes otra vez volvió "
                    + "a salir I = ∫" + integrando(original) + " d" + x + ": se pasa al otro lado como en una ecuación",
                    new Integral(f, x), antiderivada, new TrabajoIntermedio.Agrupacion(lineas)));
        }
        return Optional.of(new MotorIntegrales.Integracion(factorComun(s(antiderivada), x), pasos, PARTES));
    }

    private static Optional<Lineal> partes(Expresion g, Expresion original, char x, int nivel, Integrador resto) {
        if (nivel > MAXIMO_PARTES) {
            return Optional.empty();
        }
        Optional<Eleccion> oe = elegir(g, x, resto);
        if (oe.isEmpty()) {
            return Optional.empty();
        }
        Eleccion e = oe.get();
        Expresion du = s(Derivador.derivar(e.u(), x).derivada());
        Expresion vdu = s(por(e.v(), du));
        Expresion uv = s(por(e.u(), e.v()));
        Lineal sigue;
        String queda;
        Racional k = vdu.equals(Constante.de(0)) ? null : proporcional(vdu, original);
        if (vdu.equals(Constante.de(0))) {
            sigue = new Lineal(Constante.de(0), Racional.CERO, List.of());
            queda = "∫v du = 0";
        } else if (k != null && nivel > 1) {
            sigue = new Lineal(Constante.de(0), k, List.of());
            queda = "∫" + integrando(vdu) + " d" + x + " = " + (k.equals(Racional.UNO) ? "" : k.equals(Racional.UNO.negar())
                    ? "−" : ImpresoraPizarra.numero(k) + "·") + "I: reaparece la integral original";
        } else {
            Optional<MotorIntegrales.Integracion> directa = resto.integrar(vdu, x);
            if (directa.isPresent()) {
                Expresion fv = s(directa.get().antiderivada());
                sigue = new Lineal(fv, Racional.CERO, directa.get().pasos());
                queda = "∫" + integrando(vdu) + " d" + x + " = " + p(fv);
            } else {
                Optional<Lineal> otra = partes(vdu, original, x, nivel + 1, resto);
                if (otra.isEmpty()) {
                    return Optional.empty();
                }
                sigue = otra.get();
                queda = "∫" + integrando(vdu) + " d" + x + " necesita por partes otra vez";
            }
        }
        Expresion a = s(mas(uv, menos(sigue.a())));
        Racional cI = sigue.c().negar();
        String total = p(a) + (cI.esCero() ? "" : cI.equals(Racional.UNO.negar()) ? " − I"
                : (cI.signo() < 0 ? " − " : " + ") + ImpresoraPizarra.numero(cI.signo() < 0 ? cI.negar() : cI) + "I");
        String dvTexto = e.dv().equals(Constante.de(1)) ? "d" + x : integrando(e.dv()) + " d" + x;
        List<String> lineas = List.of(
                "∫" + integrando(g) + " d" + x + " = uv − ∫v du = " + p(uv) + " − ∫" + integrando(vdu) + " d" + x,
                queda,
                "∫" + integrando(g) + " d" + x + " = " + total);
        TrabajoIntermedio.TablaPartes tabla = new TrabajoIntermedio.TablaPartes(p(e.u()),
                integrando(du) + " d" + x, dvTexto, p(e.v()), e.ilate(), lineas);
        PasoLimite paso = new PasoLimite(PARTES, nivel == 1 ? "Por partes" : "Por partes otra vez (" + nivel + "ª vez)",
                "∫u dv = uv − ∫v du, con u elegida por ILATE (inversas, logarítmicas, algebraicas, trigonométricas, "
                        + "exponenciales)", new Integral(g, x), cI.esCero() ? a : null, tabla,
                sigue.pasos().isEmpty() ? null : "La integral que queda, ∫v du", sigue.pasos());
        return Optional.of(new Lineal(a, cI, List.of(paso)));
    }

    /** x·eˣ − eˣ queda (x − 1)·eˣ: si todos los términos llevan el mismo factor no polinómico, sale. */
    static Expresion factorComun(Expresion e, char x) {
        ConCoeficiente fuera = separar(e);
        if (!fuera.coeficiente().equals(Racional.UNO) && fuera.resto() instanceof Suma) {
            Expresion dentro = factorComun(fuera.resto(), x);
            return dentro == fuera.resto() ? e : por(c(fuera.coeficiente()), dentro);
        }
        if (!(e instanceof Suma sm) || sm.terminos().size() < 2) {
            return e;
        }
        List<List<Expresion>> factores = new ArrayList<>();
        for (Expresion t : sm.terminos()) {
            ConCoeficiente cc = separar(t);
            List<Expresion> fs = cc.resto() instanceof Producto pr ? new ArrayList<>(pr.factores())
                    : new ArrayList<>(List.of(cc.resto()));
            factores.add(fs);
        }
        for (Expresion candidato : factores.getFirst()) {
            boolean exponencial = candidato instanceof Potencia pw && pw.base() instanceof ConstanteEspecial
                    || candidato instanceof Funcion fn && fn.tipo() == TipoFuncion.EXP;
            if (!exponencial || !factores.stream().allMatch(fs -> fs.contains(candidato))) {
                continue;
            }
            List<Expresion> restos = new ArrayList<>();
            for (Expresion t : sm.terminos()) {
                restos.add(s(new Cociente(t, candidato)));
            }
            Expresion dentro = s(Simplificador.suma(restos));
            if (Expresiones.contiene(dentro, candidato)) {
                return e;
            }
            return new Producto(List.of(dentro, candidato));
        }
        return e;
    }

    /** FormaFinal.ordenar, sin fallar si no puede. */
    private static final class FormaFinalSegura {
        static Expresion ordenar(Expresion e, char x) {
            try {
                return pe.mathnova.domain.calculo.derivadas.FormaFinal.ordenar(e, x);
            } catch (RuntimeException no) {
                return e;
            }
        }
    }
}
