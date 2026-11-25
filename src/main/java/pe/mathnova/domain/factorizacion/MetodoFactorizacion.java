package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;

public interface MetodoFactorizacion {

    String codigo();

    String nombre();

    default boolean esCandidato(Polinomio p) {
        return true;
    }

    String pista();

    Aplicabilidad puedeAplicarse(Polinomio p, Campo c);

    ResultadoMetodo aplicar(Polinomio p, Campo c);

    record Aplicabilidad(boolean aplica, String motivo) {

        public static Aplicabilidad si() {
            return new Aplicabilidad(true, null);
        }

        public static Aplicabilidad no(String motivo) {
            return new Aplicabilidad(false, Objects.requireNonNull(motivo, "motivo"));
        }
    }

    record ResultadoMetodo(List<Factor> factores, Paso paso) {

        public ResultadoMetodo {
            factores = List.copyOf(factores);
            Objects.requireNonNull(paso, "paso");
        }
    }
}
