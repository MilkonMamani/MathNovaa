package pe.mathnova.domain.expresion;

import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;

public record Constante(Racional valor) implements Expresion {

    public Constante {
        Objects.requireNonNull(valor, "valor");
    }

    public static Constante de(long n) {
        return new Constante(Racional.de(n));
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
