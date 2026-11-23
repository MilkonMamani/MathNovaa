package pe.mathnova.domain.calculo.derivadas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

/**
 * El paso "Simplificar" de una derivada o una integral (Sprint 6): la forma en que se escribe el resultado.
 * <ul>
 *   <li>Un polinomio, o el numerador polinómico de una fracción, se expande y se ordena:
 *       (2x(x − 1) − (x² + 1))/(x − 1)² queda (x² − 2x − 1)/(x − 1)².</li>
 *   <li>Un factor exponencial común a todos los términos sale afuera: eˣ·cos x − eˣ·sen x queda
 *       eˣ(cos x − sen x).</li>
 *   <li>En cada producto van primero los números, después las otras letras (constantes, RN43), después
 *       las potencias de la variable, las funciones y al final los paréntesis: 6x·cos(3x²), 2a·t.</li>
 * </ul>
 */
public final class FormaFinal {

    private FormaFinal() {
    }

    public static Expresion de(Expresion e, char variable) {
        Expresion r = Simplificador.simplificar(e);
        r = expandir(r);
        r = sacarExponencial(r);
        return ordenar(r, variable);
    }

    // ------------------------------------------------------------------ polinomios

    private static Expresion expandir(Expresion e) {
        Polinomio p = polinomio(e);
        if (p != null) {
            return Simplificador.simplificar(p.aExpresion());
        }
        if (e instanceof Negacion n) {
            return Simplificador.negar(expandir(n.argumento()));
        }
        if (e instanceof Cociente c) {
            Polinomio arriba = polinomio(c.numerador());
            if (arriba != null) {
                return Simplificador.cociente(Simplificador.simplificar(arriba.aExpresion()), c.denominador());
            }
        }
        if (e instanceof Ecuacion ec) {
            return new Ecuacion(expandir(ec.izquierda()), expandir(ec.derecha()));
        }
        return e;
    }

    /** El polinomio, si e lo es y vale la pena expandirlo (no un solo término ya escrito). */
    private static Polinomio polinomio(Expresion e) {
        if (!(e instanceof Suma)) {
            return null;
        }
        try {
            Polinomio p = Polinomio.desde(e);
            return p.grado() <= 30 ? p : null;
        } catch (RuntimeException noEs) {
            return null;
        }
    }

    // ------------------------------------------------------------------ factor exponencial común

    private static boolean esExponencial(Expresion f) {
        return f instanceof Potencia p && !(p.exponente() instanceof Constante)
                || f instanceof Funcion fn && fn.tipo() == TipoFuncion.EXP;
    }

    private static List<Expresion> factores(Expresion t) {
        Expresion sinSigno = t instanceof Negacion n ? n.argumento() : t;
        return sinSigno instanceof Producto p ? p.factores() : List.of(sinSigno);
    }

    private static Expresion sacarExponencial(Expresion e) {
        if (!(e instanceof Suma s)) {
            return e;
        }
        for (Expresion candidato : factores(s.terminos().getFirst())) {
            if (!esExponencial(candidato)) {
                continue;
            }
            boolean enTodos = s.terminos().stream().allMatch(t -> factores(t).contains(candidato));
            if (!enTodos) {
                continue;
            }
            List<Expresion> restos = new ArrayList<>();
            for (Expresion t : s.terminos()) {
                List<Expresion> fs = new ArrayList<>(factores(t));
                fs.remove(candidato);
                Expresion resto = fs.isEmpty() ? Constante.de(1) : Simplificador.producto(fs);
                restos.add(t instanceof Negacion ? Simplificador.negar(resto) : resto);
            }
            return new Producto(List.of(candidato, Simplificador.suma(restos)));
        }
        return e;
    }

    // ------------------------------------------------------------------ orden de los factores

    private static int rango(Expresion f, char x) {
        return switch (f) {
            case Constante c -> 0;
            case Variable v -> v.nombre() == x ? 2 : 1;
            case Potencia p when p.base() instanceof Variable v && p.exponente() instanceof Constante ->
                    v.nombre() == x ? 2 : 1;
            case Suma s -> 4;
            default -> 3;
        };
    }

    /** Solo el orden de los factores (una integral no se expande: (x² + 1)⁶/6 queda así). */
    public static Expresion ordenar(Expresion e, char x) {
        return switch (e) {
            case Producto p -> {
                List<Expresion> fs = new ArrayList<>(p.factores().stream().map(f -> ordenar(f, x)).toList());
                fs.sort(Comparator.comparingInt(f -> rango(f, x)));
                yield new Producto(fs);
            }
            case Suma s -> new Suma(s.terminos().stream().map(t -> ordenar(t, x)).toList());
            case Cociente c -> new Cociente(ordenar(c.numerador(), x), ordenar(c.denominador(), x));
            case Negacion n -> new Negacion(ordenar(n.argumento(), x));
            case Potencia p -> new Potencia(ordenar(p.base(), x), ordenar(p.exponente(), x));
            case Raiz r -> new Raiz(r.indice(), ordenar(r.radicando(), x));
            case Funcion f -> new Funcion(f.tipo(), ordenar(f.argumento(), x), f.base());
            case Ecuacion ec -> new Ecuacion(ordenar(ec.izquierda(), x), ordenar(ec.derecha(), x));
            default -> e;
        };
    }
}
