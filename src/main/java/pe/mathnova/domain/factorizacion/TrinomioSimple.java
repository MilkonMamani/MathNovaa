package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/** F12: x² + bx + c = (x + m)(x + n) con enteros m + n = b y m·n = c. */
public final class TrinomioSimple extends MetodoBase {

    /** Más allá de esto no se buscan divisores (el banco no pasa de tres cifras). */
    private static final BigInteger LIMITE = BigInteger.valueOf(1_000_000_000_000L);

    public TrinomioSimple() {
        super("F12");
    }

    /** Solo trinomios de grado 2 en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() == 2 && p.terminos().size() == 3;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Cuadratica q = Cuadratica.de(p);
        if (!q.a().equals(BigInteger.ONE)) {
            throw noAplica("El coeficiente de " + q.variable() + "² es " + q.a() + ", no 1: corresponde F13");
        }
        BigInteger b = q.b();
        BigInteger c = q.c();
        if (c.signum() == 0) {
            throw noAplica("No tiene término independiente: corresponde F01");
        }
        if (c.abs().compareTo(LIMITE) > 0) {
            throw noAplica("El término independiente es demasiado grande para buscar m y n");
        }
        long cc = c.longValueExact();
        long bb = b.longValueExact();
        Long m = null;
        Long n = null;
        for (long d = 1; d * d <= Math.abs(cc) && m == null; d++) {
            if (cc % d != 0) {
                continue;
            }
            long e = cc / d;
            for (long[] par : new long[][] {{d, e}, {-d, -e}}) {
                if (par[0] + par[1] == bb) {
                    m = Math.max(par[0], par[1]);
                    n = Math.min(par[0], par[1]);
                    break;
                }
            }
        }
        if (m == null) {
            throw noAplica("No hay enteros m y n con m + n = " + Formato.numero(Racional.de(b))
                    + " y m·n = " + Formato.numero(Racional.de(c)));
        }
        Racional rm = Racional.de(m);
        Racional rn = Racional.de(n);
        String justificacion = Formato.numero(rm) + " + " + Formato.numeroEnParentesis(rn) + " = "
                + Formato.numero(Racional.de(b)) + " y " + Formato.numeroEnParentesis(rm) + "·"
                + Formato.numeroEnParentesis(rn) + " = " + Formato.numero(Racional.de(c));
        List<Factor> factores = List.of(
                Factor.de(q.lineal(Racional.UNO, rm)),
                Factor.de(q.lineal(Racional.UNO, rn)));
        // Aspa: (x | m) y (x | n); los productos cruzados x·n + x·m dan bx.
        String x = String.valueOf(q.variable());
        String verificacion = x + "·" + Formato.numeroEnParentesis(rn) + " + " + x + "·"
                + Formato.numeroEnParentesis(rm) + " = "
                + Formato.suma(List.of(new Termino(rn, Monomio.de(q.variable(), 1)), new Termino(rm, Monomio.de(q.variable(), 1))))
                + " = " + Formato.termino(new Termino(Racional.de(b), Monomio.de(q.variable(), 1)));
        var aspa = new TrabajoIntermedio.AspaSimple(x + "    " + conSigno(rm), x + "    " + conSigno(rn), verificacion,
                q.variable(), Racional.UNO, rm, Racional.UNO, rn);
        return resultado(justificacion, p, factores, aspa);
    }

    private static String conSigno(Racional r) {
        return r.signo() < 0 ? Formato.numero(r) : "+" + r;
    }
}
