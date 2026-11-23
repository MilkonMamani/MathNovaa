package pe.mathnova.domain.expresion;

import java.util.List;

/**
 * Escribe el árbol en el formato del banco de pruebas: "x^2", "2x", "(x - 2)(x + 3)", "2·i",
 * "e^x·(x + 1)", "√3·i/2". Lo que imprime se vuelve a leer con Parser y da el mismo árbol
 * (salvo constantes negativas o fraccionarias sueltas, que el parser nunca produce).
 *
 * <p>Multiplicación: se escribe sin signo ("2x^2(x - 1)") salvo antes de un número, de i o de una
 * raíz, después de una raíz, de un cociente o de un paréntesis seguido de otra cosa, y en los
 * productos con funciones o e^x, donde todos los factores van con "·" menos el coeficiente
 * entero inicial ("2sen(x)·(cos(x) - 1)").
 */
public final class ImpresoraTexto implements Visitante<String> {

    private static final ImpresoraTexto INSTANCIA = new ImpresoraTexto();

    public static String imprimir(Expresion e) {
        return e.aceptar(INSTANCIA);
    }

    private String p(Expresion e) {
        return e.aceptar(this);
    }

    private String entreParentesis(Expresion e) {
        return "(" + p(e) + ")";
    }

    @Override
    public String visitar(Constante c) {
        return c.valor().toString();
    }

    @Override
    public String visitar(Variable v) {
        return String.valueOf(v.nombre());
    }

    // ------------------------------------------------------------------ operaciones (HU34)

    @Override
    public String visitar(Limite l) {
        String punto = switch (l.punto()) {
            case Limite.Finito f -> p(f.valor());
            case Limite.Infinito inf -> inf.positivo() ? "∞" : "-∞";
        };
        String lado = switch (l.lado()) {
            case AMBOS -> "";
            case DERECHA -> "+";
            case IZQUIERDA -> "-";
        };
        return "lim(" + l.variable() + "→" + punto + lado + ", " + p(l.cuerpo()) + ")";
    }

    @Override
    public String visitar(Derivada d) {
        String orden = d.orden() == 1 ? "" : String.valueOf(d.orden());
        return "d" + orden + "/d" + d.variable() + orden + "(" + p(d.cuerpo()) + ")";
    }

    @Override
    public String visitar(DerivadaParcial d) {
        StringBuilder sb = new StringBuilder("∂");
        if (d.orden() > 1) {
            sb.append(d.orden());
        }
        sb.append('/');
        d.variables().forEach(v -> sb.append('∂').append(v));
        return sb.append('(').append(p(d.cuerpo())).append(')').toString();
    }

    /** "grad(x^2 + y^2)", "grad(x^2 + y^2, (1, 2))". */
    @Override
    public String visitar(Gradiente g) {
        return "grad(" + p(g.cuerpo()) + (g.enUnPunto() ? ", " + tupla(g.punto()) : "") + ")";
    }

    /** "Du(x^2 + y^2, (3, 4), (1, 2))". */
    @Override
    public String visitar(Direccional d) {
        return "Du(" + p(d.cuerpo()) + ", " + tupla(d.vector()) + ", " + tupla(d.punto()) + ")";
    }

    private String tupla(java.util.List<Expresion> cs) {
        return "(" + String.join(", ", cs.stream().map(this::p).toList()) + ")";
    }

    @Override
    public String visitar(Integral i) {
        String limites = i.esDefinida() ? ", " + p(i.inferior()) + ", " + p(i.superior()) : "";
        return "∫(" + p(i.cuerpo()) + ", " + i.variable() + limites + ")";
    }

    @Override
    public String visitar(IntegralMultiple i) {
        StringBuilder sb = new StringBuilder(i.rangos().size() == 2 ? "∬(" : "∭(").append(p(i.cuerpo()));
        for (Rango r : i.rangos()) {
            sb.append(", ").append(r.variable()).append(", ").append(p(r.inferior()))
                    .append(", ").append(p(r.superior()));
        }
        if (i.coordenadas() != IntegralMultiple.Coordenadas.CARTESIANAS) {
            sb.append("; coord=").append(i.coordenadas().clave());
        }
        return sb.append(')').toString();
    }

    /** "lim((x, y)→(0, 0), xy/(x^2 + y^2))" (Sprint 8). */
    @Override
    public String visitar(LimiteDoble l) {
        return "lim((" + l.x() + ", " + l.y() + ")→(" + p(l.a()) + ", " + p(l.b()) + "), " + p(l.cuerpo()) + ")";
    }

    /** "extremos(x y; x + y = 10)" (Sprint 8). */
    @Override
    public String visitar(Extremos x) {
        return "extremos(" + p(x.funcion()) + (x.conRestriccion() ? "; " + p(x.restriccion()) : "") + ")";
    }

    /** "x^2 - 5x + 6 = 0" (HU38). */
    @Override
    public String visitar(Ecuacion e) {
        return p(e.izquierda()) + " = " + p(e.derecha());
    }

    @Override
    public String visitar(ConstanteEspecial c) {
        return switch (c.tipo()) {
            case E -> "e";
            case PI -> "π";
            case I -> "i";
        };
    }

    @Override
    public String visitar(Suma s) {
        StringBuilder sb = new StringBuilder();
        List<Expresion> terminos = s.terminos();
        for (int k = 0; k < terminos.size(); k++) {
            Expresion t = terminos.get(k);
            if (k == 0) {
                sb.append(t instanceof Suma ? entreParentesis(t) : p(t));
            } else if (t instanceof Negacion(Expresion arg)) {
                sb.append(" - ").append(arg instanceof Suma ? entreParentesis(arg) : p(arg));
            } else {
                sb.append(" + ").append(t instanceof Suma ? entreParentesis(t) : p(t));
            }
        }
        return sb.toString();
    }

    @Override
    public String visitar(Negacion n) {
        Expresion a = n.argumento();
        boolean envolver = Precedencia.de(a) < Precedencia.SIGNO
                || (a instanceof Constante c && c.valor().signo() < 0);
        return "-" + (envolver ? entreParentesis(a) : p(a));
    }

    @Override
    public String visitar(Producto prod) {
        List<Expresion> factores = prod.factores();
        boolean trascendente = Expresiones.esTrascendente(prod);
        StringBuilder sb = new StringBuilder();
        Expresion anterior = null;
        boolean anteriorEnvuelto = false;
        for (int k = 0; k < factores.size(); k++) {
            Expresion f = factores.get(k);
            boolean envolver = envuelveEnProducto(f, k);
            String texto = envolver ? entreParentesis(f) : p(f);
            if (anterior != null) {
                sb.append(separador(anterior, anteriorEnvuelto, f, envolver, texto, k, trascendente));
            }
            sb.append(texto);
            anterior = f;
            anteriorEnvuelto = envolver;
        }
        return sb.toString();
    }

    private static boolean envuelveEnProducto(Expresion f, int k) {
        return switch (f) {
            case Suma s -> true;
            case Negacion n -> true;
            case Producto p -> true;
            case Cociente c -> k > 0 || esFraccionNumerica(c);
            case Constante c -> !c.valor().esEntero() || c.valor().signo() < 0;
            default -> false;
        };
    }

    /** 3/5 escrito como cociente de dos enteros: en un producto va como "(3/5)x". */
    private static boolean esFraccionNumerica(Expresion e) {
        return e instanceof Cociente(Constante n, Constante d) && n.valor().esEntero() && d.valor().esEntero();
    }

    private static String separador(Expresion anterior, boolean anteriorEnvuelto, Expresion f,
                                    boolean envuelto, String texto, int k, boolean trascendente) {
        char inicio = texto.charAt(0);
        if (Character.isDigit(inicio) || inicio == '√' || inicio == '∛') {
            return "·";
        }
        if (trascendente) {
            boolean coeficienteInicial = k == 1 && anterior instanceof Constante c && c.valor().esEntero();
            return coeficienteInicial ? "" : "·";
        }
        if (anteriorEnvuelto) {
            // "(x + 1)(x - 2)^2" y "(3/5)x" van juntos; "(∛2 + ∛4)·x" lleva punto.
            return anterior instanceof Suma && inicio != '(' ? "·" : "";
        }
        if (esI(f) || anterior instanceof Raiz || anterior instanceof Cociente || esI(anterior)) {
            return "·";
        }
        return "";
    }

    private static boolean esI(Expresion e) {
        return e instanceof ConstanteEspecial(ConstanteEspecial.Tipo t) && t == ConstanteEspecial.Tipo.I;
    }

    @Override
    public String visitar(Cociente c) {
        Expresion num = c.numerador();
        Expresion den = c.denominador();
        String n = Precedencia.de(num) < Precedencia.PRODUCTO ? entreParentesis(num) : p(num);
        String d = Precedencia.de(den) <= Precedencia.PRODUCTO ? entreParentesis(den) : p(den);
        return n + "/" + d;
    }

    @Override
    public String visitar(Potencia pot) {
        Expresion base = pot.base();
        String b = Precedencia.de(base) == Precedencia.ATOMO ? p(base) : entreParentesis(base);
        return b + "^" + exponente(pot.exponente());
    }

    private String exponente(Expresion e) {
        int prec = Precedencia.de(e);
        if (prec >= Precedencia.POTENCIA) {
            return p(e);
        }
        if (e instanceof Negacion(Expresion a) && Precedencia.de(a) >= Precedencia.POTENCIA) {
            return "-" + p(a);
        }
        return entreParentesis(e);
    }

    @Override
    public String visitar(Raiz r) {
        Expresion a = r.radicando();
        String rad = Precedencia.de(a) == Precedencia.ATOMO ? p(a) : entreParentesis(a);
        return switch (r.indice()) {
            case 2 -> "√" + rad;
            case 3 -> "∛" + rad;
            default -> "√[" + r.indice() + "]" + rad;
        };
    }

    @Override
    public String visitar(Funcion f) {
        String nombre = f.tipo() == TipoFuncion.LOG && f.base() != 10 ? "log_" + f.base() : f.tipo().nombre();
        return nombre + "(" + p(f.argumento()) + ")";
    }
}
