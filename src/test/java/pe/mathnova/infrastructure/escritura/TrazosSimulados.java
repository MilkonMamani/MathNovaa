package pe.mathnova.infrastructure.escritura;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;

/**
 * SOLO PARA PRUEBAS TÉCNICAS: trazos SIMULADOS (no son escritura de una persona). Cada símbolo es una
 * polilínea dibujada a mano alzada en una cuadrícula, con un temblor aleatorio fijo (semilla), puntos cada
 * ~2 px y tiempos crecientes, como los que deja el lienzo. Sirven para probar el camino completo
 * trazos → imagen → modelo → editor → Resolver; no miden cuán bien lee el modelo la letra real.
 *
 * <p>Coordenadas de los glifos: x hacia la derecha, y hacia arriba, la línea base en y = 0 y la altura de
 * un dígito = 1. {@link Lienzo} las pasa a píxeles del lienzo (y hacia abajo).
 */
public final class TrazosSimulados {

    /** Un lienzo donde se van colocando símbolos. */
    public static final class Lienzo {
        private final double escala;
        private final double baseY;
        private final Random azar;
        private final List<Trazo> trazos = new ArrayList<>();
        private double x;
        private long t;

        /**
         * @param escala píxeles por unidad (alto de un dígito)
         * @param x0     dónde empieza, en píxeles
         * @param baseY  la línea base, en píxeles desde arriba
         */
        public Lienzo(double escala, double x0, double baseY, long semilla) {
            this.escala = escala;
            this.x = x0;
            this.baseY = baseY;
            this.azar = new Random(semilla);
        }

        public List<Trazo> trazos() {
            return List.copyOf(trazos);
        }

        /** Un símbolo en la posición actual, con tamaño {@code s} y elevado {@code dy} (unidades). */
        public Lienzo simbolo(char c, double s, double dy) {
            Glifo g = glifo(c);
            for (double[][] linea : g.lineas) {
                trazo(linea, x, dy, s);
            }
            x += (g.ancho * s + 0.18 * s) * escala;
            return this;
        }

        public Lienzo simbolo(char c) {
            return simbolo(c, 1, 0);
        }

        /** Varios símbolos seguidos, tamaño normal. */
        public Lienzo texto(String s) {
            for (char c : s.toCharArray()) {
                simbolo(c);
            }
            return this;
        }

        /** Exponente: más chico y arriba. */
        public Lienzo exponente(String s) {
            for (char c : s.toCharArray()) {
                simbolo(c, 0.55, 0.75);
            }
            x += 0.1 * escala;
            return this;
        }

        /** Un espacio de {@code u} unidades. */
        public Lienzo espacio(double u) {
            x += u * escala;
            return this;
        }

        public double x() {
            return x;
        }

        /** Mueve la pluma a {@code xPx} (píxeles). */
        public Lienzo irA(double xPx) {
            x = xPx;
            return this;
        }

        /** Una raya horizontal de {@code x0} a {@code x1} (píxeles) a la altura {@code y} (unidades). */
        public Lienzo raya(double x0, double x1, double y) {
            double[][] l = {{0, y}, {(x1 - x0) / escala, y}};
            trazo(l, x0, 0, 1);
            return this;
        }

        /** Una polilínea libre (unidades), con origen en {@code xPx}. */
        public Lienzo polilinea(double xPx, double[][] l) {
            trazo(l, xPx, 0, 1);
            return this;
        }

        private void trazo(double[][] linea, double ox, double dy, double s) {
            List<Punto> ps = new ArrayList<>();
            // Un temblor suave por trazo (desplazamiento) y otro por punto.
            double jx = azar.nextGaussian() * 0.015;
            double jy = azar.nextGaussian() * 0.015;
            for (int i = 0; i + 1 < linea.length; i++) {
                double ax = linea[i][0] * s;
                double ay = linea[i][1] * s + dy;
                double bx = linea[i + 1][0] * s;
                double by = linea[i + 1][1] * s + dy;
                double largo = Math.hypot(bx - ax, by - ay) * escala;
                int n = Math.max(1, (int) Math.ceil(largo / 2.0));
                for (int k = (i == 0 ? 0 : 1); k <= n; k++) {
                    double f = (double) k / n;
                    double ux = ax + (bx - ax) * f + jx + azar.nextGaussian() * 0.004;
                    double uy = ay + (by - ay) * f + jy + azar.nextGaussian() * 0.004;
                    ps.add(new Punto(ox + ux * escala, baseY - uy * escala, t));
                    t += 8;
                }
            }
            t += 180; // levanta el lápiz
            trazos.add(new Trazo(ps));
        }
    }

    record Glifo(double ancho, double[][][] lineas) {
    }

    static double[][] arco(double cx, double cy, double rx, double ry, double desde, double hasta, int n) {
        double[][] r = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            double a = Math.toRadians(desde + (hasta - desde) * i / n);
            r[i] = new double[] {cx + rx * Math.cos(a), cy + ry * Math.sin(a)};
        }
        return r;
    }

    static double[][] unir(double[][]... partes) {
        List<double[]> r = new ArrayList<>();
        for (double[][] p : partes) {
            r.addAll(List.of(p));
        }
        return r.toArray(double[][]::new);
    }

    static Glifo glifo(char c) {
        return switch (c) {
            case '0' -> new Glifo(0.55, new double[][][] {arco(0.27, 0.5, 0.27, 0.5, 95, 455, 28)});
            case '1' -> new Glifo(0.35, new double[][][] {{{0.05, 0.78}, {0.3, 1.0}, {0.3, 0.0}}});
            case '2' -> new Glifo(0.6, new double[][][] {unir(arco(0.3, 0.7, 0.28, 0.3, 160, -10, 12),
                    new double[][] {{0.5, 0.48}, {0.02, 0.0}, {0.62, 0.02}})});
            case '3' -> new Glifo(0.55, new double[][][] {unir(arco(0.25, 0.75, 0.27, 0.25, 150, -80, 12),
                    arco(0.25, 0.27, 0.3, 0.27, 80, -150, 14))});
            case 'x' -> new Glifo(0.55, new double[][][] {{{0.0, 0.62}, {0.55, 0.0}}, {{0.55, 0.62}, {0.0, 0.0}}});
            case 'd' -> new Glifo(0.6, new double[][][] {arco(0.28, 0.3, 0.27, 0.3, 20, 350, 22),
                    {{0.56, 1.05}, {0.56, 0.0}}});
            case 'C' -> new Glifo(0.65, new double[][][] {arco(0.38, 0.5, 0.36, 0.5, 50, 310, 24)});
            case '+' -> new Glifo(0.55, new double[][][] {{{0.0, 0.38}, {0.55, 0.38}}, {{0.27, 0.1}, {0.27, 0.66}}});
            case '-' -> new Glifo(0.5, new double[][][] {{{0.0, 0.38}, {0.5, 0.38}}});
            case '(' -> new Glifo(0.3, new double[][][] {arco(0.6, 0.45, 0.55, 0.78, 130, 230, 14)});
            case ')' -> new Glifo(0.3, new double[][][] {arco(-0.3, 0.45, 0.55, 0.78, 50, -50, 14)});
            case '∫' -> new Glifo(0.5, new double[][][] {unir(arco(0.55, 1.45, 0.15, 0.15, 30, 180, 8),
                    new double[][] {{0.32, 0.9}, {0.22, -0.2}}, arco(0.0, -0.5, 0.15, 0.15, 0, -150, 8))});
            default -> throw new IllegalArgumentException("Sin glifo: " + c);
        };
    }

    // ------------------------------------------------------------------ los casos de prueba

    /** A. 3x² + 2x − 1 */
    public static List<Trazo> casoA(double escala, long semilla) {
        Lienzo l = new Lienzo(escala, 20, 2.0 * escala, semilla);
        l.texto("3x").exponente("2").espacio(0.15).texto("+").espacio(0.15).texto("2x").espacio(0.15).texto("-")
                .espacio(0.15).texto("1");
        return l.trazos();
    }

    /** B. ∫ 2x dx */
    public static List<Trazo> casoB(double escala, long semilla) {
        Lienzo l = new Lienzo(escala, 20, 2.1 * escala, semilla);
        l.simbolo('∫').espacio(0.1).texto("2x").espacio(0.3).texto("dx");
        return l.trazos();
    }

    /** C. ∫ de 0 a 1 de 2x dx */
    public static List<Trazo> casoC(double escala, long semilla) {
        Lienzo l = new Lienzo(escala, 20, 2.3 * escala, semilla);
        double x0 = l.x();
        l.simbolo('∫');
        double despues = l.x();
        // Los límites a la derecha de los extremos del ∫, como se escriben a mano.
        l.irA(x0 + 0.75 * escala).simbolo('1', 0.5, 1.35);
        l.irA(x0 + 0.3 * escala).simbolo('0', 0.5, -0.85);
        l.irA(despues + 0.35 * escala).texto("2x").espacio(0.3).texto("dx");
        return l.trazos();
    }

    /** D. (x + 1)/(x − 1) como fracción vertical. */
    public static List<Trazo> casoD(double escala, long semilla) {
        double base = 2.3 * escala;
        Lienzo arriba = new Lienzo(escala, 20, base - 0.75 * escala, semilla);
        arriba.texto("(x").espacio(0.1).texto("+").espacio(0.1).texto("1)");
        double ancho = arriba.x();
        Lienzo abajo = new Lienzo(escala, 20, base + 1.55 * escala, semilla + 1);
        abajo.texto("(x").espacio(0.1).texto("-").espacio(0.1).texto("1)");
        double fin = Math.max(ancho, abajo.x());
        List<Trazo> r = new ArrayList<>(arriba.trazos());
        Lienzo raya = new Lienzo(escala, 0, base, semilla + 2);
        raya.raya(12, fin, 0.0);
        r.addAll(raya.trazos());
        r.addAll(abajo.trazos());
        return r;
    }

    /** E. √(x² + 1): la raíz abarca toda la suma. */
    public static List<Trazo> casoE(double escala, long semilla) {
        Lienzo l = new Lienzo(escala, 20, 2.1 * escala, semilla);
        double x0 = l.x();
        l.irA(x0 + 0.85 * escala).texto("x").exponente("2").espacio(0.1).texto("+").espacio(0.1).texto("1");
        double fin = (l.x() - x0) / escala + 0.1;
        Lienzo raiz = new Lienzo(escala, x0, 2.1 * escala, semilla + 7);
        raiz.polilinea(x0, new double[][] {{0.0, 0.45}, {0.15, 0.55}, {0.38, -0.12}, {0.72, 1.12}, {fin, 1.12}});
        List<Trazo> r = new ArrayList<>(raiz.trazos());
        r.addAll(l.trazos());
        return r;
    }
}
