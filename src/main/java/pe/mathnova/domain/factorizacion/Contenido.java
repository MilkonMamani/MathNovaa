package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Contenido y parte primitiva (base de F01, F02 y de la forma final RN12).
 * El contenido es MCD(numeradores)/MCM(denominadores) con el signo del coeficiente principal,
 * así que la parte primitiva tiene coeficientes enteros sin factor común y principal positivo.
 */
final class Contenido {

    private Contenido() {
    }

    /** Contenido con signo; 1 para el polinomio cero. */
    static Racional de(Polinomio p) {
        if (p.esCero()) {
            return Racional.UNO;
        }
        BigInteger mcd = BigInteger.ZERO;
        BigInteger mcm = BigInteger.ONE;
        for (Racional c : p.terminos().values()) {
            mcd = mcd.gcd(c.num().abs());
            mcm = mcm.divide(mcm.gcd(c.den())).multiply(c.den());
        }
        Racional r = Racional.de(mcd).dividir(Racional.de(mcm));
        return principal(p).signo() < 0 ? r.negar() : r;
    }

    /** Coeficiente del mayor monomio. */
    static Racional principal(Polinomio p) {
        return p.terminos().values().iterator().next();
    }

    /** Cada letra con su menor exponente entre todos los términos. */
    static Monomio monomioComun(Polinomio p) {
        Monomio comun = null;
        for (Monomio m : p.terminos().keySet()) {
            comun = comun == null ? m : comun.mcd(m);
        }
        return comun == null ? Monomio.UNO : comun;
    }

    /** p / (contenido · monomio común). */
    static Polinomio primitiva(Polinomio p) {
        return dividir(p, de(p), monomioComun(p));
    }

    /** p / (c · m), exacta. */
    static Polinomio dividir(Polinomio p, Racional c, Monomio m) {
        Racional inverso = c.inverso();
        Polinomio r = Polinomio.CERO;
        for (var t : p.terminos().entrySet()) {
            r = r.sumar(Polinomio.termino(t.getValue().multiplicar(inverso), t.getKey().dividir(m)));
        }
        return r;
    }

    static boolean tieneFracciones(Polinomio p) {
        return p.terminos().values().stream().anyMatch(c -> !c.esEntero());
    }
}
