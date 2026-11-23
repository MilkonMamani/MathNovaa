package pe.mathnova.domain.expresion;

import java.util.Objects;

/**
 * lim((x, y) → (a, b), f) (HU29, RF54):
 * {@code <limite_doble> ::= "lim" "(" "(" <variable> "," <variable> ")" <flecha> "(" <punto_lim> "," <punto_lim> ")"
 * "," <expresion> ")"}. Como las demás operaciones, va solo en la raíz del árbol.
 *
 * @param a el punto al que tiende x: un número, una fracción o un múltiplo de π (RN46)
 * @param b el punto al que tiende y
 */
public record LimiteDoble(char x, char y, Expresion a, Expresion b, Expresion cuerpo) implements Expresion {

    public LimiteDoble {
        if (!Letras.esVariable(x) || !Letras.esVariable(y)) {
            throw new IllegalArgumentException("Letra no permitida como variable");
        }
        if (x == y) {
            throw new IllegalArgumentException("Las dos variables del límite doble deben ser distintas");
        }
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        Objects.requireNonNull(cuerpo, "cuerpo");
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
