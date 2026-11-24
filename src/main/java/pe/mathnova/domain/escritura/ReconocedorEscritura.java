package pe.mathnova.domain.escritura;

import java.util.List;

/**
 * Lee lo que el estudiante escribió a mano en la pestaña "Escribir" y lo devuelve como LaTeX. Recibe
 * los trazos como listas de puntos (no una imagen), en el orden en que se dibujaron.
 *
 * <p>Se llama fuera del hilo de la interfaz y puede tardar; si el hilo se interrumpe, la lectura se
 * abandona.
 */
public interface ReconocedorEscritura {

    ResultadoReconocimiento reconocer(List<Trazo> trazos);

    /**
     * Deja el reconocedor listo para leer (por ejemplo, carga el modelo). Bloquea: se llama fuera del hilo
     * de la interfaz, al abrir "Escribir". Los que no necesitan preparación no hacen nada.
     */
    default void preparar() {
    }

    /** false mientras {@link #preparar} no terminó: la primera lectura tardará más. */
    default boolean listo() {
        return true;
    }
}
