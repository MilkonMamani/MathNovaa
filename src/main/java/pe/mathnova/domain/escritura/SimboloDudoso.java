package pe.mathnova.domain.escritura;

import java.util.List;

/**
 * Un símbolo que el reconocedor leyó con poca confianza.
 *
 * @param indice       dónde empieza {@code elegido} dentro del LaTeX del resultado
 * @param elegido      el LaTeX que se puso ("x")
 * @param alternativas lo que también podría ser, incluido {@code elegido} ("x", "\times")
 * @param confianza    de 0 a 1
 */
public record SimboloDudoso(int indice, String elegido, List<String> alternativas, double confianza) {

    public SimboloDudoso {
        alternativas = List.copyOf(alternativas);
    }
}
