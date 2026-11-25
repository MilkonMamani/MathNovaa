package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Objects;

public record Gradiente(Expresion cuerpo, List<Expresion> punto) implements Expresion {

    public Gradiente {
        Objects.requireNonNull(cuerpo, "cuerpo");
        punto = List.copyOf(punto);
    }

    public boolean enUnPunto() {
        return !punto.isEmpty();
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
