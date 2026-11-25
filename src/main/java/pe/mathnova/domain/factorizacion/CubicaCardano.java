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

public record CubicaCardano(char variable, Racional a3, Racional a2, Racional a1, Racional a0, Racional h,
                            Racional p, Racional q, Racional delta, NumeroAlgebraico u, NumeroAlgebraico v,
                            NumeroAlgebraico raiz) {

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

    public Polinomio polinomio() {
        return enLetra(variable, a3, a2, a1, a0);
    }

    public Polinomio coeficienteLineal(char r) {
        return Polinomio.constante(a2).sumar(Polinomio.variable(r).multiplicar(a3));
    }

    public Polinomio coeficienteConstante(char r) {
        return enLetra(r, Racional.CERO, a3, a2, a1);
    }

    public Polinomio cociente(char r) {
        Polinomio x = Polinomio.variable(variable);
        return x.multiplicar(x).multiplicar(a3).sumar(coeficienteLineal(r).multiplicar(x)).sumar(coeficienteConstante(r));
    }

    public Polinomio residuo(char r) {
        return enLetra(r, a3, a2, a1, a0);
    }

    public Polinomio residuoSinConstante(char r) {
        return enLetra(r, a3, a2, a1, Racional.CERO);
    }

    public Polinomio discriminanteCociente(char r) {
        Polinomio b = coeficienteLineal(r);
        return b.multiplicar(b).restar(coeficienteConstante(r).multiplicar(a3.multiplicar(Racional.de(4))));
    }

    public boolean divisionExacta(char r) {
        Polinomio xr = Polinomio.variable(variable).restar(Polinomio.variable(r));
        return xr.multiplicar(cociente(r)).equals(polinomio().restar(residuo(r)));
    }

    public BigDecimal valor(MathContext mc) {
        return EvaluacionNumerica.evaluar(raiz, mc).re();
    }

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
