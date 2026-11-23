package pe.mathnova.domain.expresion;

/** Funciones que acepta el parser (HU22). LOG lleva base; las demás no. */
public enum TipoFuncion {
    SEN("sen"), COS("cos"), TAN("tan"), COT("cot"), SEC("sec"), CSC("csc"),
    ARCSEN("arcsen"), ARCCOS("arccos"), ARCTAN("arctan"),
    /** arcsec u = arccos(1/u): la da la sustitución x = a·sec θ (Sprint 7). */
    ARCSEC("arcsec"),
    LN("ln"), LOG("log"), EXP("exp"), SQRT("sqrt"),
    /** |u|: solo lo produce el integrador (ln|u|, RN23); el parser no lo acepta (RF62). */
    ABS("abs");

    private final String nombre;

    TipoFuncion(String nombre) {
        this.nombre = nombre;
    }

    /** Nombre canónico en español, como se escribe en el banco ("sen", "ln"). */
    public String nombre() {
        return nombre;
    }
}
