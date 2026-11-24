package pe.mathnova.domain.factorizacion;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.NumeroAlgebraico;

/**
 * Los datos de Cardano (F27) de una cúbica a₃x³ + a₂x² + a₁x + a₀ con coeficientes racionales y Δ &gt; 0
 * (una raíz real y dos complejas conjugadas). Son las mismas cuentas de {@link ExtensionCampo}: la
 * sustitución x = t − h, la reducida t³ + pt + q, el discriminante Δ = (q/2)² + (p/3)³, u, v y la raíz
 * real r = u + v − h.
 *
 * <p>También da, como polinomios en una letra r, lo que queda al dividir entre (x − r): el cociente
 * a₃x² + (a₂ + a₃r)x + (a₃r² + a₂r + a₁), su discriminante y el residuo P(r). Con ellos la presentación
 * puede escribir la factorización con el símbolo r sin perder exactitud.
 */
public record CubicaCardano(char variable, Racional a3, Racional a2, Racional a1, Racional a0, Racional h,
                            Racional p, Racional q, Racional delta, NumeroAlgebraico u, NumeroAlgebraico v,
                            NumeroAlgebraico raiz) {

    /** La cúbica de una letra con Δ &gt; 0; vacío si no es cúbica, tiene otra letra o Δ ≤ 0. */
    public static Optional<CubicaCardano> de(Polinomio cubica) {
        if (cubica == null || cubica.variables().size() != 1 || cubica.grado() != 3) {
            return Optional.empty();
        }
        char x = cubica.variables().first();
        Racional c3 = coef(cubica, x, 3);
        Racional c2 = coef(cubica, x, 2);
        Racional c1 = coef(cubica, x, 1);
        Racional c0 = coef(cubica, x, 0);
        Racional a = c2.dividir(c3);
        Racional b = c1.dividir(c3);
        Racional c = c0.dividir(c3);
        Racional h = a.dividir(Racional.de(3));
        Racional pp = b.restar(a.multiplicar(a).dividir(Racional.de(3)));
        Racional qq = Racional.de(2).multiplicar(a.potencia(3)).dividir(Racional.de(27))
                .restar(a.multiplicar(b).dividir(Racional.de(3))).sumar(c);
        Racional delta = qq.multiplicar(qq).dividir(Racional.de(4)).sumar(pp.potencia(3).dividir(Racional.de(27)));
        if (delta.signo() <= 0) {
            return Optional.empty();
        }
        try {
            NumeroAlgebraico sd = NumeroAlgebraico.raiz(2, delta);
            NumeroAlgebraico mq = NumeroAlgebraico.de(qq.negar().multiplicar(Racional.de(1, 2)));
            NumeroAlgebraico u = mq.sumar(sd).raizCubicaReal();
            NumeroAlgebraico v = mq.restar(sd).raizCubicaReal();
            NumeroAlgebraico r = u.sumar(v).sumar(NumeroAlgebraico.de(h.negar()));
            return Optional.of(new CubicaCardano(x, c3, c2, c1, c0, h, pp, qq, delta, u, v, r));
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
    }

    /** La cúbica original. */
    public Polinomio polinomio() {
        return enLetra(variable, a3, a2, a1, a0);
    }

    /** a₂ + a₃r: el coeficiente de x en el cociente. */
    public Polinomio coeficienteLineal(char r) {
        return Polinomio.constante(a2).sumar(Polinomio.variable(r).multiplicar(a3));
    }

    /** a₃r² + a₂r + a₁: el término independiente del cociente. */
    public Polinomio coeficienteConstante(char r) {
        return enLetra(r, Racional.CERO, a3, a2, a1);
    }

    /** El cociente a₃x² + (a₂ + a₃r)x + (a₃r² + a₂r + a₁), en x y r. */
    public Polinomio cociente(char r) {
        Polinomio x = Polinomio.variable(variable);
        return x.multiplicar(x).multiplicar(a3).sumar(coeficienteLineal(r).multiplicar(x)).sumar(coeficienteConstante(r));
    }

    /** P(r) = a₃r³ + a₂r² + a₁r + a₀. */
    public Polinomio residuo(char r) {
        return enLetra(r, a3, a2, a1, a0);
    }

    /** a₃r³ + a₂r² + a₁r, la parte de P(r) que vale −a₀ cuando r es raíz. */
    public Polinomio residuoSinConstante(char r) {
        return enLetra(r, a3, a2, a1, Racional.CERO);
    }

    /** El discriminante del cociente: (a₂ + a₃r)² − 4a₃(a₃r² + a₂r + a₁). */
    public Polinomio discriminanteCociente(char r) {
        Polinomio b = coeficienteLineal(r);
        return b.multiplicar(b).restar(coeficienteConstante(r).multiplicar(a3.multiplicar(Racional.de(4))));
    }

    /**
     * La identidad exacta (x − r)·cociente = P(x) − P(r), hecha con polinomios en x y r. Siempre es
     * verdadera; se calcula para que la comprobación diga lo que realmente se hizo.
     */
    public boolean divisionExacta(char r) {
        Polinomio xr = Polinomio.variable(variable).restar(Polinomio.variable(r));
        return xr.multiplicar(cociente(r)).equals(polinomio().restar(residuo(r)));
    }

    /** El valor de la raíz real con {@code mc} cifras. */
    public BigDecimal valor(MathContext mc) {
        return EvaluacionNumerica.evaluar(raiz, mc).re();
    }

    /** |P(r)| con el valor de Cardano evaluado a 40 cifras es menor que 10^−30·max|aₖ|. */
    public boolean raizComprobada() {
        MathContext mc = EvaluacionNumerica.MC;
        ComplejoDecimal r = EvaluacionNumerica.evaluar(raiz, mc);
        ComplejoDecimal suma = ComplejoDecimal.CERO;
        for (Racional c : new Racional[] {a3, a2, a1, a0}) {
            suma = suma.multiplicar(r, mc).sumar(ComplejoDecimal.de(c, mc), mc);
        }
        BigDecimal escala = BigDecimal.ONE;
        for (Racional c : new Racional[] {a3, a2, a1, a0}) {
            escala = escala.max(ComplejoDecimal.de(c, mc).modulo(mc));
        }
        return suma.modulo(mc).compareTo(escala.multiply(BigDecimal.ONE.movePointLeft(EvaluacionNumerica.CIFRAS_IGUALES)))
                < 0;
    }

    private static Polinomio enLetra(char r, Racional c3, Racional c2, Racional c1, Racional c0) {
        Polinomio x = Polinomio.variable(r);
        return x.potencia(3).multiplicar(c3).sumar(x.potencia(2).multiplicar(c2)).sumar(x.multiplicar(c1))
                .sumar(Polinomio.constante(c0));
    }

    private static Racional coef(Polinomio p, char x, int k) {
        return p.coeficiente(k == 0 ? Monomio.UNO : Monomio.de(x, k));
    }
}
