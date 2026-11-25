package pe.mathnova.domain.algebra;

import java.math.BigInteger;
import java.util.Objects;

public record Racional(BigInteger num, BigInteger den) implements Comparable<Racional> {

    public static final Racional CERO = new Racional(BigInteger.ZERO, BigInteger.ONE);
    public static final Racional UNO = new Racional(BigInteger.ONE, BigInteger.ONE);

    public Racional {
        Objects.requireNonNull(num, "num");
        Objects.requireNonNull(den, "den");
        if (den.signum() == 0) {
            throw new ArithmeticException("Denominador cero");
        }
        if (den.signum() < 0) {
            num = num.negate();
            den = den.negate();
        }
        BigInteger mcd = num.gcd(den);
        if (!mcd.equals(BigInteger.ONE)) {
            num = num.divide(mcd);
            den = den.divide(mcd);
        }
    }

    public static Racional de(long n) {
        return new Racional(BigInteger.valueOf(n), BigInteger.ONE);
    }

    public static Racional de(long num, long den) {
        return new Racional(BigInteger.valueOf(num), BigInteger.valueOf(den));
    }

    public static Racional de(BigInteger n) {
        return new Racional(n, BigInteger.ONE);
    }

    public static Racional parse(String texto) {
        String s = Objects.requireNonNull(texto, "texto").trim();
        int barra = s.indexOf('/');
        if (barra >= 0) {
            return parse(s.substring(0, barra)).dividir(parse(s.substring(barra + 1)));
        }
        boolean negativo = false;
        if (s.startsWith("-") || s.startsWith("+")) {
            negativo = s.charAt(0) == '-';
            s = s.substring(1);
        }
        int punto = s.indexOf('.');
        String entera = punto < 0 ? s : s.substring(0, punto);
        String decimales = punto < 0 ? "" : s.substring(punto + 1);
        if (entera.isEmpty() || !soloDigitos(entera) || (punto >= 0 && (decimales.isEmpty() || !soloDigitos(decimales)))) {
            throw new NumberFormatException("No es un número: \"" + texto + "\"");
        }
        BigInteger n = new BigInteger(entera + decimales);
        BigInteger d = BigInteger.TEN.pow(decimales.length());
        return new Racional(negativo ? n.negate() : n, d);
    }

    private static boolean soloDigitos(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public Racional sumar(Racional o) {
        return new Racional(num.multiply(o.den).add(o.num.multiply(den)), den.multiply(o.den));
    }

    public Racional restar(Racional o) {
        return sumar(o.negar());
    }

    public Racional multiplicar(Racional o) {
        return new Racional(num.multiply(o.num), den.multiply(o.den));
    }

    public Racional dividir(Racional o) {
        if (o.esCero()) {
            throw new ArithmeticException("División entre cero");
        }
        return new Racional(num.multiply(o.den), den.multiply(o.num));
    }

    public Racional negar() {
        return new Racional(num.negate(), den);
    }

    public Racional inverso() {
        return UNO.dividir(this);
    }

    public Racional potencia(int n) {
        if (n < 0) {
            return inverso().potencia(-n);
        }
        return new Racional(num.pow(n), den.pow(n));
    }

    public boolean esEntero() {
        return den.equals(BigInteger.ONE);
    }

    public boolean esCero() {
        return num.signum() == 0;
    }

    public int signo() {
        return num.signum();
    }

    @Override
    public int compareTo(Racional o) {
        return num.multiply(o.den).compareTo(o.num.multiply(den));
    }

    @Override
    public String toString() {
        return esEntero() ? num.toString() : num + "/" + den;
    }
}
