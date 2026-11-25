package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Monomio;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.analisis.AnalisisPolinomio;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Punto;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Raiz;
import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.calculo.limites.Evaluador;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.ResultadoLimite;
import pe.mathnova.domain.calculo.limites.Valor;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.NumeroAlgebraico;
import pe.mathnova.domain.factorizacion.Factor;
import pe.mathnova.domain.factorizacion.FormaFactorizada;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Simplificacion;

public record DatosGrafica(char variable, Polinomio numerador, Polinomio denominador, String leyendaLatex,
                           List<Raiz> raices, Optional<Racional> corteY, Optional<Punto> vertice,
                           List<Racional> asintotas, List<Punto> huecos, Extra extra) {

    public record Marca(double x, String etiqueta) {
    }

    public record Periodica(double fase, double periodo) {

        public List<Double> entre(double desde, double hasta) {
            List<Double> r = new ArrayList<>();
            long k0 = (long) Math.ceil((desde - fase) / periodo);
            long k1 = (long) Math.floor((hasta - fase) / periodo);
            for (long k = k0; k <= k1 && r.size() < 1000; k++) {
                r.add(fase + k * periodo);
            }
            return r;
        }
    }

    public record Extra(FuncionReal curva, List<Marca> marcas, List<Periodica> periodicas, List<Double> verticales,
                        Double horizontal, Recta oblicua, MarcaLimite limite, Calculo calculo) {
        public static final Extra NINGUNO = new Extra(null, List.of(), List.of());

        public Extra {
            marcas = List.copyOf(marcas);
            periodicas = List.copyOf(periodicas);
            verticales = List.copyOf(verticales);
        }

        public Extra(FuncionReal curva, List<Marca> marcas, List<Periodica> periodicas, List<Double> verticales,
                     Double horizontal, Recta oblicua, MarcaLimite limite) {
            this(curva, marcas, periodicas, verticales, horizontal, oblicua, limite, null);
        }

        public Extra(FuncionReal curva, List<Marca> marcas, List<Periodica> periodicas) {
            this(curva, marcas, periodicas, List.of(), null, null, null, null);
        }

        Extra conLimite(MarcaLimite m) {
            return new Extra(curva, marcas, periodicas, verticales, horizontal, oblicua, m, calculo);
        }
    }

    public record Calculo(String nombre, FuncionReal segunda, String nombreSegunda, FuncionReal pendiente,
                          List<PuntoMarcado> puntos, Area area) {
        public Calculo {
            puntos = List.copyOf(puntos);
        }
    }

    public record PuntoMarcado(double x, double y, String etiqueta) {
    }

    public record Area(double a, double b, String valor) {
    }

    public static DatosGrafica deCalculo(char x, Expresion f, Calculo calculo) {
        return new DatosGrafica(x, Polinomio.UNO, Polinomio.UNO, leyenda(x, ImpresoraLatex.imprimir(f)), List.of(),
                Optional.empty(), Optional.empty(), List.of(), List.of(),
                new Extra(new FuncionReal(new CurvaExpresion(f, x)), List.of(), List.of(), List.of(), null, null, null,
                        calculo));
    }

    public record Recta(double pendiente, double ordenada) {
        public double en(double x) {
            return pendiente * x + ordenada;
        }
    }

    public record MarcaLimite(Double a, Double valor, boolean hueco, boolean haciaMas, boolean izquierda,
                              boolean derecha) {
    }

    public DatosGrafica {
        raices = List.copyOf(raices);
        asintotas = List.copyOf(asintotas);
        huecos = List.copyOf(huecos);
        extra = extra == null ? Extra.NINGUNO : extra;
    }

    public DatosGrafica(char variable, Polinomio numerador, Polinomio denominador, String leyendaLatex,
                        List<Raiz> raices, Optional<Racional> corteY, Optional<Punto> vertice,
                        List<Racional> asintotas, List<Punto> huecos) {
        this(variable, numerador, denominador, leyendaLatex, raices, corteY, vertice, asintotas, huecos, Extra.NINGUNO);
    }

    public boolean esRacional() {
        return !denominador.esConstante();
    }

    public FuncionReal funcion() {
        return extra.curva() != null ? extra.curva() : new FuncionReal(numerador, denominador, variable);
    }

    public static Optional<DatosGrafica> de(Resolucion r) {
        Polinomio p = Polinomio.desde(r.entrada());
        return AnalisisPolinomio.de(p, r.resultado()).map(a -> new DatosGrafica(a.variable(), p, Polinomio.UNO,
                leyenda(a.variable(), ImpresoraLatex.imprimir(r.entrada())), a.raices(), Optional.of(a.corteY()),
                a.vertice(), List.of(), List.of(), new Extra(null, marcas(r), List.of())));
    }

    private static List<Marca> marcas(Resolucion r) {
        if (r.extension() == null) {
            return List.of();
        }
        List<Marca> m = new ArrayList<>();
        var ext = r.extension();
        if (ext.aproximada()) {
            for (String t : ext.aproximadas()) {
                if (!t.contains("i")) {
                    double x = Double.parseDouble(t);
                    m.add(new Marca(x, "(≈ " + pe.mathnova.presentation.FormateadorTexto.pizarraConNegativos(
                            String.format(java.util.Locale.ROOT, "%.4f", x)) + ", 0)"));
                }
            }
            return m;
        }
        for (var f : ext.resultado().factores()) {
            var q = f.polinomio();
            if (q.grado() != 1) {
                continue;
            }
            var raiz = q.coeficiente(0).negar().dividir(q.principal());
            if (raiz.esRacional() || !raiz.esReal()) {
                continue;
            }
            double x = pe.mathnova.domain.numeros.EvaluacionNumerica.evaluar(raiz,
                    new java.math.MathContext(20)).re().doubleValue();
            m.add(new Marca(x, "(" + pe.mathnova.presentation.FormateadorTexto.pizarraConNegativos(raiz.toString())
                    + ", 0)"));
        }
        return m;
    }

    public static Optional<DatosGrafica> de(pe.mathnova.domain.factorizacion.ResolucionFunciones r) {
        java.util.Set<Character> letras = pe.mathnova.domain.expresion.VariablePrincipal.de(r.entrada()).variables();
        if (letras.size() > 1 || !letras.isEmpty() && !letras.contains('x')) {
            return Optional.empty();
        }
        List<Periodica> periodicas = new ArrayList<>();
        pe.mathnova.domain.factorizacion.SustitucionFunciones.recorrerFunciones(r.entrada(), f -> {
            Double k = multiploDeX(f.argumento());
            if (k == null) {
                return;
            }
            Periodica p = switch (f.tipo()) {
                case TAN, SEC -> new Periodica(Math.PI / (2 * k), Math.PI / k);
                case COT, CSC -> new Periodica(0, Math.PI / k);
                default -> null;
            };
            if (p != null && !periodicas.contains(p)) {
                periodicas.add(p);
            }
        });
        FuncionReal curva = new FuncionReal(new CurvaExpresion(r.entrada(), 'x'));
        return Optional.of(new DatosGrafica('x', Polinomio.UNO, Polinomio.UNO,
                leyenda('x', ImpresoraLatex.imprimir(r.entrada())), List.of(), Optional.empty(), Optional.empty(),
                List.of(), List.of(), new Extra(curva, List.of(), periodicas)));
    }

    private static Double multiploDeX(pe.mathnova.domain.expresion.Expresion e) {
        if (e instanceof pe.mathnova.domain.expresion.Variable v && v.nombre() == 'x') {
            return 1.0;
        }
        if (e instanceof pe.mathnova.domain.expresion.Producto(List<pe.mathnova.domain.expresion.Expresion> fs)
                && fs.size() == 2 && fs.getFirst() instanceof pe.mathnova.domain.expresion.Constante c
                && c.valor().signo() > 0 && fs.get(1) instanceof pe.mathnova.domain.expresion.Variable v
                && v.nombre() == 'x') {
            return FuncionReal.aDouble(c.valor());
        }
        return null;
    }

    public static Optional<DatosGrafica> de(Simplificacion s) {
        Polinomio numOriginal = s.numerador().resultado().expandir();
        Polinomio denOriginal = s.denominador().resultado().expandir();
        Polinomio arriba = s.arriba().expandir();
        Polinomio abajo = s.abajo().expandir();
        java.util.Set<Character> letras = new java.util.TreeSet<>(numOriginal.variables());
        letras.addAll(denOriginal.variables());
        if (letras.size() != 1) {
            return Optional.empty();
        }
        char x = letras.iterator().next();

        List<Racional> asintotas = raicesLineales(s.abajo(), x).stream().map(Raiz::valor).toList();
        List<Punto> huecos = new ArrayList<>();
        for (Factor f : s.cancelados()) {
            if (f.polinomio().grado() != 1) {
                continue;
            }
            Racional a = raiz(f.polinomio(), x);
            Racional den = AnalisisPolinomio.evaluar(abajo, x, a);
            if (!den.esCero()) {
                huecos.add(new Punto(a, AnalisisPolinomio.evaluar(arriba, x, a).dividir(den)));
            }
        }
        huecos.sort(Comparator.comparing(Punto::x));
        List<Raiz> raices = raicesLineales(s.arriba(), x).stream()
                .filter(r -> !AnalisisPolinomio.evaluar(denOriginal, x, r.valor()).esCero()).toList();
        Optional<Racional> corteY = AnalisisPolinomio.evaluar(denOriginal, x, Racional.CERO).esCero()
                ? Optional.empty()
                : Optional.of(AnalisisPolinomio.evaluar(arriba, x, Racional.CERO)
                        .dividir(AnalisisPolinomio.evaluar(abajo, x, Racional.CERO)));
        List<Double> irracionales = new ArrayList<>();
        Double horizontal = null;
        Recta oblicua = null;
        Optional<Asintotas> as = Asintotas.de(s);
        if (as.isPresent()) {
            for (NumeroAlgebraico v : as.get().verticales()) {
                if (!v.esRacional()) {
                    irracionales.add(decimal(v));
                }
            }
            horizontal = as.get().horizontal().map(FuncionReal::aDouble).orElse(null);
            oblicua = as.get().oblicua().map(r -> new Recta(FuncionReal.aDouble(r.pendiente()),
                    FuncionReal.aDouble(r.ordenada()))).orElse(null);
        }
        return Optional.of(new DatosGrafica(x, arriba, abajo, leyenda(x, ImpresoraLatex.imprimir(s.entrada())),
                raices, corteY, Optional.empty(), asintotas, huecos,
                new Extra(null, List.of(), List.of(), irracionales, horizontal, oblicua, null)));
    }

    private static double decimal(NumeroAlgebraico z) {
        return EvaluacionNumerica.evaluar(z, new java.math.MathContext(20)).re().doubleValue();
    }

    public static Optional<DatosGrafica> de(ResolucionLimite r, Optional<Simplificacion> fraccion) {
        Limite l = r.entrada();
        char x = l.variable();
        Expresion f = l.cuerpo();
        java.util.Set<Character> letras = pe.mathnova.domain.expresion.Expresiones.variables(f);
        if (letras.size() > 1 || !letras.isEmpty() && !letras.contains(x)) {
            return Optional.empty();
        }
        DatosGrafica base = fraccion.flatMap(DatosGrafica::de).orElseGet(() -> new DatosGrafica(x, Polinomio.UNO,
                Polinomio.UNO, leyenda(x, ImpresoraLatex.imprimir(f)), List.of(), Optional.empty(), Optional.empty(),
                List.of(), List.of(), new Extra(new FuncionReal(new CurvaExpresion(f, x)), List.of(), List.of())));
        Double valor = r.resultado() instanceof ResultadoLimite.Finito fin ? decimal(fin.valor()) : null;
        MarcaLimite marca;
        if (l.punto() instanceof Limite.Finito p) {
            NumeroAlgebraico a = Evaluador.constante(p.valor()).orElse(NumeroAlgebraico.CERO);
            boolean definida = Evaluador.evaluar(f, x, new Valor.Numero(a)) instanceof Valor.Numero;
            marca = new MarcaLimite(decimal(a), valor, !definida, false, l.lado() != Limite.Lado.DERECHA,
                    l.lado() != Limite.Lado.IZQUIERDA);
        } else {
            boolean mas = ((Limite.Infinito) l.punto()).positivo();
            marca = new MarcaLimite(null, valor, false, mas, !mas, mas);
        }
        boolean yaHueco = marca.a() != null && base.huecos().stream()
                .anyMatch(h -> Math.abs(FuncionReal.aDouble(h.x()) - marca.a()) < 1e-12);
        MarcaLimite m = yaHueco ? new MarcaLimite(marca.a(), marca.valor(), false, marca.haciaMas(), marca.izquierda(),
                marca.derecha()) : marca;
        return Optional.of(new DatosGrafica(base.variable(), base.numerador(), base.denominador(),
                leyenda(x, ImpresoraLatex.imprimir(f)), base.raices(), base.corteY(), base.vertice(), base.asintotas(),
                base.huecos(), base.extra().conLimite(m)));
    }

    private static String leyenda(char x, String latex) {
        return "f(" + x + ") = " + latex;
    }

    private static List<Raiz> raicesLineales(FormaFactorizada forma, char x) {
        List<Raiz> r = new ArrayList<>();
        for (Factor f : forma.factores()) {
            if (f.polinomio().grado() == 1 && f.polinomio().variables().contains(x)) {
                r.add(new Raiz(raiz(f.polinomio(), x), f.multiplicidad()));
            }
        }
        r.sort(Comparator.comparing(Raiz::valor));
        return r;
    }

    private static Racional raiz(Polinomio lineal, char x) {
        return lineal.constante().negar().dividir(lineal.coeficiente(Monomio.de(x, 1)));
    }
}
