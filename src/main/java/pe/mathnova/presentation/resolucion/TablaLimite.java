package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.calculo.limites.TablaValores;

public record TablaLimite(char variable, List<TablaValores.Fila> izquierda, List<TablaValores.Fila> derecha,
                          String tiende, String conclusion) {

    public TablaLimite {
        izquierda = List.copyOf(izquierda);
        derecha = List.copyOf(derecha);
    }

    public static TablaLimite de(TablaValores t, ResultadoLimite r, String conclusion) {
        List<TablaValores.Fila> derecha = new ArrayList<>(t.derecha());
        if (!t.izquierda().isEmpty()) {
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

    public List<TablaValores.Fila> filas() {
        List<TablaValores.Fila> r = new ArrayList<>(izquierda);
        r.addAll(derecha);
        return r;
    }
}
