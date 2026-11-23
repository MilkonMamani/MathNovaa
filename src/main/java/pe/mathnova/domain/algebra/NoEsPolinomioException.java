package pe.mathnova.domain.algebra;

/** La expresión no se puede escribir como polinomio con coeficientes racionales. */
public class NoEsPolinomioException extends RuntimeException {

    public NoEsPolinomioException(String mensaje) {
        super(mensaje);
    }
}
