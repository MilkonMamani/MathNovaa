package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * Puntos críticos y extremos de f (HU30, RF55):
 * {@code <extremos> ::= "extremos" "(" <expresion> [ ";" <ecuacion> ] ")"}. Sin restricción se clasifican con
 * el criterio del Hessiano (V07); con "; g = c" se usan los multiplicadores de Lagrange (V08).
 *
 * @param restriccion la ecuación g = c, o null si no hay
 */
public record Extremos(Expresion funcion, Ecuacion restriccion) implements Expresion {

    public Extremos {
        Objects.requireNonNull(funcion, "funcion");
    }

    public Extremos(Expresion funcion) {
        this(funcion, null);
    }

    public boolean conRestriccion() {
        return restriccion != null;
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
