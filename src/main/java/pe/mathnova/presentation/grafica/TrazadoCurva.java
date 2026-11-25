package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;

public final class TrazadoCurva {

    private TrazadoCurva() {
    }

    public static List<List<double[]>> trazos(FuncionReal f, double desde, double hasta, int muestras,
                                             List<Double> asintotas, double yMin, double yMax) {
        List<List<double[]>> trazos = new ArrayList<>();
        List<double[]> actual = null;
        double xAnterior = Double.NaN;
        int n = Math.max(2, muestras);
        for (int i = 0; i < n; i++) {
            double x = desde + (hasta - desde) * i / (n - 1);
            double y = f.valor(x);
            boolean cruza = false;
            if (!Double.isNaN(xAnterior)) {
                for (double a : asintotas) {
                    if ((xAnterior - a) * (x - a) <= 0) {
                        cruza = true;
                        break;
                    }
                }
            }
            xAnterior = x;
            if (!Double.isFinite(y) || cruza) {
                actual = null;
                continue;
            }
            double recortado = Math.max(yMin, Math.min(yMax, y));
            if (actual == null) {
                actual = new ArrayList<>();
                trazos.add(actual);
            }
            actual.add(new double[] {x, recortado});
            if (recortado != y) {
                actual = null;
            }
        }
        return trazos;
    }
}
