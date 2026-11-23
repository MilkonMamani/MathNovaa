package pe.mathnova.domain.expresion;

import java.util.Objects;

/** e, π o la unidad imaginaria i. */
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
