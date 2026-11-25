package pe.mathnova.domain.numeros;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;

public final class NumeroAlgebraico {

    public static final NumeroAlgebraico CERO = new NumeroAlgebraico(new TreeMap<>());
    public static final NumeroAlgebraico UNO = de(Racional.UNO);
    public static final NumeroAlgebraico I = new NumeroAlgebraico(
            new TreeMap<>(Map.of(new Unidad(Radical.UNO, true, Collections.emptySortedMap()), Racional.UNO)));

    private static final int GRADO_INVERSO = 96;

    public record Unidad(Radical raiz, boolean imaginaria, SortedMap<Atomo, Integer> atomos)
            implements Comparable<Unidad> {

        public Unidad {
            Objects.requireNonNull(raiz, "raiz");
            if (!raiz.coeficiente().equals(Racional.UNO)) {
                throw new IllegalArgumentException("La raíz de una unidad va sin coeficiente");
            }
            atomos = Collections.unmodifiableSortedMap(new TreeMap<>(atomos));
        }

        static final Unidad UNO = new Unidad(Radical.UNO, false, Collections.emptySortedMap());

        boolean esUno() {
            return raiz.esRacional() && !imaginaria && atomos.isEmpty();
        }

        @Override
        public int compareTo(Unidad o) {
            return ORDEN.compare(this, o);
        }

        private static final Comparator<Unidad> ORDEN = Comparator
                .comparing(Unidad::imaginaria)
                .thenComparing(u -> !u.atomos.isEmpty())
                .thenComparingInt(u -> u.raiz.indice())
                .thenComparing(u -> u.raiz.radicando())
                .thenComparing(u -> u.atomos.toString());
    }

    private final SortedMap<Unidad, Racional> terminos;

    private NumeroAlgebraico(SortedMap<Unidad, Racional> terminos) {
        this.terminos = terminos;
    }

    public static NumeroAlgebraico de(Racional r) {
        TreeMap<Unidad, Racional> t = new TreeMap<>();
        if (!r.esCero()) {
            t.put(Unidad.UNO, r);
        }
        return new NumeroAlgebraico(t);
    }

    public static NumeroAlgebraico de(long n) {
        return de(Racional.de(n));
    }

    public static NumeroAlgebraico de(Radical r) {
        TreeMap<Unidad, Racional> t = new TreeMap<>();
        if (!r.esCero()) {
            t.put(new Unidad(r.unidad(), false, Collections.emptySortedMap()), r.coeficiente());
        }
        return new NumeroAlgebraico(t);
    }

    public static NumeroAlgebraico complejo(Racional a, Racional b) {
        return de(a).sumar(I.multiplicar(b));
    }

    public static NumeroAlgebraico raiz(int n, Racional q) {
        if (q.signo() < 0 && n % 2 == 0) {
            if (n != 2) {
                throw new ArithmeticException("ⁿ√ de un negativo con n par mayor que 2 no se escribe así");
            }
            return I.multiplicar(de(Radical.raiz(2, q.negar())));
        }
        return de(Radical.raiz(n, q));
    }

    public static NumeroAlgebraico raiz(int n, long q) {
        return raiz(n, Racional.de(q));
    }

    public static NumeroAlgebraico de(Atomo a) {
        TreeMap<Atomo, Integer> m = new TreeMap<>();
        m.put(a, 1);
        TreeMap<Unidad, Racional> t = new TreeMap<>();
        t.put(new Unidad(Radical.UNO, false, m), Racional.UNO);
        return new NumeroAlgebraico(t);
    }

    public NumeroAlgebraico sumar(NumeroAlgebraico o) {
        TreeMap<Unidad, Racional> r = new TreeMap<>(terminos);
        o.terminos.forEach((u, c) -> acumular(r, u, c));
        return new NumeroAlgebraico(r);
    }

    private static void acumular(Map<Unidad, Racional> r, Unidad u, Racional c) {
        Racional s = r.getOrDefault(u, Racional.CERO).sumar(c);
        if (s.esCero()) {
            r.remove(u);
        } else {
            r.put(u, s);
        }
    }

    public NumeroAlgebraico restar(NumeroAlgebraico o) {
        return sumar(o.negar());
    }

    public NumeroAlgebraico negar() {
        return multiplicar(Racional.UNO.negar());
    }

    public NumeroAlgebraico multiplicar(Racional k) {
        if (k.esCero()) {
            return CERO;
        }
        TreeMap<Unidad, Racional> r = new TreeMap<>();
        terminos.forEach((u, c) -> r.put(u, c.multiplicar(k)));
        return new NumeroAlgebraico(r);
    }

    public NumeroAlgebraico multiplicar(NumeroAlgebraico o) {
        NumeroAlgebraico r = CERO;
        TreeMap<Unidad, Racional> simples = new TreeMap<>();
        for (var a : terminos.entrySet()) {
            for (var b : o.terminos.entrySet()) {
                Racional c = a.getValue().multiplicar(b.getValue());
                Producto2 p = producto(a.getKey(), b.getKey());
                if (p.extra == null) {
                    acumular(simples, p.unidad, c.multiplicar(p.factor));
                } else {
                    r = r.sumar(p.extra.multiplicar(c.multiplicar(p.factor)));
                }
            }
        }
        return r.sumar(new NumeroAlgebraico(simples));
    }

    private record Producto2(Racional factor, Unidad unidad, NumeroAlgebraico extra) {
    }

    private static Producto2 producto(Unidad a, Unidad b) {
        Radical r = a.raiz().multiplicar(b.raiz());
        Racional factor = r.coeficiente();
        boolean im = a.imaginaria() ^ b.imaginaria();
        if (a.imaginaria() && b.imaginaria()) {
            factor = factor.negar();
        }
        if (a.atomos().isEmpty() && b.atomos().isEmpty()) {
            return new Producto2(factor, new Unidad(r.unidad(), im, Collections.emptySortedMap()), null);
        }
        TreeMap<Atomo, Integer> atomos = new TreeMap<>(a.atomos());
        b.atomos().forEach((t, e) -> atomos.merge(t, e, Integer::sum));
        NumeroAlgebraico extra = null;
        for (var it = atomos.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Atomo t = e.getKey();
            int k = e.getValue();
            if (t.indice() > 0 && k >= t.indice()) {
                NumeroAlgebraico reducido = t.radicando().potencia(k / t.indice());
                extra = extra == null ? reducido : extra.multiplicar(reducido);
                k %= t.indice();
                if (k == 0) {
                    it.remove();
                } else {
                    e.setValue(k);
                }
            }
        }
        Unidad u = new Unidad(r.unidad(), im, atomos);
        if (extra == null) {
            return new Producto2(factor, u, null);
        }
        TreeMap<Unidad, Racional> uno = new TreeMap<>();
        uno.put(u, Racional.UNO);
        return new Producto2(factor, Unidad.UNO, extra.multiplicar(new NumeroAlgebraico(uno)));
    }

    public NumeroAlgebraico potencia(int n) {
        if (n < 0) {
            return inverso().potencia(-n);
        }
        NumeroAlgebraico r = UNO;
        NumeroAlgebraico b = this;
        int k = n;
        while (k > 0) {
            if ((k & 1) == 1) {
                r = r.multiplicar(b);
            }
            k >>= 1;
            if (k > 0) {
                b = b.multiplicar(b);
            }
        }
        return r;
    }

    public NumeroAlgebraico inverso() {
        if (esCero()) {
            throw new ArithmeticException("División entre cero");
        }
        if (terminos.size() == 1) {
            var t = terminos.entrySet().iterator().next();
            Unidad u = t.getKey();
            if (u.atomos().isEmpty()) {
                int m = Primos.mcm(u.raiz().indice(), u.imaginaria() ? 4 : 1);
                NumeroAlgebraico zm = potencia(m);
                if (zm.esRacional()) {
                    return potencia(m - 1).multiplicar(zm.aRacional().inverso());
                }
            }
        }
        List<Racional> c = polinomioMinimo();
        if (c.getFirst().esCero()) {
            throw new ArithmeticException("No se puede invertir " + this + " de forma exacta");
        }
        NumeroAlgebraico r = CERO;
        for (int j = c.size() - 1; j >= 1; j--) {
            r = r.multiplicar(this).sumar(de(c.get(j)));
        }
        return r.multiplicar(c.getFirst().inverso().negar());
    }

    public List<Racional> polinomioMinimo() {
        List<Map<Unidad, Racional>> filas = new ArrayList<>();
        List<Unidad> pivotes = new ArrayList<>();
        List<Racional[]> combinaciones = new ArrayList<>();
        NumeroAlgebraico potencia = UNO;
        for (int k = 0; k <= GRADO_INVERSO; k++) {
            Map<Unidad, Racional> v = new HashMap<>(potencia.terminos);
            Racional[] comb = new Racional[k + 1];
            java.util.Arrays.fill(comb, Racional.CERO);
            comb[k] = Racional.UNO;
            for (int f = 0; f < filas.size(); f++) {
                Racional x = v.get(pivotes.get(f));
                if (x == null) {
                    continue;
                }
                Racional factor = x.dividir(filas.get(f).get(pivotes.get(f)));
                filas.get(f).forEach((u, c) -> acumular(v, u, c.multiplicar(factor).negar()));
                Racional[] cf = combinaciones.get(f);
                for (int j = 0; j < cf.length; j++) {
                    comb[j] = comb[j].restar(cf[j].multiplicar(factor));
                }
            }
            if (v.isEmpty()) {
                List<Racional> r = new ArrayList<>(List.of(comb));
                Racional principal = r.getLast();
                r.replaceAll(c -> c.dividir(principal));
                return r;
            }
            Unidad pivote = v.keySet().iterator().next();
            filas.add(v);
            pivotes.add(pivote);
            combinaciones.add(comb);
            potencia = potencia.multiplicar(this);
        }
        throw new ArithmeticException("No se encontró el polinomio mínimo de " + this);
    }

    public NumeroAlgebraico dividir(NumeroAlgebraico o) {
        return multiplicar(o.inverso());
    }

    public NumeroAlgebraico dividir(Racional k) {
        return multiplicar(k.inverso());
    }

    public NumeroAlgebraico conjugar() {
        TreeMap<Unidad, Racional> r = new TreeMap<>();
        terminos.forEach((u, c) -> r.put(u, u.imaginaria() ? c.negar() : c));
        return new NumeroAlgebraico(r);
    }

    public NumeroAlgebraico parteReal() {
        TreeMap<Unidad, Racional> r = new TreeMap<>();
        terminos.forEach((u, c) -> {
            if (!u.imaginaria()) {
                r.put(u, c);
            }
        });
        return new NumeroAlgebraico(r);
    }

    public NumeroAlgebraico parteImaginaria() {
        TreeMap<Unidad, Racional> r = new TreeMap<>();
        terminos.forEach((u, c) -> {
            if (u.imaginaria()) {
                r.put(new Unidad(u.raiz(), false, u.atomos()), c);
            }
        });
        return new NumeroAlgebraico(r);
    }

    public NumeroAlgebraico raizCuadrada() {
        if (esCero()) {
            return CERO;
        }
        if (esRacional()) {
            return raiz(2, aRacional());
        }
        if (!esReal()) {
            NumeroAlgebraico a = parteReal();
            NumeroAlgebraico b = parteImaginaria();
            NumeroAlgebraico modulo = a.multiplicar(a).sumar(b.multiplicar(b)).raizCuadrada();
            NumeroAlgebraico re = modulo.sumar(a).multiplicar(Racional.de(1, 2)).raizCuadrada();
            NumeroAlgebraico im = modulo.restar(a).multiplicar(Racional.de(1, 2)).raizCuadrada();
            return re.sumar(I.multiplicar(b.signo() < 0 ? im.negar() : im));
        }
        if (signo() < 0) {
            return I.multiplicar(negar().raizCuadrada());
        }
        NumeroAlgebraico desanidada = desanidar();
        if (desanidada != null) {
            return desanidada;
        }
        return raizReal(2);
    }

    private NumeroAlgebraico desanidar() {
        if (terminos.size() > 2 || !exacto()) {
            return null;
        }
        Racional a = Racional.CERO;
        Racional b = null;
        BigInteger c = null;
        for (var t : terminos.entrySet()) {
            Radical r = t.getKey().raiz();
            if (r.esRacional()) {
                a = t.getValue();
            } else if (r.indice() == 2) {
                b = t.getValue();
                c = r.radicando();
            } else {
                return null;
            }
        }
        if (b == null) {
            return null;
        }
        Racional d2 = a.multiplicar(a).restar(b.multiplicar(b).multiplicar(Racional.de(c)));
        if (d2.signo() < 0) {
            return null;
        }
        Radical d = Radical.raiz(2, d2);
        if (!d.esRacional()) {
            return null;
        }
        Racional mitad = Racional.de(1, 2);
        Racional m = a.sumar(d.coeficiente()).multiplicar(mitad);
        Racional n = a.restar(d.coeficiente()).multiplicar(mitad);
        if (m.signo() < 0 || n.signo() < 0) {
            return null;
        }
        NumeroAlgebraico sn = raiz(2, n);
        return raiz(2, m).sumar(b.signo() < 0 ? sn.negar() : sn);
    }

    public NumeroAlgebraico raizCubicaReal() {
        return raizReal(3);
    }

    public NumeroAlgebraico raizReal(int n) {
        if (!esReal()) {
            throw new ArithmeticException("La raíz real necesita un número real: " + this);
        }
        if (esCero()) {
            return CERO;
        }
        if (esRacional()) {
            return raiz(n, aRacional());
        }
        if (terminos.size() == 1 && exacto()) {
            var t = terminos.entrySet().iterator().next();
            Radical r = t.getKey().raiz();
            Racional c = t.getValue();
            if (c.signo() < 0 && n % 2 == 0) {
                throw new ArithmeticException("Raíz par de un negativo: " + this);
            }
            return de(Radical.raiz(n * r.indice(), c.potencia(r.indice()).multiplicar(Racional.de(r.radicando()))));
        }
        int s = signo();
        if (s < 0) {
            if (n % 2 == 0) {
                throw new ArithmeticException("Raíz par de un negativo: " + this);
            }
            return negar().raizReal(n).negar();
        }
        return de(Atomo.raiz(n, this));
    }

    public boolean esCero() {
        return terminos.isEmpty();
    }

    public boolean esRacional() {
        return terminos.isEmpty() || terminos.size() == 1 && terminos.firstKey().esUno();
    }

    public Racional aRacional() {
        if (!esRacional()) {
            throw new ArithmeticException(this + " no es racional");
        }
        return terminos.isEmpty() ? Racional.CERO : terminos.get(Unidad.UNO);
    }

    public boolean esReal() {
        return terminos.keySet().stream().noneMatch(Unidad::imaginaria);
    }

    public boolean exacto() {
        return terminos.keySet().stream().allMatch(u -> u.atomos().isEmpty());
    }

    public boolean tieneRadicales() {
        return terminos.keySet().stream().anyMatch(u -> !u.raiz().esRacional() || !u.atomos().isEmpty());
    }

    public SortedMap<Unidad, Racional> terminos() {
        return Collections.unmodifiableSortedMap(terminos);
    }

    public int signo() {
        if (!esReal()) {
            throw new ArithmeticException("Un complejo no tiene signo: " + this);
        }
        if (esCero()) {
            return 0;
        }
        if (esRacional()) {
            return aRacional().signo();
        }
        if (!exacto()) {
            return EvaluacionNumerica.signo(this);
        }
        for (int digitos = 16; digitos <= 4096; digitos *= 2) {
            Racional[] intervalo = intervalo(digitos);
            if (intervalo[0].signo() > 0) {
                return 1;
            }
            if (intervalo[1].signo() < 0) {
                return -1;
            }
        }
        throw new ArithmeticException("No se pudo decidir el signo de " + this);
    }

    Racional[] intervalo(int digitos) {
        BigInteger escala = BigInteger.TEN.pow(digitos);
        Racional bajo = Racional.CERO;
        Racional alto = Racional.CERO;
        for (var t : terminos.entrySet()) {
            Radical r = t.getKey().raiz();
            Racional c = t.getValue();
            if (r.esRacional()) {
                bajo = bajo.sumar(c);
                alto = alto.sumar(c);
                continue;
            }
            BigInteger piso = Primos.raizEntera(r.radicando().multiply(escala.pow(r.indice())), r.indice());
            Racional lo = new Racional(piso, escala);
            Racional hi = new Racional(piso.add(BigInteger.ONE), escala);
            Racional a = c.multiplicar(lo);
            Racional b = c.multiplicar(hi);
            bajo = bajo.sumar(a.compareTo(b) <= 0 ? a : b);
            alto = alto.sumar(a.compareTo(b) <= 0 ? b : a);
        }
        return new Racional[] {bajo, alto};
    }

    public int comparar(NumeroAlgebraico o) {
        return restar(o).signo();
    }

    public Expresion aExpresion() {
        if (terminos.isEmpty()) {
            return Constante.de(0);
        }
        List<Expresion> sumandos = new ArrayList<>();
        terminos.forEach((u, c) -> sumandos.add(termino(c, u)));
        return sumandos.size() == 1 ? sumandos.getFirst() : new Suma(sumandos);
    }

    private static Expresion termino(Racional c, Unidad u) {
        Racional abs = c.signo() < 0 ? c.negar() : c;
        List<Expresion> partes = new ArrayList<>();
        if (!u.raiz().esRacional()) {
            partes.add(new Raiz(u.raiz().indice(), new Constante(Racional.de(u.raiz().radicando()))));
        }
        u.atomos().forEach((a, e) -> partes.add(e == 1 ? a.expresion() : new Potencia(a.expresion(), Constante.de(e))));
        if (u.imaginaria()) {
            partes.add(new ConstanteEspecial(ConstanteEspecial.Tipo.I));
        }
        Expresion t;
        if (partes.isEmpty()) {
            t = new Constante(abs);
        } else {
            List<Expresion> arriba = new ArrayList<>();
            if (!abs.num().equals(BigInteger.ONE)) {
                arriba.add(new Constante(Racional.de(abs.num())));
            }
            arriba.addAll(partes);
            Expresion num = arriba.size() == 1 ? arriba.getFirst() : new Producto(arriba);
            t = abs.esEntero() ? num : new Cociente(num, new Constante(Racional.de(abs.den())));
        }
        return c.signo() < 0 ? new Negacion(t) : t;
    }

    @Override
    public String toString() {
        return ImpresoraTexto.imprimir(aExpresion());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NumeroAlgebraico n && n.terminos.equals(terminos);
    }

    @Override
    public int hashCode() {
        return terminos.hashCode();
    }
}
