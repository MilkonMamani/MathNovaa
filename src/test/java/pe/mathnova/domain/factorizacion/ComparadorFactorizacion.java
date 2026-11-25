package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;
import pe.mathnova.domain.parser.Parser;

public final class ComparadorFactorizacion {

    public record Forma(Racional constante, Map<Polinomio, Integer> factores) {
    }

    private ComparadorFactorizacion() {
    }

    public static Forma leer(String texto) {
        return leer(Parser.parse(texto));
    }

    public static Forma leer(Expresion e) {
        Estado s = new Estado();
        acumular(e, 1, s);
        return new Forma(s.constante, s.factores);
    }

    public record FormaFraccion(Racional constante, Map<Polinomio, Integer> numerador,
                                Map<Polinomio, Integer> denominador) {
    }

    public static FormaFraccion leerFraccion(String texto) {
        Expresion e = Parser.parse(texto);
        Racional signo = Racional.UNO;
        while (e instanceof Negacion n) {
            signo = signo.negar();
            e = n.argumento();
        }
        if (e instanceof pe.mathnova.domain.expresion.Cociente c) {
            Forma num = leer(c.numerador());
            Forma den = leer(c.denominador());
            return new FormaFraccion(signo.multiplicar(num.constante()).dividir(den.constante()), num.factores(),
                    den.factores());
        }
        Forma f = leer(e);
        return new FormaFraccion(signo.multiplicar(f.constante()), f.factores(), Map.of());
    }

    private static final class Estado {
        Racional constante = Racional.UNO;
        final Map<Polinomio, Integer> factores = new LinkedHashMap<>();
    }

    private static void acumular(Expresion e, int exponente, Estado s) {
        switch (e) {
            case Negacion n -> {
                s.constante = s.constante.multiplicar(Racional.UNO.negar().potencia(exponente));
                acumular(n.argumento(), exponente, s);
            }
            case Producto p -> p.factores().forEach(f -> acumular(f, exponente, s));
            case Potencia(Expresion base, Constante k) when k.valor().esEntero() && k.valor().signo() > 0
                    && !Polinomio.desde(base).esConstante() ->
                    acumular(base, exponente * k.valor().num().intValueExact(), s);
            default -> agregar(Polinomio.desde(e), exponente, s);
        }
    }

    private static void agregar(Polinomio q, int exponente, Estado s) {
        if (q.esConstante()) {
            s.constante = s.constante.multiplicar(q.constante().potencia(exponente));
            return;
        }
        if (q.terminos().size() == 1) {
            var t = q.terminos().entrySet().iterator().next();
            s.constante = s.constante.multiplicar(t.getValue().potencia(exponente));
            t.getKey().exponentes().forEach((letra, k) ->
                    s.factores.merge(Polinomio.variable(letra), k * exponente, Integer::sum));
            return;
        }
        Racional c = Contenido.de(q);
        s.constante = s.constante.multiplicar(c.potencia(exponente));
        s.factores.merge(Contenido.dividir(q, c, pe.mathnova.domain.algebra.Monomio.UNO), exponente, Integer::sum);
    }

    public record FormaCampo(NumeroAlgebraico constante, List<PolinomioAlgebraico> factores) {
    }

    public static FormaCampo leerEnCampo(String texto) {
        List<PolinomioAlgebraico> factores = new ArrayList<>();
        NumeroAlgebraico[] c = {NumeroAlgebraico.UNO};
        acumularEnCampo(Parser.parse(texto), 1, c, factores);
        return new FormaCampo(c[0], factores);
    }

    private static void acumularEnCampo(Expresion e, int exponente, NumeroAlgebraico[] c, List<PolinomioAlgebraico> f) {
        switch (e) {
            case Negacion n -> {
                c[0] = c[0].multiplicar(NumeroAlgebraico.de(-1).potencia(exponente));
                acumularEnCampo(n.argumento(), exponente, c, f);
            }
            case Producto p -> p.factores().forEach(x -> acumularEnCampo(x, exponente, c, f));
            case Potencia(Expresion base, Constante k) when k.valor().esEntero() && k.valor().signo() > 0
                    && PolinomioAlgebraico.desde(base, 'x').grado() > 0 ->
                    acumularEnCampo(base, exponente * k.valor().num().intValueExact(), c, f);
            default -> {
                PolinomioAlgebraico p = PolinomioAlgebraico.desde(e, 'x');
                if (p.grado() == 0) {
                    c[0] = c[0].multiplicar(p.coeficiente(0).potencia(exponente));
                    return;
                }
                c[0] = c[0].multiplicar(p.principal().potencia(exponente));
                PolinomioAlgebraico m = p.monico();
                for (int i = 0; i < exponente; i++) {
                    f.add(m);
                }
            }
        }
    }

    public static boolean igualesEnCampo(FormaCampo a, FormaCampo b) {
        if (!mismoNumero(a.constante(), b.constante()) || a.factores().size() != b.factores().size()) {
            return false;
        }
        List<PolinomioAlgebraico> resto = new ArrayList<>(b.factores());
        for (PolinomioAlgebraico p : a.factores()) {
            int i = 0;
            while (i < resto.size() && !mismoPolinomio(p, resto.get(i))) {
                i++;
            }
            if (i == resto.size()) {
                return false;
            }
            resto.remove(i);
        }
        return true;
    }

    private static boolean mismoNumero(NumeroAlgebraico a, NumeroAlgebraico b) {
        if (a.exacto() && b.exacto()) {
            return a.equals(b);
        }
        return EvaluacionNumerica.iguales(a.aExpresion(), b.aExpresion(), 'x');
    }

    private static boolean mismoPolinomio(PolinomioAlgebraico a, PolinomioAlgebraico b) {
        if (a.grado() != b.grado()) {
            return false;
        }
        if (a.exacto() && b.exacto()) {
            return a.equals(b);
        }
        return EvaluacionNumerica.iguales(a.aExpresion(), b.aExpresion(), 'x');
    }
}
