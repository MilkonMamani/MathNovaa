package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;

/**
 * El rectángulo del plano que se ve en la gráfica. Sin JavaFX.
 *
 * <p>Ventana inicial (HU35): en x, de la raíz menor a la mayor con un margen del 30 % a cada lado
 * (de −5 a 5 si no hay raíces reales); en y, lo que alcanza la curva en ese tramo, recortado a ±50
 * para que una asíntota no aplaste la gráfica.
 */
public record VentanaGrafica(double xMin, double xMax, double yMin, double yMax) {

    static final double MARGEN_X = 0.30;
    static final double RECORTE_Y = 50;
    static final double RECORTE_PERIODICA = 6;
    static final double MEDIA_SIN_RAICES = 5;
    private static final int MUESTRAS = 400;
    /** No se acerca ni se aleja más allá de esto (ancho de la ventana). */
    private static final double ANCHO_MINIMO = 1e-4;
    private static final double ANCHO_MAXIMO = 1e6;

    public double ancho() {
        return xMax - xMin;
    }

    public double alto() {
        return yMax - yMin;
    }

    public boolean contiene(double x, double y) {
        return x >= xMin && x <= xMax && y >= yMin && y <= yMax;
    }

    public static VentanaGrafica inicial(DatosGrafica d) {
        // Puntos que deben verse: las raíces y, en una racional, también asíntotas y huecos.
        List<Double> xs = new ArrayList<>();
        d.raices().forEach(r -> xs.add(FuncionReal.aDouble(r.valor())));
        d.asintotas().forEach(a -> xs.add(FuncionReal.aDouble(a)));
        d.huecos().forEach(h -> xs.add(FuncionReal.aDouble(h.x())));
        d.extra().marcas().forEach(m -> xs.add(m.x()));
        d.extra().verticales().forEach(xs::add);
        DatosGrafica.Calculo calculo = d.extra().calculo();
        if (calculo != null) {
            calculo.puntos().forEach(p -> xs.add(p.x()));
            if (calculo.area() != null) {
                xs.add(calculo.area().a());
                xs.add(calculo.area().b());
            }
        }
        DatosGrafica.MarcaLimite limite = d.extra().limite();
        if (limite != null && limite.a() != null) {
            // El punto del límite, con aire a los dos lados para ver cómo se acerca la curva.
            xs.add(limite.a() - 2);
            xs.add(limite.a() + 2);
        } else if (limite != null) {
            xs.add(0.0);
            xs.add(limite.haciaMas() ? 20.0 : -20.0);
        }
        // Con asíntotas periódicas (tan x) la curva sube sin límite: se recorta más cerca.
        double recorte = d.extra().periodicas().isEmpty() ? RECORTE_Y : RECORTE_PERIODICA;
        double xMin;
        double xMax;
        if (xs.isEmpty()) {
            xMin = -MEDIA_SIN_RAICES;
            xMax = MEDIA_SIN_RAICES;
        } else {
            double menor = xs.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            double mayor = xs.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            if (mayor - menor < 1e-9) {
                // Un solo punto: no hay tramo del que sacar el 30 %.
                xMin = menor - MEDIA_SIN_RAICES;
                xMax = mayor + MEDIA_SIN_RAICES;
            } else {
                double margen = (mayor - menor) * MARGEN_X;
                xMin = menor - margen;
                xMax = mayor + margen;
            }
        }
        FuncionReal f = d.funcion();
        double yMin = 0; // el eje x siempre a la vista
        double yMax = 0;
        for (int i = 0; i <= MUESTRAS; i++) {
            double y = f.valor(xMin + (xMax - xMin) * i / MUESTRAS);
            if (Double.isNaN(y) || Double.isInfinite(y)) {
                continue;
            }
            y = Math.max(-recorte, Math.min(recorte, y));
            yMin = Math.min(yMin, y);
            yMax = Math.max(yMax, y);
        }
        if (calculo != null) {
            for (DatosGrafica.PuntoMarcado p : calculo.puntos()) {
                if (Double.isFinite(p.y())) {
                    yMin = Math.min(yMin, Math.max(-recorte, p.y()));
                    yMax = Math.max(yMax, Math.min(recorte, p.y()));
                }
            }
        }
        // El valor del límite y la asíntota horizontal también a la vista.
        for (Double y : new Double[] {limite == null ? null : limite.valor(), d.extra().horizontal()}) {
            if (y != null && Double.isFinite(y)) {
                yMin = Math.min(yMin, y);
                yMax = Math.max(yMax, y);
            }
        }
        if (yMax - yMin < 1e-9) {
            yMin -= 1;
            yMax += 1;
        }
        double aire = (yMax - yMin) * 0.12;
        return new VentanaGrafica(xMin, xMax, yMin - aire, yMax + aire);
    }

    /**
     * Acerca (factor menor que 1) o aleja (mayor que 1) dejando fijo el punto (cx, cy), que es donde
     * está el cursor.
     */
    public VentanaGrafica escalar(double factor, double cx, double cy) {
        double nuevoAncho = ancho() * factor;
        if (nuevoAncho < ANCHO_MINIMO || nuevoAncho > ANCHO_MAXIMO) {
            return this;
        }
        return new VentanaGrafica(cx - (cx - xMin) * factor, cx + (xMax - cx) * factor,
                cy - (cy - yMin) * factor, cy + (yMax - cy) * factor);
    }

    public VentanaGrafica mover(double dx, double dy) {
        return new VentanaGrafica(xMin + dx, xMax + dx, yMin + dy, yMax + dy);
    }

    /** Separación "redonda" entre marcas (1, 2 o 5 por una potencia de 10) para unas {@code marcas} marcas. */
    public static double paso(double largo, int marcas) {
        double bruto = largo / Math.max(1, marcas);
        double potencia = Math.pow(10, Math.floor(Math.log10(bruto)));
        double resto = bruto / potencia;
        double redondo = resto < 1.5 ? 1 : resto < 3.5 ? 2 : resto < 7.5 ? 5 : 10;
        return redondo * potencia;
    }
}
