package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Extremos(Expresion funcion, Ecuacion restriccion) implements Expresion {

    public Extremos {
        Objects.requireNonNull(funcion, "funcion");
    }

    public Extremos(Expresion funcion) {
        this(funcion, null);
    }

    public boolean conRestriccion() {
        return restriccion != null;
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
