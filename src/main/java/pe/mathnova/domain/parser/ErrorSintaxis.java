package pe.mathnova.domain.parser;

/** Expresión mal escrita (RF04). La posición es base 1 sobre el texto que escribió el usuario. */
public class ErrorSintaxis extends RuntimeException {

    private final int posicion;

    public ErrorSintaxis(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    /** Posición base 1 del error; si falta algo al final, es el largo del texto + 1. */
    public int getPosicion() {
        return posicion;
    }
}
