package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

final class Contenido {

    private Contenido() {
    }

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

    static Racional principal(Polinomio p) {
        return p.terminos().values().iterator().next();
    }

    static Monomio monomioComun(Polinomio p) {
        Monomio comun = null;
        for (Monomio m : p.terminos().keySet()) {
            comun = comun == null ? m : comun.mcd(m);
        }
        return comun == null ? Monomio.UNO : comun;
    }

    static Polinomio primitiva(Polinomio p) {
        return dividir(p, de(p), monomioComun(p));
    }

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
