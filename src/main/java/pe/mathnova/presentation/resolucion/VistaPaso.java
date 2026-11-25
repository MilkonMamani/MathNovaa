package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

public record VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                        Expresion antes, TrabajoIntermedio trabajo, Expresion despues, String grupo,
                        List<VistaPaso> hijos, Desarrollo desarrollo) {

    public VistaPaso {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(titulo, "titulo");
        Objects.requireNonNull(justificacion, "justificacion");
        hijos = List.copyOf(hijos);
    }

    public VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                     Expresion antes, TrabajoIntermedio trabajo, Expresion despues, String grupo,
                     List<VistaPaso> hijos) {
        this(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, grupo, hijos, null);
    }

    public VistaPaso conDesarrollo(Desarrollo d) {
        return new VistaPaso(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, grupo, hijos, d);
    }

    public VistaPaso(int orden, String codigo, String titulo, String ayuda, String justificacion,
                     Expresion antes, TrabajoIntermedio trabajo, Expresion despues) {
        this(orden, codigo, titulo, ayuda, justificacion, antes, trabajo, despues, null, List.of());
    }
}
