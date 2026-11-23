package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * ∫(cuerpo, variable) o ∫(cuerpo, variable, inferior, superior) (HU34).
 * En la indefinida los dos límites son null; en la definida, ninguno lo es.
 */
public record Integral(Expresion cuerpo, char variable, Expresion inferior, Expresion superior)
        implements Expresion {

    public Integral {
        Objects.requireNonNull(cuerpo, "cuerpo");
        if (!Letras.esVariable(variable)) {
            throw new IllegalArgumentException("Letra no permitida como variable: " + variable);
        }
        if ((inferior == null) != (superior == null)) {
            throw new IllegalArgumentException("Una integral definida necesita los dos límites");
        }
    }

    /** Integral indefinida. */
    public Integral(Expresion cuerpo, char variable) {
        this(cuerpo, variable, null, null);
    }

    public boolean esDefinida() {
        return inferior != null;
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
