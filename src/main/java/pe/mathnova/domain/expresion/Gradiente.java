package pe.mathnova.domain.expresion;

import java.util.List;
import java.util.Objects;

/**
 * ∇f, opcionalmente en un punto (HU28, RF53): {@code <gradiente> ::= "grad" "(" <expresion> [ "," <punto> ] ")"}.
 *
 * @param punto las coordenadas del punto, en el orden x, y, z…; vacío si no se pide en un punto
 */
public record Gradiente(Expresion cuerpo, List<Expresion> punto) implements Expresion {

    public Gradiente {
        Objects.requireNonNull(cuerpo, "cuerpo");
        punto = List.copyOf(punto);
    }

    public boolean enUnPunto() {
        return !punto.isEmpty();
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
