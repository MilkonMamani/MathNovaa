package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.grafica.DatosCurvasNivel.Curva;
import pe.mathnova.presentation.grafica.DatosCurvasNivel.Flecha;
import pe.mathnova.presentation.grafica.DatosCurvasNivel.Marca;

/**
 * Curvas de nivel de f(x, y) (Sprint 8): f = c para 9 valores de c repartidos entre el mínimo y el máximo de la
 * ventana, de azul a naranja, con su leyenda; encima, las curvas, los puntos, las flechas y la región que traiga
 * {@link DatosCurvasNivel}. La rueda acerca y aleja alrededor del cursor, arrastrar mueve y el doble clic vuelve a la
 * vista inicial, como en {@link GraficaFuncion}. Las cuentas son con double: solo sirven para dibujar.
 */
public final class CurvasNivel extends Region {

    public static final int NIVELES = 9;

    private static final Color AZUL = Color.web("#2563eb");
    private static final Color NARANJA = Color.web("#ea580c");
    private static final Color MARINO = Color.web("#1e3a6e");
    private static final Color MORADO = Color.web("#7c3aed");
    private static final Color VERDE = Color.web("#16a34a");
    private static final Color ROJO = Color.web("#dc2626");
    private static final Color GRIS = Color.web("#6b7280");
    private static final Color NEGRO = Color.web("#0f172a");
    private static final Color TEXTO_2 = Color.web("#475569");
    private static final Color CUADRICULA = Color.web("#eef2f7");
    private static final Color EJE = Color.web("#64748b");
    private static final Color SOMBRA = Color.web("#2563eb", 0.18);
    private static final Font FUENTE = Font.font("System", 11);
    private static final Font FUENTE_ETIQUETA = Font.font("System", FontWeight.BOLD, 11);

    private final DatosCurvasNivel datos;
    private final Canvas lienzo = new Canvas();
    private VentanaGrafica ventana;
    /** Los rectángulos ya ocupados por la leyenda y los rótulos, para que no se tapen entre sí. */
    private final List<double[]> ocupados = new ArrayList<>();
    private double[] cajaLeyenda;
    private double arrastreX;
    private double arrastreY;

    public CurvasNivel(DatosCurvasNivel datos) {
        this.datos = datos;
        this.ventana = datos.ventana();
        getStyleClass().add("grafica-funcion");
        getChildren().add(lienzo);
        setMinSize(200, 200);
        setPrefSize(480, 340);
        setFocusTraversable(false);
        setAccessibleText(datos.funcion() == null ? "Región de integración" : "Curvas de nivel");
        configurarMouse();
    }

    public VentanaGrafica ventana() {
        return ventana;
    }

    /** Lo que se dibuja: las curvas de nivel, las trayectorias, las marcas. */
    public DatosCurvasNivel datos() {
        return datos;
    }

    /** Vuelve a la vista inicial (también con doble clic). */
    public void restablecer() {
        ventana = datos.ventana();
        dibujar();
    }

    @Override
    protected void layoutChildren() {
        lienzo.setWidth(getWidth());
        lienzo.setHeight(getHeight());
        dibujar();
    }

    /** La ventana con la misma escala en los dos ejes (un círculo se ve redondo). */
    private VentanaGrafica proporcionada() {
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        double porPx = Math.max(ventana.ancho() / w, ventana.alto() / h);
        double cx = (ventana.xMin() + ventana.xMax()) / 2;
        double cy = (ventana.yMin() + ventana.yMax()) / 2;
        return new VentanaGrafica(cx - porPx * w / 2, cx + porPx * w / 2, cy - porPx * h / 2, cy + porPx * h / 2);
    }

    // ------------------------------------------------------------------ coordenadas

    private VentanaGrafica vista;

    private double px(double x) {
        return (x - vista.xMin()) / vista.ancho() * lienzo.getWidth();
    }

    private double py(double y) {
        return (vista.yMax() - y) / vista.alto() * lienzo.getHeight();
    }

    private double planoX(double px) {
        return vista.xMin() + px / lienzo.getWidth() * vista.ancho();
    }

    private double planoY(double py) {
        return vista.yMax() - py / lienzo.getHeight() * vista.alto();
    }

    private void configurarMouse() {
        setOnScroll(e -> {
            if (vista == null) {
                return;
            }
            double factor = e.getDeltaY() > 0 ? 0.85 : 1 / 0.85;
            ventana = ventana.escalar(factor, planoX(e.getX()), planoY(e.getY()));
            dibujar();
            e.consume();
        });
        setOnMousePressed(e -> {
            arrastreX = e.getX();
            arrastreY = e.getY();
        });
        setOnMouseDragged(e -> {
            if (vista == null) {
                return;
            }
            double dx = (arrastreX - e.getX()) / lienzo.getWidth() * vista.ancho();
            double dy = (e.getY() - arrastreY) / lienzo.getHeight() * vista.alto();
            ventana = ventana.mover(dx, dy);
            arrastreX = e.getX();
            arrastreY = e.getY();
            dibujar();
        });
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                restablecer();
            }
        });
    }

    // ------------------------------------------------------------------ dibujo

    private void dibujar() {
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        vista = proporcionada();
        ocupados.clear();
        cajaLeyenda = cajaLeyenda(w, h);
        if (datos.funcion() != null) {
            ocupados.add(cajaLeyenda);
        }
        for (Marca m : datos.marcas()) {
            if (m.rotulo() != null) {
                ocupados.add(new double[] {px(m.x()) + 6, py(m.y()) - 22, m.rotulo().length() * 6.4 + 6, 15});
            }
        }
        GraphicsContext g = lienzo.getGraphicsContext2D();
        g.setImageSmoothing(true);
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        cuadriculaYEjes(g, w, h);
        region(g, w, h);
        double[] niveles = niveles(g);
        for (Curva c : datos.curvas()) {
            curva(g, c);
        }
        for (Flecha f : datos.flechas()) {
            flecha(g, f);
        }
        for (Marca m : datos.marcas()) {
            marca(g, m);
        }
        leyenda(g, w, niveles);
    }

    private void cuadriculaYEjes(GraphicsContext g, double w, double h) {
        double pasoX = VentanaGrafica.paso(vista.ancho(), (int) Math.max(4, w / 70));
        double pasoY = VentanaGrafica.paso(vista.alto(), (int) Math.max(4, h / 50));
        g.setLineWidth(1);
        g.setStroke(CUADRICULA);
        for (double x = Math.ceil(vista.xMin() / pasoX) * pasoX; x <= vista.xMax(); x += pasoX) {
            g.strokeLine(px(x), 0, px(x), h);
        }
        for (double y = Math.ceil(vista.yMin() / pasoY) * pasoY; y <= vista.yMax(); y += pasoY) {
            g.strokeLine(0, py(y), w, py(y));
        }
        double ejeY = Math.max(0, Math.min(h, py(0)));
        double ejeX = Math.max(0, Math.min(w, px(0)));
        g.setStroke(EJE);
        g.setLineWidth(1.3);
        g.strokeLine(0, ejeY, w - 2, ejeY);
        g.strokeLine(ejeX, h, ejeX, 2);
        g.setFont(FUENTE_ETIQUETA);
        g.setFill(TEXTO_2);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(String.valueOf(datos.x()), w - 6, ejeY - 6);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText(String.valueOf(datos.y()), ejeX + 8, 4);
        g.setFont(FUENTE);
        g.setLineWidth(1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        for (double x = Math.ceil(vista.xMin() / pasoX) * pasoX; x <= vista.xMax(); x += pasoX) {
            if (Math.abs(x) >= pasoX / 2) {
                g.strokeLine(px(x), ejeY - 3, px(x), ejeY + 3);
                g.fillText(numero(x, pasoX), px(x), Math.min(h - 14, ejeY + 5));
            }
        }
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.CENTER);
        for (double y = Math.ceil(vista.yMin() / pasoY) * pasoY; y <= vista.yMax(); y += pasoY) {
            if (Math.abs(y) >= pasoY / 2) {
                g.strokeLine(ejeX - 3, py(y), ejeX + 3, py(y));
                g.fillText(numero(y, pasoY), Math.max(28, ejeX - 6), py(y));
            }
        }
    }

    static String numero(double v, double paso) {
        int decimales = (int) Math.max(0, Math.ceil(-Math.log10(paso) + 1e-9));
        String s = String.format(Locale.ROOT, "%." + decimales + "f", v);
        return s.startsWith("-") ? FormateadorTexto.MENOS + s.substring(1) : s;
    }

    /** "0.25", "−1.3": dos cifras decimales a lo más. */
    static String valor(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        if (s.equals("-0")) {
            s = "0";
        }
        return s.startsWith("-") ? FormateadorTexto.MENOS + s.substring(1) : s;
    }

    /** Azul en t = 0, naranja en t = 1. */
    static Color color(double t) {
        return AZUL.interpolate(NARANJA, Math.max(0, Math.min(1, t)));
    }

    /** Las 9 curvas de nivel; devuelve los valores de c (vacío si no hay f). */
    private double[] niveles(GraphicsContext g) {
        if (datos.funcion() == null) {
            return new double[0];
        }
        double[][] m = MarchingSquares.malla(datos.funcion(), datos.x(), datos.y(), vista);
        double[] rango = MarchingSquares.rango(m);
        if (Double.isNaN(rango[0]) || rango[0] == rango[1]) {
            return new double[0];
        }
        double[] cs = MarchingSquares.niveles(rango[0], rango[1], NIVELES);
        g.setLineWidth(1.4);
        for (int k = 0; k < cs.length; k++) {
            g.setStroke(color((double) k / (cs.length - 1)));
            for (double[] s : MarchingSquares.segmentos(m, cs[k], vista)) {
                g.strokeLine(px(s[0]), py(s[1]), px(s[2]), py(s[3]));
            }
        }
        return cs;
    }

    /** La esquina de la leyenda: la que tapa menos puntos marcados (arriba a la derecha si da igual). */
    private double[] cajaLeyenda(double w, double h) {
        double ancho = 92;
        double alto = 22 + NIVELES * 15;
        double[][] esquinas = {{w - ancho - 8, 8}, {44, 8}, {w - ancho - 8, h - alto - 24}, {44, h - alto - 24}};
        double[] mejor = null;
        int menos = Integer.MAX_VALUE;
        for (double[] e : esquinas) {
            int n = 0;
            for (Marca m : datos.marcas()) {
                double sx = px(m.x());
                double sy = py(m.y());
                if (sx > e[0] - 110 && sx < e[0] + ancho + 10 && sy > e[1] - 10 && sy < e[1] + alto + 25) {
                    n++;
                }
            }
            if (n < menos) {
                menos = n;
                mejor = new double[] {e[0], e[1], ancho, alto};
            }
        }
        return mejor;
    }

    private static boolean choca(double[] a, double[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }

    private void leyenda(GraphicsContext g, double w, double[] cs) {
        if (cs.length == 0) {
            return;
        }
        double ancho = cajaLeyenda[2];
        double alto = cajaLeyenda[3];
        double x0 = cajaLeyenda[0];
        double y0 = cajaLeyenda[1];
        g.setFill(Color.web("#ffffff", 0.9));
        g.fillRoundRect(x0, y0, ancho, alto, 8, 8);
        g.setStroke(CUADRICULA);
        g.strokeRoundRect(x0, y0, ancho, alto, 8, 8);
        g.setFont(FUENTE_ETIQUETA);
        g.setFill(NEGRO);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText(datos.titulo() == null ? "f = c" : datos.titulo(), x0 + 8, y0 + 5);
        g.setFont(FUENTE);
        for (int k = cs.length - 1, fila = 0; k >= 0; k--, fila++) {
            double y = y0 + 22 + fila * 15;
            g.setStroke(color((double) k / (cs.length - 1)));
            g.setLineWidth(2.5);
            g.strokeLine(x0 + 8, y + 6, x0 + 26, y + 6);
            g.setFill(TEXTO_2);
            g.fillText("c = " + valor(cs[k]), x0 + 32, y);
        }
    }

    // ------------------------------------------------------------------ región

    private void region(GraphicsContext g, double w, double h) {
        if (datos.region() == null) {
            return;
        }
        g.setFill(SOMBRA);
        double paso = 3;
        for (double sx = 0; sx < w; sx += paso) {
            for (double sy = 0; sy < h; sy += paso) {
                if (dentro(planoX(sx + paso / 2), planoY(sy + paso / 2))) {
                    g.fillRect(sx, sy, paso, paso);
                }
            }
        }
    }

    private boolean dentro(double x, double y) {
        return switch (datos.region()) {
            case DatosCurvasNivel.RegionCartesiana r -> {
                double ext = r.interiorEsY() ? x : y;
                double in = r.interiorEsY() ? y : x;
                if (ext < r.a() || ext > r.b()) {
                    yield false;
                }
                double g1 = CurvaExpresion.evaluar(r.g1(), Map.of(r.exterior(), ext));
                double g2 = CurvaExpresion.evaluar(r.g2(), Map.of(r.exterior(), ext));
                yield in >= Math.min(g1, g2) && in <= Math.max(g1, g2);
            }
            case DatosCurvasNivel.RegionPolar r -> {
                double radio = Math.hypot(x, y);
                double t = Math.atan2(y, x);
                boolean ok = false;
                for (double tt : new double[] {t, t + 2 * Math.PI, t - 2 * Math.PI, t + 4 * Math.PI}) {
                    if (tt >= r.a() - 1e-9 && tt <= r.b() + 1e-9) {
                        double r1 = CurvaExpresion.evaluar(r.r1(), Map.of('θ', tt));
                        double r2 = CurvaExpresion.evaluar(r.r2(), Map.of('θ', tt));
                        ok |= radio >= Math.min(r1, r2) && radio <= Math.max(r1, r2);
                    }
                }
                yield ok;
            }
        };
    }

    // ------------------------------------------------------------------ curvas de encima

    private void estilo(GraphicsContext g, DatosCurvasNivel.Estilo e) {
        g.setLineDashes((double[]) null);
        switch (e) {
            case TRAYECTORIA -> {
                g.setStroke(MORADO);
                g.setLineWidth(2);
                g.setLineDashes(7, 4);
            }
            case RESTRICCION -> {
                g.setStroke(NEGRO);
                g.setLineWidth(2.2);
            }
            case NIVEL_OPTIMO -> {
                g.setStroke(VERDE);
                g.setLineWidth(2);
                g.setLineDashes(4, 3);
            }
            case BORDE -> {
                g.setStroke(MARINO);
                g.setLineWidth(2);
            }
        }
    }

    private void curva(GraphicsContext g, Curva c) {
        estilo(g, c.estilo());
        List<double[]> puntos = new ArrayList<>();
        switch (c.forma()) {
            case IMPLICITA -> {
                double[][] m = MarchingSquares.malla(c.expresion(), datos.x(), datos.y(), vista);
                List<double[]> segs = MarchingSquares.segmentos(m, 0, vista);
                for (double[] s : segs) {
                    g.strokeLine(px(s[0]), py(s[1]), px(s[2]), py(s[3]));
                }
                for (double[] s : segs) {
                    puntos.add(new double[] {s[0], s[1]});
                }
                puntos.sort((a, b) -> Double.compare(a[0], b[0]));
                rotulo(g, c, puntos);
                g.setLineDashes((double[]) null);
                return;
            }
            case RAYO -> {
                puntos.add(new double[] {0, 0});
                puntos.add(new double[] {c.hasta() * Math.cos(c.desde()), c.hasta() * Math.sin(c.desde())});
            }
            default -> {
                double desde = Double.isFinite(c.desde()) ? c.desde()
                        : c.forma() == DatosCurvasNivel.Forma.X_DE_Y ? vista.yMin() : vista.xMin();
                double hasta = Double.isFinite(c.hasta()) ? c.hasta()
                        : c.forma() == DatosCurvasNivel.Forma.X_DE_Y ? vista.yMax() : vista.xMax();
                int n = 400;
                for (int i = 0; i <= n; i++) {
                    double t = desde + (hasta - desde) * i / n;
                    double[] q = switch (c.forma()) {
                        case Y_DE_X -> new double[] {t, CurvaExpresion.evaluar(c.expresion(), Map.of(datos.x(), t))};
                        case X_DE_Y -> new double[] {CurvaExpresion.evaluar(c.expresion(), Map.of(datos.y(), t)), t};
                        default -> {
                            double r = CurvaExpresion.evaluar(c.expresion(), Map.of('θ', t));
                            yield new double[] {r * Math.cos(t), r * Math.sin(t)};
                        }
                    };
                    puntos.add(q);
                }
            }
        }
        double[] antes = null;
        for (double[] q : puntos) {
            if (antes != null && Double.isFinite(antes[0] + antes[1]) && Double.isFinite(q[0] + q[1])
                    && Math.abs(py(q[1]) - py(antes[1])) < lienzo.getHeight()) {
                g.strokeLine(px(antes[0]), py(antes[1]), px(q[0]), py(q[1]));
            }
            antes = q;
        }
        g.setLineDashes((double[]) null);
        rotulo(g, c, puntos);
    }

    /** El rótulo junto al último punto visible de la curva. */
    private void rotulo(GraphicsContext g, Curva c, List<double[]> puntos) {
        if (c.rotulo() == null) {
            return;
        }
        List<double[]> visibles = new ArrayList<>();
        for (double[] q : puntos) {
            if (Double.isFinite(q[0] + q[1]) && vista.contiene(q[0], q[1])) {
                visibles.add(q);
            }
        }
        if (visibles.isEmpty()) {
            return;
        }
        double ancho = c.rotulo().length() * 6.4 + 6;
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        // del final hacia atrás, el primer lugar donde el rótulo no tapa la leyenda ni otro rótulo
        int salto = Math.max(1, visibles.size() / 16);
        double sx = 0;
        double sy = 0;
        double[] caja = null;
        boolean derecha = false;
        for (int i = visibles.size() - 1; i >= 0; i -= salto) {
            double[] q = visibles.get(i);
            double qx = Math.min(w - 8, Math.max(8, px(q[0])));
            double qy = Math.min(h - 8, Math.max(18, py(q[1])));
            boolean d = qx > w * 0.6;
            double[] r = {d ? qx - 6 - ancho + 3 : qx + 3, qy - 15, ancho, 15};
            if (r[0] < 0 || r[0] + r[2] > w) {
                continue;
            }
            boolean libre = ocupados.stream().noneMatch(o -> choca(o, r));
            if (libre || caja == null) {
                sx = qx;
                sy = qy;
                caja = r;
                derecha = d;
                if (libre) {
                    break;
                }
            }
        }
        if (caja == null) {
            return;
        }
        ocupados.add(caja);
        g.setFont(FUENTE_ETIQUETA);
        g.setTextBaseline(VPos.BOTTOM);
        g.setTextAlign(derecha ? TextAlignment.RIGHT : TextAlignment.LEFT);
        double tx = derecha ? sx - 6 : sx + 6;
        g.setFill(Color.web("#ffffff", 0.85));
        g.fillRect(caja[0], caja[1], caja[2], caja[3]);
        g.setFill(c.estilo() == DatosCurvasNivel.Estilo.TRAYECTORIA ? MORADO
                : c.estilo() == DatosCurvasNivel.Estilo.NIVEL_OPTIMO ? VERDE
                : c.estilo() == DatosCurvasNivel.Estilo.BORDE ? MARINO : NEGRO);
        g.fillText(c.rotulo(), tx, sy - 2);
    }

    private void flecha(GraphicsContext g, Flecha f) {
        double x1 = px(f.x());
        double y1 = py(f.y());
        double x2 = px(f.x() + f.dx());
        double y2 = py(f.y() + f.dy());
        g.setStroke(ROJO);
        g.setFill(ROJO);
        g.setLineWidth(2.4);
        g.strokeLine(x1, y1, x2, y2);
        double ang = Math.atan2(y2 - y1, x2 - x1);
        double l = 10;
        g.fillPolygon(new double[] {x2, x2 - l * Math.cos(ang - 0.4), x2 - l * Math.cos(ang + 0.4)},
                new double[] {y2, y2 - l * Math.sin(ang - 0.4), y2 - l * Math.sin(ang + 0.4)}, 3);
        if (f.rotulo() != null) {
            g.setFont(FUENTE_ETIQUETA);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.BOTTOM);
            g.fillText(f.rotulo(), x2 + 6, y2 - 4);
        }
    }

    private void marca(GraphicsContext g, Marca m) {
        Color c = switch (m.tipo()) {
            case MINIMO -> VERDE;
            case MAXIMO -> ROJO;
            case SILLA -> GRIS;
            case PUNTO -> AZUL;
        };
        double sx = px(m.x());
        double sy = py(m.y());
        g.setFill(Color.WHITE);
        g.fillOval(sx - 7, sy - 7, 14, 14);
        g.setFill(c);
        g.fillOval(sx - 5.5, sy - 5.5, 11, 11);
        if (m.rotulo() != null) {
            g.setFont(FUENTE_ETIQUETA);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.BOTTOM);
            g.setFill(Color.web("#ffffff", 0.85));
            g.fillRect(sx + 6, sy - 22, m.rotulo().length() * 6.4 + 6, 15);
            g.setFill(c.darker());
            g.fillText(m.rotulo(), sx + 9, sy - 8);
        }
    }
}
