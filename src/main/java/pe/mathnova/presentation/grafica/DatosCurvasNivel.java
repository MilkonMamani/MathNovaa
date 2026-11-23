package pe.mathnova.presentation.grafica;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

/**
 * Lo que dibuja {@link CurvasNivel} (Sprint 8), sin JavaFX: las curvas de nivel de f(x, y), las curvas que van encima
 * (trayectorias, la restricción, los bordes de una región), los puntos marcados, las flechas y una región sombreada.
 *
 * @param funcion f(x, y) para las curvas de nivel, o null si solo se dibuja la región
 * @param x       la letra del eje horizontal
 * @param y       la letra del eje vertical
 * @param ventana la vista inicial
 * @param titulo  lo que encabeza la leyenda: "f(x, y) = c"
 */
public record DatosCurvasNivel(Expresion funcion, char x, char y, VentanaGrafica ventana, List<Curva> curvas,
                               List<Marca> marcas, List<Flecha> flechas, Region region, String titulo) {

    /** Cómo se dibuja una curva de encima. */
    public enum Estilo {
        /** Una trayectoria de un límite doble, en morado con guiones. */
        TRAYECTORIA,
        /** La restricción g = 0 de Lagrange, en negro. */
        RESTRICCION,
        /** La curva de nivel que toca la restricción en el óptimo, en verde. */
        NIVEL_OPTIMO,
        /** El borde de una región de integración, en azul marino. */
        BORDE
    }

    /** Cómo está dada la curva. */
    public enum Forma {
        /** y = g(x), con x entre desde y hasta. */
        Y_DE_X,
        /** x = g(y), con y entre desde y hasta. */
        X_DE_Y,
        /** G(x, y) = 0, en toda la ventana. */
        IMPLICITA,
        /** r = g(θ) en polares, con θ entre desde y hasta. */
        POLAR,
        /** El rayo θ = desde, de r = 0 a r = hasta. */
        RAYO
    }

    /**
     * @param expresion g o G; null en un RAYO
     * @param rotulo    lo que se escribe junto a la curva: "y = x²: 1/2"
     */
    public record Curva(Forma forma, Expresion expresion, double desde, double hasta, String rotulo, Estilo estilo) {
        public Curva {
            Objects.requireNonNull(forma, "forma");
            Objects.requireNonNull(estilo, "estilo");
        }
    }

    public enum TipoMarca { MINIMO, MAXIMO, SILLA, PUNTO }

    /** Un punto marcado: verde en un mínimo, rojo en un máximo, gris en una silla, azul en los demás. */
    public record Marca(double x, double y, String rotulo, TipoMarca tipo) {
    }

    /** Una flecha desde (x, y) en la dirección (dx, dy), ya con el largo con que se dibuja. */
    public record Flecha(double x, double y, double dx, double dy, String rotulo) {
    }

    /** La región de integración. */
    public sealed interface Region {
    }

    /**
     * {@code exterior} entre a y b, {@code interior} entre g1(exterior) y g2(exterior).
     *
     * @param interiorEsY si la variable de adentro es la del eje vertical
     */
    public record RegionCartesiana(boolean interiorEsY, double a, double b, Expresion g1, Expresion g2, char exterior)
            implements Region {
    }

    /** θ entre a y b, r entre r1(θ) y r2(θ). */
    public record RegionPolar(double a, double b, Expresion r1, Expresion r2) implements Region {
    }

    public DatosCurvasNivel {
        Objects.requireNonNull(ventana, "ventana");
        curvas = List.copyOf(curvas);
        marcas = List.copyOf(marcas);
        flechas = List.copyOf(flechas);
    }
}
