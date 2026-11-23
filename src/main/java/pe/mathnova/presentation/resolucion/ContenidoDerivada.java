package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.derivadas.AnalisisDerivada;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.derivadas.MotorDerivadas;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.CurvaExpresion;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.grafica.FuncionReal;

/**
 * Una derivada (Sprint 6).
 * <ul>
 *   <li>Gráfica: f(x) en azul y f′(x) en naranja con su leyenda, los puntos críticos marcados sobre f y, al
 *       pasar el cursor por f, la recta tangente con su pendiente.</li>
 *   <li>Análisis: los puntos críticos con su tipo, los intervalos de crecimiento (recta de signos de f′) y
 *       la concavidad con los puntos de inflexión (signo de f″).</li>
 *   <li>Exploración: el camino usado y los otros ("Expandir primero…" frente a "Regla del producto"), con
 *       Probar.</li>
 * </ul>
 */
public record ContenidoDerivada(String latexSolucion, Optional<DatosGrafica> grafica, List<SeccionAnalisis> secciones,
                                VistaExploracion exploracion) implements ContenidoPestanas {

    public ContenidoDerivada {
        secciones = List.copyOf(secciones);
    }

    public static ContenidoDerivada de(ResolucionDerivada r, List<CaminoCalculo> caminos, boolean codigos) {
        return new ContenidoDerivada(r.latex(), grafica(r), secciones(r),
                VistaExploracion.deCalculo(caminos, MotorDerivadas::nombreCamino, codigos));
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }

    // ------------------------------------------------------------------ Gráfica

    static Optional<DatosGrafica> grafica(ResolucionDerivada r) {
        char x = r.variable();
        Expresion f = r.funcion();
        var letras = Expresiones.variables(f);
        if (r.esImplicita() || letras.size() > 1 || !letras.isEmpty() && !letras.contains(x)) {
            return Optional.empty();
        }
        try {
            Expresion primera = r.orden() == 1 ? r.resultado() : Derivador.derivar(f, x).derivada();
            List<DatosGrafica.PuntoMarcado> puntos = new ArrayList<>();
            if (r.analisis() != null) {
                for (AnalisisDerivada.PuntoCritico c : r.analisis().criticos()) {
                    String tipo = switch (c.tipo()) {
                        case MAXIMO -> "máx ";
                        case MINIMO -> "mín ";
                        case NINGUNO -> "";
                    };
                    puntos.add(new DatosGrafica.PuntoMarcado(FuncionReal.aDouble(c.x()), FuncionReal.aDouble(c.y()),
                            tipo + "(" + FormateadorTexto.numero(c.x()) + ", " + FormateadorTexto.numero(c.y()) + ")"));
                }
            }
            DatosGrafica.Calculo calculo = new DatosGrafica.Calculo("f(" + x + ")",
                    new FuncionReal(new CurvaExpresion(r.resultado(), x)), r.nombre(),
                    new FuncionReal(new CurvaExpresion(primera, x)), puntos, null);
            return Optional.of(DatosGrafica.deCalculo(x, f, calculo));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    // ------------------------------------------------------------------ Análisis

    static List<SeccionAnalisis> secciones(ResolucionDerivada r) {
        AnalisisDerivada a = r.analisis();
        if (a == null) {
            String aviso = r.esImplicita()
                    ? "En una derivación implícita " + r.implicita() + "' depende de " + r.variable() + " y de "
                            + r.implicita() + ": no hay una sola función que analizar."
                    : r.orden() > 1 ? "El análisis de puntos críticos se hace con la primera derivada: escribe d/d"
                            + r.variable() + "(…)."
                    : "Los puntos críticos, el crecimiento y la concavidad se analizan para polinomios y fracciones "
                            + "de polinomios de una variable.";
            return List.of(new SeccionAnalisis("Análisis de la función", List.of(), List.of(), List.of(), null, aviso));
        }
        char x = a.variable();
        List<SeccionAnalisis> s = new ArrayList<>();
        List<VistaPropiedades.Fila> criticos = new ArrayList<>();
        for (AnalisisDerivada.PuntoCritico c : a.criticos()) {
            String en = FormateadorTexto.numero(c.x());
            criticos.add(new VistaPropiedades.Fila(x + " = " + en, c.tipo().texto() + " · f(" + en + ") = "
                    + FormateadorTexto.numero(c.y()) + " · " + c.criterio()));
        }
        if (criticos.isEmpty()) {
            criticos.add(new VistaPropiedades.Fila("Puntos críticos", "f'(" + x + ") = 0 no tiene soluciones racionales"));
        }
        s.add(new SeccionAnalisis("Puntos críticos (f'(" + x + ") = 0)", criticos, List.of(), List.of(), null, null));
        s.add(tramos("Crecimiento", a.crecimiento(), "Crece en", "Decrece en", "Signo de f'(" + x + ")",
                List.of()));
        List<VistaPropiedades.Fila> inflexion = List.of(new VistaPropiedades.Fila("Puntos de inflexión",
                a.inflexiones().isEmpty() ? "no tiene" : String.join(", ", a.inflexiones().stream()
                        .map(i -> "(" + FormateadorTexto.numero(i.x()) + ", " + FormateadorTexto.numero(i.y()) + ")")
                        .toList())));
        s.add(tramos("Concavidad", a.concavidad(), "Cóncava hacia arriba en", "Cóncava hacia abajo en",
                "Signo de f''(" + x + ")", a.concavidad().aviso() == null ? inflexion : List.of()));
        return s;
    }

    private static SeccionAnalisis tramos(String titulo, AnalisisDerivada.Tramos t, String positivo, String negativo,
                                          String rotulo, List<VistaPropiedades.Fila> extra) {
        if (t.aviso() != null) {
            return new SeccionAnalisis(titulo, List.of(), List.of(), List.of(), null, t.aviso());
        }
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        filas.add(new VistaPropiedades.Fila(positivo, intervalos(t, 1)));
        filas.add(new VistaPropiedades.Fila(negativo, intervalos(t, -1)));
        filas.addAll(extra);
        List<String> marcas = t.marcas().stream().map(FormateadorTexto::numero).toList();
        return new SeccionAnalisis(titulo, filas, marcas, t.signos(), rotulo, null);
    }

    /** "(−∞, −1) ∪ (3, ∞)", o "ningún intervalo". */
    static String intervalos(AnalisisDerivada.Tramos t, int signo) {
        List<String> r = new ArrayList<>();
        List<Racional> m = t.marcas();
        for (int i = 0; i < t.signos().size(); i++) {
            if (t.signos().get(i) != signo) {
                continue;
            }
            String izq = i == 0 ? "−∞" : ImpresoraPizarra.numero(m.get(i - 1));
            String der = i == m.size() ? "∞" : ImpresoraPizarra.numero(m.get(i));
            r.add("(" + izq + ", " + der + ")");
        }
        return r.isEmpty() ? "ningún intervalo" : String.join(" ∪ ", r);
    }
}
