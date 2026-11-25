package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.expresion.Visitante;

import static pe.mathnova.domain.calculo.derivadas.Simplificador.cociente;
import static pe.mathnova.domain.calculo.derivadas.Simplificador.negar;
import static pe.mathnova.domain.calculo.derivadas.Simplificador.potencia;
import static pe.mathnova.domain.calculo.derivadas.Simplificador.producto;
import static pe.mathnova.domain.calculo.derivadas.Simplificador.suma;

public final class Derivador implements Visitante<Expresion> {

    public record Resultado(Expresion derivada, List<PasoDerivada> pasos, List<PasoDerivada> arbol) {
        public Resultado {
            Objects.requireNonNull(derivada, "derivada");
            pasos = List.copyOf(pasos);
            arbol = List.copyOf(arbol);
        }
    }

    private static final Constante UNO = Constante.de(1);
    private static final Constante DOS = Constante.de(2);

    private final char variable;
    private final char funcion;
    private final List<PasoDerivada> pasos = new ArrayList<>();
    private final List<PasoDerivada> arbol = new ArrayList<>();
    private final Deque<List<PasoDerivada>> pila = new ArrayDeque<>();

    private Derivador(char variable, char funcion) {
        this.variable = variable;
        this.funcion = funcion;
        pila.push(arbol);
    }

    public static Resultado derivar(Expresion e, char variable) {
        return derivar(e, variable, (char) 0);
    }

    public static Resultado derivarImplicita(Expresion e, char variable, char funcion) {
        return derivar(e, variable, funcion);
    }

    private static Resultado derivar(Expresion e, char variable, char funcion) {
        Derivador d = new Derivador(variable, funcion);
        Expresion r = Simplificador.simplificar(d.d(e));
        return new Resultado(r, d.pasos, d.arbol);
    }

    public static Expresion prima(char funcion, char variable) {
        return new Derivada(1, variable, new Variable(funcion));
    }

    private boolean depende(Expresion e) {
        var letras = Expresiones.variables(e);
        return letras.contains(variable) || funcion != 0 && letras.contains(funcion);
    }

    private boolean esLaVariable(Expresion e) {
        return e instanceof Variable v && v.nombre() == variable;
    }

    private Expresion d(Expresion e) {
        if (!depende(e)) {
            return registrar("D01", "La derivada de una constante es 0" + constanteLiteral(e), "(c)' = 0",
                    e, t -> Constante.de(0));
        }
        return e.aceptar(this);
    }

    private String constanteLiteral(Expresion e) {
        return Expresiones.variables(e).isEmpty() ? ""
                : " (" + String.join(", ", Expresiones.variables(e).stream().map(String::valueOf).toList())
                        + " es constante respecto de " + variable + ")";
    }

    private Expresion interna(Expresion u) {
        return esLaVariable(u) ? UNO : d(u);
    }

    private Expresion registrar(String codigo, String regla, String formula, Expresion antes,
                                Function<List<String>, Expresion> calculo) {
        int i = pasos.size();
        pasos.add(null);
        List<PasoDerivada> hijos = new ArrayList<>();
        List<String> trabajo = new ArrayList<>();
        pila.push(hijos);
        Expresion r;
        try {
            Expresion aplicada = calculo.apply(trabajo);
            r = Simplificador.simplificar(aplicada);
            if (APLICADA.contains(codigo) && !pizarra(aplicada).equals(pizarra(r))) {
                trabajo.add("(" + pizarra(antes) + ")' = " + pizarra(aplicada));
            }
        } finally {
            pila.pop();
        }
        PasoDerivada p = new PasoDerivada(codigo, regla, antes, r, formula, trabajo, hijos);
        pasos.set(i, p);
        Objects.requireNonNull(pila.peek()).add(p);
        return r;
    }

    private static final java.util.Set<String> APLICADA = java.util.Set.of("D04", "D05", "D06", "D07");

    private static String texto(Expresion e) {
        return ImpresoraTexto.imprimir(e);
    }

    private static String pizarra(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private String cadena(String codigoSinCadena, Expresion u) {
        return esLaVariable(u) ? codigoSinCadena : "D06";
    }

    private String conU(Expresion u) {
        return esLaVariable(u) ? "" : ", con u = " + texto(u);
    }

    private Expresion internaConTrabajo(Expresion u, List<String> trabajo) {
        if (esLaVariable(u)) {
            return UNO;
        }
        trabajo.add("u = " + pizarra(u));
        Expresion du = d(u);
        trabajo.add("u' = " + pizarra(du));
        return du;
    }

    @Override
    public Expresion visitar(Constante c) {
        return Constante.de(0);
    }

    @Override
    public Expresion visitar(Variable v) {
        if (funcion != 0 && v.nombre() == funcion) {
            return registrar("D08", v.nombre() + " es función de " + variable + ": su derivada es " + v.nombre() + "'",
                    "(" + v.nombre() + ")' = " + v.nombre() + "'", v, t -> prima(funcion, variable));
        }
        return registrar("D02", "(" + variable + ")' = 1", "(" + variable + ")' = 1", v, t -> UNO);
    }

    @Override
    public Expresion visitar(ConstanteEspecial c) {
        return Constante.de(0);
    }

    @Override
    public Expresion visitar(Suma s) {
        return registrar("D03", "(f + g)' = f' + g': se deriva término a término", "(u + v)' = u' + v'", s, t -> {
            List<Expresion> r = new ArrayList<>();
            for (Expresion termino : s.terminos()) {
                Expresion dt = d(termino);
                t.add("(" + pizarra(termino) + ")' = " + pizarra(dt));
                r.add(dt);
            }
            return suma(r);
        });
    }

    @Override
    public Expresion visitar(Negacion n) {
        return negar(d(n.argumento()));
    }

    @Override
    public Expresion visitar(Producto p) {
        List<Expresion> constantes = p.factores().stream().filter(f -> !depende(f)).toList();
        List<Expresion> variables = p.factores().stream().filter(this::depende).toList();
        if (variables.size() == 1) {
            Expresion c = constantes.size() == 1 ? constantes.getFirst() : new Producto(constantes);
            return registrar("D04", "(c·f)' = c·f': la constante " + texto(c) + " queda como factor"
                    + constanteLiteral(c), "(c·u)' = c·u'", p, t -> {
                t.add("c = " + pizarra(c) + ", u = " + pizarra(variables.getFirst()));
                Expresion du = d(variables.getFirst());
                t.add("u' = " + pizarra(du));
                List<Expresion> r = new ArrayList<>(constantes);
                r.add(du);
                return producto(r);
            });
        }
        boolean dos = variables.size() == 2;
        String formula = dos ? "(u·v)' = u'·v + u·v'" : "(u·v·w)' = u'·v·w + u·v'·w + u·v·w'";
        return registrar("D04", "(f·g)' = f'·g + f·g'", formula, p, t -> {
            List<String> nombres = List.of("u", "v", "w", "z");
            List<String> asignacion = new ArrayList<>();
            for (int i = 0; i < variables.size(); i++) {
                asignacion.add(letra(nombres, i) + " = " + pizarra(variables.get(i)));
            }
            t.add(String.join(", ", asignacion));
            List<Expresion> derivadas = new ArrayList<>();
            List<String> primas = new ArrayList<>();
            for (int i = 0; i < variables.size(); i++) {
                Expresion di = d(variables.get(i));
                derivadas.add(di);
                primas.add(letra(nombres, i) + "' = " + pizarra(di));
            }
            t.add(String.join(", ", primas));
            List<Expresion> sumandos = new ArrayList<>();
            for (int i = 0; i < variables.size(); i++) {
                List<Expresion> r = new ArrayList<>(constantes);
                for (int j = 0; j < variables.size(); j++) {
                    r.add(i == j ? derivadas.get(j) : variables.get(j));
                }
                sumandos.add(producto(r));
            }
            return suma(sumandos);
        });
    }

    private static String letra(List<String> nombres, int i) {
        return i < nombres.size() ? nombres.get(i) : "u" + (i + 1);
    }

    @Override
    public Expresion visitar(Cociente c) {
        Expresion f = c.numerador();
        Expresion g = c.denominador();
        if (!depende(g)) {
            return registrar("D05", "(f/c)' = f'/c: el denominador " + texto(g) + " es constante", "(u/c)' = u'/c", c,
                    t -> {
                        t.add("u = " + pizarra(f) + ", c = " + pizarra(g));
                        Expresion df = d(f);
                        t.add("u' = " + pizarra(df));
                        return cociente(df, g);
                    });
        }
        if (!depende(f)) {
            return registrar("D05", "(k/g)' = −k·g'/g²", "(k/v)' = −k·v'/v²", c, t -> {
                t.add("k = " + pizarra(f) + ", v = " + pizarra(g));
                Expresion dg = d(g);
                t.add("v' = " + pizarra(dg));
                return negar(cociente(producto(List.of(f, dg)), potencia(g, DOS)));
            });
        }
        return registrar("D05", "(f/g)' = (f'·g − f·g')/g²", "(u/v)' = (u'·v − u·v')/v²", c, t -> {
            t.add("u = " + pizarra(f) + ", v = " + pizarra(g));
            Expresion df = d(f);
            Expresion dg = d(g);
            t.add("u' = " + pizarra(df) + ", v' = " + pizarra(dg));
            return cociente(suma(List.of(producto(List.of(df, g)), negar(producto(List.of(f, dg))))),
                    potencia(g, DOS));
        });
    }

    @Override
    public Expresion visitar(Potencia p) {
        Expresion b = p.base();
        Expresion n = p.exponente();
        if (!depende(n)) {
            boolean simple = esLaVariable(b);
            String formula = simple ? "(xⁿ)' = n·xⁿ⁻¹" : "(uⁿ)' = n·uⁿ⁻¹·u'";
            return registrar(cadena("D02", b), "(uⁿ)' = n·uⁿ⁻¹·u'" + conU(b) + " y n = " + texto(n), formula, p, t -> {
                t.add("n = " + pizarra(n));
                Expresion du = internaConTrabajo(b, t);
                return new Producto(List.of(n, new Potencia(b, Simplificador.simplificar(
                        suma(List.of(n, Constante.de(-1))))), du));
            });
        }
        if (!depende(b)) {
            boolean euler = b instanceof ConstanteEspecial e && e.tipo() == ConstanteEspecial.Tipo.E;
            String regla = euler ? "(eᵘ)' = eᵘ·u'" : "(aᵘ)' = aᵘ·ln a·u' con a = " + texto(b);
            String formula = esLaVariable(n) ? (euler ? "(eˣ)' = eˣ" : "(aˣ)' = aˣ·ln a")
                    : (euler ? "(eᵘ)' = eᵘ·u'" : "(aᵘ)' = aᵘ·ln a·u'");
            return registrar(cadena("D07", n), regla + conU(n), formula, p, t -> {
                if (!euler) {
                    t.add("a = " + pizarra(b));
                }
                Expresion du = internaConTrabajo(n, t);
                return euler ? producto(List.of(p, du)) : producto(List.of(p, new Funcion(TipoFuncion.LN, b), du));
            });
        }
        Expresion lnY = producto(List.of(n, new Funcion(TipoFuncion.LN, b)));
        return registrar("D09", "Base y exponente dependen de " + variable
                        + ": f^g = e^(g·ln f), así que (f^g)' = f^g·(g'·ln f + g·f'/f)",
                "y = f^g → ln y = g·ln f → y'/y = (g·ln f)' → y' = f^g·(g·ln f)'", p, t -> {
                    t.add("Tomar ln: ln y = " + pizarra(lnY));
                    Expresion dLn = d(lnY);
                    t.add("Derivar: y'/y = " + pizarra(dLn));
                    Expresion r = producto(List.of(p, dLn));
                    t.add("Despejar: y' = y·(" + pizarra(dLn) + ") = " + pizarra(Simplificador.simplificar(r)));
                    return r;
                });
    }

    @Override
    public Expresion visitar(Raiz r) {
        int k = r.indice();
        Expresion u = r.radicando();
        String regla = k == 2 ? "(√u)' = u'/(2√u)" : "(ⁿ√u)' = u'/(n·ⁿ√uⁿ⁻¹) con n = " + k;
        String formula = k == 2 ? "(√u)' = u'/(2√u)" : "(ⁿ√u)' = u'/(n·ⁿ√uⁿ⁻¹)";
        return registrar(cadena("D02", u), regla + conU(u), formula, r, t -> {
            Expresion du = internaConTrabajo(u, t);
            Expresion raiz = k == 2 ? r : new Raiz(k, potencia(u, Constante.de(k - 1)));
            return cociente(du, producto(List.of(Constante.de(k), raiz)));
        });
    }

    @Override
    public Expresion visitar(Funcion f) {
        Expresion u = f.argumento();
        boolean lnAbs = f.tipo() == TipoFuncion.LN && u instanceof Funcion a && a.tipo() == TipoFuncion.ABS;
        Expresion v = lnAbs ? ((Funcion) u).argumento() : u;
        String regla = lnAbs ? "(ln|u|)' = u'/u" : switch (f.tipo()) {
            case SEN -> "(sen u)' = cos u·u'";
            case COS -> "(cos u)' = −sen u·u'";
            case TAN -> "(tan u)' = sec² u·u'";
            case COT -> "(cot u)' = −csc² u·u'";
            case SEC -> "(sec u)' = sec u·tan u·u'";
            case CSC -> "(csc u)' = −csc u·cot u·u'";
            case ARCSEN -> "(arcsen u)' = u'/√(1 − u²)";
            case ARCCOS -> "(arccos u)' = −u'/√(1 − u²)";
            case ARCTAN -> "(arctan u)' = u'/(1 + u²)";
            case ARCSEC -> "(arcsec u)' = u'/(u·√(u² − 1))";
            case LN -> "(ln u)' = u'/u";
            case LOG -> "(log_b u)' = u'/(u·ln b) con b = " + f.base();
            case EXP -> "(eᵘ)' = eᵘ·u'";
            case SQRT -> "(√u)' = u'/(2√u)";
            case ABS -> "(|u|)' = u·u'/|u|";
        };
        String formula = esLaVariable(v) ? regla.replace("·u'", "").replace("u'", "1").replace('u', variable)
                : regla.replaceAll(" con b = \\d+", "");
        String codigo = f.tipo() == TipoFuncion.SQRT || f.tipo() == TipoFuncion.ABS ? cadena("D02", v) : cadena("D07", v);
        return registrar(codigo, regla + conU(v), formula, f, t -> {
            Expresion du = internaConTrabajo(v, t);
            if (lnAbs) {
                return cociente(du, v);
            }
            return switch (f.tipo()) {
                case SEN -> producto(List.of(new Funcion(TipoFuncion.COS, u), du));
                case COS -> negar(producto(List.of(new Funcion(TipoFuncion.SEN, u), du)));
                case TAN -> producto(List.of(potencia(new Funcion(TipoFuncion.SEC, u), DOS), du));
                case COT -> negar(producto(List.of(potencia(new Funcion(TipoFuncion.CSC, u), DOS), du)));
                case SEC -> producto(List.of(f, new Funcion(TipoFuncion.TAN, u), du));
                case CSC -> negar(producto(List.of(f, new Funcion(TipoFuncion.COT, u), du)));
                case ARCSEN -> cociente(du, new Raiz(2, suma(List.of(UNO, negar(potencia(u, DOS))))));
                case ARCCOS -> negar(cociente(du, new Raiz(2, suma(List.of(UNO, negar(potencia(u, DOS)))))));
                case ARCTAN -> cociente(du, suma(List.of(UNO, potencia(u, DOS))));
                case ARCSEC -> cociente(du, producto(List.of(u, new Raiz(2, suma(List.of(potencia(u, DOS), negar(UNO)))))));
                case LN -> cociente(du, u);
                case LOG -> cociente(du, producto(List.of(u, new Funcion(TipoFuncion.LN, Constante.de(f.base())))));
                case EXP -> producto(List.of(f, du));
                case SQRT -> cociente(du, producto(List.of(DOS, f)));
                case ABS -> cociente(producto(List.of(u, du)), f);
            };
        });
    }

    private static IllegalArgumentException operacion() {
        return new IllegalArgumentException("Una operación (límite, derivada, integral o ecuación) no se deriva");
    }

    @Override
    public Expresion visitar(Limite l) {
        throw operacion();
    }

    @Override
    public Expresion visitar(Derivada d) {
        throw operacion();
    }

    @Override
    public Expresion visitar(pe.mathnova.domain.expresion.Gradiente g) {
        throw operacion();
    }

    @Override
    public Expresion visitar(pe.mathnova.domain.expresion.Direccional d) {
        throw operacion();
    }

    @Override
    public Expresion visitar(DerivadaParcial d) {
        throw operacion();
    }

    @Override
    public Expresion visitar(Integral i) {
        throw operacion();
    }

    @Override
    public Expresion visitar(IntegralMultiple i) {
        throw operacion();
    }

    @Override
    public Expresion visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
        throw operacion();
    }

    @Override
    public Expresion visitar(pe.mathnova.domain.expresion.Extremos x) {
        throw operacion();
    }

    @Override
    public Expresion visitar(Ecuacion e) {
        throw operacion();
    }
}
