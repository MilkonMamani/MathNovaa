package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * Un paso del cálculo de un límite (L01–L07), o uno anidado dentro de él: la factorización del
 * numerador y del denominador en L02 (F01–F20) o las reglas de derivación en L07 (D01–D09).
 *
 * @param codigo        "L01"…"L07", "F05", "D02"…
 * @param titulo        el nombre que se muestra ("Factorizar y cancelar", "Diferencia de cuadrados · numerador")
 * @param justificacion por qué se hace, con los valores concretos
 * @param antes         la expresión de la que se parte, o null
 * @param despues       lo que queda, o null (una evaluación que da una indeterminación)
 * @param trabajo       renglones o dibujo de pizarra, o null
 * @param grupo         el rótulo de los pasos anidados ("Factorizar numerador y denominador"), o null
 * @param hijos         los pasos anidados (paso_padre_id en la tabla paso)
 */
public record PasoLimite(String codigo, String titulo, String justificacion, Expresion antes, Expresion despues,
                         TrabajoIntermedio trabajo, String grupo, List<PasoLimite> hijos) {

    public PasoLimite {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(justificacion, "justificacion");
        hijos = List.copyOf(hijos);
    }

    public PasoLimite(String codigo, String titulo, String justificacion, Expresion antes, Expresion despues,
                      TrabajoIntermedio trabajo) {
        this(codigo, titulo, justificacion, antes, despues, trabajo, null, List.of());
    }

    /** Este código y los de sus pasos anidados, en orden. */
    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        r.add(codigo);
        hijos.forEach(h -> r.addAll(h.codigos()));
        return r;
    }
}
