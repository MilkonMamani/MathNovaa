package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Limite;

/**
 * Un límite resuelto (HU11, HU24).
 *
 * @param pasos           en el orden en que se aplicaron, con sus pasos anidados
 * @param indeterminacion la primera que se encontró, o null si la evaluación directa decidió
 * @param metodoFinal     el método que dio el resultado ("L02"), o null si no se pudo calcular
 */
public record ResolucionLimite(Limite entrada, ResultadoLimite resultado, List<PasoLimite> pasos,
                               Indeterminacion indeterminacion, String metodoFinal) {

    public ResolucionLimite {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
    }

    /** Todos los códigos, con los de los pasos anidados (L01, L02, F05, L01…). */
    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }

    public char variable() {
        return entrada.variable();
    }
}
