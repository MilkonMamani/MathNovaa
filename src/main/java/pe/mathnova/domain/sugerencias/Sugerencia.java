package pe.mathnova.domain.sugerencias;

import java.util.Objects;

public record Sugerencia(String regla, String operacion, String titulo, String justificacion, String plantilla,
                         String plantillaLatex, boolean disponible) {

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
