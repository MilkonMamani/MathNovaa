package pe.mathnova.domain.factorizacion;

import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;

/** F07: a² ± 2ab + b² = (a ± b)². El término central debe ser ±2·√A·√C. */
public final class TrinomioCuadradoPerfecto extends MetodoBase {

    public TrinomioCuadradoPerfecto() {
        super("F07");
    }

    /** Si p es un trinomio cuadrado perfecto, el binomio r con r² = p. Lo usa F04 (3+1 y 1+3). */
    static Optional<Polinomio> raizDe(Polinomio p) {
        try {
            return Optional.of(analizar(p).binomio());
        } catch (NoAplica e) {
            return Optional.empty();
        }
    }

    private record Analisis(Termino a, Termino c, Termino central, boolean menos, Polinomio binomio) {
    }

    private static Analisis analizar(Polinomio p) {
        List<Termino> ts = Termino.de(p);
        if (ts.size() != 3) {
            throw noAplica("Tiene " + ts.size() + " términos; hacen falta 3");
        }
        Termino t1 = ts.get(0);
        Termino t2 = ts.get(1);
        Termino t3 = ts.get(2);
        for (Termino t : List.of(t1, t3)) {
            if (!t.positivo()) {
                throw noAplica(Formato.termino(t) + " es negativo: no es un cuadrado");
            }
        }
        Termino a = t1.raiz(2).orElseThrow(() -> noAplica(Formato.termino(t1) + " no es un cuadrado perfecto"));
        Termino c = t3.raiz(2).orElseThrow(() -> noAplica(Formato.termino(t3) + " no es un cuadrado perfecto"));
        Termino doble = a.multiplicar(c).multiplicar(2);
        boolean menos;
        if (t2.equals(doble)) {
            menos = false;
        } else if (t2.equals(doble.negar())) {
            menos = true;
        } else {
            throw noAplica(Formato.termino(t1) + " y " + Formato.termino(t3) + " son cuadrados, pero "
                    + Formato.termino(t2.abs()) + " no es 2·" + Formato.termino(a) + "·" + Formato.termino(c)
                    + " = " + Formato.termino(doble));
        }
        Polinomio binomio = menos ? a.aPolinomio().restar(c.aPolinomio()) : a.aPolinomio().sumar(c.aPolinomio());
        return new Analisis(a, c, t2, menos, binomio);
    }

    /** Solo con 3 términos. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.terminos().size() == 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Analisis an = analizar(p);
        List<Termino> ts = Termino.de(p);
        String justificacion = Formato.termino(ts.get(0)) + " = " + Formato.base(an.a()) + "², "
                + Formato.termino(ts.get(2)) + " = " + Formato.base(an.c()) + "² y "
                + Formato.termino(an.central().abs()) + " = 2·" + Formato.termino(an.a()) + "·"
                + Formato.termino(an.c());
        String signo = an.menos() ? " − " : " + ";
        var trabajo = new TrabajoIntermedio.Comparacion("a²" + signo + "2ab + b² = (a" + signo + "b)² con a = "
                + Formato.termino(an.a()) + " y b = " + Formato.termino(an.c()));
        return resultado(justificacion, p, List.of(new Factor(an.binomio(), 2)), trabajo);
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "¿Los extremos son cuadrados y el término del medio es el doble del producto de sus raíces?";
    }
}
