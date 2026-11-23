package pe.mathnova.domain.expresion;

/** Una letra que hace de variable o de constante literal (RF57): a-z sin e ni i, α-ω sin π. */
public record Variable(char nombre) implements Expresion {

    public Variable {
        if (!Letras.esVariable(nombre)) {
            throw new IllegalArgumentException("Letra no permitida como variable: " + nombre);
        }
    }

    @Override
    public <R> R aceptar(Visitante<R> v) {
        return v.visitar(this);
    }
}
