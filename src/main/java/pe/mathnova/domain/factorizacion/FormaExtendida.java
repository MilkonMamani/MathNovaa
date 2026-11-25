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

public record FormaExtendida(Racional constante, List<Factor> factores) {

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

    public static FormaExtendida de(FormaFactorizada q) {
        List<Factor> l = new ArrayList<>();
        for (var f : q.factores()) {
            l.add(new Factor(PolinomioAlgebraico.desde(f.polinomio()), f.multiplicidad()));
        }
        return new FormaExtendida(q.constante(), l);
    }

    public boolean exacta() {
        return factores.stream().allMatch(f -> f.polinomio().exacto());
    }

    public PolinomioAlgebraico expandir() {
        char v = factores.isEmpty() ? 'x' : factores.getFirst().polinomio().variable();
        PolinomioAlgebraico r = PolinomioAlgebraico.constante(v, NumeroAlgebraico.de(constante));
        for (Factor f : factores) {
            r = r.multiplicar(f.polinomio().potencia(f.multiplicidad()));
        }
        return r;
    }

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

    List<Polinomio> factoresDeQ() {
        return factores.stream().filter(f -> f.polinomio().coeficientesRacionales())
                .map(f -> f.polinomio().aPolinomio()).toList();
    }
}
