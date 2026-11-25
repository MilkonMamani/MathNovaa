package pe.mathnova.domain.sugerencias;

import java.util.List;

final class AsintotasDeRacional implements ReglaSugerencia {

    @Override
    public String codigo() {
        return "RN19";
    }

    @Override
    public List<Sugerencia> aplica(EjercicioResuelto r) {
        if (!r.esFraccion() || r.gradoAbajo() < 1) {
            return List.of();
        }
        String plantilla = r.texto();
        return List.of(new Sugerencia(codigo(), "ASINTOTAS", "Asíntotas",
                "Es una función racional con denominador no constante: tiene asíntotas verticales donde se anula "
                        + "el denominador simplificado, y la horizontal u oblicua sale de comparar los grados.",
                plantilla, Reglas.latex(plantilla), true));
    }
}
