package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

/**
 * Sustitución u = g(x) (RN32): cada función de x (sen x, ln x, eˣ…) pasa a una letra nueva y vuelve
 * al final. e^(kx) con k entero es (eˣ)ᵏ, así e^(2x) − 3eˣ + 2 queda u² − 3u + 2.
 */
public final class SustitucionFunciones {

    /** Letras para las funciones, en orden; nunca x. */
    private static final String LETRAS = "uvwzstpqrabcdfghjklmnoy";

    private SustitucionFunciones() {
    }

    /** Si hay alguna función de x o una potencia con x en el exponente. */
    public static boolean tieneFunciones(Expresion e) {
        boolean[] hay = {false};
        recorrer(e, n -> {
            if (n instanceof Funcion || n instanceof Potencia(Expresion b, Expresion k) && !esConstante(k)) {
                hay[0] = true;
            }
        });
        return hay[0];
    }

    /**
     * Cambia cada función por su letra. {@code letras} va de la clave de la función ("sen(x)",
     * "e^x") a la letra y se completa con las nuevas, así dos lecturas con el mismo mapa usan las
     * mismas letras.
     */
    public static Expresion aLetras(Expresion e, Map<String, Character> letras) {
        return transformar(e, n -> switch (n) {
            case Funcion f when f.tipo() == TipoFuncion.EXP -> exponencial(f.argumento(), letras);
            case Funcion f -> letra(ImpresoraTexto.imprimir(f), letras);
            case Potencia(ConstanteEspecial c, Expresion k) when c.tipo() == ConstanteEspecial.Tipo.E && !esConstante(k) ->
                    exponencial(k, letras);
            default -> n;
        });
    }

    /** e^(k·a) con k entero → (letra de e^a)^k. */
    private static Expresion exponencial(Expresion exponente, Map<String, Character> letras) {
        Expresion base = exponente;
        long k = 1;
        if (exponente instanceof Producto(List<Expresion> fs) && fs.size() == 2 && fs.getFirst() instanceof Constante c
                && c.valor().esEntero() && c.valor().signo() > 0) {
            k = c.valor().num().longValueExact();
            base = fs.get(1);
        }
        Expresion clave = new Potencia(new ConstanteEspecial(ConstanteEspecial.Tipo.E), base);
        Variable v = letra(ImpresoraTexto.imprimir(clave), letras);
        return k == 1 ? v : new Potencia(v, Constante.de(k));
    }

    private static Variable letra(String clave, Map<String, Character> letras) {
        Character c = letras.get(clave);
        if (c == null) {
            c = LETRAS.chars().mapToObj(i -> (char) i).filter(l -> !letras.containsValue(l)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Demasiadas funciones distintas"));
            letras.put(clave, c);
        }
        return new Variable(c);
    }

    /** Deshace la sustitución: cada letra vuelve a su función. */
    public static Expresion deLetras(Expresion e, Map<Character, Expresion> funciones) {
        return transformar(e, n -> n instanceof Variable v && funciones.containsKey(v.nombre())
                ? funciones.get(v.nombre()) : n);
    }

    // ------------------------------------------------------------------ recorrido

    static boolean esConstante(Expresion e) {
        boolean[] variable = {false};
        recorrer(e, n -> {
            if (n instanceof Variable) {
                variable[0] = true;
            }
        });
        return !variable[0];
    }

    /** Cada llamada a función del árbol, también las anidadas. */
    public static void recorrerFunciones(Expresion e, java.util.function.Consumer<Funcion> visita) {
        recorrer(e, n -> {
            if (n instanceof Funcion f) {
                visita.accept(f);
            }
        });
    }

    static void recorrer(Expresion e, java.util.function.Consumer<Expresion> visita) {
        visita.accept(e);
        for (Expresion h : hijos(e)) {
            recorrer(h, visita);
        }
    }

    private static List<Expresion> hijos(Expresion e) {
        return switch (e) {
            case Suma s -> s.terminos();
            case Producto p -> p.factores();
            case Cociente c -> List.of(c.numerador(), c.denominador());
            case Potencia p -> List.of(p.base(), p.exponente());
            case Negacion n -> List.of(n.argumento());
            case Raiz r -> List.of(r.radicando());
            case Funcion f -> List.of(f.argumento());
            default -> List.of();
        };
    }

    /** De abajo hacia arriba: primero los hijos, luego el nodo. */
    static Expresion transformar(Expresion e, UnaryOperator<Expresion> f) {
        Expresion r = switch (e) {
            case Suma s -> new Suma(mapa(s.terminos(), f));
            case Producto p -> new Producto(mapa(p.factores(), f));
            case Cociente c -> new Cociente(transformar(c.numerador(), f), transformar(c.denominador(), f));
            case Potencia p -> new Potencia(transformar(p.base(), f), transformar(p.exponente(), f));
            case Negacion n -> new Negacion(transformar(n.argumento(), f));
            case Raiz z -> new Raiz(z.indice(), transformar(z.radicando(), f));
            case Funcion fn -> fn; // el argumento de una función no se toca: la función entera es la unidad
            default -> e;
        };
        return f.apply(r);
    }

    private static List<Expresion> mapa(List<Expresion> l, UnaryOperator<Expresion> f) {
        List<Expresion> r = new ArrayList<>();
        for (Expresion x : l) {
            r.add(transformar(x, f));
        }
        return r;
    }
}
