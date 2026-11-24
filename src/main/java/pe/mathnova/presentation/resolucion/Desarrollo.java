package pe.mathnova.presentation.resolucion;

import java.util.List;

/**
 * Lo que se escribe en la Pizarra para un paso largo (Cardano), renglón por renglón: las explicaciones en
 * español van aparte de las fórmulas, y cada fórmula lleva una sola operación. Sin JavaFX: lo dibuja
 * {@link VistaDesarrollo}.
 */
public record Desarrollo(List<Renglon> renglones) {

    public Desarrollo {
        renglones = List.copyOf(renglones);
    }

    /** Un renglón de la pizarra. */
    public sealed interface Renglon permits Texto, Formula, Definicion {
    }

    /** Una frase en español ("Discriminante:"). */
    public record Texto(String texto) implements Renglon {
    }

    /** Una fórmula en LaTeX. */
    public record Formula(String latex) implements Renglon {
    }

    /**
     * Un símbolo que se define una vez y se reutiliza ("r = u + v − 8/9 ≈ −3.0717"): junto a la fórmula, el
     * control {@code accion} ("Ver valor exacto de r") despliega {@code detalle}.
     *
     * @param texto   la frase antes de la fórmula, o null
     * @param latex   la definición
     * @param accion  el texto del control que despliega el detalle
     * @param detalle los renglones exactos (LaTeX), uno por operación
     */
    public record Definicion(String texto, String latex, String accion, List<String> detalle) implements Renglon {
        public Definicion {
            detalle = List.copyOf(detalle);
        }
    }
}
