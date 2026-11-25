package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * F15: quita y pon. En ax²ᵐ + bxᵐ + c con a y c cuadrados, se suma y se resta lo que falta para un
 * trinomio cuadrado perfecto (√a·xᵐ ± √c)²; si lo que se resta es un cuadrado k²xᵐ, queda una
 * diferencia de cuadrados: x⁴ + 4 = (x² + 2)² − (2x)² = (x² + 2x + 2)(x² − 2x + 2).
 */
public final class QuitaYPon extends MetodoBase {

    public QuitaYPon() {
        super("F15");
    }

    /** Solo grado 4k con tres términos o menos, en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() >= 4 && p.grado() % 4 == 0 && p.terminos().size() <= 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        int grado = u.grado();
        int m = grado / 2;
        int terminos = p.terminos().size();
        boolean forma = grado >= 4 && grado % 4 == 0 && (terminos == 2 || terminos == 3 && !u.coef(m).esCero())
                && !u.coef(0).esCero() && u.enteros();
        for (int i = 1; forma && i < grado; i++) {
            if (i != m && !u.coef(i).esCero()) {
                forma = false;
            }
        }
        if (!forma) {
            throw noAplica("No tiene la forma ax⁴ + bx² + c (o con exponentes 4k, 2k y 0)");
        }
        char x = u.variable();
        Racional a = u.coef(grado);
        Racional b = u.coef(m);
        Racional c = u.coef(0);
        Racional s = Termino.raiz(a, 2).orElseThrow(() -> noAplica(Formato.termino(new Termino(a, Monomio.de(x, grado)))
                + " no es un cuadrado perfecto"));
        Racional t = Termino.raiz(c, 2).orElseThrow(() -> noAplica(Formato.numero(c) + " no es un cuadrado perfecto"));
        Racional dosST = s.multiplicar(t).multiplicar(Racional.de(2));
        for (boolean mas : List.of(true, false)) {
            // (s·xᵐ ± t)² − p = (±2st − b)·xᵐ
            Racional d = (mas ? dosST : dosST.negar()).restar(b);
            if (d.signo() <= 0) {
                continue;
            }
            Optional<Racional> k = Termino.raiz(d, 2);
            if (k.isEmpty()) {
                continue;
            }
            return armar(p, x, grado, m, s, t, mas, d, k.get());
        }
        throw noAplica("Sumando y restando para completar (" + Formato.termino(new Termino(s, Monomio.de(x, m)))
                + " ± " + Formato.numero(t) + ")² no queda un cuadrado: " + Formato.numero(dosST.restar(b)) + " y "
                + Formato.numero(dosST.negar().restar(b)) + " no son cuadrados positivos");
    }

    private ResultadoMetodo armar(Polinomio p, char x, int grado, int m, Racional s, Racional t, boolean mas,
                                  Racional d, Racional k) {
        Polinomio base = Polinomio.termino(s, Monomio.de(x, m)).sumar(Polinomio.constante(mas ? t : t.negar()));
        Polinomio kx = Polinomio.termino(k, Monomio.de(x, m / 2));
        Polinomio pon = Polinomio.termino(d, Monomio.de(x, m));
        Polinomio cuadrado = base.multiplicar(base);
        List<Factor> factores = List.of(Factor.de(base.sumar(kx)), Factor.de(base.restar(kx)));

        String ponTexto = Formato.polinomio(pon);
        String kTexto = Formato.base(Termino.de(kx).getFirst());
        String justificacion = "Para completar el trinomio cuadrado perfecto " + Formato.parentesis(base) + "² = "
                + Formato.polinomio(cuadrado) + " se suma y se resta " + ponTexto
                + ": queda una diferencia de cuadrados";
        var trabajo = new TrabajoIntermedio.Agrupacion(List.of(
                Formato.polinomio(p) + " = " + Formato.polinomio(cuadrado) + " " + Formato.MENOS + " " + ponTexto,
                "= " + Formato.parentesis(base) + "² " + Formato.MENOS + " " + kTexto + "²",
                "= " + Formato.parentesis(base.sumar(kx)) + Formato.parentesis(base.restar(kx))));
        return resultado(justificacion, p, factores, trabajo);
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "Le falta poco para ser un cuadrado perfecto: suma y resta el mismo término para completarlo.";
    }
}
