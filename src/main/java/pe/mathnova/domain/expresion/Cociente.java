package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Cociente(Expresion numerador, Expresion denominador) implements Expresion {

    public Cociente {
        Objects.requireNonNull(numerador, "numerador");
        Objects.requireNonNull(denominador, "denominador");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
