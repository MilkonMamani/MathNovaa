package pe.mathnova.application.service;

/**
 * Fases reales de una factorización, en el orden en que ocurren. La pantalla de resolución las
 * muestra mientras el cálculo corre fuera del hilo de la interfaz (RNF06).
 */
public enum FaseFactorizacion {
    /** El parser lee el texto. */
    LECTURA,
    /** El motor prueba los métodos F01–F13. */
    METODOS,
    /** Se expande el resultado y se compara con la entrada (HU18). */
    VERIFICACION
}
