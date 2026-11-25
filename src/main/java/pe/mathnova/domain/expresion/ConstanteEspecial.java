package pe.mathnova.domain.expresion;

import java.util.Objects;

public record ConstanteEspecial(Tipo tipo) implements Expresion {

    public enum Tipo { E, PI, I }

    public ConstanteEspecial {
        Objects.requireNonNull(tipo, "tipo");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
