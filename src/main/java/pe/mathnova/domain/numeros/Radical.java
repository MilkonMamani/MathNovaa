package pe.mathnova.domain.numeros;

import java.math.BigInteger;
import java.util.Map;
import java.util.Objects;

import pe.mathnova.domain.algebra.Racional;

/**
 * coeficiente · ⁿ√radicando, real. Siempre simplificado: el radicando es un entero positivo sin
 * factores que sean potencias n-ésimas (√8 = 2√2, ∛16 = 2∛2) y el índice es el menor posible
 * (⁴√4 = √2). Un racional es un radical de índice 1 y radicando 1. Con índice impar y radicando
 * negativo, el signo pasa al coeficiente (∛−2 = −∛2); la raíz par de un negativo no es real y la
 * lleva NumeroAlgebraico con la unidad i.
 */
public record Radical(Racional coeficiente, int indice, BigInteger radicando) {

    public static final Radical UNO = new Radical(Racional.UNO, 1, BigInteger.ONE);

    public Radical {
        Objects.requireNonNull(coeficiente, "coeficiente");
        Objects.requireNonNull(radicando, "radicando");
        if (indice < 1) {
            throw new IllegalArgumentException("El índice debe ser al menos 1");
        }
        if (radicando.signum() < 0) {
            if (indice % 2 == 0) {
                throw new ArithmeticException("La raíz de índice par de un negativo no es real");
            }
            coeficiente = coeficiente.negar();
            radicando = radicando.negate();
        }
        if (radicando.signum() == 0 || coeficiente.esCero()) {
            coeficiente = Racional.CERO;
            indice = 1;
            radicando = BigInteger.ONE;
        } else if (indice > 1 && !radicando.equals(BigInteger.ONE)) {
            // Se sacan las potencias n-ésimas y se reduce el índice por el MCD de los exponentes.
            Map<BigInteger, Integer> primos = Primos.factorizar(radicando);
            BigInteger fuera = BigInteger.ONE;
            int g = indice;
            for (var e : primos.entrySet()) {
                fuera = fuera.multiply(e.getKey().pow(e.getValue() / indice));
                g = Primos.mcd(g, e.getValue() % indice);
            }
            BigInteger dentro = BigInteger.ONE;
            int nuevo = g == 0 ? 1 : indice / g;
            for (var e : primos.entrySet()) {
                int resto = e.getValue() % indice;
                if (resto != 0) {
                    dentro = dentro.multiply(e.getKey().pow(resto / g));
                }
            }
            coeficiente = coeficiente.multiplicar(Racional.de(fuera));
            indice = dentro.equals(BigInteger.ONE) ? 1 : nuevo;
            radicando = dentro;
        } else {
            coeficiente = coeficiente.multiplicar(Racional.de(radicando)); // índice 1: ¹√r = r
            indice = 1;
            radicando = BigInteger.ONE;
        }
    }

    public static Radical de(Racional r) {
        return new Radical(r, 1, BigInteger.ONE);
    }

    /** ⁿ√r con r racional: ⁿ√(a/b) = ⁿ√(a·bⁿ⁻¹)/b. */
    public static Radical raiz(int n, Racional r) {
        BigInteger b = r.den();
        return new Radical(new Racional(BigInteger.ONE, b), n, r.num().multiply(b.pow(n - 1)));
    }

    public static Radical raiz(int n, long r) {
        return raiz(n, Racional.de(r));
    }

    public boolean esRacional() {
        return indice == 1;
    }

    public boolean esCero() {
        return coeficiente.esCero();
    }

    /** La parte sin coeficiente: ⁿ√radicando. */
    public Radical unidad() {
        return new Radical(Racional.UNO, indice, radicando);
    }

    /** ⁿ√a · ᵐ√b = ˡ√(a^(l/n) · b^(l/m)) con l = mcm(n, m), ya simplificado. */
    public Radical multiplicar(Radical o) {
        int l = Primos.mcm(indice, o.indice);
        BigInteger r = radicando.pow(l / indice).multiply(o.radicando.pow(l / o.indice));
        return new Radical(coeficiente.multiplicar(o.coeficiente), l, r);
    }

    public Radical multiplicar(Racional k) {
        return new Radical(coeficiente.multiplicar(k), indice, radicando);
    }

    /** Potencia entera no negativa. */
    public Radical potencia(int k) {
        if (k < 0) {
            throw new IllegalArgumentException("Para exponentes negativos use NumeroAlgebraico.inverso");
        }
        return new Radical(coeficiente.potencia(k), indice, radicando.pow(k));
    }

    /** "√", "∛", "⁴√"… */
    static String simbolo(int indice) {
        return switch (indice) {
            case 2 -> "√";
            case 3 -> "∛";
            case 4 -> "∜";
            default -> superindice(indice) + "√";
        };
    }

    private static String superindice(int n) {
        String digitos = "⁰¹²³⁴⁵⁶⁷⁸⁹";
        StringBuilder sb = new StringBuilder();
        for (char c : Integer.toString(n).toCharArray()) {
            sb.append(digitos.charAt(c - '0'));
        }
        return sb.toString();
    }

    /** "2√2", "-∛4", "√6/2", "3/2". */
    @Override
    public String toString() {
        if (esRacional()) {
            return coeficiente.toString();
        }
        String raiz = simbolo(indice) + radicando;
        Racional c = coeficiente;
        String signo = c.signo() < 0 ? "-" : "";
        BigInteger num = c.num().abs();
        String arriba = num.equals(BigInteger.ONE) ? raiz : num + raiz;
        return signo + (c.esEntero() ? arriba : arriba + "/" + c.den());
    }
}
