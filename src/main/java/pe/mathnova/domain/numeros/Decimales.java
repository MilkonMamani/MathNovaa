package pe.mathnova.domain.numeros;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

final class Decimales {

    private Decimales() {
    }

    private static MathContext guarda(MathContext mc) {
        return new MathContext(mc.getPrecision() + 10);
    }

    static BigDecimal pi(MathContext mc) {
        MathContext g = guarda(mc);
        BigDecimal a = arctanSerie(BigDecimal.ONE.divide(BigDecimal.valueOf(5), g), g);
        BigDecimal b = arctanSerie(BigDecimal.ONE.divide(BigDecimal.valueOf(239), g), g);
        return a.multiply(BigDecimal.valueOf(16)).subtract(b.multiply(BigDecimal.valueOf(4))).round(mc);
    }

    private static BigDecimal arctanSerie(BigDecimal x, MathContext mc) {
        BigDecimal x2 = x.multiply(x, mc);
        BigDecimal termino = x;
        BigDecimal suma = x;
        BigDecimal limite = BigDecimal.ONE.movePointLeft(mc.getPrecision() + 2);
        for (int n = 1; termino.abs().compareTo(limite) > 0 && n < 100_000; n++) {
            termino = termino.multiply(x2, mc).negate();
            suma = suma.add(termino.divide(BigDecimal.valueOf(2L * n + 1), mc), mc);
        }
        return suma;
    }

    static BigDecimal arctan(BigDecimal x, MathContext mc) {
        MathContext g = guarda(mc);
        if (x.signum() < 0) {
            return arctan(x.negate(), mc).negate();
        }
        if (x.compareTo(BigDecimal.ONE) > 0) {
            return pi(g).divide(BigDecimal.valueOf(2), g).subtract(arctan(BigDecimal.ONE.divide(x, g), g)).round(mc);
        }
        BigDecimal y = x;
        int veces = 0;
        while (y.compareTo(new BigDecimal("0.2")) > 0) {
            y = y.divide(BigDecimal.ONE.add(BigDecimal.ONE.add(y.multiply(y, g), g).sqrt(g), g), g);
            veces++;
        }
        BigDecimal r = arctanSerie(y, g);
        return r.multiply(BigDecimal.valueOf(2).pow(veces), g).round(mc);
    }

    static BigDecimal arcsen(BigDecimal x, MathContext mc) {
        MathContext g = guarda(mc);
        BigDecimal uno = BigDecimal.ONE;
        if (x.abs().compareTo(uno) > 0) {
            throw new ArithmeticException("arcsen fuera de [-1, 1]");
        }
        BigDecimal raiz = uno.subtract(x.multiply(x, g), g).sqrt(g);
        if (raiz.signum() == 0) {
            BigDecimal mitad = pi(g).divide(BigDecimal.valueOf(2), g);
            return (x.signum() < 0 ? mitad.negate() : mitad).round(mc);
        }
        return arctan(x.divide(raiz, g), g).round(mc);
    }

    static BigDecimal arccos(BigDecimal x, MathContext mc) {
        MathContext g = guarda(mc);
        return pi(g).divide(BigDecimal.valueOf(2), g).subtract(arcsen(x, g), g).round(mc);
    }

    private static BigDecimal reducir(BigDecimal x, MathContext g) {
        BigDecimal dosPi = pi(g).multiply(BigDecimal.valueOf(2), g);
        BigDecimal k = x.divide(dosPi, 0, java.math.RoundingMode.HALF_EVEN);
        return x.subtract(dosPi.multiply(k, g), g);
    }

    static BigDecimal sen(BigDecimal x, MathContext mc) {
        MathContext g = new MathContext(mc.getPrecision() + 10 + Math.max(0, x.precision() - x.scale()));
        BigDecimal y = reducir(x, g);
        BigDecimal y2 = y.multiply(y, g);
        BigDecimal termino = y;
        BigDecimal suma = y;
        BigDecimal limite = BigDecimal.ONE.movePointLeft(g.getPrecision());
        for (int n = 1; termino.abs().compareTo(limite) > 0; n++) {
            termino = termino.multiply(y2, g).divide(BigDecimal.valueOf((2L * n) * (2L * n + 1)), g).negate();
            suma = suma.add(termino, g);
        }
        return suma.round(mc);
    }

    static BigDecimal cos(BigDecimal x, MathContext mc) {
        MathContext g = new MathContext(mc.getPrecision() + 10 + Math.max(0, x.precision() - x.scale()));
        BigDecimal y = reducir(x, g);
        BigDecimal y2 = y.multiply(y, g);
        BigDecimal termino = BigDecimal.ONE;
        BigDecimal suma = BigDecimal.ONE;
        BigDecimal limite = BigDecimal.ONE.movePointLeft(g.getPrecision());
        for (int n = 1; termino.abs().compareTo(limite) > 0; n++) {
            termino = termino.multiply(y2, g).divide(BigDecimal.valueOf((2L * n - 1) * (2L * n)), g).negate();
            suma = suma.add(termino, g);
        }
        return suma.round(mc);
    }

    static BigDecimal exp(BigDecimal x, MathContext mc) {
        MathContext g = guarda(mc);
        int k = 0;
        BigDecimal y = x;
        while (y.abs().compareTo(new BigDecimal("0.5")) > 0) {
            y = y.divide(BigDecimal.valueOf(2), g);
            k++;
        }
        MathContext g2 = new MathContext(g.getPrecision() + k);
        BigDecimal termino = BigDecimal.ONE;
        BigDecimal suma = BigDecimal.ONE;
        BigDecimal limite = BigDecimal.ONE.movePointLeft(g2.getPrecision());
        for (int n = 1; termino.abs().compareTo(limite) > 0; n++) {
            termino = termino.multiply(y, g2).divide(BigDecimal.valueOf(n), g2);
            suma = suma.add(termino, g2);
        }
        for (int i = 0; i < k; i++) {
            suma = suma.multiply(suma, g2);
        }
        return suma.round(mc);
    }

    static BigDecimal ln(BigDecimal x, MathContext mc) {
        if (x.signum() <= 0) {
            throw new ArithmeticException("ln de un número no positivo");
        }
        MathContext g = guarda(mc);
        int k = 0;
        BigDecimal m = x;
        BigDecimal dos = BigDecimal.valueOf(2);
        while (m.compareTo(dos) > 0) {
            m = m.divide(dos, g);
            k++;
        }
        while (m.compareTo(BigDecimal.ONE) < 0) {
            m = m.multiply(dos, g);
            k--;
        }
        return lnCercaDeUno(m, g).add(lnCercaDeUno(dos, g).multiply(BigDecimal.valueOf(k), g), g).round(mc);
    }

    private static BigDecimal lnCercaDeUno(BigDecimal m, MathContext g) {
        BigDecimal z = m.subtract(BigDecimal.ONE, g).divide(m.add(BigDecimal.ONE, g), g);
        BigDecimal z2 = z.multiply(z, g);
        BigDecimal termino = z;
        BigDecimal suma = z;
        BigDecimal limite = BigDecimal.ONE.movePointLeft(g.getPrecision());
        for (int n = 1; termino.abs().compareTo(limite) > 0; n++) {
            termino = termino.multiply(z2, g);
            suma = suma.add(termino.divide(BigDecimal.valueOf(2L * n + 1), g), g);
        }
        return suma.multiply(BigDecimal.valueOf(2), g);
    }

    static BigDecimal raiz(BigDecimal x, int n, MathContext mc) {
        if (x.signum() == 0) {
            return BigDecimal.ZERO;
        }
        if (x.signum() < 0) {
            if (n % 2 == 0) {
                throw new ArithmeticException("Raíz par de un negativo");
            }
            return raiz(x.negate(), n, mc).negate();
        }
        if (n == 2) {
            return x.sqrt(mc);
        }
        MathContext g = guarda(mc);
        int escala = g.getPrecision();
        BigInteger entero = x.movePointRight(n * escala).toBigInteger();
        BigDecimal y = new BigDecimal(Primos.raizEntera(entero, n).max(BigInteger.ONE)).movePointLeft(escala);
        BigDecimal nn = BigDecimal.valueOf(n);
        for (int i = 0; i < 200; i++) {
            BigDecimal siguiente = y.multiply(nn.subtract(BigDecimal.ONE), g)
                    .add(x.divide(y.pow(n - 1, g), g), g).divide(nn, g);
            if (siguiente.subtract(y).abs().compareTo(BigDecimal.ONE.movePointLeft(g.getPrecision()).multiply(y.abs().max(BigDecimal.ONE))) <= 0) {
                y = siguiente;
                break;
            }
            y = siguiente;
        }
        return y.round(mc);
    }
}
