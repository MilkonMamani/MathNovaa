package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;

/**
 * Los trazos de la curva, en coordenadas del plano y sin JavaFX. Se empieza un trazo nuevo donde el
 * valor no es finito, donde hay una asíntota entre dos muestras (así no se une una rama con la
 * otra) y donde la curva se va muy lejos de la ventana.
 */
public final class TrazadoCurva {

    private TrazadoCurva() {
    }

    /**
     * @param desde,hasta  tramo de x que se muestrea
     * @param muestras     cuántas muestras (al menos 2)
     * @param asintotas    las x de las asíntotas verticales
     * @param yMin,yMax    más allá de esto el trazo se recorta y se corta
     * @return cada trazo es una lista de puntos {x, y}
     */
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
                actual = null; // se fue muy lejos: lo que sigue es otro trazo
            }
        }
        return trazos;
    }
}
