package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/** ax² + bx + c en una sola letra, con coeficientes enteros. Base de F12, F13 y A1. */
record Cuadratica(char variable, BigInteger a, BigInteger b, BigInteger c) {

    /** Lanza NoAplica si p no es de grado 2 en una sola letra con coeficientes enteros. */
    static Cuadratica de(Polinomio p) {
        if (p.variables().size() != 1) {
            throw MetodoBase.noAplica("Tiene más de una letra");
        }
        if (p.grado() != 2) {
            throw MetodoBase.noAplica("Es de grado " + p.grado() + ", no 2");
        }
        if (Contenido.tieneFracciones(p)) {
            throw MetodoBase.noAplica("Tiene coeficientes fraccionarios");
        }
        char v = p.variables().first();
        return new Cuadratica(v, entero(p.coeficiente(Monomio.de(v, 2))), entero(p.coeficiente(Monomio.de(v, 1))),
                entero(p.constante()));
    }

    private static BigInteger entero(Racional r) {
        return r.num();
    }

    BigInteger discriminante() {
        return b.multiply(b).subtract(BigInteger.valueOf(4).multiply(a).multiply(c));
    }

    /** "Δ = 1² − 4·6·(−2) = 49". */
    String textoDiscriminante() {
        return "Δ = " + Formato.numeroEnParentesis(Racional.de(b)) + "² − 4·" + Formato.numeroEnParentesis(Racional.de(a))
                + "·" + Formato.numeroEnParentesis(Racional.de(c)) + " = " + Formato.numero(Racional.de(discriminante()));
    }

    /** a·x + b como polinomio en la letra de la cuadrática. */
    Polinomio lineal(Racional coefX, Racional independiente) {
        return Polinomio.termino(coefX, Monomio.de(variable, 1)).sumar(Polinomio.constante(independiente));
    }
}
