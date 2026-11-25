package pe.mathnova.domain.parser;

public class ErrorSintaxis extends RuntimeException {

    private final int posicion;

    public ErrorSintaxis(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    public int getPosicion() {
        return posicion;
    }
}
