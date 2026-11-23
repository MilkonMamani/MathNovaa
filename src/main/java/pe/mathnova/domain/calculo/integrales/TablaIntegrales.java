package pe.mathnova.domain.calculo.integrales;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

/**
 * I01: la tabla de integrales básicas, término a término. Cada término es una constante por una entrada
 * de la tabla (xⁿ, 1/x, eˣ, aˣ, sen x, cos x, sec² x, csc² x, 1/(1 + x²), 1/√(1 − x²)); si alguno no lo es,
 * la integral no es inmediata.
 */
final class TablaIntegrales {

    /** Una fila de la tabla, con la variable que se use (x o u). */
    enum Fila {
        CONSTANTE("∫k dx = k·x + C"),
        POTENCIA("∫xⁿ dx = xⁿ⁺¹/(n + 1) + C, con n ≠ −1"),
        LOGARITMO("∫1/x dx = ln|x| + C"),
        EXPONENCIAL("∫eˣ dx = eˣ + C"),
        EXPONENCIAL_A("∫aˣ dx = aˣ/ln a + C"),
        SENO("∫sen x dx = −cos x + C"),
        COSENO("∫cos x dx = sen x + C"),
        SECANTE("∫sec² x dx = tan x + C"),
        COSECANTE("∫csc² x dx = −cot x + C"),
        ARCOTANGENTE("∫1/(1 + x²) dx = arctan x + C"),
        ARCOSENO("∫1/√(1 − x²) dx = arcsen x + C");

        private final String formula;

        Fila(String formula) {
            this.formula = formula;
        }

        /** La fórmula con la variable v: "∫uⁿ du = uⁿ⁺¹/(n + 1) + C, con n ≠ −1". */
        String formula(char v) {
            if (v == 'x') {
                return formula;
            }
            String sup = switch (v) {
                case 'u' -> "ᵘ";
                case 't' -> "ᵗ";
                case 'y' -> "ʸ";
                default -> "^" + v;
            };
            return formula.replace("dx", "d" + v).replace("ˣ", sup).replace('x', v);
        }
    }

    /**
     * La integral de un término y la fila usada.
     *
     * @param integral sin la constante C
     */
    record Termino(Expresion termino, Expresion integral, Fila fila) {
    }

    /** La integral término a término de f, o vacío si algún término no está en la tabla. */
    static Optional<List<Termino>> integrar(Expresion f, char x) {
        Expresion s = Simplificador.simplificar(f);
        List<Expresion> terminos = s instanceof Suma suma ? suma.terminos() : List.of(s);
        List<Termino> r = new ArrayList<>();
        for (Expresion t : terminos) {
            Optional<Termino> i = termino(t, x);
            if (i.isEmpty()) {
                return Optional.empty();
            }
            r.add(i.get());
        }
        return Optional.of(r);
    }

    static Expresion suma(List<Termino> terminos) {
        return Simplificador.suma(terminos.stream().map(Termino::integral).toList());
    }

    /** Las filas usadas, sin repetir y en el orden de la tabla. */
    static List<String> formulas(List<Termino> terminos, char x) {
        Set<Fila> filas = new LinkedHashSet<>();
        for (Fila f : Fila.values()) {
            if (terminos.stream().anyMatch(t -> t.fila() == f)) {
                filas.add(f);
            }
        }
        return filas.stream().map(f -> f.formula(x)).toList();
    }

    // ------------------------------------------------------------------ un término

    /** coeficiente (sin x) × resto. */
    private record Separado(Expresion coeficiente, Expresion resto) {
    }

    private static Separado separar(Expresion t, char x) {
        if (!depende(t, x)) {
            return new Separado(t, Constante.de(1));
        }
        return switch (t) {
            case Negacion n -> {
                Separado a = separar(n.argumento(), x);
                yield new Separado(Simplificador.negar(a.coeficiente()), a.resto());
            }
            case Producto p -> {
                List<Expresion> c = new ArrayList<>();
                List<Expresion> r = new ArrayList<>();
                for (Expresion f : p.factores()) {
                    Separado s = separar(f, x);
                    c.add(s.coeficiente());
                    r.add(s.resto());
                }
                yield new Separado(Simplificador.producto(c), Simplificador.producto(r));
            }
            case Cociente q -> {
                Separado a = separar(q.numerador(), x);
                Separado b = separar(q.denominador(), x);
                yield new Separado(Simplificador.cociente(a.coeficiente(), b.coeficiente()),
                        Simplificador.cociente(a.resto(), b.resto()));
            }
            default -> new Separado(Constante.de(1), t);
        };
    }

    private static boolean depende(Expresion e, char x) {
        return Expresiones.variables(e).contains(x);
    }

    private static boolean esX(Expresion e, char x) {
        return e instanceof Variable v && v.nombre() == x;
    }

    static Optional<Termino> termino(Expresion t, char x) {
        Separado s = separar(Simplificador.simplificar(t), x);
        Optional<Base> b = base(s.resto(), x);
        return b.map(base -> new Termino(t, Simplificador.simplificar(
                Simplificador.producto(List.of(s.coeficiente(), base.integral()))), base.fila()));
    }

    private record Base(Expresion integral, Fila fila) {
    }

    private static Optional<Base> base(Expresion g, char x) {
        Variable v = new Variable(x);
        Constante uno = Constante.de(1);
        if (g.equals(uno)) {
            return Optional.of(new Base(v, Fila.CONSTANTE));
        }
        Racional n = exponente(g, x);
        if (n != null) {
            if (n.equals(Racional.UNO.negar())) {
                return Optional.of(new Base(ln(v), Fila.LOGARITMO));
            }
            Racional m = n.sumar(Racional.UNO);
            return Optional.of(new Base(Simplificador.cociente(Simplificador.potencia(v, new Constante(m)),
                    new Constante(m)), Fila.POTENCIA));
        }
        if (g instanceof Potencia p && esX(p.exponente(), x) && !depende(p.base(), x)) {
            if (p.base() instanceof ConstanteEspecial e && e.tipo() == ConstanteEspecial.Tipo.E) {
                return Optional.of(new Base(g, Fila.EXPONENCIAL));
            }
            return Optional.of(new Base(Simplificador.cociente(g, new Funcion(TipoFuncion.LN, p.base())),
                    Fila.EXPONENCIAL_A));
        }
        if (g instanceof Funcion f && esX(f.argumento(), x)) {
            switch (f.tipo()) {
                case EXP -> {
                    return Optional.of(new Base(g, Fila.EXPONENCIAL));
                }
                case SEN -> {
                    return Optional.of(new Base(Simplificador.negar(new Funcion(TipoFuncion.COS, v)), Fila.SENO));
                }
                case COS -> {
                    return Optional.of(new Base(new Funcion(TipoFuncion.SEN, v), Fila.COSENO));
                }
                default -> {
                }
            }
        }
        if (g instanceof Potencia p && p.exponente().equals(Constante.de(2)) && p.base() instanceof Funcion f
                && esX(f.argumento(), x)) {
            if (f.tipo() == TipoFuncion.SEC) {
                return Optional.of(new Base(new Funcion(TipoFuncion.TAN, v), Fila.SECANTE));
            }
            if (f.tipo() == TipoFuncion.CSC) {
                return Optional.of(new Base(Simplificador.negar(new Funcion(TipoFuncion.COT, v)), Fila.COSECANTE));
            }
        }
        if (g instanceof Cociente q && q.numerador().equals(uno)) {
            Polinomio unoMasX2 = Polinomio.constante(Racional.UNO).sumar(Polinomio.variable(x).potencia(2));
            if (esPolinomio(q.denominador(), unoMasX2)) {
                return Optional.of(new Base(new Funcion(TipoFuncion.ARCTAN, v), Fila.ARCOTANGENTE));
            }
            Polinomio unoMenosX2 = Polinomio.constante(Racional.UNO).restar(Polinomio.variable(x).potencia(2));
            if (q.denominador() instanceof Raiz r && r.indice() == 2 && esPolinomio(r.radicando(), unoMenosX2)
                    || q.denominador() instanceof Funcion f && f.tipo() == TipoFuncion.SQRT
                    && esPolinomio(f.argumento(), unoMenosX2)) {
                return Optional.of(new Base(new Funcion(TipoFuncion.ARCSEN, v), Fila.ARCOSENO));
            }
        }
        return Optional.empty();
    }

    /** n si g es xⁿ (también 1/xⁿ, √x, 1/√x); null si no. */
    private static Racional exponente(Expresion g, char x) {
        if (esX(g, x)) {
            return Racional.UNO;
        }
        if (g instanceof Potencia p && esX(p.base(), x) && p.exponente() instanceof Constante k) {
            return k.valor();
        }
        if (g instanceof Raiz r && esX(r.radicando(), x)) {
            return Racional.de(1, r.indice());
        }
        if (g instanceof Funcion f && f.tipo() == TipoFuncion.SQRT && esX(f.argumento(), x)) {
            return Racional.de(1, 2);
        }
        if (g instanceof Cociente q && q.numerador().equals(Constante.de(1))) {
            Racional n = exponente(q.denominador(), x);
            return n == null ? null : n.negar();
        }
        return null;
    }

    private static boolean esPolinomio(Expresion e, Polinomio p) {
        try {
            return Polinomio.desde(e).equals(p);
        } catch (RuntimeException noEs) {
            return false;
        }
    }

    /** Quita |u| donde u es siempre positivo: ln|x² + 1| queda ln(x² + 1) (RN23). */
    static Expresion sinAbsolutoInnecesario(Expresion e) {
        return switch (e) {
            case Funcion f when f.tipo() == TipoFuncion.ABS && siemprePositivo(f.argumento()) ->
                    sinAbsolutoInnecesario(f.argumento());
            case Funcion f -> new Funcion(f.tipo(), sinAbsolutoInnecesario(f.argumento()), f.base());
            case Suma s -> new Suma(s.terminos().stream().map(TablaIntegrales::sinAbsolutoInnecesario).toList());
            case Producto p -> new Producto(p.factores().stream().map(TablaIntegrales::sinAbsolutoInnecesario).toList());
            case Cociente c -> new Cociente(sinAbsolutoInnecesario(c.numerador()), sinAbsolutoInnecesario(c.denominador()));
            case Negacion n -> new Negacion(sinAbsolutoInnecesario(n.argumento()));
            case Potencia p -> new Potencia(sinAbsolutoInnecesario(p.base()), sinAbsolutoInnecesario(p.exponente()));
            default -> e;
        };
    }

    /** ln|u|, o ln u si u es siempre positivo (RN23). */
    static Expresion ln(Expresion u) {
        return new Funcion(TipoFuncion.LN, siemprePositivo(u) ? u : new Funcion(TipoFuncion.ABS, u));
    }

    /**
     * Constantes positivas, eᵘ, y polinomios que no se anulan y son positivos: los de grado 2 con
     * discriminante negativo y a > 0, o los que solo tienen exponentes pares y coeficientes positivos.
     */
    static boolean siemprePositivo(Expresion u) {
        if (u instanceof Constante c) {
            return c.valor().signo() > 0;
        }
        if (u instanceof Potencia p && p.base() instanceof ConstanteEspecial) {
            return true;
        }
        try {
            Polinomio p = Polinomio.desde(u);
            if (p.variables().size() != 1 || p.esConstante()) {
                return false;
            }
            char x = p.variables().first();
            boolean pares = p.terminos().entrySet().stream()
                    .allMatch(e -> e.getKey().exponente(x) % 2 == 0 && e.getValue().signo() > 0);
            if (pares && p.constante().signo() > 0) {
                return true;
            }
            if (p.gradoEn(x) == 2) {
                Racional a = p.coeficiente(pe.mathnova.domain.algebra.Monomio.de(x, 2));
                Racional b = p.coeficiente(pe.mathnova.domain.algebra.Monomio.de(x, 1));
                Racional c = p.constante();
                return a.signo() > 0 && b.multiplicar(b).restar(Racional.de(4).multiplicar(a).multiplicar(c)).signo() < 0;
            }
            return false;
        } catch (RuntimeException noEs) {
            return false;
        }
    }
}
