package pe.mathnova.domain.expresion;

import java.util.Objects;

public record LimiteDoble(char x, char y, Expresion a, Expresion b, Expresion cuerpo) implements Expresion {

    public LimiteDoble {
        if (!Letras.esVariable(x) || !Letras.esVariable(y)) {
            throw new IllegalArgumentException("Letra no permitida como variable");
        }
        if (x == y) {
            throw new IllegalArgumentException("Las dos variables del límite doble deben ser distintas");
        }
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(cuerpo, "cuerpo");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
