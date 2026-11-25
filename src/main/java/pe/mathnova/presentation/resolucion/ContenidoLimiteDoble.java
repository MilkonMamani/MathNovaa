package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Trayectoria;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.LimiteDoble;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.Variable;
import pe.mathnova.presentation.grafica.CurvaExpresion;
import pe.mathnova.presentation.grafica.DatosCurvasNivel;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.grafica.VentanaGrafica;

public record ContenidoLimiteDoble(String latexSolucion, List<SeccionAnalisis> secciones, DatosCurvasNivel curvas)
        implements ContenidoPestanas {

    public ContenidoLimiteDoble {
        secciones = List.copyOf(secciones);
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    public static ContenidoLimiteDoble de(ResolucionLimiteDoble r) {
        return new ContenidoLimiteDoble(r.latex(), secciones(r), curvas(r));
    }

    @Override
    public Optional<DatosGrafica> grafica() {
        return Optional.empty();
    }

    @Override
    public Optional<DatosCurvasNivel> curvasNivel() {
        return Optional.ofNullable(curvas);
    }

    @Override
    public VistaExploracion exploracion() {
        return VistaExploracion.vacia();
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }

    static List<SeccionAnalisis> secciones(ResolucionLimiteDoble r) {
        List<SeccionAnalisis> s = new ArrayList<>();
        if (!r.trayectorias().isEmpty()) {
            List<VistaPropiedades.Fila> filas = new ArrayList<>();
            for (Trayectoria t : r.trayectorias()) {
                filas.add(new VistaPropiedades.Fila(t.nombre(), t.texto() + (t.dependeDeM() ? " (depende de "
                        + t.parametro() + ")" : "")));
            }
            String aviso = r.estado() == ResolucionLimiteDoble.Estado.NO_EXISTE && r.polar() == null ? r.motivo()
                    : "Que varias trayectorias den lo mismo no prueba que el límite exista (RN38).";
            s.add(new SeccionAnalisis("Trayectorias", filas, List.of(), List.of(), null, aviso));
        }
        if (r.polar() != null) {
            List<VistaPropiedades.Fila> filas = new ArrayList<>();
            LimiteDoble l = r.entrada();
            boolean origen = esCero(l.a()) && esCero(l.b());
            filas.add(new VistaPropiedades.Fila("Cambio", l.x() + " = " + (origen ? "" : p(l.a()) + " + ")
                    + "r cos θ,  " + l.y() + " = " + (origen ? "" : p(l.b()) + " + ") + "r sen θ"));
            filas.add(new VistaPropiedades.Fila("f en polares", p(r.polar().forma())));
            if (r.polar().cota() != null) {
                filas.add(new VistaPropiedades.Fila("Cota", r.polar().cota()));
            }
            s.add(new SeccionAnalisis("Forma polar", filas, List.of(), List.of(), null, r.polar().cota() == null
                    ? null : "Por el teorema del sándwich, el límite es 0."));
        }
        if (s.isEmpty()) {
            s.add(new SeccionAnalisis("Evaluación directa", List.of(new VistaPropiedades.Fila("Resultado",
                    r.texto())), List.of(), List.of(), null, "f es continua en el punto: no hacen falta trayectorias."));
        }
        return s;
    }

    private static boolean esCero(Expresion e) {
        return Simplificador.simplificar(e).equals(Constante.de(0));
    }

    private static double d(Expresion e) {
        return CurvaExpresion.evaluar(e, Map.of());
    }

    static DatosCurvasNivel curvas(ResolucionLimiteDoble r) {
        LimiteDoble l = r.entrada();
        if (!Expresiones.variables(l.cuerpo()).stream().allMatch(c -> c == l.x() || c == l.y())) {
            return null;
        }
        double a = d(l.a());
        double b = d(l.b());
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return null;
        }
        VentanaGrafica v = new VentanaGrafica(a - 2, a + 2, b - 2, b + 2);
        List<DatosCurvasNivel.Curva> curvas = new ArrayList<>();
        Variable x = new Variable(l.x());
        Expresion xa = new Suma(List.of(x, Simplificador.negar(l.a())));
        for (Trayectoria t : r.trayectorias()) {
            switch (t.camino()) {
                case HORIZONTAL -> curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.Y_DE_X, l.b(),
                        Double.NaN, Double.NaN, t.nombre() + ": " + t.texto(), DatosCurvasNivel.Estilo.TRAYECTORIA));
                case VERTICAL -> curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.X_DE_Y, l.a(),
                        Double.NaN, Double.NaN, t.nombre() + ": " + t.texto(), DatosCurvasNivel.Estilo.TRAYECTORIA));
                case RECTA -> {
                    for (int m : new int[] {1, -1}) {
                        Expresion recta = Simplificador.simplificar(new Suma(List.of(l.b(),
                                new Producto(List.of(Constante.de(m), xa)))));
                        Expresion valor = Simplificador.simplificar(Expresiones.reemplazar(t.valor(),
                                new Variable(t.parametro()), Constante.de(m)));
                        String nombre = t.nombre().replace(t.parametro() + "", m == 1 ? "" : "−");
                        curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.Y_DE_X, recta, Double.NaN,
                                Double.NaN, nombre + ": " + p(valor), DatosCurvasNivel.Estilo.TRAYECTORIA));
                    }
                }
                case POTENCIA_EN_X -> curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.Y_DE_X,
                        new Suma(List.of(l.b(), new Potencia(xa, Constante.de(t.n())))), Double.NaN, Double.NaN,
                        t.nombre() + ": " + t.texto(), DatosCurvasNivel.Estilo.TRAYECTORIA));
                case POTENCIA_EN_Y -> {
                    Expresion yb = new Suma(List.of(new Variable(l.y()), Simplificador.negar(l.b())));
                    curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.X_DE_Y,
                            new Suma(List.of(l.a(), new Potencia(yb, Constante.de(t.n())))), Double.NaN, Double.NaN,
                            t.nombre() + ": " + t.texto(), DatosCurvasNivel.Estilo.TRAYECTORIA));
                }
            }
        }
        List<DatosCurvasNivel.Marca> marcas = List.of(new DatosCurvasNivel.Marca(a, b,
                "(" + p(l.a()) + ", " + p(l.b()) + ")", DatosCurvasNivel.TipoMarca.PUNTO));
        return new DatosCurvasNivel(l.cuerpo(), l.x(), l.y(), v, curvas, marcas, List.of(), null, "f(" + l.x() + ", "
                + l.y() + ") = c");
    }
}
