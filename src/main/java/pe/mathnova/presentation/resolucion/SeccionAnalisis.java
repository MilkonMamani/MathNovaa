package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Objects;

/**
 * Una sección de la pestaña Análisis (Sprint 6), sin JavaFX: un título, una tabla pequeña y, si hay, una
 * recta de signos con su rótulo. La usan las derivadas (puntos críticos, crecimiento, concavidad) y las
 * integrales (verificación, regla de Simpson).
 *
 * @param marcas       los puntos de la recta, de menor a mayor
 * @param signos       +1 o −1 por tramo (uno más que marcas); vacío si no hay recta
 * @param rotuloSignos "Signo de f′(x)"; null si no hay recta
 * @param aviso        una nota en gris (por qué no hay recta, por ejemplo), o null
 */
public record SeccionAnalisis(String titulo, List<VistaPropiedades.Fila> filas, List<String> marcas,
                              List<Integer> signos, String rotuloSignos, String aviso) {

    public SeccionAnalisis {
        Objects.requireNonNull(titulo, "titulo");
        filas = List.copyOf(filas);
        marcas = List.copyOf(marcas);
        signos = List.copyOf(signos);
    }

    public SeccionAnalisis(String titulo, List<VistaPropiedades.Fila> filas) {
        this(titulo, filas, List.of(), List.of(), null, null);
    }
}
