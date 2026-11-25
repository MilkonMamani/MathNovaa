package pe.mathnova.domain.expresion;

import java.util.List;

public record Suma(List<Expresion> terminos) implements Expresion {

    public Suma {
        terminos = List.copyOf(terminos);
        if (terminos.size() < 2) {
            throw new IllegalArgumentException("Una suma necesita al menos dos términos");
        }
    }

    public Suma(Expresion... terminos) {
        this(List.of(terminos));
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
