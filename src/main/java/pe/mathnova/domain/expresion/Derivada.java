package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Derivada(int orden, char variable, Expresion cuerpo) implements Expresion {

    public Derivada {
        if (orden < 1) {
            throw new IllegalArgumentException("El orden de la derivada debe ser al menos 1");
        }
        if (!Letras.esVariable(variable)) {
            throw new IllegalArgumentException("Letra no permitida como variable: " + variable);
        }
        Objects.requireNonNull(cuerpo, "cuerpo");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
