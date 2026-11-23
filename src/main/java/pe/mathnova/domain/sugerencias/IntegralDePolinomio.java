package pe.mathnova.domain.sugerencias;

import java.util.List;

/** RN18: un polinomio se integra término a término, sin factorizar. */
final class IntegralDePolinomio implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN18";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esPolinomio() || r.polinomio().grado() < 1) {
            return List.of();
        }
        String plantilla = "∫(" + r.texto() + ", " + r.variable() + ")";
        return List.of(new Sugerencia(codigo(), "INTEGRAR", "Integral término a término",
                "Es un polinomio: se integra cada término por separado. Para integrarlo no hace falta "
                        + "factorizarlo.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
