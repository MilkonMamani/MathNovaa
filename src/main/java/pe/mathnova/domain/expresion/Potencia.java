package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Potencia(Expresion base, Expresion exponente) implements Expresion {

    public Potencia {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(exponente, "exponente");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
