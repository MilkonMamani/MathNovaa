package pe.mathnova.domain.algebra;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.DerivadaParcial;
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

public final class Polinomio {

    public static final int GRADO_MAXIMO = 100;

    public static final Polinomio CERO = new Polinomio(new TreeMap<>(Comparator.reverseOrder()));
    public static final Polinomio UNO = constante(Racional.UNO);

    private final SortedMap<Monomio, Racional> terminos;

    private Polinomio(TreeMap<Monomio, Racional> terminos) {
        this.terminos = Collections.unmodifiableSortedMap(terminos);
    }

    public static Polinomio constante(Racional c) {
        return termino(c, Monomio.UNO);
    }

    public static Polinomio variable(char v) {
        return termino(Racional.UNO, Monomio.de(v, 1));
    }

    public static Polinomio termino(Racional coeficiente, Monomio monomio) {
        TreeMap<Monomio, Racional> t = vacio();
        if (!coeficiente.esCero()) {
            t.put(monomio, coeficiente);
        }
        return new Polinomio(t);
    }

    public static Polinomio de(Map<Monomio, Racional> terminos) {
        TreeMap<Monomio, Racional> t = vacio();
        terminos.forEach((m, c) -> acumular(t, m, c));
        return new Polinomio(t);
    }

    public static Polinomio desde(Expresion e) {
        return e.aceptar(CONVERSOR);
    }

    private static TreeMap<Monomio, Racional> vacio() {
        return new TreeMap<>(Comparator.reverseOrder());
    }

    private static void acumular(TreeMap<Monomio, Racional> t, Monomio m, Racional c) {
        Racional nuevo = t.getOrDefault(m, Racional.CERO).sumar(c);
        if (nuevo.esCero()) {
            t.remove(m);
        } else {
            t.put(m, nuevo);
        }
    }

    public Polinomio sumar(Polinomio o) {
        TreeMap<Monomio, Racional> t = new TreeMap<>(terminos);
        o.terminos.forEach((m, c) -> acumular(t, m, c));
        return new Polinomio(t);
    }

    public Polinomio restar(Polinomio o) {
        return sumar(o.negar());
    }

    public Polinomio negar() {
        TreeMap<Monomio, Racional> t = vacio();
        terminos.forEach((m, c) -> t.put(m, c.negar()));
        return new Polinomio(t);
    }

    public Polinomio multiplicar(Polinomio o) {
        TreeMap<Monomio, Racional> t = vacio();
        terminos.forEach((m1, c1) -> o.terminos.forEach((m2, c2) -> acumular(t, m1.multiplicar(m2), c1.multiplicar(c2))));
        return new Polinomio(t);
    }

    public Polinomio multiplicar(Racional c) {
        return multiplicar(constante(c));
    }

    public Polinomio potencia(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Exponente negativo: " + n);
        }
        Polinomio resultado = UNO;
        Polinomio base = this;
        while (n > 0) {
            if ((n & 1) == 1) {
                resultado = resultado.multiplicar(base);
            }
            n >>= 1;
            if (n > 0) {
                base = base.multiplicar(base);
            }
        }
        return resultado;
    }

    public int grado() {
        return terminos.keySet().stream().mapToInt(Monomio::grado).max().orElse(-1);
    }

    public Expresion aExpresion() {
        if (terminos.isEmpty()) {
            return Constante.de(0);
        }
        java.util.List<Expresion> sumandos = new java.util.ArrayList<>();
        terminos.forEach((m, c) -> {
            Racional abs = c.signo() < 0 ? c.negar() : c;
            java.util.List<Expresion> factores = new java.util.ArrayList<>();
            if (m.equals(Monomio.UNO) || !abs.equals(Racional.UNO)) {
                factores.add(new Constante(abs));
            }
            m.exponentes().forEach((letra, e) -> factores.add(e == 1
                    ? new Variable(letra)
                    : new Potencia(new Variable(letra), Constante.de(e))));
            Expresion t = factores.size() == 1 ? factores.getFirst() : new Producto(factores);
            sumandos.add(c.signo() < 0 ? new Negacion(t) : t);
        });
        return sumandos.size() == 1 ? sumandos.getFirst() : new Suma(sumandos);
    }

    public SortedSet<Character> variables() {
        TreeSet<Character> letras = new TreeSet<>();
        terminos.keySet().forEach(m -> letras.addAll(m.variables()));
        return Collections.unmodifiableSortedSet(letras);
    }

    public int gradoEn(char variable) {
        return terminos.keySet().stream().mapToInt(m -> m.exponente(variable)).max().orElse(-1);
    }

    public boolean esCero() {
        return terminos.isEmpty();
    }

    public boolean esConstante() {
        return terminos.isEmpty() || (terminos.size() == 1 && terminos.containsKey(Monomio.UNO));
    }

    public Racional coeficiente(Monomio m) {
        return terminos.getOrDefault(m, Racional.CERO);
    }

    public Racional constante() {
        return coeficiente(Monomio.UNO);
    }

    public SortedMap<Monomio, Racional> terminos() {
        return terminos;
    }

    public Racional evaluar(Map<Character, Racional> valores) {
        Racional total = Racional.CERO;
        for (Map.Entry<Monomio, Racional> t : terminos.entrySet()) {
            Racional valor = t.getValue();
            for (Map.Entry<Character, Integer> e : t.getKey().exponentes().entrySet()) {
                valor = valor.multiplicar(valorDe(e.getKey(), e.getValue(), valores));
            }
            total = total.sumar(valor);
        }
        return total;
    }

    private static Racional valorDe(char v, int exponente, Map<Character, Racional> valores) {
        if (exponente == 0) {
            return Racional.UNO;
        }
        Racional r = valores.get(v);
        if (r == null) {
            throw new IllegalArgumentException("Falta el valor de " + v);
        }
        return r.potencia(exponente);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Polinomio p && terminos.equals(p.terminos);
    }

    @Override
    public int hashCode() {
        return terminos.hashCode();
    }

    @Override
    public String toString() {
        if (terminos.isEmpty()) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Monomio, Racional> t : terminos.entrySet()) {
            Racional c = t.getValue();
            String literal = t.getKey().toString();
            if (sb.isEmpty()) {
                sb.append(c.signo() < 0 ? "-" : "");
            } else {
                sb.append(c.signo() < 0 ? " - " : " + ");
            }
            Racional abs = c.signo() < 0 ? c.negar() : c;
            if (literal.isEmpty() || !abs.equals(Racional.UNO)) {
                sb.append(abs);
            }
            sb.append(literal);
        }
        return sb.toString();
    }

    private static final Visitante<Polinomio> CONVERSOR = new Visitante<>() {

        @Override
        public Polinomio visitar(Constante c) {
            return constante(c.valor());
        }

        @Override
        public Polinomio visitar(Variable v) {
            return variable(v.nombre());
        }

        @Override
        public Polinomio visitar(ConstanteEspecial c) {
            throw new NoEsPolinomioException("Tiene la constante " + switch (c.tipo()) {
                case E -> "e";
                case PI -> "π";
                case I -> "i";
            });
        }

        @Override
        public Polinomio visitar(Suma s) {
            Polinomio r = CERO;
            for (Expresion t : s.terminos()) {
                r = r.sumar(t.aceptar(this));
            }
            return r;
        }

        @Override
        public Polinomio visitar(Producto p) {
            Polinomio r = UNO;
            for (Expresion f : p.factores()) {
                r = limitar(r.multiplicar(f.aceptar(this)));
            }
            return r;
        }

        @Override
        public Polinomio visitar(Cociente c) {
            Polinomio num = c.numerador().aceptar(this);
            Polinomio den = c.denominador().aceptar(this);
            if (!den.esConstante()) {
                throw new NoEsPolinomioException("Divide entre una expresión que no es constante");
            }
            if (den.esCero()) {
                throw new ArithmeticException("División entre cero");
            }
            return num.multiplicar(den.constante().inverso());
        }

        @Override
        public Polinomio visitar(Potencia p) {
            Polinomio exp = p.exponente().aceptar(this);
            if (!exp.esConstante()) {
                throw new NoEsPolinomioException("El exponente tiene variables");
            }
            Racional n = exp.constante();
            if (!n.esEntero()) {
                throw new NoEsPolinomioException("El exponente " + n + " no es entero");
            }
            if (n.signo() < 0) {
                throw new NoEsPolinomioException("El exponente " + n + " es negativo");
            }
            Polinomio base = p.base().aceptar(this);
            if (base.grado() > 0 && n.compareTo(Racional.de(GRADO_MAXIMO)) > 0) {
                throw new NoEsPolinomioException("El grado pasa de " + GRADO_MAXIMO);
            }
            if (base.esConstante() && n.compareTo(Racional.de(10_000)) > 0) {
                throw new NoEsPolinomioException("El exponente " + n + " es demasiado grande");
            }
            return limitar(base.potencia(n.num().intValueExact()));
        }

        private Polinomio limitar(Polinomio r) {
            if (r.grado() > GRADO_MAXIMO) {
                throw new NoEsPolinomioException("El grado pasa de " + GRADO_MAXIMO);
            }
            return r;
        }

        @Override
        public Polinomio visitar(Negacion n) {
            return n.argumento().aceptar(this).negar();
        }

        @Override
        public Polinomio visitar(Raiz r) {
            throw new NoEsPolinomioException("Tiene una raíz");
        }

        @Override
        public Polinomio visitar(Funcion f) {
            throw new NoEsPolinomioException("Tiene la función " + f.tipo().nombre());
        }

        @Override
        public Polinomio visitar(Limite l) {
            throw new NoEsPolinomioException("Es un límite");
        }

        @Override
        public Polinomio visitar(Derivada d) {
            throw new NoEsPolinomioException("Es una derivada");
        }

        @Override
        public Polinomio visitar(pe.mathnova.domain.expresion.Gradiente g) {
            throw new NoEsPolinomioException("Es un gradiente");
        }

        @Override
        public Polinomio visitar(pe.mathnova.domain.expresion.Direccional d) {
            throw new NoEsPolinomioException("Es una derivada direccional");
        }

        @Override
        public Polinomio visitar(DerivadaParcial d) {
            throw new NoEsPolinomioException("Es una derivada parcial");
        }

        @Override
        public Polinomio visitar(Integral i) {
            throw new NoEsPolinomioException("Es una integral");
        }

        @Override
        public Polinomio visitar(IntegralMultiple i) {
            throw new NoEsPolinomioException("Es una integral múltiple");
        }

        @Override
        public Polinomio visitar(pe.mathnova.domain.expresion.LimiteDoble l) {
            throw new NoEsPolinomioException("Es un límite doble");
        }

        @Override
        public Polinomio visitar(pe.mathnova.domain.expresion.Extremos x) {
            throw new NoEsPolinomioException("Es un cálculo de extremos");
        }

        @Override
        public Polinomio visitar(Ecuacion e) {
            throw new NoEsPolinomioException("Es una ecuación");
        }
    };
}
