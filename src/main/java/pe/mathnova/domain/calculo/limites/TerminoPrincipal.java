package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

/**
 * L04: cómo crece una expresión algebraica cuando x → +∞. Es c·xᵖ, con c ≠ 0 exacto y p racional:
 * 2x² + 1 crece como 2x², √(x² + x) como x, (3x + 1)/(x² + 5) como 3x⁻¹. Comparar grados es comparar p.
 * Si en una suma los términos principales se cancelan (√(x² + x) − x), no hay término principal: es
 * la forma ∞ − ∞ y hace falta el conjugado.
 */
record TerminoPrincipal(NumeroAlgebraico coeficiente, Racional exponente) {

    static Optional<TerminoPrincipal> de(Expresion e, char x) {
        return switch (e) {
            case Constante c -> c.valor().esCero() ? Optional.empty()
                    : Optional.of(new TerminoPrincipal(NumeroAlgebraico.de(c.valor()), Racional.CERO));
            case ConstanteEspecial c -> c.tipo() == ConstanteEspecial.Tipo.I ? Optional.empty()
                    : Optional.of(new TerminoPrincipal(c.tipo() == ConstanteEspecial.Tipo.E ? Exactos.E : Exactos.PI,
                    Racional.CERO));
            case Variable v -> v.nombre() == x ? Optional.of(new TerminoPrincipal(NumeroAlgebraico.UNO, Racional.UNO))
                    : Optional.empty();
            case Negacion n -> de(n.argumento(), x).map(t -> new TerminoPrincipal(t.coeficiente.negar(), t.exponente));
            case Suma s -> suma(s.terminos(), x);
            case Producto p -> {
                TerminoPrincipal r = new TerminoPrincipal(NumeroAlgebraico.UNO, Racional.CERO);
                for (Expresion f : p.factores()) {
                    Optional<TerminoPrincipal> t = de(f, x);
                    if (t.isEmpty()) {
                        yield Optional.empty();
                    }
                    r = new TerminoPrincipal(r.coeficiente.multiplicar(t.get().coeficiente),
                            r.exponente.sumar(t.get().exponente));
                }
                yield Optional.of(r);
            }
            case Cociente c -> {
                Optional<TerminoPrincipal> a = de(c.numerador(), x);
                Optional<TerminoPrincipal> b = de(c.denominador(), x);
                if (a.isEmpty() || b.isEmpty()) {
                    yield Optional.empty();
                }
                yield Optional.of(new TerminoPrincipal(Exactos.dividir(a.get().coeficiente, b.get().coeficiente),
                        a.get().exponente.restar(b.get().exponente)));
            }
            case Potencia p -> Evaluador.constante(p.exponente()).filter(NumeroAlgebraico::esRacional)
                    .flatMap(k -> de(p.base(), x).flatMap(t -> potencia(t, k.aRacional())));
            case Raiz r -> de(r.radicando(), x).flatMap(t -> potencia(t, Racional.de(1, r.indice())));
            case Funcion f when f.tipo() == TipoFuncion.SQRT ->
                    de(f.argumento(), x).flatMap(t -> potencia(t, Racional.de(1, 2)));
            default -> Optional.empty();
        };
    }

    private static Optional<TerminoPrincipal> suma(List<Expresion> terminos, char x) {
        List<TerminoPrincipal> ts = new ArrayList<>();
        for (Expresion t : terminos) {
            if (t instanceof Constante c && c.valor().esCero()) {
                continue;
            }
            Optional<TerminoPrincipal> tp = de(t, x);
            if (tp.isEmpty()) {
                return Optional.empty();
            }
            ts.add(tp.get());
        }
        if (ts.isEmpty()) {
            return Optional.empty();
        }
        Racional mayor = ts.stream().map(TerminoPrincipal::exponente).max(Racional::compareTo).orElseThrow();
        NumeroAlgebraico c = NumeroAlgebraico.CERO;
        for (TerminoPrincipal t : ts) {
            if (t.exponente.equals(mayor)) {
                c = c.sumar(t.coeficiente);
            }
        }
        if (c.esCero()) {
            return Optional.empty(); // los términos principales se cancelan: ∞ − ∞
        }
        return Optional.of(new TerminoPrincipal(c, mayor));
    }

    /** (c·xᵖ)^(m/q) = c^(m/q)·x^(p·m/q); c debe ser positivo si q es par. */
    private static Optional<TerminoPrincipal> potencia(TerminoPrincipal t, Racional k) {
        if (k.num().bitLength() > 16 || k.den().bitLength() > 16) {
            return Optional.empty();
        }
        int m = k.num().intValueExact();
        int q = k.den().intValueExact();
        NumeroAlgebraico cm;
        try {
            cm = t.coeficiente.potencia(m);
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
        Optional<NumeroAlgebraico> c = q == 1 ? Optional.of(cm) : Exactos.raiz(q, cm);
        return c.map(v -> new TerminoPrincipal(v, t.exponente.multiplicar(k)));
    }

    /**
     * Para mostrar: e dividida entre xᵖ término a término. 2x² + 1 entre x² da 2 + 1/x²; √(x² + x)
     * entre x da √(1 + 1/x).
     */
    static Expresion dividida(Expresion e, char x, Racional p) {
        if (p.esCero()) {
            return e;
        }
        Expresion xp = Simplificador.potencia(new Variable(x), new Constante(p));
        return switch (e) {
            case Suma s -> Simplificador.suma(s.terminos().stream().map(t -> dividida(t, x, p)).toList());
            case Negacion n -> Simplificador.negar(dividida(n.argumento(), x, p));
            case Raiz r -> new Raiz(r.indice(), dividida(r.radicando(), x, p.multiplicar(Racional.de(r.indice()))));
            case Producto pr when pr.factores().getFirst() instanceof Constante c -> {
                List<Expresion> resto = pr.factores().subList(1, pr.factores().size());
                Expresion r = resto.size() == 1 ? resto.getFirst() : new Producto(resto);
                yield Simplificador.producto(List.of(c, dividida(r, x, p)));
            }
            default -> Simplificador.simplificar(new Cociente(e, xp));
        };
    }
}
