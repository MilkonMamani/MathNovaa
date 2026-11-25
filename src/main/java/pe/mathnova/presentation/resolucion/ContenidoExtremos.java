package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos;
import pe.mathnova.domain.calculo.extremos.ResolucionExtremos.Punto;
import pe.mathnova.domain.calculo.derivadas.Simplificador;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.presentation.grafica.DatosCurvasNivel;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.grafica.FuncionReal;
import pe.mathnova.presentation.grafica.VentanaGrafica;

public record ContenidoExtremos(String latexSolucion, List<SeccionAnalisis> secciones, DatosCurvasNivel curvas)
        implements ContenidoPestanas {

    public ContenidoExtremos {
        secciones = List.copyOf(secciones);
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static String n(Racional q) {
        return ImpresoraPizarra.numero(q);
    }

    public static ContenidoExtremos de(ResolucionExtremos r) {
        return new ContenidoExtremos(r.latex(), secciones(r), curvas(r));
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

    static List<SeccionAnalisis> secciones(ResolucionExtremos r) {
        List<SeccionAnalisis> s = new ArrayList<>();
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        boolean lagrange = r.tipo() == ResolucionExtremos.Tipo.LAGRANGE;
        for (Punto pt : r.puntos()) {
            String valor = lagrange
                    ? "λ = " + n(pt.lambda()) + ",  f = " + n(pt.valor()) + ":  " + pt.clase().texto()
                    : "D = " + n(pt.d()) + ",  fxx = " + n(pt.fxx()) + ":  " + pt.clase().texto();
            filas.add(new VistaPropiedades.Fila(pt.texto(), valor));
        }
        if (filas.isEmpty()) {
            filas.add(new VistaPropiedades.Fila("Puntos", r.texto()));
        }
        String aviso = r.avisos().isEmpty() ? lagrange
                ? "Se compara f en los puntos hallados sobre la restricción."
                : "D = fxx·fyy − fxy². D > 0 y fxx > 0: mínimo; D > 0 y fxx < 0: máximo; D < 0: silla; D = 0: el "
                        + "criterio no decide (RN39)."
                : String.join(" ", r.avisos());
        s.add(new SeccionAnalisis(lagrange ? "Puntos sobre la restricción" : "Puntos críticos", filas, List.of(),
                List.of(), null, aviso));
        if (!r.hessiana().isEmpty()) {
            List<VistaPropiedades.Fila> h = new ArrayList<>();
            for (int i = 0; i < r.hessiana().size(); i++) {
                List<String> fila = r.hessiana().get(i).stream().map(ContenidoExtremos::p).toList();
                h.add(new VistaPropiedades.Fila("Fila " + (i + 1), "[ " + String.join("   ", fila) + " ]"));
            }
            s.add(new SeccionAnalisis("Matriz hessiana", h));
        }
        return s;
    }

    static DatosCurvasNivel curvas(ResolucionExtremos r) {
        if (r.variables().size() != 2) {
            return null;
        }
        char x = r.variables().get(0);
        char y = r.variables().get(1);
        double xmin = -2;
        double xmax = 2;
        double ymin = -2;
        double ymax = 2;
        for (Punto pt : r.puntos()) {
            double px = FuncionReal.aDouble(pt.coordenadas().get(0));
            double py = FuncionReal.aDouble(pt.coordenadas().get(1));
            xmin = Math.min(xmin, px - 2);
            xmax = Math.max(xmax, px + 2);
            ymin = Math.min(ymin, py - 2);
            ymax = Math.max(ymax, py + 2);
        }
        VentanaGrafica v = new VentanaGrafica(xmin, xmax, ymin, ymax);
        List<DatosCurvasNivel.Marca> marcas = new ArrayList<>();
        for (Punto pt : r.puntos()) {
            DatosCurvasNivel.TipoMarca tipo = pt.clase().esMinimo() ? DatosCurvasNivel.TipoMarca.MINIMO
                    : pt.clase().esMaximo() ? DatosCurvasNivel.TipoMarca.MAXIMO
                    : pt.clase() == ResolucionExtremos.Clase.SILLA ? DatosCurvasNivel.TipoMarca.SILLA
                    : DatosCurvasNivel.TipoMarca.PUNTO;
            marcas.add(new DatosCurvasNivel.Marca(FuncionReal.aDouble(pt.coordenadas().get(0)),
                    FuncionReal.aDouble(pt.coordenadas().get(1)), pt.texto() + " " + pt.clase().texto(), tipo));
        }
        List<DatosCurvasNivel.Curva> curvas = new ArrayList<>();
        if (r.restriccion() != null) {
            curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.IMPLICITA, r.restriccion(), Double.NaN,
                    Double.NaN, "g = 0", DatosCurvasNivel.Estilo.RESTRICCION));
            for (Punto pt : r.puntos()) {
                if (pt.clase().esMaximo() || pt.clase().esMinimo()) {
                    Expresion nivel = Simplificador.simplificar(Simplificador.suma(List.of(r.funcion(),
                            Simplificador.negar(new Constante(pt.valor())))));
                    curvas.add(new DatosCurvasNivel.Curva(DatosCurvasNivel.Forma.IMPLICITA, nivel, Double.NaN,
                            Double.NaN, "f = " + n(pt.valor()), DatosCurvasNivel.Estilo.NIVEL_OPTIMO));
                }
            }
        }
        return new DatosCurvasNivel(r.funcion(), x, y, v, curvas, marcas, List.of(), null,
                "f(" + x + ", " + y + ") = c");
    }
}
