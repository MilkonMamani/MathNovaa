package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

/**
 * Lo que se hace en R o en C después de factorizar en Q (RN27): los pasos "Pasar a R/C" y los de
 * F21–F30 y A5, la forma final y por qué los factores que quedan ya no se parten.
 *
 * @param pasos       en orden, empezando por "Pasar a R" o "Pasar a C"; vacío si nada se partió
 * @param resultado   la forma final en el campo
 * @param notas       una frase por factor que no se parte en el campo ("x² + 1: Δ = −4 < 0")
 * @param aproximadas raíces de A5 con ≈ (RN30), ya como texto con 10 decimales; vacío si no hubo
 */
public record Extension(Campo campo, List<Paso> pasos, FormaExtendida resultado, List<String> notas,
                        List<String> aproximadas) {

    public Extension {
        Objects.requireNonNull(campo, "campo");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
        notas = List.copyOf(notas);
        aproximadas = List.copyOf(aproximadas);
    }

    /**
     * Un paso en R o C.
     *
     * @param codigoTecnica "PR", "PC", "F21"…"F30", "A5"
     * @param antes         lo que se parte (un factor de Q o un cuadrático con radicales)
     * @param despues       lo que queda, ya en el campo
     * @param trabajo       los renglones del cálculo (Agrupacion o Comparacion), o null
     */
    public record Paso(int orden, String codigoTecnica, String justificacion, Expresion antes, Expresion despues,
                       TrabajoIntermedio trabajo) {

        public Paso {
            Objects.requireNonNull(codigoTecnica, "codigoTecnica");
            Objects.requireNonNull(justificacion, "justificacion");
        }

        public String nombreTecnica() {
            return Tecnicas.nombre(codigoTecnica);
        }
    }

    /** Si las raíces se aproximaron (A5): el resultado lleva ≈. */
    public boolean aproximada() {
        return !aproximadas.isEmpty();
    }

    /** "Irreducible en Q; raíces reales ≈ 1.1673039783" o la forma en el campo. */
    public String texto(boolean irreducibleEnQ) {
        if (!aproximada()) {
            return resultado.aTexto();
        }
        String raices = (campo == Campo.R ? "raíces reales ≈ " : "raíces ≈ ") + String.join(", ", aproximadas);
        return (irreducibleEnQ ? "Irreducible en Q" : resultado.aTexto()) + "; " + raices;
    }
}
