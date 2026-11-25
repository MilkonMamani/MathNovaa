package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;

public final class CadenaExpansion {

    public record Resultado(List<String> expresiones, boolean coincide) {
        public Resultado {
            expresiones = List.copyOf(expresiones);
        }

        public List<CadenaIgualdades.Eslabon> eslabones() {
            return expresiones.stream().map(CadenaIgualdades.Eslabon::new).toList();
        }
    }

    private CadenaExpansion() {
    }

    public static Resultado de(FormaFactorizada f, Polinomio entrada, String inicial) {
        List<Bloque<Polinomio>> fs = new ArrayList<>();
        for (Factor x : f.factores()) {
            fs.add(new Bloque<>(x.polinomio(), x.multiplicidad()));
        }
        return cadena(POLINOMIO, f.constante(), fs, entrada, inicial);
    }

    public static Optional<Resultado> de(FormaExtendida f, Polinomio entrada, String inicial) {
        if (!f.exacta() || entrada.variables().size() > 1) {
            return Optional.empty();
        }
        char v = f.factores().isEmpty() ? (entrada.variables().isEmpty() ? 'x' : entrada.variables().first())
                : f.factores().getFirst().polinomio().variable();
        List<Bloque<PolinomioAlgebraico>> fs = new ArrayList<>();
        for (FormaExtendida.Factor x : f.factores()) {
            fs.add(new Bloque<>(x.polinomio(), x.multiplicidad()));
        }
        Aritmetica<PolinomioAlgebraico> ar = algebraica(v);
        PolinomioAlgebraico e = entrada.esCero() ? PolinomioAlgebraico.constante(v, NumeroAlgebraico.CERO)
                : PolinomioAlgebraico.desde(entrada);
        return Optional.of(cadena(ar, f.constante(), fs, e, inicial));
    }

    private interface Aritmetica<T> {
        T multiplicar(T a, T b);

        T potencia(T a, int n);

        T sumar(T a, T b);

        T restar(T a, T b);

        T constante(Racional k);

        int terminos(T a);

        int grado(T a);

        String latex(T a);

        default String distribuido(T a, T b) {
            return null;
        }
    }

    private record Bloque<T>(T base, int exp) {
    }

    private record Par<T>(T a, T b, int exp) {
    }

    private static <T> Resultado cadena(Aritmetica<T> ar, Racional c, List<Bloque<T>> factores, T entrada,
                                        String inicial) {
        T frente = ar.constante(c);
        StringBuilder latexFrente = new StringBuilder(coef(c));
        List<Bloque<T>> resto = new ArrayList<>();
        for (Bloque<T> b : factores) {
            if (ar.terminos(b.base()) == 1) {
                frente = ar.multiplicar(frente, ar.potencia(b.base(), b.exp()));
                latexFrente.append(ar.latex(b.base())).append(b.exp() > 1 ? "^{" + b.exp() + "}" : "");
            } else {
                resto.add(b);
            }
        }
        String adelante = latexFrente.toString();
        List<String> l = new ArrayList<>();
        l.add(inicial);

        List<Object> items = emparejar(ar, resto);
        if (adelante.isEmpty() && items.size() == 1 && items.getFirst() instanceof Par<?> p && p.exp() == 1) {
            items = new ArrayList<>(resto);
        }
        if (items.stream().anyMatch(o -> o instanceof Par)) {
            agregar(l, adelante + items(ar, items));
            List<Object> multiplicados = new ArrayList<>();
            for (Object o : items) {
                multiplicados.add(o instanceof Par<?> p ? new Bloque<>(ar.multiplicar(cast(p.a()), cast(p.b())), p.exp()) : o);
            }
            items = multiplicados;
            agregar(l, adelante + items(ar, items));
        }
        List<Bloque<T>> bloques = new ArrayList<>();
        for (Object o : items) {
            bloques.add(cast(o));
        }
        if (bloques.stream().anyMatch(b -> b.exp() > 1)) {
            bloques.replaceAll(b -> b.exp() > 1 ? new Bloque<>(ar.potencia(b.base(), b.exp()), 1) : b);
            agregar(l, adelante.isEmpty() && bloques.size() == 1 ? ar.latex(bloques.getFirst().base())
                    : adelante + bloques(ar, bloques));
        }
        while (bloques.size() > 1) {
            T a = bloques.get(0).base();
            T b = bloques.get(1).base();
            T producto = ar.multiplicar(a, b);
            String distribuido = ar.distribuido(a, b);
            List<Bloque<T>> resto2 = bloques.subList(2, bloques.size());
            boolean solo = adelante.isEmpty() && resto2.isEmpty();
            if (distribuido != null) {
                agregar(l, adelante + (solo ? distribuido : "\\left(" + distribuido + "\\right)") + bloques(ar, resto2));
            }
            bloques.remove(0);
            bloques.set(0, new Bloque<>(producto, 1));
            agregar(l, adelante + (solo ? ar.latex(producto) : bloques(ar, bloques)));
        }
        T total = bloques.isEmpty() ? frente : ar.multiplicar(frente, bloques.getFirst().base());
        agregar(l, ar.latex(total));
        return new Resultado(l, total.equals(entrada));
    }

    private static void agregar(List<String> l, String latex) {
        if (!sinEspacios(l.getLast()).equals(sinEspacios(latex))) {
            l.add(latex);
        }
    }

    private static String sinEspacios(String s) {
        return s.replace(" ", "");
    }

    @SuppressWarnings("unchecked")
    private static <X> X cast(Object o) {
        return (X) o;
    }

    private static <T> List<Object> emparejar(Aritmetica<T> ar, List<Bloque<T>> resto) {
        List<Object> items = new ArrayList<>();
        boolean[] usado = new boolean[resto.size()];
        for (int i = 0; i < resto.size(); i++) {
            if (usado[i]) {
                continue;
            }
            Bloque<T> a = resto.get(i);
            int pareja = -1;
            if (ar.grado(a.base()) == 1) {
                for (int j = i + 1; j < resto.size() && pareja < 0; j++) {
                    Bloque<T> b = resto.get(j);
                    if (!usado[j] && ar.grado(b.base()) == 1 && conjugados(ar, a.base(), b.base())) {
                        pareja = j;
                    }
                }
            }
            if (pareja < 0) {
                items.add(a);
                continue;
            }
            usado[pareja] = true;
            Bloque<T> b = resto.get(pareja);
            int e = Math.min(a.exp(), b.exp());
            boolean masPrimero = ar.latex(a.base()).contains("+") || !ar.latex(b.base()).contains("+");
            items.add(masPrimero ? new Par<>(a.base(), b.base(), e) : new Par<>(b.base(), a.base(), e));
            if (a.exp() > e) {
                items.add(new Bloque<>(a.base(), a.exp() - e));
            }
            if (b.exp() > e) {
                items.add(new Bloque<>(b.base(), b.exp() - e));
            }
        }
        return items;
    }

    private static <T> boolean conjugados(Aritmetica<T> ar, T a, T b) {
        T suma = ar.sumar(a, b);
        T resta = ar.restar(a, b);
        return ar.terminos(suma) == 1 && ar.grado(suma) == 1 && ar.terminos(resta) == 1 && ar.grado(resta) == 0;
    }

    private static <T> String items(Aritmetica<T> ar, List<Object> items) {
        StringBuilder sb = new StringBuilder();
        for (Object o : items) {
            if (o instanceof Par<?> p) {
                String par = parentesis(ar, cast(p.a())) + parentesis(ar, cast(p.b()));
                sb.append(p.exp() > 1 ? "\\left[" + par + "\\right]^{" + p.exp() + "}" : par);
            } else {
                Bloque<T> b = cast(o);
                sb.append(bloque(ar, b));
            }
        }
        return sb.toString();
    }

    private static <T> String bloques(Aritmetica<T> ar, List<Bloque<T>> bloques) {
        StringBuilder sb = new StringBuilder();
        bloques.forEach(b -> sb.append(bloque(ar, b)));
        return sb.toString();
    }

    private static <T> String bloque(Aritmetica<T> ar, Bloque<T> b) {
        return parentesis(ar, b.base()) + (b.exp() > 1 ? "^{" + b.exp() + "}" : "");
    }

    private static <T> String parentesis(Aritmetica<T> ar, T a) {
        return "\\left(" + ar.latex(a) + "\\right)";
    }

    private static String coef(Racional k) {
        if (k.equals(Racional.UNO)) {
            return "";
        }
        if (k.equals(Racional.UNO.negar())) {
            return "-";
        }
        return FormaLegible.racional(k);
    }

    private static final Aritmetica<Polinomio> POLINOMIO = new Aritmetica<>() {
        @Override
        public Polinomio multiplicar(Polinomio a, Polinomio b) {
            return a.multiplicar(b);
        }

        @Override
        public Polinomio potencia(Polinomio a, int n) {
            return a.potencia(n);
        }

        @Override
        public Polinomio sumar(Polinomio a, Polinomio b) {
            return a.sumar(b);
        }

        @Override
        public Polinomio restar(Polinomio a, Polinomio b) {
            return a.restar(b);
        }

        @Override
        public Polinomio constante(Racional k) {
            return Polinomio.constante(k);
        }

        @Override
        public int terminos(Polinomio a) {
            return a.terminos().size();
        }

        @Override
        public int grado(Polinomio a) {
            return a.esCero() ? 0 : a.grado();
        }

        @Override
        public String latex(Polinomio a) {
            return ImpresoraLatex.imprimir(a.aExpresion());
        }

        @Override
        public String distribuido(Polinomio a, Polinomio b) {
            if (a.terminos().size() * b.terminos().size() > 9) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (var ta : a.terminos().entrySet()) {
                for (var tb : b.terminos().entrySet()) {
                    Polinomio t = Polinomio.termino(ta.getValue(), ta.getKey())
                            .multiplicar(Polinomio.termino(tb.getValue(), tb.getKey()));
                    String x = ImpresoraLatex.imprimir(t.aExpresion());
                    if (sb.isEmpty()) {
                        sb.append(x);
                    } else if (x.startsWith("-")) {
                        sb.append(" - ").append(x.substring(1));
                    } else {
                        sb.append(" + ").append(x);
                    }
                }
            }
            return sb.toString();
        }
    };

    private static Aritmetica<PolinomioAlgebraico> algebraica(char v) {
        return new Aritmetica<>() {
            @Override
            public PolinomioAlgebraico multiplicar(PolinomioAlgebraico a, PolinomioAlgebraico b) {
                return a.multiplicar(b);
            }

            @Override
            public PolinomioAlgebraico potencia(PolinomioAlgebraico a, int n) {
                return a.potencia(n);
            }

            @Override
            public PolinomioAlgebraico sumar(PolinomioAlgebraico a, PolinomioAlgebraico b) {
                return a.sumar(b);
            }

            @Override
            public PolinomioAlgebraico restar(PolinomioAlgebraico a, PolinomioAlgebraico b) {
                return a.restar(b);
            }

            @Override
            public PolinomioAlgebraico constante(Racional k) {
                return PolinomioAlgebraico.constante(v, NumeroAlgebraico.de(k));
            }

            @Override
            public int terminos(PolinomioAlgebraico a) {
                return (int) a.coeficientes().stream().filter(k -> !k.esCero()).count();
            }

            @Override
            public int grado(PolinomioAlgebraico a) {
                return a.esCero() ? 0 : a.grado();
            }

            @Override
            public String latex(PolinomioAlgebraico a) {
                return ImpresoraLatex.imprimir(a.aExpresion());
            }
        };
    }
}
