package pe.mathnova.domain.calculo.limites;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

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

    public List<String> codigos() {
        List<String> r = new ArrayList<>();
        r.add(codigo);
        hijos.forEach(h -> r.addAll(h.codigos()));
        return r;
    }
}
