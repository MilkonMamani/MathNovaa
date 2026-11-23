package pe.mathnova.domain.factorizacion;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;

/**
 * Parte E: si la forma en R o C es igual a la entrada. Con coeficientes exactos (sin radicales
 * anidados) se expande y se compara; si no, se evalúan entrada y resultado en cinco puntos con
 * {@value EvaluacionNumerica#CIFRAS} cifras y deben coincidir en {@value EvaluacionNumerica#CIFRAS_IGUALES}.
 *
 * @param numerica true si se comparó con números y no expandiendo
 */
public record VerificacionCampo(boolean coincide, boolean numerica) {

    public static VerificacionCampo de(Resolucion r) {
        Polinomio entrada = Polinomio.desde(r.entrada());
        if (r.extension() == null || r.extension().pasos().isEmpty()) {
            return new VerificacionCampo(r.resultado().expandir().equals(entrada), false);
        }
        FormaExtendida forma = r.extension().resultado();
        return de(entrada, forma);
    }

    public static VerificacionCampo de(Polinomio entrada, FormaExtendida forma) {
        if (forma.exacta() && entrada.variables().size() <= 1) {
            return new VerificacionCampo(forma.expandir().equals(PolinomioAlgebraico.desde(entrada)), false);
        }
        char x = entrada.variables().isEmpty() ? 'x' : entrada.variables().first();
        boolean igual;
        try {
            igual = EvaluacionNumerica.iguales(entrada.aExpresion(), forma.aExpresion(), x);
        } catch (ArithmeticException e) {
            igual = false;
        }
        return new VerificacionCampo(igual, true);
    }
}
