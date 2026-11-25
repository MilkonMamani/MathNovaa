package pe.mathnova.domain.expresion;

import java.util.List;

public record Producto(List<Expresion> factores) implements Expresion {

    public Producto {
        factores = List.copyOf(factores);
        if (factores.size() < 2) {
            throw new IllegalArgumentException("Un producto necesita al menos dos factores");
        }
    }

    public Producto(Expresion... factores) {
        this(List.of(factores));
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
