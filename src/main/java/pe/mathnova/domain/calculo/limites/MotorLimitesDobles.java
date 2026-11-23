package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.FuncionRacional;
import pe.mathnova.domain.calculo.Polinomios;
import pe.mathnova.domain.calculo.Trigonometricas;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Camino;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Estado;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Polar;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Trayectoria;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.LimiteDoble;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;

/**
 * HU29 (RF54): lim((x, y) → (a, b), f) con los métodos en este orden.
 * <ol>
 *   <li>L01, evaluación directa: si f(a, b) es un número, ese es el límite.</li>
 *   <li>V05, trayectorias: y = b, x = a, y − b = m(x − a) y, si el numerador o el denominador tiene términos
 *       que se igualan con otra potencia, y − b = (x − a)ⁿ (o x − a = (y − b)ⁿ). Si dos dan valores distintos, o
 *       si la recta da un valor que depende de m, el límite no existe (RN38).</li>
 *   <li>V06, polares: x = a + r cos θ, y = b + r sen θ. Si f queda como r^k·g(θ) con k &gt; 0 y g acotada, el
 *       límite es 0 por el teorema del sándwich: |r^k·g(θ)| ≤ r^k·M → 0.</li>
 * </ol>
 * Nunca se declara que existe solo porque varias trayectorias coinciden (RN38): si las polares no deciden, se
 * dice que no se pudo decidir. Trayectorias y polares se hacen con f racional; lo demás solo se evalúa.
 */
public final class MotorLimitesDobles {

    public static final String DIRECTA = "L01";
    public static final String TRAYECTORIAS = "V05";
    public static final String POLARES = "V06";
    public static final String NO_DECIDIDO = "No se pudo decidir con los métodos disponibles";
    static final String RN38 = "que varias trayectorias den lo mismo no prueba que el límite exista (RN38)";

    private static final char R = 'r';

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static String n(Racional q) {
        return ImpresoraPizarra.numero(q);
    }

    public ResolucionLimiteDoble resolver(LimiteDoble l) {
        char x = l.x();
        char y = l.y();
        Expresion f = l.cuerpo();
        List<PasoLimite> pasos = new ArrayList<>();
        Set<Character> letras = Expresiones.variables(f);
        letras.remove(x);
        letras.remove(y);
        Racional a = racional(l.a());
        Racional b = racional(l.b());
        Optional<FuncionRacional> fr = letras.isEmpty() && a != null && b != null ? FuncionRacional.de(f)
                : Optional.empty();
        String punto = "(" + p(l.a()) + ", " + p(l.b()) + ")";
        String asignacion = x + " = " + p(l.a()) + ", " + y + " = " + p(l.b());
        Expresion sustituida = Expresiones.reemplazar(Expresiones.reemplazar(f, new Variable(x), l.a()),
                new Variable(y), l.b());

        if (fr.isEmpty()) {
            Optional<Expresion> v = evaluarDirecto(f, sustituida);
            if (v.isPresent()) {
                pasos.add(directa(f, sustituida, v.get(), asignacion));
                return new ResolucionLimiteDoble(l, Estado.EXISTE, v.get(), DIRECTA, List.of(), null, pasos, null);
            }
            String motivo = letras.isEmpty() ? "La evaluación directa no da un número y f no es una fracción de "
                    + "polinomios: las trayectorias y las polares necesitan una fracción de polinomios"
                    : "f tiene otras letras (" + String.join(", ", letras.stream().map(String::valueOf).toList())
                    + ") además de " + x + " y " + y;
            pasos.add(new PasoLimite(DIRECTA, "Evaluación directa", "Se reemplaza " + asignacion, f, null,
                    new TrabajoIntermedio.Agrupacion(List.of("f" + punto + " = " + p(sustituida), motivo))));
            return new ResolucionLimiteDoble(l, Estado.NO_DECIDIDO, null, null, List.of(), null, pasos,
                    NO_DECIDIDO + ": " + minuscula(motivo));
        }

        Polinomio num = fr.get().numerador();
        Polinomio den = fr.get().denominador();
        Map<Character, Racional> enPunto = Map.of(x, a, y, b);
        Racional nv = num.evaluar(enPunto);
        Racional dv = den.evaluar(enPunto);
        if (!dv.esCero()) {
            Expresion valor = new Constante(nv.dividir(dv));
            pasos.add(directa(f, sustituida, valor, asignacion));
            return new ResolucionLimiteDoble(l, Estado.EXISTE, valor, DIRECTA, List.of(), null, pasos, null);
        }
        if (!nv.esCero()) {
            String motivo = "Al reemplazar queda " + n(nv) + "/0: cerca de " + punto + " f crece sin cota, así que el "
                    + "límite no es un número";
            pasos.add(new PasoLimite(DIRECTA, "Evaluación directa", "Se reemplaza " + asignacion, f, null,
                    new TrabajoIntermedio.Agrupacion(List.of("f" + punto + " = " + n(nv) + "/0", motivo))));
            return new ResolucionLimiteDoble(l, Estado.NO_EXISTE, null, DIRECTA, List.of(), null, pasos, motivo);
        }
        pasos.add(new PasoLimite(DIRECTA, "Evaluación directa", "Se reemplaza " + asignacion + ": queda 0/0, una "
                + "indeterminación", f, null, new TrabajoIntermedio.Agrupacion(List.of("f" + punto + " = 0/0"))));

        // El punto se lleva al origen: P(x, y) = N(x + a, y + b).
        boolean origen = a.esCero() && b.esCero();
        Polinomio pn = trasladar(num, x, y, a, b);
        Polinomio pd = trasladar(den, x, y, a, b);

        List<Trayectoria> trayectorias = trayectorias(pn, pd, x, y, a, b, origen);
        List<String> lineas = new ArrayList<>();
        if (!origen) {
            lineas.add("Con t = " + x + " − " + n(a) + " (o " + y + " − " + n(b) + ") el punto queda en t = 0");
        }
        for (Trayectoria t : trayectorias) {
            lineas.add(t.nombre() + ": " + t.detalle());
        }
        String contradiccion = contradiccion(trayectorias);
        if (contradiccion != null) {
            lineas.add(contradiccion);
            pasos.add(new PasoLimite(TRAYECTORIAS, "Trayectorias", "Se acerca al punto por varios caminos: si dos "
                    + "dan valores distintos, el límite no existe", null, null,
                    new TrabajoIntermedio.Agrupacion(lineas)));
            return new ResolucionLimiteDoble(l, Estado.NO_EXISTE, null, TRAYECTORIAS, trayectorias, null, pasos,
                    contradiccion);
        }
        Expresion comun = trayectorias.stream().map(Trayectoria::valor).filter(v -> v != null).findFirst()
                .orElse(null);
        lineas.add(comun == null ? "Ninguna trayectoria está en el dominio de f"
                : "Todas dan " + p(comun) + ", pero " + RN38 + ": se prueba con polares");
        pasos.add(new PasoLimite(TRAYECTORIAS, "Trayectorias", "Se acerca al punto por varios caminos: si dos dan "
                + "valores distintos, el límite no existe", null, null, new TrabajoIntermedio.Agrupacion(lineas)));

        if (letrasPolares(x, y)) {
            Optional<Decision> d = polares(pn, pd, l, origen);
            if (d.isPresent()) {
                pasos.add(d.get().paso());
                Decision dd = d.get();
                return new ResolucionLimiteDoble(l, dd.estado(), dd.valor(), POLARES, trayectorias, dd.polar(), pasos,
                        dd.motivo());
            }
        }
        String motivo = NO_DECIDIDO + ": " + RN38 + ", y en polares f no queda como r^k·g(θ) con g acotada";
        return new ResolucionLimiteDoble(l, Estado.NO_DECIDIDO, null, null, trayectorias, null, pasos, motivo);
    }

    private static String minuscula(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean letrasPolares(char x, char y) {
        return x != R && y != R && x != Trigonometricas.THETA && y != Trigonometricas.THETA
                && !Trigonometricas.esAuxiliar(x) && !Trigonometricas.esAuxiliar(y);
    }

    // ================================================================== L01

    private static Racional racional(Expresion e) {
        try {
            Polinomio q = Polinomio.desde(e);
            return q.esConstante() ? q.constante() : null;
        } catch (RuntimeException no) {
            return null;
        }
    }

    private static PasoLimite directa(Expresion f, Expresion sustituida, Expresion valor, String asignacion) {
        String crudo = p(sustituida);
        String v = p(valor);
        return new PasoLimite(DIRECTA, "Evaluación directa", "Se reemplaza " + asignacion + ": f es continua en el "
                + "punto y da un número", f, valor, new TrabajoIntermedio.Agrupacion(List.of(
                        crudo.equals(v) ? v : crudo + " = " + v)));
    }

    /** Fuera de las fracciones de polinomios: el valor si el punto está en el dominio de f. */
    private static Optional<Expresion> evaluarDirecto(Expresion f, Expresion sustituida) {
        try {
            if (sustituida instanceof Cociente c) {
                Expresion d = Simplificador.simplificar(c.denominador());
                if (d.equals(Constante.de(0))) {
                    return Optional.empty();
                }
            }
            Expresion v = Simplificador.simplificar(sustituida);
            if (!Expresiones.variables(v).isEmpty()) {
                return Optional.empty();
            }
            ComplejoDecimal z = EvaluacionNumerica.evaluar(sustituida, Map.of(), EvaluacionNumerica.MC);
            return z.esReal(30) ? Optional.of(v) : Optional.empty();
        } catch (RuntimeException fuera) {
            return Optional.empty();
        }
    }

    // ================================================================== V05

    private static Polinomio trasladar(Polinomio q, char x, char y, Racional a, Racional b) {
        Polinomio r = Polinomios.sustituir(q, x, Polinomio.variable(x).sumar(Polinomio.constante(a)));
        return Polinomios.sustituir(r, y, Polinomio.variable(y).sumar(Polinomio.constante(b)));
    }

    /** Lo que da p(t)/q(t) cuando t → 0, con los coeficientes en m. */
    private record Limite1(boolean enDominio, Expresion valor, boolean dependeDeM, String cancelacion) {
    }

    private static Limite1 limite1(Polinomio pp, Polinomio qq, char t, char v) {
        if (qq.esCero()) {
            return new Limite1(false, null, false, null);
        }
        if (pp.esCero()) {
            return new Limite1(true, Constante.de(0), false, null);
        }
        TreeMap<Integer, Polinomio> np = Polinomios.enPotenciasDe(pp, t);
        TreeMap<Integer, Polinomio> nq = Polinomios.enPotenciasDe(qq, t);
        int op = np.firstKey();
        int oq = nq.firstKey();
        String potencia = v + superindice(Math.min(op, oq));
        if (op > oq) {
            return new Limite1(true, Constante.de(0), false, "el numerador tiene " + v + superindice(op)
                    + " y el denominador " + v + superindice(oq));
        }
        if (op < oq) {
            return new Limite1(true, null, false, "el denominador tiene " + v + superindice(oq) + " y el numerador "
                    + v + superindice(op));
        }
        Polinomio cp = np.get(op);
        Polinomio cq = nq.get(oq);
        Optional<Racional> k = proporcion(cp, cq);
        String cancela = op == 0 ? null : "se cancela " + potencia;
        if (k.isPresent()) {
            return new Limite1(true, new Constante(k.get()), false, cancela);
        }
        return new Limite1(true, cociente(cp, cq), true, cancela);
    }

    /** k con cp = k·cq, si existe. */
    private static Optional<Racional> proporcion(Polinomio cp, Polinomio cq) {
        Map.Entry<Monomio, Racional> lp = cp.terminos().entrySet().iterator().next();
        Racional cqm = cq.coeficiente(lp.getKey());
        if (cqm.esCero()) {
            return Optional.empty();
        }
        Racional k = lp.getValue().dividir(cqm);
        return cp.equals(cq.multiplicar(k)) ? Optional.of(k) : Optional.empty();
    }

    /** m/(1 + m²): el denominador con el término constante primero, como se escribe a mano. */
    private static Expresion cociente(Polinomio cp, Polinomio cq) {
        return Simplificador.simplificar(new Cociente(ascendente(cp), ascendente(cq)));
    }

    private static Expresion ascendente(Polinomio q) {
        List<Expresion> terminos = new ArrayList<>();
        q.terminos().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(t ->
                terminos.add(Polinomio.termino(t.getValue(), t.getKey()).aExpresion()));
        if (terminos.size() == 1) {
            return terminos.getFirst();
        }
        return new pe.mathnova.domain.expresion.Suma(terminos);
    }

    private static String superindice(int k) {
        if (k == 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(k).toCharArray()) {
            sb.append("⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(c - '0'));
        }
        return sb.toString();
    }

    private static char parametro(char x, char y) {
        for (char c : new char[] {'m', 'k', 'c', 'p'}) {
            if (c != x && c != y) {
                return c;
            }
        }
        return 'm';
    }

    private static List<Trayectoria> trayectorias(Polinomio pn, Polinomio pd, char x, char y, Racional a,
                                                  Racional b, boolean origen) {
        List<Trayectoria> r = new ArrayList<>();
        char m = parametro(x, y);
        String xa = origen || a.esCero() ? String.valueOf(x) : "(" + x + " − " + n(a) + ")";
        String yb = origen || b.esCero() ? String.valueOf(y) : y + " − " + n(b);
        // y = b
        agregar(r, y + " = " + n(b), Camino.HORIZONTAL, 1, (char) 0,
                Polinomios.sustituir(pn, y, Racional.CERO), Polinomios.sustituir(pd, y, Racional.CERO), x,
                origen ? "f(" + x + ", 0)" : "f(" + n(a) + " + t, " + n(b) + ")", origen ? x : 't');
        // x = a
        agregar(r, x + " = " + n(a), Camino.VERTICAL, 1, (char) 0,
                Polinomios.sustituir(pn, x, Racional.CERO), Polinomios.sustituir(pd, x, Racional.CERO), y,
                origen ? "f(0, " + y + ")" : "f(" + n(a) + ", " + n(b) + " + t)", origen ? y : 't');
        // y − b = m(x − a)
        Polinomio mx = Polinomio.variable(m).multiplicar(Polinomio.variable(x));
        agregar(r, yb + " = " + m + xa, Camino.RECTA, 1, m, Polinomios.sustituir(pn, y, mx),
                Polinomios.sustituir(pd, y, mx), x, origen ? "f(" + x + ", " + m + x + ")"
                        : "f(" + n(a) + " + t, " + n(b) + " + " + m + "t)", origen ? x : 't');
        // potencias que igualan grados
        for (int k : potencias(pn, pd, x, y)) {
            Polinomio xn = Polinomio.variable(x).potencia(k);
            agregar(r, yb + " = " + xa + superindice(k), Camino.POTENCIA_EN_X, k, (char) 0,
                    Polinomios.sustituir(pn, y, xn), Polinomios.sustituir(pd, y, xn), x,
                    origen ? "f(" + x + ", " + x + superindice(k) + ")" : "f(" + n(a) + " + t, " + n(b) + " + t"
                            + superindice(k) + ")", origen ? x : 't');
        }
        for (int k : potencias(pn, pd, y, x)) {
            Polinomio yn = Polinomio.variable(y).potencia(k);
            String xs = origen || a.esCero() ? String.valueOf(x) : x + " − " + n(a);
            String ys = origen || b.esCero() ? String.valueOf(y) : "(" + y + " − " + n(b) + ")";
            agregar(r, xs + " = " + ys + superindice(k), Camino.POTENCIA_EN_Y, k, (char) 0,
                    Polinomios.sustituir(pn, x, yn), Polinomios.sustituir(pd, x, yn), y,
                    origen ? "f(" + y + superindice(k) + ", " + y + ")" : "f(" + n(a) + " + t" + superindice(k) + ", "
                            + n(b) + " + t)", origen ? y : 't');
        }
        return r;
    }

    /**
     * Los n ≥ 2 con que y = xⁿ iguala el grado de dos términos del numerador o del denominador: en x⁴ + y², con
     * y = x² los dos quedan de grado 4.
     */
    private static List<Integer> potencias(Polinomio pn, Polinomio pd, char x, char y) {
        LinkedHashSet<Integer> r = new LinkedHashSet<>();
        for (Polinomio q : List.of(pd, pn)) {
            List<Monomio> ms = new ArrayList<>(q.terminos().keySet());
            for (int i = 0; i < ms.size(); i++) {
                for (int j = i + 1; j < ms.size(); j++) {
                    int a1 = ms.get(i).exponente(x);
                    int b1 = ms.get(i).exponente(y);
                    int a2 = ms.get(j).exponente(x);
                    int b2 = ms.get(j).exponente(y);
                    if (b1 != b2 && (a1 - a2) % (b2 - b1) == 0) {
                        int k = (a1 - a2) / (b2 - b1);
                        if (k >= 2 && k <= 6) {
                            r.add(k);
                        }
                    }
                }
            }
        }
        return List.copyOf(r);
    }

    private static void agregar(List<Trayectoria> r, String nombre, Camino camino, int k, char m, Polinomio pp,
                                Polinomio qq, char t, String llamada, char mostrar) {
        Limite1 l = limite1(pp, qq, t, mostrar);
        if (!l.enDominio()) {
            return;
        }
        Expresion fraccion = new Cociente(pp.aExpresion(), qq.aExpresion());
        if (mostrar != t) {
            fraccion = Expresiones.reemplazar(fraccion, new Variable(t), new Variable(mostrar));
        }
        String cuenta = llamada + " = " + p(fraccion);
        String detalle;
        if (l.valor() == null) {
            detalle = cuenta + ": " + l.cancelacion() + ", así que crece sin cota";
        } else {
            String flecha = l.dependeDeM() || l.cancelacion() != null && l.cancelacion().startsWith("se cancela")
                    ? " = " : " → ";
            detalle = cuenta + flecha + p(l.valor()) + (l.cancelacion() == null ? ""
                    : " (" + l.cancelacion() + ")");
        }
        r.add(new Trayectoria(nombre, camino, k, m, l.valor(), l.dependeDeM(), detalle));
    }

    /** El primer motivo por el que el límite no existe, o null si todas coinciden. */
    private static String contradiccion(List<Trayectoria> ts) {
        Trayectoria primera = null;
        for (Trayectoria t : ts) {
            if (t.valor() == null) {
                return "Por " + t.nombre() + " f crece sin cota: el límite no existe";
            }
            if (t.dependeDeM()) {
                return "Con " + t.nombre() + " da " + p(t.valor()) + ", que depende de " + t.parametro()
                        + ": cada recta da un valor distinto, así que el límite no existe";
            }
            if (primera == null) {
                primera = t;
            } else if (!iguales(primera.valor(), t.valor())) {
                return "Por " + primera.nombre() + " da " + p(primera.valor()) + " y por " + t.nombre() + " da "
                        + p(t.valor()) + ": dos trayectorias dan valores distintos, así que el límite no existe";
            }
        }
        return null;
    }

    private static boolean iguales(Expresion u, Expresion v) {
        if (u instanceof Constante cu && v instanceof Constante cv) {
            return cu.valor().equals(cv.valor());
        }
        return Simplificador.simplificar(Simplificador.suma(List.of(u, Simplificador.negar(v))))
                .equals(Constante.de(0));
    }

    // ================================================================== V06

    private record Decision(Estado estado, Expresion valor, Polar polar, PasoLimite paso, String motivo) {
    }

    private static Polinomio enAngulo(Polinomio q, char x, char y) {
        Polinomio r = Polinomios.sustituir(q, x, Polinomio.variable(Trigonometricas.COS_T));
        return Polinomios.sustituir(r, y, Polinomio.variable(Trigonometricas.SEN_T));
    }

    /**
     * Una cota inferior m &gt; 0 de D(cos θ, sen θ): exacta si D queda constante con sen² + cos² = 1, o
     * min(a, b)·2^(1 − d/2) si D = a·x^d + b·y^d + (términos con exponentes pares y coeficientes positivos).
     */
    private static Optional<Racional> cotaInferior(Polinomio dAng, Polinomio d, int grado, char x, char y) {
        if (dAng.esConstante() && dAng.constante().signo() > 0) {
            return Optional.of(dAng.constante());
        }
        if (grado % 2 != 0) {
            return Optional.empty();
        }
        for (Map.Entry<Monomio, Racional> t : d.terminos().entrySet()) {
            if (t.getValue().signo() <= 0 || t.getKey().exponente(x) % 2 != 0 || t.getKey().exponente(y) % 2 != 0) {
                return Optional.empty();
            }
        }
        Racional ax = d.coeficiente(Monomio.de(x, grado));
        Racional by = d.coeficiente(Monomio.de(y, grado));
        if (ax.esCero() || by.esCero()) {
            return Optional.empty();
        }
        Racional min = ax.compareTo(by) <= 0 ? ax : by;
        return Optional.of(min.dividir(Racional.de(2).potencia(grado / 2 - 1)));
    }

    private static Optional<Decision> polares(Polinomio pn, Polinomio pd, LimiteDoble l, boolean origen) {
        char x = l.x();
        char y = l.y();
        TreeMap<Integer, Polinomio> hd = Polinomios.homogeneas(pd);
        if (hd.size() != 1) {
            return Optional.empty();
        }
        int d = hd.firstKey();
        Polinomio dAng = Trigonometricas.reducir(enAngulo(pd, x, y));
        Optional<Racional> m = cotaInferior(dAng, pd, d, x, y);
        if (m.isEmpty()) {
            return Optional.empty();
        }
        TreeMap<Integer, Polinomio> hn = Polinomios.homogeneas(pn);
        int kmin = hn.firstKey();
        String sx = origen ? "" : p(l.a()) + " + ";
        String sy = origen ? "" : p(l.b()) + " + ";
        String cambio = x + " = " + sx + "r cos θ, " + y + " = " + sy + "r sen θ";

        // f = Σ r^(k − d)·Nₖ(θ) / D(θ)
        Polinomio arriba = Polinomio.CERO;
        Racional sumaM = Racional.CERO;
        for (Map.Entry<Integer, Polinomio> e : hn.entrySet()) {
            Polinomio nk = Trigonometricas.reducir(enAngulo(e.getValue(), x, y));
            int pot = e.getKey() - d;
            if (pot >= 0) {
                nk = nk.multiplicar(Polinomio.termino(Racional.UNO, Monomio.de(R, pot)));
            }
            arriba = arriba.sumar(nk);
            sumaM = sumaM.sumar(Polinomios.sumaDeAbsolutos(e.getValue()));
        }
        boolean constante = dAng.esConstante();
        Expresion forma = constante ? Trigonometricas.aExpresion(arriba.multiplicar(dAng.constante().inverso()))
                : Simplificador.simplificar(new Cociente(Trigonometricas.aExpresion(arriba),
                        Trigonometricas.aExpresion(dAng)));
        Expresion denTexto = Trigonometricas.aExpresion(enAngulo(pd, x, y));
        List<String> lineas = new ArrayList<>();
        lineas.add(cambio);
        lineas.add("El denominador es r" + superindice(d) + "·(" + p(denTexto) + ")" + (constante
                ? " = " + (dAng.constante().equals(Racional.UNO) ? "r" + superindice(d)
                : n(dAng.constante()) + "r" + superindice(d)) + ", porque sen²θ + cos²θ = 1" : ""));
        lineas.add("f = " + p(forma));

        if (kmin - d > 0) {
            int j = kmin - d;
            Racional cota = sumaM.dividir(m.get());
            String rj = "r" + superindice(j);
            String cotaTexto;
            Expresion g = null;
            if (hn.size() == 1) {
                g = constante ? Trigonometricas.aExpresion(Trigonometricas.reducir(enAngulo(pn, x, y))
                        .multiplicar(dAng.constante().inverso())) : null;
                String gTexto = g == null ? "g(θ)" : p(g);
                cotaTexto = "|" + p(forma) + "| ≤ " + rj + "·" + n(cota) + (cota.equals(Racional.UNO) ? " = " + rj : "")
                        + " → 0";
                lineas.add("|" + gTexto + "| ≤ " + n(cota) + (constante ? " porque |cos θ| ≤ 1 y |sen θ| ≤ 1"
                        : " porque el denominador es al menos " + n(m.get())));
            } else {
                cotaTexto = "|f| ≤ " + n(cota) + "·" + rj + " si r ≤ 1, y " + n(cota) + "·" + rj + " → 0";
                lineas.add("Cada término tiene una potencia de r de exponente ≥ " + j + " por una función acotada de θ");
            }
            lineas.add(cotaTexto + " cuando r → 0");
            lineas.add("Por el teorema del sándwich el límite es 0");
            Polar polar = new Polar(forma, j, g, cotaTexto);
            PasoLimite paso = new PasoLimite(POLARES, "Coordenadas polares", "Se escribe f en polares alrededor del "
                    + "punto: queda r" + superindice(j) + " por una función acotada de θ, que tiende a 0 sin importar θ",
                    null, Constante.de(0), new TrabajoIntermedio.Agrupacion(lineas));
            return Optional.of(new Decision(Estado.EXISTE, Constante.de(0), polar, paso, null));
        }
        if (kmin == d && hn.size() == 1) {
            Polinomio nAng = Trigonometricas.reducir(enAngulo(pn, x, y));
            Optional<Racional> k = nAng.esCero() ? Optional.of(Racional.CERO) : proporcion(nAng, dAng);
            if (k.isPresent()) {
                Expresion valor = new Constante(k.get());
                lineas.add("No depende de r ni de θ: el límite es " + p(valor));
                PasoLimite paso = new PasoLimite(POLARES, "Coordenadas polares", "En polares f queda constante",
                        null, valor, new TrabajoIntermedio.Agrupacion(lineas));
                return Optional.of(new Decision(Estado.EXISTE, valor, new Polar(forma, 0, forma, null), paso, null));
            }
            String motivo = "En polares f = " + p(forma) + " no depende de r pero sí de θ: cada dirección da un valor "
                    + "distinto, así que el límite no existe";
            lineas.add(motivo);
            PasoLimite paso = new PasoLimite(POLARES, "Coordenadas polares", "En polares f solo depende de θ", null,
                    null, new TrabajoIntermedio.Agrupacion(lineas));
            return Optional.of(new Decision(Estado.NO_EXISTE, null, new Polar(forma, 0, forma, null), paso, motivo));
        }
        return Optional.empty();
    }
}
