package pe.mathnova.domain.numeros;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.domain.expresion.Visitante;

/**
 * Polinomio en una letra con coeficientes {@link NumeroAlgebraico}: x² − √2·x + 1, x − 1 − √3·i.
 * El índice de la lista es el exponente. Inmutable.
 */
public final class PolinomioAlgebraico {

    private final char variable;
    /** c[k] es el coeficiente de xᵏ; el último no es cero (salvo en el polinomio cero). */
    private final List<NumeroAlgebraico> c;

    private PolinomioAlgebraico(char variable, List<NumeroAlgebraico> coeficientes) {
        this.variable = variable;
        List<NumeroAlgebraico> l = new ArrayList<>(coeficientes);
        while (l.size() > 1 && l.getLast().esCero()) {
            l.removeLast();
        }
        if (l.isEmpty()) {
            l.add(NumeroAlgebraico.CERO);
        }
        this.c = Collections.unmodifiableList(l);
    }

    /** De menor a mayor grado. */
    public static PolinomioAlgebraico de(char variable, List<NumeroAlgebraico> coeficientes) {
        return new PolinomioAlgebraico(variable, coeficientes);
    }

    public static PolinomioAlgebraico constante(char variable, NumeroAlgebraico k) {
        return new PolinomioAlgebraico(variable, List.of(k));
    }

    /** x − r. */
    public static PolinomioAlgebraico lineal(char variable, NumeroAlgebraico raiz) {
        return new PolinomioAlgebraico(variable, List.of(raiz.negar(), NumeroAlgebraico.UNO));
    }

    /** x² + b·x + c. */
    public static PolinomioAlgebraico cuadratico(char variable, NumeroAlgebraico b, NumeroAlgebraico c) {
        return new PolinomioAlgebraico(variable, List.of(c, b, NumeroAlgebraico.UNO));
    }

    /** @throws IllegalArgumentException si p tiene más de una letra */
    public static PolinomioAlgebraico desde(Polinomio p) {
        if (p.variables().size() > 1) {
            throw new IllegalArgumentException("Solo polinomios en una letra: " + p);
        }
        char v = p.variables().isEmpty() ? 'x' : p.variables().first();
        int g = p.esCero() ? 0 : p.grado();
        List<NumeroAlgebraico> l = new ArrayList<>(Collections.nCopies(g + 1, NumeroAlgebraico.CERO));
        p.terminos().forEach((m, k) -> l.set(m.exponente(v), NumeroAlgebraico.de(k)));
        return new PolinomioAlgebraico(v, l);
    }

    /**
     * Lee un polinomio en {@code variable} con coeficientes con radicales e i: "x - √3·i",
     * "x^2 + (∛2 + ∛4)·x - 2 + ∛4 + 2·∛2", "x + 1/2 - √(1 + 4√2)/2".
     *
     * @throws NoEsPolinomioException si no lo es
     */
    public static PolinomioAlgebraico desde(Expresion e, char variable) {
        return e.aceptar(new Lector(variable));
    }

    // ------------------------------------------------------------------ consultas

    public char variable() {
        return variable;
    }

    public int grado() {
        return c.size() - 1;
    }

    public NumeroAlgebraico coeficiente(int k) {
        return k < 0 || k >= c.size() ? NumeroAlgebraico.CERO : c.get(k);
    }

    public NumeroAlgebraico principal() {
        return c.getLast();
    }

    /** De menor a mayor grado. */
    public List<NumeroAlgebraico> coeficientes() {
        return c;
    }

    public boolean esCero() {
        return c.size() == 1 && c.getFirst().esCero();
    }

    /** Todos los coeficientes sin átomos. */
    public boolean exacto() {
        return c.stream().allMatch(NumeroAlgebraico::exacto);
    }

    public boolean coeficientesRacionales() {
        return c.stream().allMatch(NumeroAlgebraico::esRacional);
    }

    public boolean coeficientesReales() {
        return c.stream().allMatch(NumeroAlgebraico::esReal);
    }

    /** El polinomio de Q, si todos los coeficientes son racionales. */
    public Polinomio aPolinomio() {
        Polinomio r = Polinomio.CERO;
        for (int k = 0; k < c.size(); k++) {
            r = r.sumar(Polinomio.termino(c.get(k).aRacional(), k == 0 ? Monomio.UNO : Monomio.de(variable, k)));
        }
        return r;
    }

    // ------------------------------------------------------------------ aritmética

    public PolinomioAlgebraico sumar(PolinomioAlgebraico o) {
        int n = Math.max(c.size(), o.c.size());
        List<NumeroAlgebraico> r = new ArrayList<>();
        for (int k = 0; k < n; k++) {
            r.add(coeficiente(k).sumar(o.coeficiente(k)));
        }
        return new PolinomioAlgebraico(variable, r);
    }

    public PolinomioAlgebraico restar(PolinomioAlgebraico o) {
        return sumar(o.multiplicar(NumeroAlgebraico.UNO.negar()));
    }

    public PolinomioAlgebraico multiplicar(PolinomioAlgebraico o) {
        List<NumeroAlgebraico> r = new ArrayList<>(Collections.nCopies(c.size() + o.c.size() - 1, NumeroAlgebraico.CERO));
        for (int i = 0; i < c.size(); i++) {
            if (c.get(i).esCero()) {
                continue;
            }
            for (int j = 0; j < o.c.size(); j++) {
                r.set(i + j, r.get(i + j).sumar(c.get(i).multiplicar(o.c.get(j))));
            }
        }
        return new PolinomioAlgebraico(variable, r);
    }

    public PolinomioAlgebraico multiplicar(NumeroAlgebraico k) {
        return new PolinomioAlgebraico(variable, c.stream().map(x -> x.multiplicar(k)).toList());
    }

    public PolinomioAlgebraico potencia(int n) {
        PolinomioAlgebraico r = constante(variable, NumeroAlgebraico.UNO);
        for (int i = 0; i < n; i++) {
            r = r.multiplicar(this);
        }
        return r;
    }

    /** Dividido entre su coeficiente principal. */
    public PolinomioAlgebraico monico() {
        return multiplicar(principal().inverso());
    }

    /** [cociente, resto]. El divisor debe tener coeficiente principal invertible. */
    public PolinomioAlgebraico[] dividir(PolinomioAlgebraico d) {
        if (d.esCero()) {
            throw new ArithmeticException("División entre el polinomio cero");
        }
        List<NumeroAlgebraico> resto = new ArrayList<>(c);
        int gd = d.grado();
        if (grado() < gd) {
            return new PolinomioAlgebraico[] {constante(variable, NumeroAlgebraico.CERO), this};
        }
        NumeroAlgebraico inv = d.principal().equals(NumeroAlgebraico.UNO) ? NumeroAlgebraico.UNO : d.principal().inverso();
        List<NumeroAlgebraico> q = new ArrayList<>(Collections.nCopies(grado() - gd + 1, NumeroAlgebraico.CERO));
        for (int i = grado(); i >= gd; i--) {
            NumeroAlgebraico k = resto.get(i).multiplicar(inv);
            q.set(i - gd, k);
            for (int j = 0; j <= gd; j++) {
                resto.set(i - gd + j, resto.get(i - gd + j).restar(k.multiplicar(d.c.get(j))));
            }
            resto.set(i, NumeroAlgebraico.CERO); // exacto aunque haya átomos
        }
        return new PolinomioAlgebraico[] {new PolinomioAlgebraico(variable, q), new PolinomioAlgebraico(variable, resto)};
    }

    public NumeroAlgebraico evaluar(NumeroAlgebraico x) {
        NumeroAlgebraico r = NumeroAlgebraico.CERO;
        for (int i = c.size() - 1; i >= 0; i--) {
            r = r.multiplicar(x).sumar(c.get(i));
        }
        return r;
    }

    /** Cambia x por x + h. */
    public PolinomioAlgebraico trasladar(NumeroAlgebraico h) {
        PolinomioAlgebraico base = de(variable, List.of(h, NumeroAlgebraico.UNO));
        PolinomioAlgebraico r = constante(variable, NumeroAlgebraico.CERO);
        for (int i = c.size() - 1; i >= 0; i--) {
            r = r.multiplicar(base).sumar(constante(variable, c.get(i)));
        }
        return r;
    }

    public PolinomioAlgebraico conjugar() {
        return new PolinomioAlgebraico(variable, c.stream().map(NumeroAlgebraico::conjugar).toList());
    }

    // ------------------------------------------------------------------ texto

    /** "x^2 - √2·x + 1", "x - 1 - √3·i", "x^2 + (∛2 + ∛4)·x - 2 + ∛4 + 2·∛2". */
    public Expresion aExpresion() {
        List<Expresion> sumandos = new ArrayList<>();
        for (int k = c.size() - 1; k >= 0; k--) {
            NumeroAlgebraico a = c.get(k);
            if (a.esCero()) {
                continue;
            }
            if (k == 0) {
                Expresion e = a.aExpresion();
                if (e instanceof Suma s) {
                    sumandos.addAll(s.terminos());
                } else {
                    sumandos.add(e);
                }
                continue;
            }
            Expresion potencia = k == 1 ? new Variable(variable) : new Potencia(new Variable(variable), Constante.de(k));
            if (a.equals(NumeroAlgebraico.UNO)) {
                sumandos.add(potencia);
            } else if (a.equals(NumeroAlgebraico.UNO.negar())) {
                sumandos.add(new Negacion(potencia));
            } else {
                Expresion coef = a.aExpresion();
                if (coef instanceof Negacion n) {
                    sumandos.add(new Negacion(producto(n.argumento(), potencia)));
                } else {
                    sumandos.add(producto(coef, potencia));
                }
            }
        }
        if (sumandos.isEmpty()) {
            return Constante.de(0);
        }
        return sumandos.size() == 1 ? sumandos.getFirst() : new Suma(sumandos);
    }

    private static Expresion producto(Expresion coef, Expresion potencia) {
        if (coef instanceof Constante k) {
            return new Producto(k, potencia);
        }
        return new Producto(coef, potencia);
    }

    @Override
    public String toString() {
        return pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(aExpresion());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PolinomioAlgebraico p && p.variable == variable && p.c.equals(c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variable, c);
    }

    // ------------------------------------------------------------------ lectura

    private record Lector(char variable) implements Visitante<PolinomioAlgebraico> {

        private PolinomioAlgebraico k(NumeroAlgebraico n) {
            return constante(variable, n);
        }

        private NumeroAlgebraico constanteDe(Expresion e) {
            PolinomioAlgebraico p = e.aceptar(this);
            if (p.grado() > 0) {
                throw new NoEsPolinomioException("Tiene " + variable + " donde va una constante");
            }
            return p.coeficiente(0);
        }

        @Override
        public PolinomioAlgebraico visitar(Constante c) {
            return k(NumeroAlgebraico.de(c.valor()));
        }

        @Override
        public PolinomioAlgebraico visitar(Variable v) {
            if (v.nombre() != variable) {
                throw new NoEsPolinomioException("Tiene otra letra: " + v.nombre());
            }
            return de(variable, List.of(NumeroAlgebraico.CERO, NumeroAlgebraico.UNO));
        }

        @Override
        public PolinomioAlgebraico visitar(ConstanteEspecial c) {
            return switch (c.tipo()) {
                case I -> k(NumeroAlgebraico.I);
                default -> k(NumeroAlgebraico.de(Atomo.simbolico(c)));
            };
        }

        @Override
        public PolinomioAlgebraico visitar(Suma s) {
            PolinomioAlgebraico r = k(NumeroAlgebraico.CERO);
            for (Expresion t : s.terminos()) {
                r = r.sumar(t.aceptar(this));
            }
            return r;
        }

        @Override
        public PolinomioAlgebraico visitar(Producto p) {
            PolinomioAlgebraico r = k(NumeroAlgebraico.UNO);
            for (Expresion f : p.factores()) {
                r = r.multiplicar(f.aceptar(this));
            }
            return r;
        }

        @Override
        public PolinomioAlgebraico visitar(Cociente c) {
            return c.numerador().aceptar(this).multiplicar(constanteDe(c.denominador()).inverso());
        }

        @Override
        public PolinomioAlgebraico visitar(Potencia p) {
            if (!(p.exponente() instanceof Constante e) || !e.valor().esEntero()) {
                throw new NoEsPolinomioException("Exponente no entero");
            }
            int n = e.valor().num().intValueExact();
            if (n < 0) {
                return k(constanteDe(p.base()).potencia(n));
            }
            return p.base().aceptar(this).potencia(n);
        }

        @Override
        public PolinomioAlgebraico visitar(Negacion n) {
            return n.argumento().aceptar(this).multiplicar(NumeroAlgebraico.UNO.negar());
        }

        @Override
        public PolinomioAlgebraico visitar(Raiz r) {
            NumeroAlgebraico z = constanteDe(r.radicando());
            if (z.esRacional()) {
                return k(NumeroAlgebraico.raiz(r.indice(), z.aRacional()));
            }
            if (r.indice() == 2) {
                return k(z.raizCuadrada());
            }
            return k(z.raizReal(r.indice()));
        }

        @Override
        public PolinomioAlgebraico visitar(Funcion f) {
            constanteDe(f.argumento()); // solo funciones de constantes
            return k(NumeroAlgebraico.de(Atomo.simbolico(f)));
        }

        private static PolinomioAlgebraico no() {
            throw new NoEsPolinomioException("No es un polinomio");
        }

        @Override
        public PolinomioAlgebraico visitar(Limite l) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(Derivada d) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(pe.mathnova.domain.expresion.Gradiente g) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(pe.mathnova.domain.expresion.Direccional d) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(DerivadaParcial d) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(Integral i) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(IntegralMultiple i) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(pe.mathnova.domain.expresion.Extremos x) {
            return no();
        }

        @Override
        public PolinomioAlgebraico visitar(Ecuacion e) {
            return no();
        }
    }
}
