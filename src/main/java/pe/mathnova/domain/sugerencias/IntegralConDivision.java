package pe.mathnova.domain.sugerencias;

import java.util.List;

/**
 * RN17: fracción impropia: la integral empieza por la división. También cuando al simplificar se cancela
 * todo el denominador (Sprint 6).
 */
final class IntegralConDivision implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN17";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esFraccion()) {
            return List.of();
        }
        int n = r.gradoArriba();
        int m = r.gradoAbajo();
        String plantilla = "∫(" + r.texto() + ", " + r.variable() + ")";
        if (m < 1) {
            // Sprint 6: al simplificar se canceló todo el denominador
            return List.of(new Sugerencia(codigo(), "INTEGRAR", "Integral (queda un polinomio)",
                    "Al simplificar se cancela el denominador: se dividen los polinomios y lo que queda se integra "
                            + "término a término.",
                    plantilla, Reglas.latex(plantilla), true));
        }
        if (n < m) {
            return List.of();
        }
        return List.of(new Sugerencia(codigo(), "INTEGRAR", "Integral (primero se divide)",
                "La fracción es impropia (grado " + n + " ≥ " + m + "): antes de integrar se dividen los "
                        + "polinomios y queda un polinomio más una fracción propia.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
