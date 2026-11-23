package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Objects;

/**
 * Derivada direccional D_u f(P) (HU28, RF53):
 * {@code <direccional> ::= "Du" "(" <expresion> "," <vector> "," <punto> ")"}. Si el vector no es unitario
 * se normaliza al resolver.
 */
public record Direccional(Expresion cuerpo, List<Expresion> vector, List<Expresion> punto) implements Expresion {

    public Direccional {
        Objects.requireNonNull(cuerpo, "cuerpo");
        vector = List.copyOf(vector);
        punto = List.copyOf(punto);
        if (vector.isEmpty() || punto.isEmpty()) {
            throw new IllegalArgumentException("La derivada direccional necesita un vector y un punto");
        }
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
