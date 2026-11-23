package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Los símbolos que se confunden al escribir a mano: x/×, 2/z, 1/l, 5/s, 0/o y el signo menos con la
 * raya de una fracción. Los símbolos van en LaTeX ("\times", "\frac").
 */
public final class TablaConfusiones {

    /** La raya de una fracción, como alternativa del menos. */
    public static final String RAYA_FRACCION = "\\frac";

    private static final List<List<String>> PAREJAS = List.of(
            List.of("x", "\\times"),
            List.of("2", "z"),
            List.of("1", "l"),
            List.of("5", "s"),
            List.of("0", "o"),
            List.of("-", RAYA_FRACCION));

    /** Cómo se escribe cada símbolo en un botón. */
    private static final Map<String, String> A_LA_VISTA = Map.of(
            "\\times", "×",
            "-", "−",
            RAYA_FRACCION, "raya de fracción");

    private TablaConfusiones() {
    }

    /**
     * El símbolo y aquello con lo que se confunde, empezando por él mismo; solo él si no se confunde
     * con nada.
     */
    public static List<String> alternativas(String simbolo) {
        List<String> r = new ArrayList<>(List.of(simbolo));
        for (List<String> pareja : PAREJAS) {
            if (pareja.contains(simbolo)) {
                pareja.stream().filter(s -> !s.equals(simbolo)).forEach(r::add);
            }
        }
        return List.copyOf(r);
    }

    /** "×" para "\times"; los demás, igual. */
    public static String aLaVista(String simbolo) {
        return A_LA_VISTA.getOrDefault(simbolo, simbolo);
    }
}
