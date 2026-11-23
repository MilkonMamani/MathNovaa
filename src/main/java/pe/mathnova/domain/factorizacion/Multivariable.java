package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Cuentas con polinomios en varias letras (HU27, sección 2.13) que usan M04–M08 y A6: la variable
 * principal, si es homogéneo, sustituir una letra por un polinomio, la división exacta y la reducción de
 * Kronecker para dos y tres letras.
 */
final class Multivariable {

    /** Hasta este grado total se certifica con A6 (RF51). */
    static final int GRADO_TOTAL_MAXIMO = 6;
    /** Grado máximo del polinomio en una letra que deja la reducción de Kronecker. */
    private static final int GRADO_REDUCIDO_MAXIMO = 40;
    private static final int COMBINACIONES_MAXIMAS = 4096;

    private Multivariable() {
    }

    /** La primera letra de x, y, z que aparezca; si no, la primera en orden alfabético (a, b, …). */
    static char principal(Polinomio p) {
        SortedSet<Character> v = p.variables();
        for (char c : new char[] {'x', 'y', 'z'}) {
            if (v.contains(c)) {
                return c;
            }
        }
        return v.first();
    }

    /** Con dos letras, la que no es la principal. */
    static char segunda(Polinomio p) {
        char x = principal(p);
        return p.variables().stream().filter(c -> c != x).findFirst().orElseThrow();
    }

    /** Todos los términos del mismo grado total. */
    static boolean homogeneo(Polinomio p) {
        int g = p.grado();
        return p.terminos().keySet().stream().allMatch(m -> m.grado() == g);
    }

    /** p con la letra v cambiada por q. */
    static Polinomio sustituir(Polinomio p, char v, Polinomio q) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(v);
            TreeMap<Character, Integer> resto = new TreeMap<>(t.getKey().exponentes());
            resto.remove(v);
            r = r.sumar(Polinomio.termino(t.getValue(), new Monomio(resto)).multiplicar(q.potencia(e)));
        }
        return r;
    }

    private static boolean divide(Monomio a, Monomio b) {
        return a.exponentes().entrySet().stream().allMatch(e -> b.exponente(e.getKey()) >= e.getValue());
    }

    /**
     * p/q si es exacta. Con un solo divisor, el algoritmo de división (orden por grado y luego
     * lexicográfico) deja resto 0 si y solo si q divide a p.
     */
    static Optional<Polinomio> dividir(Polinomio p, Polinomio q) {
        if (q.esCero()) {
            return Optional.empty();
        }
        Monomio lq = q.terminos().firstKey();
        Racional cq = q.terminos().get(lq);
        Polinomio resto = p;
        Polinomio cociente = Polinomio.CERO;
        int vueltas = 0;
        while (!resto.esCero()) {
            Monomio lr = resto.terminos().firstKey();
            if (!divide(lq, lr) || ++vueltas > 10_000) {
                return Optional.empty();
            }
            Polinomio t = Polinomio.termino(resto.terminos().get(lr).dividir(cq), lr.dividir(lq));
            cociente = cociente.sumar(t);
            resto = resto.restar(t.multiplicar(q));
        }
        return Optional.of(cociente);
    }

    /** Coeficiente de xᵏ como polinomio en las demás letras. */
    static Polinomio coeficienteEn(Polinomio p, char x, int k) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            if (t.getKey().exponente(x) == k) {
                TreeMap<Character, Integer> resto = new TreeMap<>(t.getKey().exponentes());
                resto.remove(x);
                r = r.sumar(Polinomio.termino(t.getValue(), new Monomio(resto)));
            }
        }
        return r;
    }

    /** Lo que hace la reducción de Kronecker (A6). */
    record Reduccion(char principal, List<Character> otras, List<Integer> pesos, Polinomio enUnaLetra,
                     Optional<Polinomio> factor, boolean completa) {

        /** "y = x³" o "y = x³, z = x⁹". */
        String sustitucion() {
            List<String> r = new ArrayList<>();
            for (int i = 0; i < otras.size(); i++) {
                r.add(otras.get(i) + " = " + principal + Formato.superindice(pesos.get(i)));
            }
            return String.join(", ", r);
        }

        boolean irreducible() {
            return factor.isEmpty() && completa;
        }
    }

    /**
     * A6, reducción de Kronecker: con y = x^d (y z = x^(d·e)), d mayor que el grado en x y e mayor
     * que el grado en y, cada factor de p pasa a un factor del polinomio en una letra y se puede regresar
     * sin ambigüedad. Se factoriza ese polinomio en Q y se prueban todas las combinaciones de sus factores.
     *
     * @return vacío si no hay 2 o 3 letras o el grado total pasa de 6
     */
    static Optional<Reduccion> kronecker(Polinomio p) {
        SortedSet<Character> letras = p.variables();
        if (letras.size() < 2 || letras.size() > 3 || p.grado() > GRADO_TOTAL_MAXIMO) {
            return Optional.empty();
        }
        char x = principal(p);
        List<Character> otras = letras.stream().filter(c -> c != x).toList();
        List<Integer> pesos = new ArrayList<>();
        List<Integer> bases = new ArrayList<>();
        bases.add(p.gradoEn(x) + 1);
        int peso = bases.getFirst();
        for (int i = 0; i < otras.size(); i++) {
            pesos.add(peso);
            int base = p.gradoEn(otras.get(i)) + 1;
            bases.add(base);
            peso *= base;
        }
        Polinomio u = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : p.terminos().entrySet()) {
            int e = t.getKey().exponente(x);
            for (int i = 0; i < otras.size(); i++) {
                e += t.getKey().exponente(otras.get(i)) * pesos.get(i);
            }
            u = u.sumar(Polinomio.termino(t.getValue(), Monomio.de(x, e)));
        }
        if (u.grado() > GRADO_REDUCIDO_MAXIMO) {
            return Optional.of(new Reduccion(x, otras, pesos, u, Optional.empty(), false));
        }
        Resolucion r = Univariados.factorizar(u);
        List<Factor> fs = r.resultado().factores();
        boolean completa = r.irreducibles().stream().allMatch(Irreducibilidad::certificada);
        int[] maximos = fs.stream().mapToInt(Factor::multiplicidad).toArray();
        int[] actual = new int[maximos.length];
        int probadas = 0;
        while (avanzar(actual, maximos)) {
            if (++probadas > COMBINACIONES_MAXIMAS) {
                completa = false;
                break;
            }
            if (java.util.Arrays.equals(actual, maximos)) {
                continue;
            }
            Polinomio candidato = Polinomio.UNO;
            for (int i = 0; i < actual.length; i++) {
                candidato = candidato.multiplicar(fs.get(i).polinomio().potencia(actual[i]));
            }
            Optional<Polinomio> q = regresar(candidato, x, otras, bases);
            if (q.isEmpty() || q.get().esConstante()) {
                continue;
            }
            Optional<Polinomio> cociente = dividir(p, q.get());
            if (cociente.isPresent() && !cociente.get().esConstante()) {
                return Optional.of(new Reduccion(x, otras, pesos, u, q, completa));
            }
        }
        return Optional.of(new Reduccion(x, otras, pesos, u, Optional.empty(), completa));
    }

    /** Cuenta en base mixta: 0…maximos[i] en cada posición. */
    private static boolean avanzar(int[] actual, int[] maximos) {
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] < maximos[i]) {
                actual[i]++;
                return true;
            }
            actual[i] = 0;
        }
        return false;
    }

    /** x^n → x^(n mod d)·y^(⌊n/d⌋ mod e)·z^(⌊n/(d·e)⌋). Vacío si algún exponente no cabe. */
    private static Optional<Polinomio> regresar(Polinomio u, char x, List<Character> otras, List<Integer> bases) {
        Polinomio r = Polinomio.CERO;
        for (Map.Entry<Monomio, Racional> t : u.terminos().entrySet()) {
            int n = t.getKey().exponente(x);
            TreeMap<Character, Integer> e = new TreeMap<>();
            e.put(x, n % bases.getFirst());
            n /= bases.getFirst();
            for (int i = 0; i < otras.size(); i++) {
                e.put(otras.get(i), n % bases.get(i + 1));
                n /= bases.get(i + 1);
            }
            if (n != 0) {
                return Optional.empty();
            }
            r = r.sumar(Polinomio.termino(t.getValue(), new Monomio(e)));
        }
        return Optional.of(r);
    }

    /** Divisores positivos de |n| (n ≠ 0). */
    static List<BigInteger> divisores(BigInteger n) {
        BigInteger a = n.abs();
        List<BigInteger> r = new ArrayList<>();
        if (a.bitLength() > 40) {
            return r;
        }
        long m = a.longValueExact();
        for (long d = 1; d * d <= m; d++) {
            if (m % d == 0) {
                r.add(BigInteger.valueOf(d));
                if (d != m / d) {
                    r.add(BigInteger.valueOf(m / d));
                }
            }
        }
        r.sort(null);
        return r;
    }

    /** El orquestador para los polinomios en una letra (t, o x después de la reducción). */
    static final class Univariados {
        private static OrquestadorFactorizacion instancia;

        private Univariados() {
        }

        static synchronized Resolucion factorizar(Polinomio p) {
            if (instancia == null) {
                instancia = new OrquestadorFactorizacion();
            }
            return instancia.resolver(p.aExpresion(), Campo.Q);
        }
    }
}
