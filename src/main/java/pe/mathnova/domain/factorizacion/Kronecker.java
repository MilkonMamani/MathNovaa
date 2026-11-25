package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;

final class Kronecker {

    static final int GRADO_MAXIMO = 8;
    private static final long COMBINACIONES_MAXIMAS = 20_000_000L;
    private static final Map<Univariado, Respuesta> MEMORIA = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Univariado, Respuesta> e) {
            return size() > 256;
        }
    };

    record Respuesta(Optional<Univariado> factor, boolean completa) {
        boolean irreducible() {
            return factor.isEmpty() && completa;
        }
    }

    private Kronecker() {
    }

    static Respuesta buscar(Univariado u) {
        Univariado p = u.primitivo();
        synchronized (MEMORIA) {
            Respuesta r = MEMORIA.get(p);
            if (r != null) {
                return r;
            }
        }
        Respuesta r = calcular(p);
        synchronized (MEMORIA) {
            MEMORIA.put(p, r);
        }
        return r;
    }

    private static Respuesta calcular(Univariado p) {
        int n = p.grado();
        if (n <= 1) {
            return new Respuesta(Optional.empty(), true);
        }
        if (n > GRADO_MAXIMO || !p.enteros()) {
            return new Respuesta(Optional.empty(), false);
        }
        Optional<List<Racional>> candidatos = RaicesRacionales.candidatos(p);
        if (candidatos.isEmpty()) {
            return new Respuesta(Optional.empty(), false);
        }
        for (Racional r : candidatos.get()) {
            if (p.evaluar(r).esCero()) {
                return new Respuesta(Optional.of(Univariado.de(p.variable(),
                        List.of(Racional.de(r.num()).negar(), Racional.de(r.den())))), true);
            }
        }
        boolean completa = true;
        for (int k = 2; k <= n / 2; k++) {
            Busqueda b = new Busqueda(p, k);
            Optional<Univariado> f = b.correr();
            if (f.isPresent()) {
                return new Respuesta(f, true);
            }
            completa &= b.completa;
        }
        return new Respuesta(Optional.empty(), completa);
    }

    private static final class Busqueda {

        private final Univariado p;
        private final int k;
        private final long an;
        private final long a0;
        private long[] puntos;
        private List<long[]> divisores;
        private long[][] pesos;
        private long denominador;
        private boolean completa = true;

        Busqueda(Univariado p, int k) {
            this.p = p;
            this.k = k;
            this.an = p.principal().num().abs().longValueExact();
            this.a0 = p.coef(0).num().abs().longValueExact();
        }

        Optional<Univariado> correr() {
            if (!elegirPuntos()) {
                completa = false;
                return Optional.empty();
            }
            long total = 1;
            for (long[] d : divisores) {
                total = Math.multiplyExact(total, d.length);
                if (total > COMBINACIONES_MAXIMAS) {
                    completa = false;
                    return Optional.empty();
                }
            }
            long[] valores = new long[k + 1];
            try {
                return probar(0, valores);
            } catch (ArithmeticException desborde) {
                completa = false;
                return Optional.empty();
            }
        }

        private boolean elegirPuntos() {
            List<long[]> opciones = new ArrayList<>();
            for (int i = 0; i <= 4 * k + 6; i++) {
                long x = i % 2 == 0 ? i / 2 : -(i + 1) / 2;
                Racional v = p.evaluar(Racional.de(x));
                BigInteger num = v.num().abs();
                if (num.bitLength() > 40) {
                    continue;
                }
                long valor = num.longValueExact();
                List<Long> d = RaicesRacionales.divisores(valor);
                opciones.add(new long[] {x, valor, d.size()});
            }
            if (opciones.size() < k + 1) {
                return false;
            }
            opciones.sort(Comparator.comparingLong((long[] o) -> o[2]).thenComparingLong(o -> Math.abs(o[0])));
            puntos = new long[k + 1];
            divisores = new ArrayList<>();
            for (int i = 0; i <= k; i++) {
                long[] o = opciones.get(i);
                puntos[i] = o[0];
                List<Long> d = RaicesRacionales.divisores(o[1]);
                long[] conSigno = new long[i == 0 ? d.size() : 2 * d.size()];
                for (int j = 0; j < d.size(); j++) {
                    conSigno[j] = d.get(j);
                    if (i > 0) {
                        conSigno[d.size() + j] = -d.get(j);
                    }
                }
                divisores.add(conSigno);
            }
            preparar();
            return true;
        }

        private void preparar() {
            int m = k + 1;
            long[][] numeradores = new long[m][];
            long[] dens = new long[m];
            for (int i = 0; i < m; i++) {
                long[] poli = {1};
                long den = 1;
                for (int j = 0; j < m; j++) {
                    if (j == i) {
                        continue;
                    }
                    poli = porLineal(poli, -puntos[j]);
                    den = Math.multiplyExact(den, puntos[i] - puntos[j]);
                }
                numeradores[i] = poli;
                dens[i] = den;
            }
            long d = 1;
            for (long x : dens) {
                d = mcm(d, Math.abs(x));
            }
            denominador = d;
            pesos = new long[m][];
            for (int i = 0; i < m; i++) {
                long factor = d / dens[i];
                pesos[i] = new long[numeradores[i].length];
                for (int j = 0; j < numeradores[i].length; j++) {
                    pesos[i][j] = Math.multiplyExact(numeradores[i][j], factor);
                }
            }
        }

        private Optional<Univariado> probar(int i, long[] valores) {
            if (i == valores.length) {
                return interpolar(valores);
            }
            for (long d : divisores.get(i)) {
                valores[i] = d;
                Optional<Univariado> r = probar(i + 1, valores);
                if (r.isPresent()) {
                    return r;
                }
            }
            return Optional.empty();
        }

        private Optional<Univariado> interpolar(long[] valores) {
            long lider = coeficiente(valores, k);
            if (lider == 0 || lider % denominador != 0 || an % Math.abs(lider / denominador) != 0) {
                return Optional.empty();
            }
            long independiente = coeficiente(valores, 0);
            if (independiente == 0 || independiente % denominador != 0
                    || a0 % Math.abs(independiente / denominador) != 0) {
                return Optional.empty();
            }
            List<Racional> c = new ArrayList<>();
            for (int j = 0; j <= k; j++) {
                long v = j == k ? lider : j == 0 ? independiente : coeficiente(valores, j);
                if (v % denominador != 0) {
                    return Optional.empty();
                }
                c.add(Racional.de(v / denominador));
            }
            Univariado g = Univariado.de(p.variable(), c);
            return p.cocienteExacto(g).filter(Univariado::enteros).map(q -> g);
        }

        private long coeficiente(long[] valores, int j) {
            long s = 0;
            for (int i = 0; i < valores.length; i++) {
                if (j < pesos[i].length) {
                    s = Math.addExact(s, Math.multiplyExact(valores[i], pesos[i][j]));
                }
            }
            return s;
        }

        private static long[] porLineal(long[] poli, long c) {
            long[] r = new long[poli.length + 1];
            for (int i = 0; i < poli.length; i++) {
                r[i + 1] = Math.addExact(r[i + 1], poli[i]);
                r[i] = Math.addExact(r[i], Math.multiplyExact(poli[i], c));
            }
            return r;
        }

        private static long mcm(long a, long b) {
            return a / BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).longValueExact() * b;
        }
    }
}
