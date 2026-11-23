package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * {@code <ecuacion> ::= <expresion> "=" <expresion>} (HU38). Como las operaciones, solo va en la
 * raíz del árbol y lleva un solo "=".
 */
public record Ecuacion(Expresion izquierda, Expresion derecha) implements Expresion {

    public Ecuacion {
        Objects.requireNonNull(izquierda, "izquierda");
        Objects.requireNonNull(derecha, "derecha");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
