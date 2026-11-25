package pe.mathnova.domain.expresion;

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
