package pe.mathnova.domain.calculo.limites;

/** Las siete formas indeterminadas (RN21, RN35). */
public enum Indeterminacion {
    CERO_SOBRE_CERO("0/0"),
    INFINITO_SOBRE_INFINITO("∞/∞"),
    INFINITO_MENOS_INFINITO("∞ − ∞"),
    CERO_POR_INFINITO("0·∞"),
    UNO_A_LA_INFINITO("1^∞"),
    CERO_A_LA_CERO("0⁰"),
    INFINITO_A_LA_CERO("∞⁰");

    private final String texto;

    Indeterminacion(String texto) {
        this.texto = texto;
    }

    /** "0/0", "∞ − ∞"… */
    public String texto() {
        return texto;
    }

    /** Las que admite L'Hôpital sin transformar (RN35). */
    public boolean esCociente() {
        return this == CERO_SOBRE_CERO || this == INFINITO_SOBRE_INFINITO;
    }
}
