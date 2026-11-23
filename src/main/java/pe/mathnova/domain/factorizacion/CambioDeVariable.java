package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * F14: cambio de variable u = xⁿ para trinomios con exponentes 2n, n y 0 (x⁴ − 5x² + 4,
 * x⁶ − 7x³ − 8). Se factoriza au² + bu + c en Q y se regresa a x.
 */
public final class CambioDeVariable extends MetodoBase {

    public CambioDeVariable() {
        super("F14");
    }

    /** Solo trinomios de grado par desde 4, en una letra. */
    @Override
    public boolean esCandidato(Polinomio p) {
        return p.variables().size() == 1 && p.terminos().size() == 3 && p.grado() >= 4 && p.grado() % 2 == 0;
    }

    @Override
    ResultadoMetodo intentar(Polinomio p) {
        Univariado u = Univariado.de(p).orElseThrow(() -> noAplica("Tiene más de una letra"));
        List<Termino> ts = Termino.de(p);
        int grado = u.grado();
        if (ts.size() != 3 || grado % 2 != 0 || grado < 4) {
            throw noAplica("No es un trinomio con exponentes 2n, n y 0");
        }
        int n = grado / 2;
        if (u.coef(n).esCero() || u.coef(0).esCero() || !u.enteros()) {
            throw noAplica("No es un trinomio con exponentes " + grado + ", " + n + " y 0");
        }
        char x = u.variable();
        char nueva = x == 'u' ? 'w' : 'u';
        BigInteger a = u.coef(grado).num();
        BigInteger b = u.coef(n).num();
        BigInteger c = u.coef(0).num();
        BigInteger delta = b.multiply(b).subtract(BigInteger.valueOf(4).multiply(a).multiply(c));
        Polinomio enU = trinomio(nueva, 1, Racional.de(a), Racional.de(b), Racional.de(c));
        String dTexto = "Δ = " + Formato.numero(Racional.de(delta));
        if (delta.signum() < 0) {
            throw noAplica("Con " + nueva + " = " + Formato.monomio(Monomio.de(x, n)) + " queda "
                    + Formato.polinomio(enU) + ", que no se factoriza en Q: " + dTexto + " < 0");
        }
        BigInteger s = Termino.raizEntera(delta, 2).orElseThrow(() -> noAplica("Con " + nueva + " = "
                + Formato.monomio(Monomio.de(x, n)) + " queda " + Formato.polinomio(enU)
                + ", que no se factoriza en Q: " + dTexto + " no es un cuadrado perfecto"));

        Racional dosA = Racional.de(a.multiply(BigInteger.TWO));
        Racional r1 = Racional.de(b.negate().add(s)).dividir(dosA);
        Racional r2 = Racional.de(b.negate().subtract(s)).dividir(dosA);
        Racional k = Racional.de(a).dividir(Racional.de(r1.den().multiply(r2.den())));

        List<Factor> enX = new ArrayList<>();
        List<Factor> enNueva = new ArrayList<>();
        if (!k.equals(Racional.UNO)) {
            enX.add(Factor.de(Polinomio.constante(k)));
            enNueva.add(Factor.de(Polinomio.constante(k)));
        }
        if (r1.equals(r2)) {
            enX.add(new Factor(binomio(x, n, r1), 2));
            enNueva.add(new Factor(binomio(nueva, 1, r1), 2));
        } else {
            for (Racional r : List.of(r2, r1)) { // de menor a mayor
                enX.add(Factor.de(binomio(x, n, r)));
                enNueva.add(Factor.de(binomio(nueva, 1, r)));
            }
        }
        String xn = Formato.monomio(Monomio.de(x, n));
        String formaU = texto(enNueva);
        String formaX = texto(enX);
        var trabajo = new TrabajoIntermedio.Sustitucion(String.valueOf(nueva), nueva + " = " + xn,
                Formato.polinomio(p) + " = " + Formato.polinomio(enU),
                Formato.polinomio(enU) + " = " + formaU,
                formaU + " = " + formaX);
        String justificacion = "Los exponentes son " + grado + ", " + n + " y 0: con " + nueva + " = " + xn
                + " queda " + Formato.polinomio(enU) + ", y " + dTexto
                + (delta.signum() == 0 ? " (cuadrado perfecto)" : " = " + s + "², cuadrado perfecto");
        return resultado(justificacion, p, enX, trabajo);
    }

    /** a·vᵉ²ᵏ + b·vᵏ + c. */
    private static Polinomio trinomio(char v, int k, Racional a, Racional b, Racional c) {
        return Polinomio.termino(a, Monomio.de(v, 2 * k)).sumar(Polinomio.termino(b, Monomio.de(v, k)))
                .sumar(Polinomio.constante(c));
    }

    /** (q·vᵏ − p) para la raíz p/q. */
    private static Polinomio binomio(char v, int k, Racional r) {
        return Polinomio.termino(Racional.de(r.den()), Monomio.de(v, k))
                .sumar(Polinomio.constante(Racional.de(r.num()).negar()));
    }

    /** "(u − 1)(u − 4)", "2(x² − 3)²". */
    static String texto(List<Factor> factores) {
        StringBuilder sb = new StringBuilder();
        for (Factor f : factores) {
            if (f.polinomio().esConstante()) {
                Racional c = f.polinomio().constante();
                sb.append(c.equals(Racional.UNO.negar()) ? Formato.MENOS : Formato.numero(c));
            } else {
                sb.append(Formato.parentesis(f.polinomio()));
                if (f.multiplicidad() > 1) {
                    sb.append(Formato.superindice(f.multiplicidad()));
                }
            }
        }
        return sb.toString();
    }
}
