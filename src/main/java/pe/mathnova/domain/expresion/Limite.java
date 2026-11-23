package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * lim(variable → punto, cuerpo) (HU34). Solo se reconoce y se dibuja; se resuelve desde el Sprint 5.
 *
 * @param punto un número, una fracción o un múltiplo de π (RN46), o ±∞
 * @param lado  AMBOS para el límite bilateral; un límite al infinito siempre es AMBOS
 */
public record Limite(char variable, Punto punto, Lado lado, Expresion cuerpo) implements Expresion {

    public enum Lado { AMBOS, DERECHA, IZQUIERDA }

    /** Hacia dónde tiende la variable. */
    public sealed interface Punto {
    }

    /** Un valor finito: 2, 1/2, -π/2, 3π. */
    public record Finito(Expresion valor) implements Punto {
        public Finito {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /** +∞ o -∞. */
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
