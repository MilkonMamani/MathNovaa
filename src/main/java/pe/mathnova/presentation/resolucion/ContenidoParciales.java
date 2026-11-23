package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pe.mathnova.domain.calculo.parciales.MotorParciales;
import pe.mathnova.domain.calculo.parciales.ResolucionParcial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.presentation.grafica.DatosGrafica;

/**
 * Parciales, gradiente, derivada direccional y regla de la cadena (Sprint 7, HU28).
 * <ul>
 *   <li>Gráfica (Sprint 8): con dos variables, las curvas de nivel de f; para el gradiente (o la derivada
 *       direccional) en un punto, una flecha ∇f(P) en ese punto, perpendicular a la curva de nivel que pasa por él.</li>
 *   <li>Análisis: el dominio, la tabla de todas las parciales de primer y segundo orden y la matriz
 *       hessiana ya calculada (la usará el Sprint 8).</li>
 *   <li>Exploración: vacía, con su estado vacío.</li>
 * </ul>
 *
 * @param parciales las parciales y la hessiana de f, o null si no se pudieron calcular
 * @param curvas    las curvas de nivel (Sprint 8), o null si f no tiene exactamente dos variables
 */
public record ContenidoParciales(String latexSolucion, List<SeccionAnalisis> secciones,
                                 MotorParciales.TablaParciales parciales,
                                 pe.mathnova.presentation.grafica.DatosCurvasNivel curvas) implements ContenidoPestanas {

    public ContenidoParciales {
        secciones = List.copyOf(secciones);
    }

    public static ContenidoParciales de(ResolucionParcial r) {
        MotorParciales.TablaParciales t;
        try {
            t = MotorParciales.tabla(r.funcion());
        } catch (RuntimeException e) {
            t = null;
        }
        return new ContenidoParciales(r.latex(), secciones(r, t), t, curvas(r, t));
    }

    @Override
    public Optional<pe.mathnova.presentation.grafica.DatosCurvasNivel> curvasNivel() {
        return Optional.ofNullable(curvas);
    }

    /** Las curvas de nivel de f(x, y) y, si hay un punto, la flecha del gradiente en él. */
    static pe.mathnova.presentation.grafica.DatosCurvasNivel curvas(ResolucionParcial r,
                                                                    MotorParciales.TablaParciales t) {
        if (r.variables().size() != 2 || r.tipo() == ResolucionParcial.Tipo.CADENA) {
            return null;
        }
        char x = r.variables().get(0);
        char y = r.variables().get(1);
        List<Expresion> punto = switch (r.entrada()) {
            case pe.mathnova.domain.expresion.Gradiente g -> g.punto();
            case pe.mathnova.domain.expresion.Direccional d -> d.punto();
            default -> List.of();
        };
        double cx = 0;
        double cy = 0;
        List<pe.mathnova.presentation.grafica.DatosCurvasNivel.Flecha> flechas = new java.util.ArrayList<>();
        List<pe.mathnova.presentation.grafica.DatosCurvasNivel.Marca> marcas = new java.util.ArrayList<>();
        if (punto.size() == 2 && t != null) {
            cx = pe.mathnova.presentation.grafica.CurvaExpresion.evaluar(punto.get(0), Map.of());
            cy = pe.mathnova.presentation.grafica.CurvaExpresion.evaluar(punto.get(1), Map.of());
            Map<Character, Double> en = Map.of(x, cx, y, cy);
            double gx = pe.mathnova.presentation.grafica.CurvaExpresion.evaluar(t.primeras().get("f" + x), en);
            double gy = pe.mathnova.presentation.grafica.CurvaExpresion.evaluar(t.primeras().get("f" + y), en);
            String nombre = "(" + p(punto.get(0)) + ", " + p(punto.get(1)) + ")";
            marcas.add(new pe.mathnova.presentation.grafica.DatosCurvasNivel.Marca(cx, cy, null,
                    pe.mathnova.presentation.grafica.DatosCurvasNivel.TipoMarca.PUNTO));
            double largo = Math.hypot(gx, gy);
            if (Double.isFinite(largo) && largo > 0) {
                double escala = 1.5 / largo;
                flechas.add(new pe.mathnova.presentation.grafica.DatosCurvasNivel.Flecha(cx, cy, gx * escala,
                        gy * escala, "∇f" + nombre));
            }
        }
        if (!Double.isFinite(cx) || !Double.isFinite(cy)) {
            return null;
        }
        return new pe.mathnova.presentation.grafica.DatosCurvasNivel(r.funcion(), x, y,
                new pe.mathnova.presentation.grafica.VentanaGrafica(cx - 3, cx + 3, cy - 3, cy + 3), List.of(), marcas,
                flechas, null, "f(" + x + ", " + y + ") = c");
    }

    @Override
    public Optional<DatosGrafica> grafica() {
        return Optional.empty();
    }

    @Override
    public VistaExploracion exploracion() {
        return VistaExploracion.vacia();
    }

    @Override
    public Optional<String> comparacion() {
        return Optional.empty();
    }

    private static String p(Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    static List<SeccionAnalisis> secciones(ResolucionParcial r, MotorParciales.TablaParciales t) {
        List<SeccionAnalisis> s = new ArrayList<>();
        s.add(new SeccionAnalisis("Dominio de f", List.of(
                new VistaPropiedades.Fila("f", p(r.funcion())),
                new VistaPropiedades.Fila("Dominio", MotorParciales.textoDominio(r.dominio(), r.variables())))));
        if (t == null) {
            return s;
        }
        List<VistaPropiedades.Fila> primeras = new ArrayList<>();
        for (Map.Entry<String, Expresion> e : t.primeras().entrySet()) {
            primeras.add(new VistaPropiedades.Fila(nombre(e.getKey()), p(e.getValue())));
        }
        s.add(new SeccionAnalisis("Parciales de primer orden", primeras));
        List<VistaPropiedades.Fila> segundas = new ArrayList<>();
        for (Map.Entry<String, Expresion> e : t.segundas().entrySet()) {
            segundas.add(new VistaPropiedades.Fila(nombre(e.getKey()), p(e.getValue())));
        }
        s.add(new SeccionAnalisis("Parciales de segundo orden", segundas));
        List<VistaPropiedades.Fila> filas = new ArrayList<>();
        for (int i = 0; i < t.hessiana().size(); i++) {
            List<String> fila = t.hessiana().get(i).stream().map(ContenidoParciales::p).toList();
            filas.add(new VistaPropiedades.Fila("Fila " + (i + 1), "[ " + String.join("   ", fila) + " ]"));
        }
        boolean simetrica = true;
        for (int i = 0; i < t.hessiana().size(); i++) {
            for (int j = 0; j < i; j++) {
                simetrica &= p(t.hessiana().get(i).get(j)).equals(p(t.hessiana().get(j).get(i)));
            }
        }
        s.add(new SeccionAnalisis("Matriz hessiana", filas, List.of(), List.of(), null, simetrica
                ? "Es simétrica: las derivadas cruzadas coinciden (teorema de Schwarz). Con ella se clasifican los "
                        + "puntos críticos."
                : "Con ella se clasifican los puntos críticos."));
        return s;
    }

    /** "fx" → "∂f/∂x", "fxy" → "∂²f/∂y∂x" (primero x, luego y). */
    static String nombre(String clave) {
        if (clave.length() == 2) {
            return "∂f/∂" + clave.charAt(1);
        }
        char a = clave.charAt(1);
        char b = clave.charAt(2);
        return a == b ? "∂²f/∂" + a + "²" : "∂²f/∂" + b + "∂" + a;
    }
}
