package pe.mathnova.domain.escritura;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TablaConfusiones {

    public static final String RAYA_FRACCION = "\\frac";

    private static final List<List<String>> PAREJAS = List.of(
            List.of("x", "\\times"),
            List.of("2", "z"),
            List.of("1", "l"),
            List.of("5", "s"),
            List.of("0", "o"),
            List.of("-", RAYA_FRACCION));

    private static final Map<String, String> A_LA_VISTA = Map.of(
            "\\times", "×",
            "-", "−",
            RAYA_FRACCION, "raya de fracción");

    private TablaConfusiones() {
    }

    public static List<String> alternativas(String simbolo) {
        List<String> r = new ArrayList<>(List.of(simbolo));
        for (List<String> pareja : PAREJAS) {
            if (pareja.contains(simbolo)) {
                pareja.stream().filter(s -> !s.equals(simbolo)).forEach(r::add);
            }
        }
        return List.copyOf(r);
    }

    public static String aLaVista(String simbolo) {
        return A_LA_VISTA.getOrDefault(simbolo, simbolo);
    }
}
