package pe.mathnova.domain.sugerencias;

import java.util.List;

public interface ReglaSugerencia {

    String codigo();

    default boolean variasVariables() {
        return false;
    }

    List<Sugerencia> aplica(EjercicioResuelto resuelto);
}
