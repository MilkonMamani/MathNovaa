package pe.mathnova.domain.factorizacion;

import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;

/**
 * Por qué un factor ya no se factoriza en Q (HU07).
 *
 * @param codigoTecnica "A1" (discriminante) o "A4" (verificación de irreducibilidad)
 * @param justificacion "Δ = −3 < 0", "Es de grado 1"…
 * @param certificada   false cuando solo se sabe que F01–F13 no aplican (la certificación completa
 *                      con A4 y Ruffini es del Sprint 3)
 */
public record Irreducibilidad(Polinomio factor, String codigoTecnica, String justificacion, boolean certificada) {

    public Irreducibilidad {
        Objects.requireNonNull(factor, "factor");
        Objects.requireNonNull(codigoTecnica, "codigoTecnica");
        Objects.requireNonNull(justificacion, "justificacion");
    }
}
