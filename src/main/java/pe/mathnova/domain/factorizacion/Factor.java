package pe.mathnova.domain.factorizacion;

import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;

public record Factor(Polinomio polinomio, int multiplicidad) {

    public Factor {
        Objects.requireNonNull(polinomio, "polinomio");
        if (multiplicidad < 1) {
            throw new IllegalArgumentException("La multiplicidad debe ser al menos 1");
        }
    }

    public static Factor de(Polinomio p) {
        return new Factor(p, 1);
    }

    public Polinomio expandir() {
        return polinomio.potencia(multiplicidad);
    }
}
