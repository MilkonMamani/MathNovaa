package pe.mathnova.domain.expresion;

import java.util.List;

import pe.mathnova.domain.algebra.Racional;

/**
 * Convierte el árbol a LaTeX para la vista previa (HU02). Las operaciones se dibujan como en la
 * pizarra (HU34): {@code \lim_{x \to 2}}, {@code \frac{d}{dx}}, {@code \frac{\partial}{\partial y}},
 * {@code \int_{0}^{1} … \, dx}, {@code \iint}; las griegas como {@code \alpha}.
 */
public final class ImpresoraLatex implements Visitante<String> {

    private static final ImpresoraLatex INSTANCIA = new ImpresoraLatex();

    public static String imprimir(Expresion e) {
        return e.aceptar(INSTANCIA);
    }

    private String p(Expresion e) {
        return e.aceptar(this);
    }

    private String parentesis(String s) {
        return "\\left(" + s + "\\right)";
    }

    @Override
    public String visitar(Constante c) {
        Racional r = c.valor();
        if (r.esEntero()) {
            return r.toString();
        }
        String frac = "\\frac{" + r.num().abs() + "}{" + r.den() + "}";
        return r.signo() < 0 ? "-" + frac : frac;
    }

    @Override
    public String visitar(Variable v) {
        return Letras.latex(v.nombre());
    }

    // ------------------------------------------------------------------ operaciones (HU34)

    @Override
    public String visitar(Limite l) {
        String punto = switch (l.punto()) {
            case Limite.Finito f -> p(f.valor());
            case Limite.Infinito inf -> inf.positivo() ? "\\infty" : "-\\infty";
        };
        String lado = switch (l.lado()) {
            case AMBOS -> "";
            case DERECHA -> "^{+}";
            case IZQUIERDA -> "^{-}";
        };
        return "\\lim_{" + Letras.latex(l.variable()) + " \\to " + punto + lado + "} " + cuerpo(l.cuerpo());
    }

    @Override
    public String visitar(Derivada d) {
        if (d.orden() == 1 && d.cuerpo() instanceof Variable y) {
            // D08: y' dentro de una derivación implícita
            return Letras.latex(y.nombre()).strip() + "'";
        }
        String orden = d.orden() == 1 ? "" : "^{" + d.orden() + "}";
        return "\\frac{d" + orden + "}{d" + Letras.latex(d.variable()).strip() + orden + "}" + parentesis(p(d.cuerpo()));
    }

    /** Las variables iguales seguidas se agrupan: [x, x, y] es {@code \partial x^{2} \partial y}. */
    @Override
    public String visitar(DerivadaParcial d) {
        String orden = d.orden() == 1 ? "" : "^{" + d.orden() + "}";
        StringBuilder abajo = new StringBuilder();
        List<Character> vars = d.variables();
        int k = 0;
        while (k < vars.size()) {
            char v = vars.get(k);
            int veces = 0;
            while (k < vars.size() && vars.get(k) == v) {
                veces++;
                k++;
            }
            if (!abajo.isEmpty()) {
                abajo.append(' ');
            }
            abajo.append("\\partial ").append(Letras.latex(v).strip());
            if (veces > 1) {
                abajo.append("^{").append(veces).append('}');
            }
        }
        return "\\frac{\\partial" + orden + "}{" + abajo + "}" + parentesis(p(d.cuerpo()));
    }

    @Override
    public String visitar(Integral i) {
        String limites = i.esDefinida() ? "_{" + p(i.inferior()) + "}^{" + p(i.superior()) + "}" : "";
        return "\\int" + limites + " " + cuerpo(i.cuerpo()) + diferencial(i.variable());
    }

    /**
     * Región como producto de intervalos: {@code \iint_{[0, 1] \times [0, 2]} … \, dx \, dy}. Si algún límite
     * depende de otra variable (Sprint 8), como integrales iteradas: {@code \int_{0}^{1}\int_{0}^{x} … \, dy \, dx}.
     */
    @Override
    public String visitar(IntegralMultiple i) {
        boolean constantes = i.rangos().stream().allMatch(r -> Expresiones.variables(r.inferior()).isEmpty()
                && Expresiones.variables(r.superior()).isEmpty());
        String coord = i.coordenadas() == IntegralMultiple.Coordenadas.CARTESIANAS ? ""
                : " \\quad \\text{(" + i.coordenadas().nombre() + ")}";
        if (!constantes || coord.length() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int k = i.rangos().size() - 1; k >= 0; k--) {
                Rango r = i.rangos().get(k);
                sb.append("\\int_{").append(p(r.inferior())).append("}^{").append(p(r.superior())).append("}");
            }
            sb.append(' ').append(cuerpo(i.cuerpo()));
            for (Rango r : i.rangos()) {
                sb.append(diferencial(r.variable()));
            }
            return sb.append(coord).toString();
        }
        StringBuilder region = new StringBuilder();
        StringBuilder diferenciales = new StringBuilder();
        for (Rango r : i.rangos()) {
            if (!region.isEmpty()) {
                region.append(" \\times ");
            }
            region.append('[').append(p(r.inferior())).append(", ").append(p(r.superior())).append(']');
            diferenciales.append(diferencial(r.variable()));
        }
        String simbolo = i.rangos().size() == 2 ? "\\iint" : "\\iiint";
        return simbolo + "_{" + region + "} " + cuerpo(i.cuerpo()) + diferenciales;
    }

    /** {@code \lim_{(x, y) \to (0, 0)} …} (Sprint 8). */
    @Override
    public String visitar(LimiteDoble l) {
        return "\\lim_{(" + Letras.latex(l.x()).strip() + ", " + Letras.latex(l.y()).strip() + ") \\to (" + p(l.a())
                + ", " + p(l.b()) + ")} " + cuerpo(l.cuerpo());
    }

    /** {@code \operatorname{extremos}\left(f\right)}, o con la restricción después de ";" (Sprint 8). */
    @Override
    public String visitar(Extremos x) {
        String r = x.conRestriccion() ? ";\\ " + p(x.restriccion()) : "";
        return "\\operatorname{extremos}\\left(" + p(x.funcion()) + r + "\\right)";
    }

    private static String diferencial(char variable) {
        return " \\, d" + Letras.latex(variable).strip();
    }

    /** Cuerpo de un límite o una integral: entre paréntesis si es una suma o lleva signo. */
    private String cuerpo(Expresion e) {
        return e instanceof Suma || e instanceof Negacion ? parentesis(p(e)) : p(e);
    }

    /** \nabla f, o \nabla f(1, 2). */
    @Override
    public String visitar(Gradiente g) {
        String f = p(g.cuerpo());
        String cuerpo = g.cuerpo() instanceof Suma || g.cuerpo() instanceof Negacion ? "\\left(" + f + "\\right)" : f;
        return "\\nabla " + cuerpo + (g.enUnPunto() ? tupla(g.punto()) : "");
    }

    /** D_{\vec u} f(1, 2) con \vec u = (3, 4). */
    @Override
    public String visitar(Direccional d) {
        String f = p(d.cuerpo());
        String cuerpo = d.cuerpo() instanceof Suma || d.cuerpo() instanceof Negacion ? "\\left(" + f + "\\right)" : f;
        return "D_{\\vec u} " + cuerpo + tupla(d.punto()) + ",\\ \\vec u = " + tupla(d.vector());
    }

    private String tupla(List<Expresion> cs) {
        return "\\left(" + String.join(", ", cs.stream().map(this::p).toList()) + "\\right)";
    }

    @Override
    public String visitar(Ecuacion e) {
        return p(e.izquierda()) + " = " + p(e.derecha());
    }

    @Override
    public String visitar(ConstanteEspecial c) {
        return switch (c.tipo()) {
            case E -> "e";
            case PI -> "\\pi ";
            case I -> "i";
        };
    }

    @Override
    public String visitar(Suma s) {
        StringBuilder sb = new StringBuilder();
        List<Expresion> terminos = s.terminos();
        for (int k = 0; k < terminos.size(); k++) {
            Expresion t = terminos.get(k);
            if (k > 0 && t instanceof Negacion(Expresion arg)) {
                sb.append(" - ").append(arg instanceof Suma ? parentesis(p(arg)) : p(arg));
            } else {
                if (k > 0) {
                    sb.append(" + ");
                }
                sb.append(t instanceof Suma ? parentesis(p(t)) : p(t));
            }
        }
        return sb.toString();
    }

    @Override
    public String visitar(Negacion n) {
        Expresion a = n.argumento();
        boolean envolver = a instanceof Suma || a instanceof Negacion
                || (a instanceof Constante c && c.valor().signo() < 0);
        return "-" + (envolver ? parentesis(p(a)) : p(a));
    }

    @Override
    public String visitar(Producto prod) {
        StringBuilder sb = new StringBuilder();
        for (Expresion f : prod.factores()) {
            boolean envolver = f instanceof Suma || f instanceof Negacion
                    || (f instanceof Constante c && c.valor().signo() < 0);
            String texto = envolver ? parentesis(p(f)) : p(f);
            if (!sb.isEmpty() && (Character.isDigit(texto.charAt(0)) || texto.startsWith("\\frac"))) {
                sb.append(" \\cdot ");
            }
            sb.append(texto);
        }
        return sb.toString();
    }

    @Override
    public String visitar(Cociente c) {
        return "\\frac{" + p(c.numerador()) + "}{" + p(c.denominador()) + "}";
    }

    @Override
    public String visitar(Potencia pot) {
        Expresion base = pot.base();
        String exp = "^{" + p(pot.exponente()) + "}";
        if (base instanceof Funcion f && pot.exponente() instanceof Constante c && c.valor().esEntero()
                && c.valor().signo() > 0 && f.tipo() != TipoFuncion.SQRT && f.tipo() != TipoFuncion.EXP
                && f.tipo() != TipoFuncion.ABS) {
            // sen^2(x)
            return nombre(f) + exp + parentesis(p(f.argumento()));
        }
        boolean atomo = base instanceof Variable || base instanceof ConstanteEspecial
                || (base instanceof Constante c && c.valor().esEntero() && c.valor().signo() >= 0);
        return (atomo ? p(base) : parentesis(p(base))) + exp;
    }

    @Override
    public String visitar(Raiz r) {
        String rad = p(r.radicando());
        return r.indice() == 2 ? "\\sqrt{" + rad + "}" : "\\sqrt[" + r.indice() + "]{" + rad + "}";
    }

    @Override
    public String visitar(Funcion f) {
        if (f.tipo() == TipoFuncion.SQRT) {
            return "\\sqrt{" + p(f.argumento()) + "}";
        }
        if (f.tipo() == TipoFuncion.ABS) {
            return "\\left|" + p(f.argumento()) + "\\right|";
        }
        if (f.tipo() == TipoFuncion.LN && f.argumento() instanceof Funcion a && a.tipo() == TipoFuncion.ABS) {
            return "\\ln" + p(a);
        }
        return nombre(f) + parentesis(p(f.argumento()));
    }

    private static String nombre(Funcion f) {
        return switch (f.tipo()) {
            case SEN -> "\\mathrm{sen}";
            case COS -> "\\cos";
            case TAN -> "\\tan";
            case COT -> "\\cot";
            case SEC -> "\\sec";
            case CSC -> "\\csc";
            case ARCSEN -> "\\mathrm{arcsen}";
            case ARCCOS -> "\\arccos";
            case ARCTAN -> "\\arctan";
            case ARCSEC -> "\\operatorname{arcsec}";
            case LN -> "\\ln";
            case LOG -> f.base() == 10 ? "\\log" : "\\log_{" + f.base() + "}";
            case EXP -> "\\exp";
            case SQRT -> "\\sqrt";
            case ABS -> "";
        };
    }
}
