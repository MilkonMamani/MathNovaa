package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import pe.mathnova.domain.algebra.Racional;

/**
 * Teorema de las raíces racionales: los candidatos p/q, con p divisor del término independiente y
 * q del coeficiente principal, en el orden de RN09: enteros positivos de menor a mayor, luego los
 * negativos, luego las fracciones positivas y las negativas.
 */
final class RaicesRacionales {

    /** Más allá de esto no se enumeran divisores (el banco no pasa de tres cifras). */
    static final BigInteger LIMITE = BigInteger.valueOf(1_000_000_000_000L);

    private RaicesRacionales() {
    }

    /** Vacío si los coeficientes son demasiado grandes. El polinomio se pasa a enteros primitivos. */
    static Optional<List<Racional>> candidatos(Univariado u) {
        Univariado e = u.primitivo();
        BigInteger a0 = e.coef(0).num().abs();
        BigInteger an = e.principal().num().abs();
        if (a0.signum() == 0) {
            return Optional.of(List.of(Racional.CERO));
        }
        if (a0.compareTo(LIMITE) > 0 || an.compareTo(LIMITE) > 0) {
            return Optional.empty();
        }
        TreeSet<Racional> positivos = new TreeSet<>();
        for (long p : divisores(a0.longValueExact())) {
            for (long q : divisores(an.longValueExact())) {
                positivos.add(Racional.de(p, q));
            }
        }
        List<Racional> r = new ArrayList<>();
        positivos.stream().filter(Racional::esEntero).forEach(r::add);
        positivos.stream().filter(Racional::esEntero).map(Racional::negar).forEach(r::add);
        positivos.stream().filter(x -> !x.esEntero()).forEach(r::add);
        positivos.stream().filter(x -> !x.esEntero()).map(Racional::negar).forEach(r::add);
        return Optional.of(r);
    }

    /** "±1, ±2, ±1/2" (a lo más {@code maximo}, luego "…"). */
    static String texto(List<Racional> candidatos, int maximo) {
        List<String> textos = new ArrayList<>();
        candidatos.stream().filter(c -> c.signo() > 0).forEach(c -> textos.add("±" + c));
        if (textos.size() > maximo) {
            List<String> corta = new ArrayList<>(textos.subList(0, maximo));
            corta.add("…");
            return String.join(", ", corta);
        }
        return String.join(", ", textos);
    }

    static List<Long> divisores(long n) {
        List<Long> chicos = new ArrayList<>();
        List<Long> grandes = new ArrayList<>();
        for (long d = 1; d * d <= n; d++) {
            if (n % d == 0) {
                chicos.add(d);
                if (d != n / d) {
                    grandes.addFirst(n / d);
                }
            }
        }
        chicos.addAll(grandes);
        return chicos;
    }
}
