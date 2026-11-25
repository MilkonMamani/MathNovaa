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

final class Multivariable {

    static final int GRADO_TOTAL_MAXIMO = 6;
    private static final int GRADO_REDUCIDO_MAXIMO = 40;
    private static final int COMBINACIONES_MAXIMAS = 4096;

    private Multivariable() {
    }

    static char principal(Polinomio p) {
        SortedSet<Character> v = p.variables();
        for (char c : new char[] {'x', 'y', 'z'}) {
            if (v.contains(c)) {
                return c;
            }
        }
        return v.first();
    }

    static char segunda(Polinomio p) {
        char x = principal(p);
        return p.variables().stream().filter(c -> c != x).findFirst().orElseThrow();
    }

    static boolean homogeneo(Polinomio p) {
        int g = p.grado();
        return p.terminos().keySet().stream().allMatch(m -> m.grado() == g);
    }

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

    record Reduccion(char principal, List<Character> otras, List<Integer> pesos, Polinomio enUnaLetra,
                     Optional<Polinomio> factor, boolean completa) {

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
