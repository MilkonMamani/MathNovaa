package pe.mathnova.domain.sugerencias;

import java.util.List;

final class DerivadaDeFunciones implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN18";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esFunciones()) {
            return List.of();
        }
        var sustitucion = r.funciones().sustitucion();
        String con = sustitucion == null ? "" : " (con " + sustitucion.definicion() + ")";
        String plantilla = "d/dx(" + r.texto() + ")";
        return List.of(new Sugerencia(codigo(), "PUNTOS_CRITICOS", "Derivada y puntos críticos",
                "Es polinómica en sus funciones" + con + ": se deriva con la regla de la cadena y f′(x) = 0 "
                        + "da los puntos críticos.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
