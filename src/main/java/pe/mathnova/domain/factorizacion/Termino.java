package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

record Termino(Racional coeficiente, Monomio monomio) {

    static List<Termino> de(Polinomio p) {
        List<Termino> lista = new ArrayList<>();
        p.terminos().forEach((m, c) -> lista.add(new Termino(c, m)));
        return lista;
    }

    static Polinomio suma(List<Termino> terminos) {
        Polinomio r = Polinomio.CERO;
        for (Termino t : terminos) {
            r = r.sumar(t.aPolinomio());
        }
        return r;
    }

    Polinomio aPolinomio() {
        return Polinomio.termino(coeficiente, monomio);
    }

    boolean positivo() {
        return coeficiente.signo() > 0;
    }

    Termino negar() {
        return new Termino(coeficiente.negar(), monomio);
    }

    Termino abs() {
        return positivo() ? this : negar();
    }

    Termino multiplicar(Termino o) {
        return new Termino(coeficiente.multiplicar(o.coeficiente), monomio.multiplicar(o.monomio));
    }

    Termino multiplicar(long k) {
        return new Termino(coeficiente.multiplicar(Racional.de(k)), monomio);
    }

    Termino potencia(int n) {
        return new Termino(coeficiente.potencia(n), monomio.potencia(n));
    }

    Optional<Termino> raiz(int n) {
        Optional<Monomio> m = monomio.raiz(n);
        Optional<Racional> c = raiz(coeficiente, n);
        if (m.isEmpty() || c.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Termino(c.get(), m.get()));
    }

    static Optional<Racional> raiz(Racional r, int n) {
        if (r.signo() < 0) {
            if (n % 2 == 0) {
                return Optional.empty();
            }
            return raiz(r.negar(), n).map(Racional::negar);
        }
        Optional<BigInteger> num = raizEntera(r.num(), n);
        Optional<BigInteger> den = raizEntera(r.den(), n);
        if (num.isEmpty() || den.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Racional(num.get(), den.get()));
    }

    static Optional<BigInteger> raizEntera(BigInteger a, int n) {
        if (a.signum() == 0 || a.equals(BigInteger.ONE)) {
            return Optional.of(a);
        }
        BigInteger bajo = BigInteger.ONE;
        BigInteger alto = BigInteger.ONE.shiftLeft(a.bitLength() / n + 1);
        while (bajo.compareTo(alto) <= 0) {
            BigInteger medio = bajo.add(alto).shiftRight(1);
            int c = medio.pow(n).compareTo(a);
            if (c == 0) {
                return Optional.of(medio);
            }
            if (c < 0) {
                bajo = medio.add(BigInteger.ONE);
            } else {
                alto = medio.subtract(BigInteger.ONE);
            }
        }
        return Optional.empty();
    }
}
