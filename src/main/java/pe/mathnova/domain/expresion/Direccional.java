package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Objects;

public record Direccional(Expresion cuerpo, List<Expresion> vector, List<Expresion> punto) implements Expresion {

    public Direccional {
        Objects.requireNonNull(cuerpo, "cuerpo");
        vector = List.copyOf(vector);
        punto = List.copyOf(punto);
        if (vector.isEmpty() || punto.isEmpty()) {
            throw new IllegalArgumentException("La derivada direccional necesita un vector y un punto");
        }
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
