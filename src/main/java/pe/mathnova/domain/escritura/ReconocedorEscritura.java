package pe.mathnova.domain.escritura;

import java.util.List;

public interface ReconocedorEscritura {

    ResultadoReconocimiento reconocer(List<Trazo> trazos);

    default void preparar() {
    }

    default boolean listo() {
        return true;
    }
}
