package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

public record Extension(Campo campo, List<Paso> pasos, FormaExtendida resultado, List<String> notas,
                        List<String> aproximadas) {

    public Extension {
        Objects.requireNonNull(campo, "campo");
        Objects.requireNonNull(resultado, "resultado");
        pasos = List.copyOf(pasos);
        notas = List.copyOf(notas);
        aproximadas = List.copyOf(aproximadas);
    }

    public record Paso(int orden, String codigoTecnica, String justificacion, Expresion antes, Expresion despues,
                       TrabajoIntermedio trabajo) {

        public Paso {
            Objects.requireNonNull(codigoTecnica, "codigoTecnica");
            Objects.requireNonNull(justificacion, "justificacion");
        }

        public String nombreTecnica() {
            return Tecnicas.nombre(codigoTecnica);
        }
    }

    public boolean aproximada() {
        return !aproximadas.isEmpty();
    }

    public String texto(boolean irreducibleEnQ) {
        if (!aproximada()) {
            return resultado.aTexto();
        }
        String raices = (campo == Campo.R ? "raíces reales ≈ " : "raíces ≈ ") + String.join(", ", aproximadas);
        return (irreducibleEnQ ? "Irreducible en Q" : resultado.aTexto()) + "; " + raices;
    }
}
