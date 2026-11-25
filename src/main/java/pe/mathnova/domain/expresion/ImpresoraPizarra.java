package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Racional;

public final class ImpresoraPizarra implements Visitante<String> {

    public static final String MENOS = "−";
    private static final String DIGITOS = "⁰¹²³⁴⁵⁶⁷⁸⁹";
    private static final Map<Character, Character> LETRAS = Map.of('x', 'ˣ', 'n', 'ⁿ', 'y', 'ʸ', 't', 'ᵗ',
            'a', 'ᵃ', 'b', 'ᵇ', 'k', 'ᵏ', 'm', 'ᵐ', 'u', 'ᵘ');
    private static final ImpresoraPizarra INSTANCIA = new ImpresoraPizarra();

    private ImpresoraPizarra() {
    }

    public static String imprimir(Expresion e) {
        return e.aceptar(INSTANCIA);
    }

    public static String numero(Racional r) {
        return r.signo() < 0 ? MENOS + r.negar() : r.toString();
    }

    private String p(Expresion e) {
        return e.aceptar(this);
    }

    private static String parentesis(String s) {
        return "(" + s + ")";
    }

    private static boolean atomo(Expresion e) {
        return e instanceof Variable || e instanceof ConstanteEspecial
                || e instanceof Constante c && c.valor().esEntero() && c.valor().signo() >= 0
                || esPrima(e);
    }

    private static boolean esPrima(Expresion e) {
        return e instanceof Derivada d && d.orden() == 1 && d.cuerpo() instanceof Variable;
    }

    static String superindice(Expresion exponente) {
        if (exponente instanceof Constante c && c.valor().esEntero()) {
            StringBuilder sb = new StringBuilder();
            String n = c.valor().num().toString();
            for (char d : n.toCharArray()) {
                sb.append(d == '-' ? '⁻' : DIGITOS.charAt(d - '0'));
            }
            return sb.toString();
        }
        if (exponente instanceof Negacion(Constante c) && c.valor().esEntero()) {
            String s = superindice(c);
            return s == null ? null : "⁻" + s;
        }
        if (exponente instanceof Variable v && LETRAS.containsKey(v.nombre())) {
            return String.valueOf(LETRAS.get(v.nombre()));
        }
        return null;
    }

    @Override
    public String visitar(Constante c) {
        return numero(c.valor());
    }

    @Override
    public String visitar(Variable v) {
        return String.valueOf(v.nombre());
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
        List<Expresion> t = s.terminos();
        for (int k = 0; k < t.size(); k++) {
            Expresion x = t.get(k);
            if (k == 0) {
                sb.append(x instanceof Suma ? parentesis(p(x)) : p(x));
                continue;
            }
            Signos.Termino signo = Signos.separar(x);
            Expresion v = signo.valor();
            sb.append(signo.negativo() ? " " + MENOS + " " : " + ").append(v instanceof Suma ? parentesis(p(v)) : p(v));
        }
        return sb.toString();
    }

    @Override
    public String visitar(Negacion n) {
        Expresion a = n.argumento();
        boolean envolver = a instanceof Suma || Signos.conMenos(a);
        return MENOS + (envolver ? parentesis(p(a)) : p(a));
    }

    @Override
    public String visitar(Producto prod) {
        if (prod.factores().getFirst() instanceof Constante k && k.valor().signo() < 0) {
            Expresion resto = Signos.sinMenos(prod).orElseThrow();
            return MENOS + (resto instanceof Suma || Signos.conMenos(resto) ? parentesis(p(resto)) : p(resto));
        }
        StringBuilder sb = new StringBuilder();
        Expresion anterior = null;
        for (Expresion f : prod.factores()) {
            boolean envolver = f instanceof Suma || f instanceof Cociente || Signos.conMenos(f)
                    || f instanceof Constante && anterior != null;
            String texto = envolver ? parentesis(p(f)) : p(f);
            if (anterior != null) {
                boolean pegado = texto.startsWith("(")
                        || anterior instanceof Constante && !Character.isDigit(texto.charAt(0));
                if (!pegado) {
                    sb.append('·');
                }
            }
            sb.append(texto);
            anterior = f;
        }
        return sb.toString();
    }

    @Override
    public String visitar(Cociente c) {
        Expresion num = c.numerador();
        Expresion den = c.denominador();
        if (den instanceof Constante d && d.valor().esEntero() && d.valor().signo() > 0) {
            Racional coef = Racional.UNO;
            Expresion resto = num;
            if (num instanceof Producto(List<Expresion> fs) && fs.size() == 2 && fs.getFirst() instanceof Constante k) {
                coef = k.valor();
                resto = fs.get(1);
            }
            if (resto instanceof Funcion f && f.tipo() != TipoFuncion.SQRT && f.tipo() != TipoFuncion.ABS) {
                return parentesis(coef.dividir(d.valor()).toString()) + p(resto);
            }
        }
        boolean envolverArriba = num instanceof Suma || num instanceof Negacion || num instanceof Cociente;
        boolean envolverAbajo = den instanceof Suma || den instanceof Producto || den instanceof Cociente
                || den instanceof Negacion || den instanceof Constante k && !k.valor().esEntero();
        return (envolverArriba ? parentesis(p(num)) : p(num)) + "/" + (envolverAbajo ? parentesis(p(den)) : p(den));
    }

    @Override
    public String visitar(Potencia pot) {
        Expresion base = pot.base();
        String sup = superindice(pot.exponente());
        if (base instanceof Funcion f && sup != null && pot.exponente() instanceof Constante c && c.valor().signo() > 0
                && f.tipo() != TipoFuncion.SQRT && f.tipo() != TipoFuncion.ABS && f.tipo() != TipoFuncion.EXP) {
            return nombre(f) + sup + argumento(f.argumento()).strip();
        }
        String b = atomo(base) ? p(base) : parentesis(p(base));
        if (sup != null) {
            return b + sup;
        }
        Expresion e = pot.exponente();
        return b + "^" + (atomo(e) ? p(e) : parentesis(p(e)));
    }

    @Override
    public String visitar(Raiz r) {
        String simbolo = switch (r.indice()) {
            case 2 -> "√";
            case 3 -> "∛";
            case 4 -> "∜";
            default -> superindice(Constante.de(r.indice())) + "√";
        };
        Expresion u = r.radicando();
        return simbolo + (atomo(u) ? p(u) : parentesis(p(u)));
    }

    @Override
    public String visitar(Funcion f) {
        Expresion u = f.argumento();
        return switch (f.tipo()) {
            case SQRT -> "√" + (atomo(u) ? p(u) : parentesis(p(u)));
            case ABS -> "|" + p(u) + "|";
            case EXP -> "e" + (superindice(u) != null ? superindice(u) : "^" + (atomo(u) ? p(u) : parentesis(p(u))));
            case LN -> u instanceof Funcion a && a.tipo() == TipoFuncion.ABS ? "ln" + p(a) : "ln" + argumento(u);
            default -> nombre(f) + argumento(u);
        };
    }

    private static String nombre(Funcion f) {
        return f.tipo() == TipoFuncion.LOG && f.base() != 10 ? "log_" + f.base() : f.tipo().nombre();
    }

    private String argumento(Expresion u) {
        return atomo(u) ? " " + p(u) : parentesis(p(u));
    }

    @Override
    public String visitar(Derivada d) {
        if (esPrima(d)) {
            return p(d.cuerpo()) + "'";
        }
        String orden = d.orden() == 1 ? "" : superindice(Constante.de(d.orden()));
        return "d" + orden + "/d" + d.variable() + orden + parentesis(p(d.cuerpo()));
    }

    @Override
    public String visitar(Ecuacion e) {
        return p(e.izquierda()) + " = " + p(e.derecha());
    }

    @Override
    public String visitar(Integral i) {
        String cuerpo = i.cuerpo() instanceof Suma ? parentesis(p(i.cuerpo())) : p(i.cuerpo());
        String limites = i.esDefinida() ? "[" + p(i.inferior()) + ", " + p(i.superior()) + "]" : "";
        return "∫" + limites + " " + cuerpo + " d" + i.variable();
    }

    @Override
    public String visitar(Limite l) {
        return ImpresoraTexto.imprimir(l);
    }

    @Override
    public String visitar(DerivadaParcial d) {
        return ImpresoraTexto.imprimir(d);
    }

    @Override
    public String visitar(Gradiente g) {
        return "∇(" + imprimir(g.cuerpo()) + ")" + (g.enUnPunto() ? " en " + tupla(g.punto()) : "");
    }

    @Override
    public String visitar(Direccional d) {
        return "D_u(" + imprimir(d.cuerpo()) + ") con u = " + tupla(d.vector()) + " en " + tupla(d.punto());
    }

    private static String tupla(java.util.List<Expresion> cs) {
        return "(" + String.join(", ", cs.stream().map(ImpresoraPizarra::imprimir).toList()) + ")";
    }

    @Override
    public String visitar(IntegralMultiple i) {
        return ImpresoraTexto.imprimir(i);
    }

    @Override
    public String visitar(LimiteDoble l) {
        return "lim((" + l.x() + ", " + l.y() + ") → (" + imprimir(l.a()) + ", " + imprimir(l.b()) + "), "
                + imprimir(l.cuerpo()) + ")";
    }

    @Override
    public String visitar(Extremos x) {
        return "extremos(" + imprimir(x.funcion()) + (x.conRestriccion() ? "; " + imprimir(x.restriccion().izquierda())
                + " = " + imprimir(x.restriccion().derecha()) : "") + ")";
    }
}
