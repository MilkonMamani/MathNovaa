package pe.mathnova.domain.escritura;

import java.util.List;

/** Lo que se dibuja entre apoyar y levantar el lápiz. */
public record Trazo(List<Punto> puntos) {

    public Trazo {
        puntos = List.copyOf(puntos);
    }
}
