package pe.mathnova.domain.sugerencias;

import java.util.List;

/**
 * RN16: fracción propia. Con denominador de grado ≥ 2 se integra por fracciones parciales; con
 * denominador lineal la integral es directamente un logaritmo (Sprint 6).
 */
final class FraccionesParciales implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN16";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esFraccion()) {
            return List.of();
        }
        int n = r.gradoArriba();
        int m = r.gradoAbajo();
        if (m < 1 || n >= m) {
            return List.of();
        }
        String plantilla = "∫(" + r.texto() + ", " + r.variable() + ")";
        if (m == 1) {
            return List.of(new Sugerencia(codigo(), "INTEGRAR", "Integral de la fracción",
                    "La fracción es propia y el denominador es lineal (" + r.fraccion().abajo().aTexto()
                            + "): su integral es un logaritmo con valor absoluto, ln|…|.",
                    plantilla, Reglas.latex(plantilla), true));
        }
        return List.of(new Sugerencia(codigo(), "INTEGRAR", "Integral por fracciones parciales",
                "La fracción es propia (grado " + n + " < " + m + ") y el denominador ya está factorizado: "
                        + r.fraccion().abajo().aTexto() + ". Se integra separándola en fracciones parciales.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
