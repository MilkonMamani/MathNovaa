package pe.mathnova.domain.expresion;

import java.util.Objects;

public record Limite(char variable, Punto punto, Lado lado, Expresion cuerpo) implements Expresion {

    public enum Lado { AMBOS, DERECHA, IZQUIERDA }

    public sealed interface Punto {
    }

    public record Finito(Expresion valor) implements Punto {
        public Finito {
            Objects.requireNonNull(valor, "valor");
        }
    }

    public record Infinito(boolean positivo) implements Punto {
    }

    public Limite {
        if (!Letras.esVariable(variable)) {
            throw new IllegalArgumentException("Letra no permitida como variable: " + variable);
        }
        Objects.requireNonNull(punto, "punto");
        Objects.requireNonNull(lado, "lado");
        Objects.requireNonNull(cuerpo, "cuerpo");
        if (punto instanceof Infinito && lado != Lado.AMBOS) {
            throw new IllegalArgumentException("Un límite al infinito no lleva lado");
        }
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
