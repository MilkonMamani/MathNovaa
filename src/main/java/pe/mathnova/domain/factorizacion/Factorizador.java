package pe.mathnova.domain.factorizacion;

/**
 * Contrato del motor de factorización. Java puro: sin Spring, JavaFX ni JPA (RNF15).
 * Devuelve la forma final como texto, en el formato del banco de pruebas (RN12).
 */
public interface Factorizador {

    String factorizar(String entrada, Campo campo);
}
