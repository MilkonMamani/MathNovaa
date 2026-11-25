package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.analisis.AnalisisPolinomio;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.FormaExtendida;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;

public record AnalisisFactorizacion(List<Fila> filas, String notaRaices, Optional<Recta> recta) {

    public static final String RAICES = "Raíces de f(x) = 0";

    public AnalisisFactorizacion {
        filas = List.copyOf(filas);
    }

    public record Fila(String nombre, String latex, String texto) {

        static Fila formula(String nombre, String latex) {
            return new Fila(nombre, latex, null);
        }

        static Fila frase(String nombre, String texto) {
            return new Fila(nombre, null, texto);
        }
    }

    public record Raiz(String latex, int multiplicidad) {
    }

    public record Recta(List<Raiz> raices, List<Integer> signos) {

        public Recta {
            raices = List.copyOf(raices);
            signos = List.copyOf(signos);
            if (signos.size() != raices.size() + 1) {
                throw new IllegalArgumentException("Hace falta un signo por tramo");
            }
        }
    }

    public enum Condicion {
        MAYOR("f(x) > 0", "f(x) > 0"),
        MAYOR_IGUAL("f(x) ≥ 0", "f(x) \\geq 0"),
        MENOR("f(x) < 0", "f(x) < 0"),
        MENOR_IGUAL("f(x) ≤ 0", "f(x) \\leq 0"),
        IGUAL("f(x) = 0", "f(x) = 0");

        private final String texto;
        private final String latex;

        Condicion(String texto, String latex) {
            this.texto = texto;
            this.latex = latex;
        }

        public String texto() {
            return texto;
        }

        public String latex() {
            return latex;
        }

        boolean tramo(int signo) {
            return switch (this) {
                case MAYOR, MAYOR_IGUAL -> signo > 0;
                case MENOR, MENOR_IGUAL -> signo < 0;
                case IGUAL -> false;
            };
        }

        boolean raices() {
            return this == MAYOR_IGUAL || this == MENOR_IGUAL || this == IGUAL;
        }
    }

    public sealed interface Parte permits Intervalo, Puntos {
    }

    public record Intervalo(int desde, int hasta, boolean cerradoDesde, boolean cerradoHasta) implements Parte {
    }

    public record Puntos(List<Integer> indices) implements Parte {

        public Puntos {
            indices = List.copyOf(indices);
        }
    }

    public static Optional<AnalisisFactorizacion> de(Resolucion r) {
        Optional<AnalisisPolinomio> q;
        try {
            q = AnalisisPolinomio.de(r);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        return q.map(a -> de(a, r.extension() == null || r.extension().campo() == Campo.Q ? null
                : r.extension().resultado(), r.extension() == null ? List.of() : r.extension().aproximadas(),
                r.extension() == null ? Campo.Q : r.extension().campo()));
    }

    private record Valor(NumeroAlgebraico numero, int multiplicidad) {
    }

    static AnalisisFactorizacion de(AnalisisPolinomio a, FormaExtendida extendida, List<String> aproximadas,
                                    Campo campo) {
        List<Fila> filas = new ArrayList<>();
        filas.add(Fila.formula("Grado", String.valueOf(a.grado())));
        filas.add(Fila.formula("Coeficiente principal", latex(a.coeficientePrincipal())));
        filas.add(Fila.formula("Corte con el eje y", "\\left(0, " + latex(a.corteY()) + "\\right)"));
        a.vertice().ifPresent(v -> filas.add(Fila.formula("Vértice", "\\left(" + latex(v.x()) + ", "
                + latex(v.y()) + "\\right)")));

        List<Valor> reales = new ArrayList<>();
        List<Valor> complejas = new ArrayList<>();
        boolean todasLasReales;
        if (extendida != null && aproximadas.isEmpty() && extendida.exacta()) {
            todasLasReales = true;
            for (FormaExtendida.Factor f : extendida.factores()) {
                PolinomioAlgebraico p = f.polinomio();
                if (p.grado() == 1) {
                    NumeroAlgebraico z = p.coeficiente(0).negar().dividir(p.principal());
                    agregar(z.esReal() ? reales : complejas, z, f.multiplicidad());
                } else if (!sinRaicesReales(p)) {
                    todasLasReales = false;
                }
            }
        } else {
            todasLasReales = a.signosCompletos() && aproximadas.isEmpty();
            for (AnalisisPolinomio.Raiz z : a.raices()) {
                agregar(reales, NumeroAlgebraico.de(z.valor()), z.multiplicidad());
            }
        }
        reales.sort(Comparator.comparing(Valor::numero, NumeroAlgebraico::comparar));

        String nombreReales = todasLasReales || campo != Campo.Q ? RAICES : "Raíces racionales";
        if (!reales.isEmpty()) {
            filas.add(Fila.formula(nombreReales, "x \\in " + conjunto(reales.stream().map(v -> latex(v.numero()))
                    .toList())));
        } else if (todasLasReales) {
            filas.add(Fila.frase(nombreReales, "no tiene raíces reales"));
        } else {
            filas.add(Fila.frase(nombreReales, campo == Campo.Q ? "no tiene raíces racionales" : "no tiene"));
        }
        if (!complejas.isEmpty()) {
            filas.add(Fila.formula("Raíces complejas", "x \\in " + conjunto(complejas.stream()
                    .map(v -> latex(v.numero())).toList())));
        }
        if (!aproximadas.isEmpty()) {
            filas.add(Fila.frase("Raíces aproximadas", String.join(", ", aproximadas.stream()
                    .map(t -> "x ≈ " + t.replace("-", ImpresoraPizarra.MENOS)).toList())));
        }
        List<Valor> todas = new ArrayList<>(reales);
        todas.addAll(complejas);
        String nota = notaMultiplicidad(todas);

        Optional<Recta> recta = Optional.empty();
        if (todasLasReales) {
            List<Raiz> raices = reales.stream().map(v -> new Raiz(latex(v.numero()), v.multiplicidad())).toList();
            List<Integer> signos;
            if (extendida == null) {
                signos = a.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList();
            } else {
                signos = signosPorParidad(a.coeficientePrincipal().signo(), reales);
            }
            if (signos.size() == raices.size() + 1) {
                recta = Optional.of(new Recta(raices, signos));
            }
        }
        return new AnalisisFactorizacion(filas, nota, recta);
    }

    private static void agregar(List<Valor> l, NumeroAlgebraico z, int multiplicidad) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).numero().equals(z) || (z.esReal() && l.get(i).numero().esReal()
                    && l.get(i).numero().comparar(z) == 0)) {
                l.set(i, new Valor(l.get(i).numero(), l.get(i).multiplicidad() + multiplicidad));
                return;
            }
        }
        l.add(new Valor(z, multiplicidad));
    }

    private static boolean sinRaicesReales(PolinomioAlgebraico p) {
        if (p.grado() != 2 || !p.coeficientesReales()) {
            return false;
        }
        NumeroAlgebraico a = p.coeficiente(2);
        NumeroAlgebraico b = p.coeficiente(1);
        NumeroAlgebraico c = p.coeficiente(0);
        try {
            return b.multiplicar(b).restar(a.multiplicar(c).multiplicar(Racional.de(4))).signo() < 0;
        } catch (ArithmeticException e) {
            return false;
        }
    }

    private static List<Integer> signosPorParidad(int principal, List<Valor> reales) {
        int n = reales.size();
        Integer[] s = new Integer[n + 1];
        s[n] = principal;
        for (int i = n - 1; i >= 0; i--) {
            s[i] = reales.get(i).multiplicidad() % 2 == 1 ? -s[i + 1] : s[i + 1];
        }
        return List.of(s);
    }

    static String notaMultiplicidad(List<Valor> raices) {
        Map<Integer, List<String>> porVeces = new TreeMap<>();
        for (Valor v : raices) {
            if (v.multiplicidad() > 1) {
                porVeces.computeIfAbsent(v.multiplicidad(), k -> new ArrayList<>())
                        .add(ImpresoraPizarra.imprimir(v.numero().aExpresion()));
            }
        }
        if (porVeces.isEmpty()) {
            return null;
        }
        List<String> partes = new ArrayList<>();
        porVeces.forEach((veces, l) -> partes.add(l.size() == 1
                ? "la raíz " + l.getFirst() + " aparece " + veces(veces)
                : "las raíces " + lista(l) + " aparecen " + veces(veces) + " cada una"));
        String frase = lista(partes) + " en la factorización.";
        return Character.toUpperCase(frase.charAt(0)) + frase.substring(1);
    }

    private static String veces(int n) {
        return switch (n) {
            case 2 -> "dos veces";
            case 3 -> "tres veces";
            case 4 -> "cuatro veces";
            case 5 -> "cinco veces";
            case 6 -> "seis veces";
            default -> n + " veces";
        };
    }

    private static String lista(List<String> l) {
        if (l.size() == 1) {
            return l.getFirst();
        }
        return String.join(", ", l.subList(0, l.size() - 1)) + " y " + l.getLast();
    }

    public static List<Parte> solucion(Recta recta, Condicion c) {
        int n = recta.raices().size();
        boolean[] incluida = new boolean[2 * n + 1];
        for (int k = 0; k <= 2 * n; k++) {
            incluida[k] = k % 2 == 0 ? c.tramo(recta.signos().get(k / 2)) : c.raices();
        }
        List<Parte> partes = new ArrayList<>();
        int k = 0;
        while (k <= 2 * n) {
            if (!incluida[k]) {
                k++;
                continue;
            }
            int inicio = k;
            while (k + 1 <= 2 * n && incluida[k + 1]) {
                k++;
            }
            int fin = k;
            if (inicio == fin && inicio % 2 == 1) {
                int raiz = (inicio - 1) / 2;
                if (!partes.isEmpty() && partes.getLast() instanceof Puntos p) {
                    List<Integer> l = new ArrayList<>(p.indices());
                    l.add(raiz);
                    partes.set(partes.size() - 1, new Puntos(l));
                } else {
                    partes.add(new Puntos(List.of(raiz)));
                }
            } else {
                int desde = inicio % 2 == 0 ? inicio / 2 - 1 : (inicio - 1) / 2;
                int hasta = fin % 2 == 0 ? fin / 2 : (fin - 1) / 2;
                partes.add(new Intervalo(desde, hasta, inicio % 2 == 1, fin % 2 == 1));
            }
            k++;
        }
        return partes;
    }

    public static String latex(Recta recta, List<Parte> partes) {
        if (partes.isEmpty()) {
            return "\\varnothing";
        }
        int n = recta.raices().size();
        List<String> trozos = new ArrayList<>();
        for (Parte p : partes) {
            switch (p) {
                case Intervalo i -> {
                    String a = i.desde() < 0 ? "-\\infty" : recta.raices().get(i.desde()).latex();
                    String b = i.hasta() >= n ? "+\\infty" : recta.raices().get(i.hasta()).latex();
                    trozos.add("\\left" + (i.cerradoDesde() ? "[" : "(") + a + ", " + b + "\\right"
                            + (i.cerradoHasta() ? "]" : ")"));
                }
                case Puntos ps -> trozos.add(conjunto(ps.indices().stream()
                        .map(j -> recta.raices().get(j).latex()).toList()));
            }
        }
        return String.join(" \\cup ", trozos);
    }

    public String conjuntoSolucion(Condicion c) {
        Recta r = recta.orElseThrow();
        return "\\text{C.S.} = " + latex(r, solucion(r, c));
    }

    private static String conjunto(List<String> elementos) {
        return "\\left\\{" + String.join(", ", elementos) + "\\right\\}";
    }

    static String latex(Racional r) {
        return ImpresoraLatex.imprimir(new Constante(r));
    }

    static String latex(NumeroAlgebraico z) {
        Expresion e = z.aExpresion();
        return ImpresoraLatex.imprimir(e);
    }

    Map<String, Fila> porNombre() {
        Map<String, Fila> m = new LinkedHashMap<>();
        filas.forEach(f -> m.put(f.nombre(), f));
        return m;
    }
}
