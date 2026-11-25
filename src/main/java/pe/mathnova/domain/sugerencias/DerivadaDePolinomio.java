package pe.mathnova.domain.sugerencias;

import java.util.List;

final class DerivadaDePolinomio implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN18";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esPolinomio() || r.polinomio().grado() < 1) {
            return List.of();
        }
        char x = r.variable();
        String plantilla = "d/d" + x + "(" + r.texto() + ")";
        return List.of(new Sugerencia(codigo(), "PUNTOS_CRITICOS", "Derivada y puntos críticos",
                "Es un polinomio: se deriva término a término y f′(" + x + ") = 0 da los puntos críticos. "
                        + "Para derivarlo no hace falta factorizarlo.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
