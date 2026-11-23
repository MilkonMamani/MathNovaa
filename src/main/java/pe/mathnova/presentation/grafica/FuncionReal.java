package pe.mathnova.presentation.grafica;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Una función racional p(x)/q(x) evaluada con double, SOLO para trazar la curva. Es el único lugar
 * donde se usa double: vive en presentation, y el dominio sigue siendo exacto (RNF01). Las raíces, el
 * vértice y los cortes no salen de aquí sino del motor (ver {@link DatosGrafica}).
 */
public final class FuncionReal {

    private final double[] numerador;
    private final double[] denominador;
    /** Una expresión con funciones (HU23); null en una función racional. */
    private final java.util.function.DoubleUnaryOperator curva;

    /** Los dos polinomios deben estar en la misma letra (o ser constantes). */
    public FuncionReal(Polinomio numerador, Polinomio denominador, char variable) {
        this.numerador = coeficientes(numerador, variable);
        this.denominador = coeficientes(denominador, variable);
        this.curva = null;
    }

    /** Una curva cualquiera, como sen x o ln x (ver {@link CurvaExpresion}). */
    public FuncionReal(java.util.function.DoubleUnaryOperator curva) {
        this.numerador = new double[] {0};
        this.denominador = new double[] {1};
        this.curva = curva;
    }

    /** c[i] es el coeficiente de x^i. */
    private static double[] coeficientes(Polinomio p, char x) {
        double[] c = new double[(p.esCero() ? 0 : p.grado()) + 1];
        p.terminos().forEach((Monomio m, Racional k) ->
                c[m.exponente(x)] = k.num().doubleValue() / k.den().doubleValue());
        return c;
    }

    private static double horner(double[] c, double x) {
        double r = 0;
        for (int i = c.length - 1; i >= 0; i--) {
            r = r * x + c[i];
        }
        return r;
    }

    /** f(x); NaN o infinito donde el denominador se anula. */
    public double valor(double x) {
        if (curva != null) {
            return curva.applyAsDouble(x);
        }
        return horner(numerador, x) / horner(denominador, x);
    }

    public static double aDouble(Racional r) {
        return r.num().doubleValue() / r.den().doubleValue();
    }
}
