package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.calculo.limites.TablaValores;

/**
 * La tabla de valores de un límite tal como se ve en Análisis (Sprint 5, Parte F), sin JavaFX: dos
 * columnas, "por la izquierda" y "por la derecha", cada una ordenada de modo que la última fila es la
 * más cercana al punto (x = 1.9, 1.99, 1.999 y x = 2.1, 2.01, 2.001), y una fila final "→ 4" en verde.
 * Al infinito solo hay una columna (x = 10, 100, …, 10⁶).
 *
 * @param izquierda las filas que se acercan por la izquierda (vacía al infinito)
 * @param derecha   las que se acercan por la derecha, o hacia ±∞
 * @param tiende    la fila final, "→ 4", "→ +∞" o "no existe"; null si el límite no se calculó
 */
public record TablaLimite(char variable, List<TablaValores.Fila> izquierda, List<TablaValores.Fila> derecha,
                          String tiende, String conclusion) {

    public TablaLimite {
        izquierda = List.copyOf(izquierda);
        derecha = List.copyOf(derecha);
    }

    public static TablaLimite de(TablaValores t, ResultadoLimite r, String conclusion) {
        List<TablaValores.Fila> derecha = new ArrayList<>(t.derecha());
        if (!t.izquierda().isEmpty()) {
            // El motor da a + 0.001, a + 0.01, a + 0.1: se muestra acercándose (la última, la más cercana).
            java.util.Collections.reverse(derecha);
        }
        String tiende = switch (r) {
            case ResultadoLimite.Finito f -> "→ " + f.texto();
            case ResultadoLimite.Infinito i -> "→ " + i.texto();
            case ResultadoLimite.NoExiste n -> "no existe";
            case ResultadoLimite.NoCalculado n -> null;
        };
        return new TablaLimite(t.variable(), t.izquierda(), derecha, tiende, conclusion);
    }

    public boolean alInfinito() {
        return izquierda.isEmpty();
    }

    /** Todas las filas: primero la izquierda, después la derecha. */
    public List<TablaValores.Fila> filas() {
        List<TablaValores.Fila> r = new ArrayList<>(izquierda);
        r.addAll(derecha);
        return r;
    }
}
