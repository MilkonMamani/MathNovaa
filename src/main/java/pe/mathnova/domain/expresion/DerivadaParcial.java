package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Objects;

public record DerivadaParcial(List<Character> variables, Expresion cuerpo) implements Expresion {

    public DerivadaParcial {
        variables = List.copyOf(variables);
        if (variables.isEmpty()) {
            throw new IllegalArgumentException("Una derivada parcial necesita al menos una variable");
        }
        for (char v : variables) {
            if (!Letras.esVariable(v)) {
                throw new IllegalArgumentException("Letra no permitida como variable: " + v);
            }
        }
        Objects.requireNonNull(cuerpo, "cuerpo");
    }

    public int orden() {
        return variables.size();
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
