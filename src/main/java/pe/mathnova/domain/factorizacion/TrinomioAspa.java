package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.algebra.Polinomio;

/**
 * F13: ax² + bx + c por aspa simple. Exige Δ = b² − 4ac cuadrado perfecto; las raíces
 * p/q dan los factores (qx − p), y el aspa muestra que los productos cruzados suman bx.
 */
public final class TrinomioAspa extends MetodoBase {

    public TrinomioAspa() {
        super("F13");
    }

    /** Solo trinomios de grado 2 en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() == 2 && p.terminos().size() == 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Cuadratica q = Cuadratica.de(p);
        BigInteger delta = q.discriminante();
        if (delta.signum() < 0) {
            throw noAplica(q.textoDiscriminante() + " < 0");
        }
        BigInteger s = Termino.raizEntera(delta, 2).orElseThrow(() ->
                noAplica(q.textoDiscriminante() + " no es un cuadrado perfecto"));

        Racional a = Racional.de(q.a());
        Racional b = Racional.de(q.b());
        Racional dosA = a.multiplicar(Racional.de(2));
        Racional r1 = b.negar().sumar(Racional.de(s)).dividir(dosA);
        Racional r2 = b.negar().restar(Racional.de(s)).dividir(dosA);

        // Filas del aspa: (q₁x | −p₁) y (q₂x | −p₂), la de menor q arriba.
        Racional[] fila1 = {Racional.de(r1.den()), r1.negar().multiplicar(Racional.de(r1.den()))};
        Racional[] fila2 = {Racional.de(r2.den()), r2.negar().multiplicar(Racional.de(r2.den()))};
        if (fila1[0].compareTo(fila2[0]) > 0) {
            Racional[] t = fila1;
            fila1 = fila2;
            fila2 = t;
        }
        Monomio x = Monomio.de(q.variable(), 1);
        Termino x1 = new Termino(fila1[0], x);
        Termino x2 = new Termino(fila2[0], x);
        Termino cruce1 = x1.multiplicar(new Termino(fila2[1], Monomio.UNO));
        Termino cruce2 = x2.multiplicar(new Termino(fila1[1], Monomio.UNO));
        String verificacion = Formato.termino(x1) + "·" + Formato.numeroEnParentesis(fila2[1]) + " + "
                + Formato.termino(x2) + "·" + Formato.numeroEnParentesis(fila1[1]) + " = "
                + Formato.suma(List.of(cruce1, cruce2)) + " = " + Formato.termino(new Termino(b, x));
        var aspa = new TrabajoIntermedio.AspaSimple(
                Formato.termino(x1) + "    " + conSigno(fila1[1]),
                Formato.termino(x2) + "    " + conSigno(fila2[1]),
                verificacion, q.variable(), fila1[0], fila1[1], fila2[0], fila2[1]);

        String justificacion = q.textoDiscriminante() + " = " + s + "², cuadrado perfecto";
        List<Factor> factores = new ArrayList<>();
        Racional k = a.dividir(fila1[0].multiplicar(fila2[0]));
        if (!k.equals(Racional.UNO)) {
            factores.add(Factor.de(Polinomio.constante(k)));
        }
        factores.add(Factor.de(q.lineal(fila1[0], fila1[1])));
        factores.add(Factor.de(q.lineal(fila2[0], fila2[1])));
        return resultado(justificacion, p, factores, aspa);
    }

    private static String conSigno(Racional r) {
        return r.signo() < 0 ? Formato.numero(r) : "+" + r;
    }

    /** Sprint 9, parte D: orienta sin decir el nombre del método. */
    @Override
    public String pista() {
        return "El coeficiente de x² no es 1: descompón el primero y el último término y cruza los productos para obtener el del medio.";
    }
}
