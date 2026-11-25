package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pe.mathnova.domain.expresion.Expresion;

public final class MarchingSquares {

    public static final int LADO = 200;

    private MarchingSquares() {
    }

    public static double[][] malla(Expresion f, char x, char y, VentanaGrafica v) {
        double[][] r = new double[LADO + 1][LADO + 1];
        for (int i = 0; i <= LADO; i++) {
            double xi = v.xMin() + v.ancho() * i / LADO;
            for (int j = 0; j <= LADO; j++) {
                double yj = v.yMin() + v.alto() * j / LADO;
                r[i][j] = CurvaExpresion.evaluar(f, Map.of(x, xi, y, yj));
            }
        }
        return r;
    }

    public static double[] rango(double[][] m) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double[] fila : m) {
            for (double v : fila) {
                if (Double.isFinite(v)) {
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
            }
        }
        return min > max ? new double[] {Double.NaN, Double.NaN} : new double[] {min, max};
    }

    public static double[] niveles(double min, double max, int n) {
        double[] r = new double[n];
        for (int k = 0; k < n; k++) {
            r[k] = min + (max - min) * (k + 1) / (n + 1);
        }
        return r;
    }

    public static List<double[]> segmentos(double[][] m, double c, VentanaGrafica v) {
        int n = m.length - 1;
        double hx = v.ancho() / n;
        double hy = v.alto() / n;
        List<double[]> r = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double a = m[i][j];
                double b = m[i + 1][j];
                double d = m[i + 1][j + 1];
                double e = m[i][j + 1];
                if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(d) || !Double.isFinite(e)) {
                    continue;
                }
                int caso = (a > c ? 1 : 0) | (b > c ? 2 : 0) | (d > c ? 4 : 0) | (e > c ? 8 : 0);
                if (caso == 0 || caso == 15) {
                    continue;
                }
                double x0 = v.xMin() + i * hx;
                double y0 = v.yMin() + j * hy;
                double[] abajo = {x0 + hx * t(a, b, c), y0};
                double[] derecha = {x0 + hx, y0 + hy * t(b, d, c)};
                double[] arriba = {x0 + hx * t(e, d, c), y0 + hy};
                double[] izquierda = {x0, y0 + hy * t(a, e, c)};
                switch (caso) {
                    case 1, 14 -> agregar(r, izquierda, abajo);
                    case 2, 13 -> agregar(r, abajo, derecha);
                    case 3, 12 -> agregar(r, izquierda, derecha);
                    case 4, 11 -> agregar(r, derecha, arriba);
                    case 6, 9 -> agregar(r, abajo, arriba);
                    case 7, 8 -> agregar(r, izquierda, arriba);
                    case 5 -> {
                        agregar(r, izquierda, arriba);
                        agregar(r, abajo, derecha);
                    }
                    case 10 -> {
                        agregar(r, izquierda, abajo);
                        agregar(r, derecha, arriba);
                    }
                    default -> {
                    }
                }
            }
        }
        return r;
    }

    private static double t(double p, double q, double c) {
        return p == q ? 0.5 : (c - p) / (q - p);
    }

    private static void agregar(List<double[]> r, double[] p, double[] q) {
        r.add(new double[] {p[0], p[1], q[0], q[1]});
    }
}
