package pe.mathnova.domain.sugerencias;

import java.util.List;

/**
 * Una regla de sugerencia (patrón Strategy): mira lo que se resolvió y, si aplica, propone una o más
 * operaciones con su justificación (RF21, RF22). RN15, por ejemplo, da una tarjeta por cada valor que
 * anula el numerador y el denominador.
 */
public interface ReglaSugerencia {

    /** "RN15"… */
    String codigo();

    /**
     * true si la regla es para expresiones de dos o más variables (RN41); las demás son de una letra y no
     * se les pasan las de varias.
     */
    default boolean variasVariables() {
        return false;
    }

    /** Vacía si la regla no aplica. */
    List<Sugerencia> aplica(EjercicioResuelto resuelto);
}
