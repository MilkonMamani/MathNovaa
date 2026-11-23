package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Polinomio;

/** Un método de factorización (patrón Strategy). Lo encadena OrquestadorFactorizacion. */
public interface MetodoFactorizacion {

    /** "F05". */
    String codigo();

    /** "Diferencia de cuadrados". */
    String nombre();

    /**
     * Si tiene sentido probar el método según la forma del polinomio (número de términos, grado,
     * letras). El orquestador lo consulta antes de {@link #puedeAplicarse}: lo que no es candidato ni
     * se prueba ni aparece entre los descartados (HU05).
     */
    default boolean esCandidato(Polinomio p) {
        return true;
    }

    /** Si aplica; si no, el motivo legible ("x² + 4 es una suma, no una diferencia"). */
    Aplicabilidad puedeAplicarse(Polinomio p, Campo c);

    /**
     * @throws IllegalArgumentException si el método no aplica, con el motivo
     */
    ResultadoMetodo aplicar(Polinomio p, Campo c);

    /** Respuesta de puedeAplicarse. */
    record Aplicabilidad(boolean aplica, String motivo) {

        public static Aplicabilidad si() {
            return new Aplicabilidad(true, null);
        }

        public static Aplicabilidad no(String motivo) {
            return new Aplicabilidad(false, Objects.requireNonNull(motivo, "motivo"));
        }
    }

    /** Lo que deja un método: los factores (en el orden en que salen) y el paso que lo explica. */
    record ResultadoMetodo(List<Factor> factores, Paso paso) {

        public ResultadoMetodo {
            factores = List.copyOf(factores);
            Objects.requireNonNull(paso, "paso");
        }
    }
}
