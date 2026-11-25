package pe.mathnova.domain.factorizacion;

import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;

public record Irreducibilidad(Polinomio factor, String codigoTecnica, String justificacion, boolean certificada) {

    public Irreducibilidad {
        Objects.requireNonNull(factor, "factor");
        Objects.requireNonNull(codigoTecnica, "codigoTecnica");
        Objects.requireNonNull(justificacion, "justificacion");
    }
}
