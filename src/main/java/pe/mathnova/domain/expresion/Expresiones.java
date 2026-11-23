package pe.mathnova.domain.expresion;

import java.util.SortedSet;
import java.util.TreeSet;

/** Consultas simples sobre un árbol de expresión. */
public final class Expresiones {

    private Expresiones() {
    }

    /**
     * Letras que aparecen (variables y constantes literales como la a de "a x"), en orden alfabético.
     * En una operación incluye su variable y lo que haya en los límites.
     */
    public static SortedSet<Character> variables(Expresion e) {
        SortedSet<Character> vars = new TreeSet<>();
        recolectar(e, vars);
        return vars;
    }

    private static void recolectar(Expresion e, SortedSet<Character> vars) {
        switch (e) {
            case Variable v -> vars.add(v.nombre());
            case Constante c -> { }
            case ConstanteEspecial c -> { }
            case Suma s -> s.terminos().forEach(t -> recolectar(t, vars));
            case Producto p -> p.factores().forEach(f -> recolectar(f, vars));
            case Cociente c -> {
                recolectar(c.numerador(), vars);
                recolectar(c.denominador(), vars);
            }
            case Potencia p -> {
                recolectar(p.base(), vars);
                recolectar(p.exponente(), vars);
            }
            case Negacion n -> recolectar(n.argumento(), vars);
            case Raiz r -> recolectar(r.radicando(), vars);
            case Funcion f -> recolectar(f.argumento(), vars);
            case Limite l -> {
                vars.add(l.variable());
                if (l.punto() instanceof Limite.Finito(Expresion valor)) {
                    recolectar(valor, vars);
                }
                recolectar(l.cuerpo(), vars);
            }
            case Derivada d -> {
                vars.add(d.variable());
                recolectar(d.cuerpo(), vars);
            }
            case DerivadaParcial d -> {
                vars.addAll(d.variables());
                recolectar(d.cuerpo(), vars);
            }
            case Integral i -> {
                vars.add(i.variable());
                recolectar(i.cuerpo(), vars);
                if (i.esDefinida()) {
                    recolectar(i.inferior(), vars);
                    recolectar(i.superior(), vars);
                }
            }
            case IntegralMultiple i -> {
                recolectar(i.cuerpo(), vars);
                for (Rango r : i.rangos()) {
                    vars.add(r.variable());
                    recolectar(r.inferior(), vars);
                    recolectar(r.superior(), vars);
                }
            }
            case Ecuacion ec -> {
                recolectar(ec.izquierda(), vars);
                recolectar(ec.derecha(), vars);
            }
            case Gradiente g -> {
                recolectar(g.cuerpo(), vars);
                g.punto().forEach(c -> recolectar(c, vars));
            }
            case Direccional d -> {
                recolectar(d.cuerpo(), vars);
                d.vector().forEach(c -> recolectar(c, vars));
                d.punto().forEach(c -> recolectar(c, vars));
            }
            case LimiteDoble l -> {
                vars.add(l.x());
                vars.add(l.y());
                recolectar(l.cuerpo(), vars);
            }
            case Extremos x -> {
                recolectar(x.funcion(), vars);
                if (x.conRestriccion()) {
                    recolectar(x.restriccion(), vars);
                }
            }
        }
    }

    /**
     * e con cada aparición de {@code buscado} cambiada por {@code por} (sin simplificar). No entra en
     * las operaciones (límites, derivadas, integrales), salvo que la operación entera sea lo buscado.
     */
    public static Expresion reemplazar(Expresion e, Expresion buscado, Expresion por) {
        if (e.equals(buscado)) {
            return por;
        }
        return switch (e) {
            case Suma s -> new Suma(s.terminos().stream().map(t -> reemplazar(t, buscado, por)).toList());
            case Producto p -> new Producto(p.factores().stream().map(f -> reemplazar(f, buscado, por)).toList());
            case Cociente c -> new Cociente(reemplazar(c.numerador(), buscado, por),
                    reemplazar(c.denominador(), buscado, por));
            case Potencia p -> new Potencia(reemplazar(p.base(), buscado, por), reemplazar(p.exponente(), buscado, por));
            case Negacion n -> new Negacion(reemplazar(n.argumento(), buscado, por));
            case Raiz r -> new Raiz(r.indice(), reemplazar(r.radicando(), buscado, por));
            case Funcion f -> new Funcion(f.tipo(), reemplazar(f.argumento(), buscado, por), f.base());
            case Ecuacion ec -> new Ecuacion(reemplazar(ec.izquierda(), buscado, por),
                    reemplazar(ec.derecha(), buscado, por));
            default -> e;
        };
    }

    /** Si {@code buscado} aparece en e. */
    public static boolean contiene(Expresion e, Expresion buscado) {
        if (e.equals(buscado)) {
            return true;
        }
        return switch (e) {
            case Suma s -> s.terminos().stream().anyMatch(t -> contiene(t, buscado));
            case Producto p -> p.factores().stream().anyMatch(f -> contiene(f, buscado));
            case Cociente c -> contiene(c.numerador(), buscado) || contiene(c.denominador(), buscado);
            case Potencia p -> contiene(p.base(), buscado) || contiene(p.exponente(), buscado);
            case Negacion n -> contiene(n.argumento(), buscado);
            case Raiz r -> contiene(r.radicando(), buscado);
            case Funcion f -> contiene(f.argumento(), buscado);
            case Ecuacion ec -> contiene(ec.izquierda(), buscado) || contiene(ec.derecha(), buscado);
            default -> false;
        };
    }

    /** Verdadero si la raíz del árbol es un límite, una derivada, una integral, un gradiente o una Du (HU34). */
    public static boolean esOperacion(Expresion e) {
        return e instanceof Limite || e instanceof Derivada || e instanceof DerivadaParcial
                || e instanceof Integral || e instanceof IntegralMultiple || e instanceof Gradiente
                || e instanceof Direccional || e instanceof LimiteDoble || e instanceof Extremos;
    }

    /**
     * Verdadero si hay funciones trascendentes (sen, ln, exp...) o una potencia con
     * variable en el exponente (e^x). sqrt y las raíces no cuentan.
     */
    public static boolean esTrascendente(Expresion e) {
        return switch (e) {
            case Variable v -> false;
            case Constante c -> false;
            case ConstanteEspecial c -> false;
            case Funcion f -> f.tipo() != TipoFuncion.SQRT || esTrascendente(f.argumento());
            case Suma s -> s.terminos().stream().anyMatch(Expresiones::esTrascendente);
            case Producto p -> p.factores().stream().anyMatch(Expresiones::esTrascendente);
            case Cociente c -> esTrascendente(c.numerador()) || esTrascendente(c.denominador());
            case Potencia p -> !variables(p.exponente()).isEmpty()
                    || esTrascendente(p.base()) || esTrascendente(p.exponente());
            case Negacion n -> esTrascendente(n.argumento());
            case Raiz r -> esTrascendente(r.radicando());
            case Limite l -> esTrascendente(l.cuerpo());
            case Derivada d -> esTrascendente(d.cuerpo());
            case DerivadaParcial d -> esTrascendente(d.cuerpo());
            case Integral i -> esTrascendente(i.cuerpo());
            case IntegralMultiple i -> esTrascendente(i.cuerpo());
            case Ecuacion ec -> esTrascendente(ec.izquierda()) || esTrascendente(ec.derecha());
            case Gradiente g -> esTrascendente(g.cuerpo());
            case Direccional d -> esTrascendente(d.cuerpo());
            case LimiteDoble l -> esTrascendente(l.cuerpo());
            case Extremos x -> esTrascendente(x.funcion())
                    || x.conRestriccion() && esTrascendente(x.restriccion());
        };
    }
}
