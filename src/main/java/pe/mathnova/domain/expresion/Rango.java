package pe.mathnova.domain.expresion;

import java.util.Objects;

/** Variable de integración con sus límites, dentro de una IntegralMultiple. */
public record Rango(char variable, Expresion inferior, Expresion superior) {

    public Rango {
        if (!Letras.esVariable(variable)) {
            throw new IllegalArgumentException("Letra no permitida como variable: " + variable);
        }
        Objects.requireNonNull(inferior, "inferior");
        Objects.requireNonNull(superior, "superior");
    }
}
