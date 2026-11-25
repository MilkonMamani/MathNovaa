package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Raiz(int indice, Expresion radicando) implements Expresion {

    public Raiz {
        if (indice < 2) {
            throw new IllegalArgumentException("El índice de una raíz debe ser al menos 2");
        }
        Objects.requireNonNull(radicando, "radicando");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
