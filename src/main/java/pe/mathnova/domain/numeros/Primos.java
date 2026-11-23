package pe.mathnova.domain.numeros;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;

/** Factorización en primos y raíces enteras, con BigInteger (sin double). */
final class Primos {

    /** Hasta aquí se prueba por división; lo que queda se toma como primo si lo parece. */
    private static final BigInteger LIMITE_DIVISION = BigInteger.valueOf(2_000_000);

    private Primos() {
    }

    /** n > 0 → primo y exponente. Un resto compuesto muy grande queda como un solo "primo". */
    static Map<BigInteger, Integer> factorizar(BigInteger n) {
        if (n.signum() <= 0) {
            throw new IllegalArgumentException("Se factorizan enteros positivos: " + n);
        }
        Map<BigInteger, Integer> r = new TreeMap<>();
        BigInteger resto = n;
        BigInteger p = BigInteger.TWO;
        while (resto.compareTo(BigInteger.ONE) > 0 && p.compareTo(LIMITE_DIVISION) <= 0
                && p.multiply(p).compareTo(resto) <= 0) {
            while (resto.mod(p).signum() == 0) {
                r.merge(p, 1, Integer::sum);
                resto = resto.divide(p);
            }
            p = p.equals(BigInteger.TWO) ? BigInteger.valueOf(3) : p.add(BigInteger.TWO);
        }
        if (resto.compareTo(BigInteger.ONE) > 0) {
            // Un resto compuesto puede ser una potencia perfecta: se separa si lo es.
            for (int k = 64; k >= 2; k--) {
                BigInteger raiz = raizEntera(resto, k);
                if (raiz.compareTo(BigInteger.ONE) > 0 && raiz.pow(k).equals(resto)) {
                    final int veces = k;
                    factorizar(raiz).forEach((q, e) -> r.merge(q, e * veces, Integer::sum));
                    return r;
                }
            }
            r.merge(resto, 1, Integer::sum);
        }
        return r;
    }

    /** ⌊ⁿ√a⌋ para a ≥ 0 (Newton entero). */
    static BigInteger raizEntera(BigInteger a, int n) {
        if (a.signum() < 0) {
            throw new IllegalArgumentException("Radicando negativo");
        }
        if (a.signum() == 0 || n == 1) {
            return a;
        }
        if (n == 2) {
            return a.sqrt();
        }
        BigInteger x = BigInteger.ONE.shiftLeft(a.bitLength() / n + 1);
        BigInteger nn = BigInteger.valueOf(n);
        while (true) {
            BigInteger y = x.multiply(nn.subtract(BigInteger.ONE)).add(a.divide(x.pow(n - 1))).divide(nn);
            if (y.compareTo(x) >= 0) {
                break;
            }
            x = y;
        }
        while (x.pow(n).compareTo(a) > 0) {
            x = x.subtract(BigInteger.ONE);
        }
        while (x.add(BigInteger.ONE).pow(n).compareTo(a) <= 0) {
            x = x.add(BigInteger.ONE);
        }
        return x;
    }

    static int mcd(int a, int b) {
        return b == 0 ? Math.abs(a) : mcd(b, a % b);
    }

    static int mcm(int a, int b) {
        return a / mcd(a, b) * b;
    }
}
