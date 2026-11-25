package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Limite;

public record ResolucionLimite(Limite entrada, ResultadoLimite resultado, List<PasoLimite> pasos,
                               Indeterminacion indeterminacion, String metodoFinal) {

    public ResolucionLimite {
        Objects.requireNonNull(entrada, "entrada");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
    }

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        pasos.forEach(p -> r.addAll(p.codigos()));
        return r;
    }

    public char variable() {
        return entrada.variable();
    }
}
