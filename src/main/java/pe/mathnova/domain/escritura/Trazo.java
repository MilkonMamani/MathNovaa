package pe.mathnova.domain.escritura;

import java.util.List;

public record Trazo(List<Punto> puntos) {

    public Trazo {
        puntos = List.copyOf(puntos);
    }
}
