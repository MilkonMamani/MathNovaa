package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;
import pe.mathnova.domain.parser.Parser;

/**
 * Forma final en R o C: constante racional × factores con coeficientes {@link NumeroAlgebraico},
 * como 2(x − 1 + √6/2)(x − 1 − √6/2). Los factores que salen de partir uno de Q son mónicos; los
 * que no se partieron quedan como estaban en Q.
 */
public record FormaExtendida(Racional constante, List<Factor> factores) {

    /** Un factor con su multiplicidad. {@code polinomio} es exacto o lleva átomos (anidados). */
    public record Factor(PolinomioAlgebraico polinomio, int multiplicidad) {
        public Factor {
            Objects.requireNonNull(polinomio, "polinomio");
            if (multiplicidad < 1) {
                throw new IllegalArgumentException("La multiplicidad debe ser al menos 1");
            }
        }
    }

    public FormaExtendida {
        Objects.requireNonNull(constante, "constante");
        factores = List.copyOf(factores);
    }

    /** La forma de Q tal cual: sin partir nada. */
    public static FormaExtendida de(FormaFactorizada q) {
        List<Factor> l = new ArrayList<>();
        for (var f : q.factores()) {
            l.add(new Factor(PolinomioAlgebraico.desde(f.polinomio()), f.multiplicidad()));
        }
        return new FormaExtendida(q.constante(), l);
    }

    /** Si todos los coeficientes son exactos (sin radicales anidados ni valores trigonométricos). */
    public boolean exacta() {
        return factores.stream().allMatch(f -> f.polinomio().exacto());
    }

    /** constante × Π factorᵐ, con las cuentas exactas o formales. */
    public PolinomioAlgebraico expandir() {
        char v = factores.isEmpty() ? 'x' : factores.getFirst().polinomio().variable();
        PolinomioAlgebraico r = PolinomioAlgebraico.constante(v, NumeroAlgebraico.de(constante));
        for (Factor f : factores) {
            r = r.multiplicar(f.polinomio().potencia(f.multiplicidad()));
        }
        return r;
    }

    /** "(x - √2)(x + √2)", "2(x - 1 + √6/2)(x - 1 - √6/2)", "-(x + 2·i)(x - 2·i)". */
    public String aTexto() {
        if (factores.isEmpty()) {
            return constante.toString();
        }
        StringBuilder sb = new StringBuilder();
        if (constante.equals(Racional.UNO.negar())) {
            sb.append('-');
        } else if (!constante.equals(Racional.UNO)) {
            sb.append(constante.esEntero() ? constante.toString() : "(" + constante + ")");
        }
        boolean solo = factores.size() == 1 && factores.getFirst().multiplicidad() == 1 && sb.isEmpty();
        for (Factor f : factores) {
            String t = f.polinomio().toString();
            boolean monomio = f.polinomio().coeficientes().stream().filter(c -> !c.esCero()).count() == 1
                    && f.polinomio().principal().equals(NumeroAlgebraico.UNO);
            if (solo || monomio && f.multiplicidad() == 1) {
                sb.append(t);
            } else {
                sb.append('(').append(t).append(')');
            }
            if (f.multiplicidad() > 1) {
                sb.append('^').append(f.multiplicidad());
            }
        }
        return sb.toString();
    }

    public Expresion aExpresion() {
        return Parser.parse(aTexto());
    }

    /** Los factores que siguen siendo de Q (coeficientes racionales), para la nota de irreducibles. */
    List<Polinomio> factoresDeQ() {
        return factores.stream().filter(f -> f.polinomio().coeficientesRacionales())
                .map(f -> f.polinomio().aPolinomio()).toList();
    }
}
