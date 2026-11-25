package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Negacion(Expresion argumento) implements Expresion {

    public Negacion {
        Objects.requireNonNull(argumento, "argumento");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
