package pe.mathnova.domain.sugerencias;

import java.util.Objects;

/**
 * Una operación que tiene sentido hacer con lo que se acaba de factorizar o simplificar (HU10, RF21,
 * RF22).
 *
 * @param regla          RN15…RN19
 * @param operacion      código de la tabla operacion: LIMITE, ASINTOTAS, INTEGRAR, DERIVAR
 * @param titulo         "Límite cuando x → 2"
 * @param justificacion  una o dos frases: por qué se sugiere
 * @param plantilla      lo que se escribe en el editor, en la sintaxis de la sección 9:
 *                       "lim(x→2, (x^2 - 4)/(x - 2))"
 * @param plantillaLatex lo mismo en LaTeX
 * @param disponible     RN20: false si el sistema todavía no la resuelve (se muestra desactivada)
 */
public record Sugerencia(String regla, String operacion, String titulo, String justificacion, String plantilla,
                         String plantillaLatex, boolean disponible) {

    /** El tooltip de una sugerencia desactivada (RN20). */
    public static final String PROXIMA_VERSION = "Disponible en una próxima versión";

    public Sugerencia {
        Objects.requireNonNull(regla, "regla");
        Objects.requireNonNull(operacion, "operacion");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(justificacion, "justificacion");
        Objects.requireNonNull(plantilla, "plantilla");
        Objects.requireNonNull(plantillaLatex, "plantillaLatex");
    }

    Sugerencia conDisponible(boolean d) {
        return new Sugerencia(regla, operacion, titulo, justificacion, plantilla, plantillaLatex, d);
    }
}
