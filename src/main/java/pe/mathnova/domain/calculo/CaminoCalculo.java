package pe.mathnova.domain.calculo;

import java.util.Objects;

/**
 * Un camino para la pestaña Exploración de una derivada o una integral (Sprint 6): el usado, otro que
 * también sirve (con lo que da) o uno que no aplica (con su motivo).
 *
 * @param codigo "D04", "I02", "EXP"…; con él "Probar" vuelve a resolver empezando por ese camino
 * @param nombre "Regla del producto", "Expandir primero y derivar término a término"
 * @param texto  la vista previa si sirve, el motivo si no aplica, o null
 */
public record CaminoCalculo(String codigo, String nombre, Estado estado, String texto) {

    public enum Estado { USADO, SIRVE, NO_APLICA }

    public CaminoCalculo {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(estado, "estado");
    }
}
