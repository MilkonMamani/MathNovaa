package pe.mathnova.presentation.grafica;

import java.util.List;
import java.util.Objects;

import pe.mathnova.domain.expresion.Expresion;

public record DatosCurvasNivel(Expresion funcion, char x, char y, VentanaGrafica ventana, List<Curva> curvas,
                               List<Marca> marcas, List<Flecha> flechas, Region region, String titulo) {

    public enum Estilo {
        TRAYECTORIA,
        RESTRICCION,
        NIVEL_OPTIMO,
        BORDE
    }

    public enum Forma {
        Y_DE_X,
        X_DE_Y,
        IMPLICITA,
        POLAR,
        RAYO
    }

    public record Curva(Forma forma, Expresion expresion, double desde, double hasta, String rotulo, Estilo estilo) {
        public Curva {
            Objects.requireNonNull(forma, "forma");
            Objects.requireNonNull(estilo, "estilo");
        }
    }

    public enum TipoMarca { MINIMO, MAXIMO, SILLA, PUNTO }

    public record Marca(double x, double y, String rotulo, TipoMarca tipo) {
    }

    public record Flecha(double x, double y, double dx, double dy, String rotulo) {
    }

    public sealed interface Region {
    }

    public record RegionCartesiana(boolean interiorEsY, double a, double b, Expresion g1, Expresion g2, char exterior)
            implements Region {
    }

    public record RegionPolar(double a, double b, Expresion r1, Expresion r2) implements Region {
    }

    public DatosCurvasNivel {
        Objects.requireNonNull(ventana, "ventana");
        curvas = List.copyOf(curvas);
        marcas = List.copyOf(marcas);
        flechas = List.copyOf(flechas);
    }
}
