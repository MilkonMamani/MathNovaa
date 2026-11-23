package pe.mathnova.domain.factorizacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * F19: aspa doble especial para ax⁴ + bx³ + cx² + dx + e sin raíces racionales:
 * (a1x² + px + e1)(a2x² + qx + e2). Se descomponen los extremos (a = a1·a2, e = e1·e2), se ve
 * cuánto falta para el término x² y se reparte en la columna central (p·q), verificando las aspas
 * laterales: a1·q + a2·p = b (x³) y p·e2 + q·e1 = d (x).
 */
public final class AspaDobleEspecial extends MetodoBase {

    public AspaDobleEspecial() {
        super("F19");
    }

    /** Solo grado 4, en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.grado() == 4;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        if (u.grado() != 4) {
            throw noAplica("Es de grado " + u.grado() + "; el aspa doble especial es para grado 4");
        }
        if (!u.enteros() || u.coef(0).esCero()) {
            throw noAplica("No tiene término independiente");
        }
        Optional<Racional> raiz = primeraRaiz(u);
        if (raiz.isPresent()) {
            throw noAplica(u.variable() + " = " + Formato.numero(raiz.get()) + " es raíz: corresponde Ruffini");
        }
        long a = u.coef(4).num().longValueExact();
        long b = u.coef(3).num().longValueExact();
        long c = u.coef(2).num().longValueExact();
        long d = u.coef(1).num().longValueExact();
        long e = u.coef(0).num().longValueExact();
        for (long a1 : RaicesRacionales.divisores(Math.abs(a))) {
            long a2 = a / a1;
            for (long e1 : conSigno(RaicesRacionales.divisores(Math.abs(e)))) {
                long e2 = e / e1;
                long falta = c - (a1 * e2 + a2 * e1);
                for (long[] pq : centrales(a1, a2, e1, e2, b, d, falta)) {
                    long pp = pq[0];
                    long qq = pq[1];
                    if (a2 * pp + a1 * qq == b && pp * e2 + qq * e1 == d && pp * qq == falta) {
                        return armar(p, u.variable(), a1, pp, e1, a2, qq, e2, c);
                    }
                }
            }
        }
        throw noAplica("Ninguna descomposición de los extremos deja correctas las aspas laterales");
    }

    /** Valores (p, q) de la columna central que pueden cumplir las dos aspas laterales. */
    private static List<long[]> centrales(long a1, long a2, long e1, long e2, long b, long d, long falta) {
        List<long[]> r = new ArrayList<>();
        long det = a2 * e1 - a1 * e2;
        if (det != 0) {
            long pn = b * e1 - a1 * d;
            long qn = a2 * d - e2 * b;
            if (pn % det == 0 && qn % det == 0) {
                r.add(new long[] {pn / det, qn / det});
            }
            return r;
        }
        if (falta == 0) {
            if (a1 != 0 && b % a1 == 0) {
                r.add(new long[] {0, b / a1});
            }
            if (a2 != 0 && b % a2 == 0) {
                r.add(new long[] {b / a2, 0});
            }
            return r;
        }
        for (long k : conSigno(RaicesRacionales.divisores(Math.abs(falta)))) {
            r.add(new long[] {k, falta / k});
        }
        return r;
    }

    static List<Long> conSigno(List<Long> positivos) {
        List<Long> r = new ArrayList<>(positivos);
        positivos.forEach(x -> r.add(-x));
        return r;
    }

    static Optional<Racional> primeraRaiz(Univariado u) {
        Optional<List<Racional>> candidatos = RaicesRacionales.candidatos(u);
        if (candidatos.isEmpty()) {
            return Optional.empty();
        }
        return candidatos.get().stream().filter(r -> u.evaluar(r).esCero()).findFirst();
    }

    private ResultadoMetodo armar(Polinomio p, char x, long a1, long pp, long e1, long a2, long qq, long e2, long c) {
        Racional ra1 = Racional.de(a1);
        Racional rp = Racional.de(pp);
        Racional re1 = Racional.de(e1);
        Racional ra2 = Racional.de(a2);
        Racional rq = Racional.de(qq);
        Racional re2 = Racional.de(e2);
        Polinomio f1 = Univariado.de(x, List.of(re1, rp, ra1)).aPolinomio();
        Polinomio f2 = Univariado.de(x, List.of(re2, rq, ra2)).aPolinomio();
        long tiene = a1 * e2 + a2 * e1;
        long falta = c - tiene;
        String x2 = x + "²";
        String justificacion = "Se descomponen los extremos (" + Formato.numero(Racional.de(a1 * a2)) + x + "⁴ = "
                + termino(ra1, x, 2) + "·" + termino(ra2, x, 2) + " y " + Formato.numero(Racional.de(e1 * e2)) + " = "
                + Formato.numeroEnParentesis(re1) + "·" + Formato.numeroEnParentesis(re2) + "); se tiene "
                + Formato.numero(Racional.de(tiene)) + x2 + " y " + (falta >= 0 ? "faltan " : "sobran ")
                + Formato.numero(Racional.de(Math.abs(falta))) + x2 + ", que se reparten como "
                + termino(rp, x, 1) + "·" + termino(rq, x, 1);
        var trabajo = new TrabajoIntermedio.AspaDobleEspecial(x, ra1, rp, re1, ra2, rq, re2, Racional.de(c));
        return resultado(justificacion, p, List.of(Factor.de(f1), Factor.de(f2)), trabajo);
    }

    private static String termino(Racional c, char x, int e) {
        return Formato.termino(new Termino(c, e == 0 ? pe.mathnova.domain.algebra.Monomio.UNO
                : pe.mathnova.domain.algebra.Monomio.de(x, e)));
    }
}
