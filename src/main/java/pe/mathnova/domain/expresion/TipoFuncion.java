package pe.mathnova.domain.expresion;

public enum TipoFuncion {
    SEN("sen"), COS("cos"), TAN("tan"), COT("cot"), SEC("sec"), CSC("csc"),
    ARCSEN("arcsen"), ARCCOS("arccos"), ARCTAN("arctan"),
    ARCSEC("arcsec"),
    LN("ln"), LOG("log"), EXP("exp"), SQRT("sqrt"),
    ABS("abs");

    private final String nombre;

    TipoFuncion(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
