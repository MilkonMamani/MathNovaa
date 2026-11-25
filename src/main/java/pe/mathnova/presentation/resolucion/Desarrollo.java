package pe.mathnova.presentation.resolucion;

import java.util.List;

public record Desarrollo(List<Renglon> renglones) {

    public Desarrollo {
        renglones = List.copyOf(renglones);
    }

    public sealed interface Renglon permits Texto, Formula, Definicion {
    }

    public record Texto(String texto) implements Renglon {
    }

    public record Formula(String latex) implements Renglon {
    }

    public record Definicion(String texto, String latex, String accion, List<String> detalle) implements Renglon {
        public Definicion {
            detalle = List.copyOf(detalle);
        }
    }
}
