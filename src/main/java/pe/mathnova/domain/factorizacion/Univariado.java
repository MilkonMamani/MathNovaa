package pe.mathnova.domain.factorizacion;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;

/**
 * Polinomio en una sola letra con coeficientes densos (el índice es el exponente). Base de Ruffini,
 * del recíproco, del aspa doble, de los coeficientes indeterminados y de Kronecker.
 */
final class Univariado {

    private final char variable;
    /** c[i] es el coeficiente de x^i; c[grado] ≠ 0 salvo para el cero. */
    private final Racional[] c;

    private Univariado(char variable, Racional[] c) {
        this.variable = variable;
        int g = c.length - 1;
        while (g > 0 && c[g].esCero()) {
            g--;
        }
        this.c = Arrays.copyOf(c, Math.max(g + 1, 1));
        if (this.c[0] == null) {
            this.c[0] = Racional.CERO;
        }
    }

    /** Vacío si p tiene más de una letra. Una constante queda en la letra 'x'. */
    static Optional<Univariado> de(Polinomio p) {
        if (p.variables().size() > 1) {
            return Optional.empty();
        }
        char v = p.variables().isEmpty() ? 'x' : p.variables().first();
        int g = p.esCero() ? 0 : p.grado();
        Racional[] c = new Racional[g + 1];
        Arrays.fill(c, Racional.CERO);
        p.terminos().forEach((m, k) -> c[m.exponente(v)] = k);
        return Optional.of(new Univariado(v, c));
    }

    static Univariado de(char variable, List<Racional> coeficientesDeMenorAMayor) {
        return new Univariado(variable, coeficientesDeMenorAMayor.toArray(Racional[]::new));
    }

    /** De mayor a menor, como la fila de arriba de la tabla de Ruffini. */
    static Univariado desdeMayor(char variable, List<Racional> coeficientes) {
        List<Racional> r = new ArrayList<>(coeficientes);
        java.util.Collections.reverse(r);
        return de(variable, r);
    }

    char variable() {
        return variable;
    }

    int grado() {
        return c.length - 1;
    }

    Racional coef(int i) {
        return i < 0 || i >= c.length ? Racional.CERO : c[i];
    }

    Racional principal() {
        return c[c.length - 1];
    }

    boolean esCero() {
        return c.length == 1 && c[0].esCero();
    }

    /** Coeficientes de mayor a menor grado, con los ceros de los términos que faltan. */
    List<Racional> deMayorAMenor() {
        List<Racional> r = new ArrayList<>();
        for (int i = c.length - 1; i >= 0; i--) {
            r.add(c[i]);
        }
        return r;
    }

    boolean enteros() {
        return Arrays.stream(c).allMatch(Racional::esEntero);
    }

    Polinomio aPolinomio() {
        Polinomio r = Polinomio.CERO;
        for (int i = 0; i < c.length; i++) {
            r = r.sumar(Polinomio.termino(c[i], i == 0 ? Monomio.UNO : Monomio.de(variable, i)));
        }
        return r;
    }

    Racional evaluar(Racional x) {
        Racional r = Racional.CERO;
        for (int i = c.length - 1; i >= 0; i--) {
            r = r.multiplicar(x).sumar(c[i]);
        }
        return r;
    }

    Univariado multiplicar(Univariado o) {
        Racional[] r = new Racional[grado() + o.grado() + 1];
        Arrays.fill(r, Racional.CERO);
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < o.c.length; j++) {
                r[i + j] = r[i + j].sumar(c[i].multiplicar(o.c[j]));
            }
        }
        return new Univariado(variable, r);
    }

    Univariado multiplicar(Racional k) {
        Racional[] r = new Racional[c.length];
        for (int i = 0; i < c.length; i++) {
            r[i] = c[i].multiplicar(k);
        }
        return new Univariado(variable, r);
    }

    Univariado sumar(Univariado o) {
        Racional[] r = new Racional[Math.max(c.length, o.c.length)];
        for (int i = 0; i < r.length; i++) {
            r[i] = coef(i).sumar(o.coef(i));
        }
        return new Univariado(variable, r);
    }

    /** [cociente, resto] de la división larga. */
    Univariado[] dividir(Univariado d) {
        if (d.esCero()) {
            throw new ArithmeticException("División entre el polinomio cero");
        }
        Racional[] resto = Arrays.copyOf(c, c.length);
        int gd = d.grado();
        if (grado() < gd) {
            return new Univariado[] {new Univariado(variable, new Racional[] {Racional.CERO}), this};
        }
        Racional[] q = new Racional[grado() - gd + 1];
        Arrays.fill(q, Racional.CERO);
        for (int i = grado(); i >= gd; i--) {
            Racional k = resto[i].dividir(d.principal());
            q[i - gd] = k;
            for (int j = 0; j <= gd; j++) {
                resto[i - gd + j] = resto[i - gd + j].restar(k.multiplicar(d.c[j]));
            }
        }
        return new Univariado[] {new Univariado(variable, q), new Univariado(variable, resto)};
    }

    /** El cociente si d divide exactamente; vacío si no. */
    Optional<Univariado> cocienteExacto(Univariado d) {
        Univariado[] qr = dividir(d);
        return qr[1].esCero() ? Optional.of(qr[0]) : Optional.empty();
    }

    /** Multiplica por el MCM de los denominadores y divide por el MCD: enteros sin factor común. */
    Univariado primitivo() {
        BigInteger mcm = BigInteger.ONE;
        for (Racional r : c) {
            mcm = mcm.divide(mcm.gcd(r.den())).multiply(r.den());
        }
        BigInteger mcd = BigInteger.ZERO;
        for (Racional r : c) {
            mcd = mcd.gcd(r.multiplicar(Racional.de(mcm)).num());
        }
        if (mcd.signum() == 0) {
            return this;
        }
        Racional k = Racional.de(mcm).dividir(Racional.de(mcd));
        if (principal().signo() < 0) {
            k = k.negar();
        }
        return multiplicar(k);
    }

    /** División sintética entre (x − r): la fila del medio y la de abajo (con el residuo al final). */
    TrabajoIntermedio.TablaRuffini.Division ruffini(Racional r, List<Integer> rellenos) {
        List<Racional> arriba = deMayorAMenor();
        List<Racional> medio = new ArrayList<>();
        List<Racional> abajo = new ArrayList<>();
        Racional acumulado = Racional.CERO;
        for (int i = 0; i < arriba.size(); i++) {
            if (i == 0) {
                acumulado = arriba.getFirst();
            } else {
                Racional producto = acumulado.multiplicar(r);
                medio.add(producto);
                acumulado = arriba.get(i).sumar(producto);
            }
            abajo.add(acumulado);
        }
        return new TrabajoIntermedio.TablaRuffini.Division(r, arriba, medio, abajo, rellenos);
    }

    /** Cociente de una división sintética: la fila de abajo sin el residuo. */
    static Univariado cociente(char variable, TrabajoIntermedio.TablaRuffini.Division d) {
        return desdeMayor(variable, d.resultados().subList(0, d.resultados().size() - 1));
    }

    /** Posiciones (en la fila de arriba) de los términos que faltan, sin contar el principal. */
    List<Integer> ceros() {
        List<Integer> r = new ArrayList<>();
        List<Racional> arriba = deMayorAMenor();
        for (int i = 1; i < arriba.size(); i++) {
            if (arriba.get(i).esCero()) {
                r.add(i);
            }
        }
        return r;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Univariado u && u.variable == variable && Arrays.equals(u.c, c);
    }

    @Override
    public int hashCode() {
        return 31 * variable + Arrays.hashCode(c);
    }

    @Override
    public String toString() {
        return aPolinomio().toString();
    }
}
