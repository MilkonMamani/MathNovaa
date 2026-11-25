package pe.mathnova.presentation.grafica;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Punto;
import pe.mathnova.domain.analisis.AnalisisPolinomio.Raiz;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.resolucion.VistaPropiedades;

public final class GraficaFuncion extends Region {

    private static final Color AZUL = Color.web("#2563eb");
    private static final Color MARINO = Color.web("#1e3a6e");
    private static final Color ROJO = Color.web("#dc2626");
    private static final Color NARANJA = Color.web("#ea580c");
    private static final Color TEXTO = Color.web("#0f172a");
    private static final Color TEXTO_2 = Color.web("#475569");
    private static final Color CUADRICULA = Color.web("#eef2f7");
    private static final Color EJE = Color.web("#64748b");
    private static final Font FUENTE = Font.font("System", 11);
    private static final Font FUENTE_ETIQUETA = Font.font("System", FontWeight.BOLD, 11);
    private static final double PX_POR_MUESTRA = 1.5;
    private static final double CERCANIA = 24;

    private final DatosGrafica datos;
    private final FuncionReal funcion;
    private final VentanaGrafica inicial;
    private final Canvas lienzo = new Canvas();
    private final Canvas consulta = new Canvas();
    private final HBox controles = new HBox(3);
    private final Image leyenda;
    private final DoubleProperty progreso = new SimpleDoubleProperty(1);

    private VentanaGrafica ventana;
    private double toqueX;
    private double toqueY;
    private boolean toqueDeslizado;
    private double cursorX = Double.NaN;
    private double cursorY = Double.NaN;
    private String nombreCursor = "f";

    public GraficaFuncion(DatosGrafica datos, RenderizadorLatex latex) {
        this.datos = datos;
        this.funcion = datos.funcion();
        this.inicial = VentanaGrafica.inicial(datos);
        this.ventana = inicial;
        this.leyenda = leyenda(datos, latex);
        getStyleClass().add("grafica-funcion");
        consulta.setMouseTransparent(true);
        controles.getChildren().addAll(control("+", "Acercar", 0.85),
                control("−", "Alejar", 1 / 0.85));
        Button restablecer = new Button("Restablecer vista");
        restablecer.getStyleClass().add("grafica-control");
        restablecer.setFocusTraversable(false);
        restablecer.setOnAction(e -> restablecer());
        controles.getChildren().add(restablecer);
        getChildren().addAll(lienzo, consulta, controles);
        setMinSize(200, 200);
        setPrefSize(480, 340);
        setFocusTraversable(false);
        setAccessibleText("Gráfica de " + datos.leyendaLatex());
        progreso.addListener((obs, antes, ahora) -> dibujar());
        configurarConsulta();
    }

    private Button control(String texto, String ayuda, double factor) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("grafica-control");
        boton.setTooltip(new Tooltip(ayuda));
        boton.setFocusTraversable(false);
        boton.setOnAction(e -> {
            ventana = ventana.escalar(factor, (ventana.xMin() + ventana.xMax()) / 2,
                    (ventana.yMin() + ventana.yMax()) / 2);
            ocultarConsulta();
            dibujar();
        });
        return boton;
    }

    public DoubleProperty progresoProperty() {
        return progreso;
    }

    public VentanaGrafica ventana() {
        return ventana;
    }

    public void restablecer() {
        ventana = inicial;
        ocultarConsulta();
        dibujar();
    }

    private static Image leyenda(DatosGrafica d, RenderizadorLatex latex) {
        try {
            return SwingFXUtils.toFXImage(latex.dibujar(d.leyendaLatex()), null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected void layoutChildren() {
        lienzo.setWidth(getWidth());
        lienzo.setHeight(getHeight());
        consulta.setWidth(getWidth());
        consulta.setHeight(getHeight());
        controles.resizeRelocate(Math.max(4, getWidth() - controles.prefWidth(-1) - 8), 8,
                controles.prefWidth(-1), controles.prefHeight(-1));
        dibujar();
    }

    private double px(double x) {
        return (x - ventana.xMin()) / ventana.ancho() * lienzo.getWidth();
    }

    private double py(double y) {
        return (ventana.yMax() - y) / ventana.alto() * lienzo.getHeight();
    }

    private double planoX(double px) {
        return ventana.xMin() + px / lienzo.getWidth() * ventana.ancho();
    }

    private double planoY(double py) {
        return ventana.yMax() - py / lienzo.getHeight() * ventana.alto();
    }

    private void configurarConsulta() {
        setOnMouseMoved(e -> {
            if (!e.isSynthesized()) actualizarCursor(e.getX(), e.getY());
        });
        setOnMouseExited(e -> {
            if (!e.isSynthesized()) ocultarConsulta();
        });
        setOnTouchPressed(e -> {
            toqueX = e.getTouchPoint().getX();
            toqueY = e.getTouchPoint().getY();
            toqueDeslizado = false;
        });
        setOnTouchMoved(e -> {
            if (Math.hypot(e.getTouchPoint().getX() - toqueX,
                    e.getTouchPoint().getY() - toqueY) > 10) {
                toqueDeslizado = true;
                ocultarConsulta();
            }
        });
        setOnTouchReleased(e -> {
            if (!toqueDeslizado) actualizarCursor(e.getTouchPoint().getX(), e.getTouchPoint().getY());
        });
    }

    private void ocultarConsulta() {
        cursorX = Double.NaN;
        dibujarConsulta();
    }

    private void actualizarCursor(double mx, double my) {
        cursorX = Double.NaN;
        double mejor = CERCANIA;
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        FuncionReal segunda = calculo == null ? null : calculo.segunda();
        for (double a : discontinuidades()) {
            if (Math.abs(px(a) - mx) <= 2) {
                dibujarConsulta();
                return;
            }
        }
        FuncionReal[] funciones = segunda == null ? new FuncionReal[] {funcion}
                : new FuncionReal[] {funcion, segunda};
        for (int curva = 0; curva < funciones.length; curva++) {
            for (double sx = Math.max(0, mx - CERCANIA); sx <= Math.min(lienzo.getWidth(), mx + CERCANIA); sx += 1) {
                double x = planoX(sx);
                double y = funciones[curva].valor(x);
                if (!Double.isFinite(y) || !ventana.contiene(x, y)) continue;
                double d = Math.hypot(sx - mx, py(y) - my);
                if (d < mejor) {
                    mejor = d;
                    cursorX = x;
                    cursorY = y;
                    nombreCursor = curva == 0 ? (segunda == null ? "f" : calculo.nombre())
                            : calculo.nombreSegunda();
                }
            }
        }
        if (segunda == null) {
            for (Raiz r : datos.raices()) {
                double x = FuncionReal.aDouble(r.valor());
                double d = Math.hypot(px(x) - mx, py(0) - my);
                if (d < mejor && ventana.contiene(x, 0)) {
                    mejor = d;
                    cursorX = x;
                    cursorY = 0;
                }
            }
            if (datos.corteY().isPresent()) {
                double y = FuncionReal.aDouble(datos.corteY().get());
                double d = Math.hypot(px(0) - mx, py(y) - my);
                if (d < mejor && ventana.contiene(0, y)) {
                    cursorX = 0;
                    cursorY = y;
                }
            }
        }
        dibujarConsulta();
    }

    private List<Double> discontinuidades() {
        List<Double> xs = new ArrayList<>();
        datos.asintotas().forEach(a -> xs.add(FuncionReal.aDouble(a)));
        xs.addAll(datos.extra().verticales());
        xs.addAll(periodicas());
        return xs;
    }

    private void dibujar() {
        double w = lienzo.getWidth();
        double h = lienzo.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        GraphicsContext g = lienzo.getGraphicsContext2D();
        g.setImageSmoothing(true);
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        cuadriculaYEjes(g, w, h);
        asintotas(g, h);
        verticeYEje(g, h);
        area(g);
        curva(g, w, funcion, AZUL);
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        if (calculo != null && calculo.segunda() != null) {
            curva(g, w, calculo.segunda(), NARANJA);
        }
        limite(g, w, h);
        puntos(g);
        leyendaArriba(g, w);
        leyendaCurvas(g);
        dibujarConsulta();
    }

    private void cuadriculaYEjes(GraphicsContext g, double w, double h) {
        double pasoX = VentanaGrafica.paso(ventana.ancho(), (int) Math.max(4, w / 70));
        double pasoY = VentanaGrafica.paso(ventana.alto(), (int) Math.max(4, h / 50));
        g.setLineWidth(1);
        g.setStroke(CUADRICULA);
        for (double x = Math.ceil(ventana.xMin() / pasoX) * pasoX; x <= ventana.xMax(); x += pasoX) {
            g.strokeLine(px(x), 0, px(x), h);
        }
        for (double y = Math.ceil(ventana.yMin() / pasoY) * pasoY; y <= ventana.yMax(); y += pasoY) {
            g.strokeLine(0, py(y), w, py(y));
        }
        double ejeY = Math.max(0, Math.min(h, py(0)));
        double ejeX = Math.max(0, Math.min(w, px(0)));
        g.setStroke(EJE);
        g.setLineWidth(1.3);
        g.strokeLine(0, ejeY, w - 2, ejeY);
        g.strokeLine(ejeX, h, ejeX, 2);
        g.setFill(EJE);
        g.fillPolygon(new double[] {w - 2, w - 10, w - 10}, new double[] {ejeY, ejeY - 4, ejeY + 4}, 3);
        g.fillPolygon(new double[] {ejeX, ejeX - 4, ejeX + 4}, new double[] {2, 10, 10}, 3);
        g.setFont(FUENTE_ETIQUETA);
        g.setFill(TEXTO_2);
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.BOTTOM);
        g.fillText(String.valueOf(datos.variable()), w - 6, ejeY - 6);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.TOP);
        g.fillText("y", ejeX + 8, 4);

        g.setFont(FUENTE);
        g.setStroke(EJE);
        g.setLineWidth(1);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        for (double x = Math.ceil(ventana.xMin() / pasoX) * pasoX; x <= ventana.xMax(); x += pasoX) {
            if (Math.abs(x) < pasoX / 2) {
                continue;
            }
            g.strokeLine(px(x), ejeY - 3, px(x), ejeY + 3);
            g.fillText(numero(x, pasoX), px(x), Math.min(h - 14, ejeY + 5));
        }
        g.setTextAlign(TextAlignment.RIGHT);
        g.setTextBaseline(VPos.CENTER);
        for (double y = Math.ceil(ventana.yMin() / pasoY) * pasoY; y <= ventana.yMax(); y += pasoY) {
            if (Math.abs(y) < pasoY / 2) {
                continue;
            }
            g.strokeLine(ejeX - 3, py(y), ejeX + 3, py(y));
            g.fillText(numero(y, pasoY), Math.max(28, ejeX - 6), py(y));
        }
    }

    private static String numero(double v, double paso) {
        int decimales = (int) Math.max(0, Math.ceil(-Math.log10(paso) + 1e-9));
        String s = String.format(Locale.ROOT, "%." + decimales + "f", v);
        return s.startsWith("-") ? FormateadorTexto.MENOS + s.substring(1) : s;
    }

    private void asintotas(GraphicsContext g, double h) {
        g.setStroke(ROJO);
        g.setLineWidth(1.4);
        g.setLineDashes(6, 5);
        for (Racional a : datos.asintotas()) {
            double x = px(FuncionReal.aDouble(a));
            g.strokeLine(x, 0, x, h);
        }
        for (double a : periodicas()) {
            double x = px(a);
            g.strokeLine(x, 0, x, h);
        }
        for (double a : datos.extra().verticales()) {
            double x = px(a);
            g.strokeLine(x, 0, x, h);
        }
        double w = lienzo.getWidth();
        if (datos.extra().horizontal() != null) {
            double y = py(datos.extra().horizontal());
            g.strokeLine(0, y, w, y);
        }
        DatosGrafica.Recta r = datos.extra().oblicua();
        if (r != null) {
            g.strokeLine(0, py(r.en(ventana.xMin())), w, py(r.en(ventana.xMax())));
        }
        g.setLineDashes((double[]) null);
    }

    private void limite(GraphicsContext g, double w, double h) {
        DatosGrafica.MarcaLimite m = datos.extra().limite();
        if (m == null) {
            return;
        }
        g.setStroke(TEXTO_2);
        g.setLineWidth(1.2);
        g.setLineDashes(5, 5);
        if (m.a() != null) {
            g.strokeLine(px(m.a()), 0, px(m.a()), h);
        } else if (m.valor() != null) {
            g.setStroke(MARINO);
            g.strokeLine(0, py(m.valor()), w, py(m.valor()));
        }
        g.setLineDashes((double[]) null);
        if (progreso.get() < 1) {
            return;
        }
        double paso = ventana.ancho() * 0.12;
        if (m.a() != null) {
            if (m.izquierda()) {
                flecha(g, m.a() - paso, m.a() - paso * 0.75);
            }
            if (m.derecha()) {
                flecha(g, m.a() + paso, m.a() + paso * 0.75);
            }
            if (m.valor() != null && ventana.contiene(m.a(), m.valor())) {
                double x = px(m.a());
                double y = py(m.valor());
                if (m.hueco()) {
                    g.setFill(Color.WHITE);
                    g.fillOval(x - 5, y - 5, 10, 10);
                    g.setStroke(AZUL);
                    g.setLineWidth(2);
                    g.strokeOval(x - 5, y - 5, 10, 10);
                } else {
                    g.setFill(MARINO);
                    g.fillOval(x - 4, y - 4, 8, 8);
                }
            }
        } else {
            double desde = m.haciaMas() ? ventana.xMax() - paso * 1.2 : ventana.xMin() + paso * 1.2;
            flecha(g, desde, desde + (m.haciaMas() ? paso * 0.25 : -paso * 0.25));
        }
    }

    private void flecha(GraphicsContext g, double x1, double x2) {
        double y1 = funcion.valor(x1);
        double y2 = funcion.valor(x2);
        if (!Double.isFinite(y1) || !Double.isFinite(y2)) {
            return;
        }
        double sx1 = px(x1);
        double sy1 = py(y1);
        double sx2 = px(x2);
        double sy2 = py(y2);
        double ang = Math.atan2(sy2 - sy1, sx2 - sx1);
        double largo = 9;
        double[] xs = {sx2, sx2 - largo * Math.cos(ang - 0.45), sx2 - largo * Math.cos(ang + 0.45)};
        double[] ys = {sy2, sy2 - largo * Math.sin(ang - 0.45), sy2 - largo * Math.sin(ang + 0.45)};
        g.setFill(ROJO);
        g.fillPolygon(xs, ys, 3);
    }

    private void verticeYEje(GraphicsContext g, double h) {
        datos.vertice().ifPresent(v -> {
            double x = px(FuncionReal.aDouble(v.x()));
            g.setStroke(TEXTO_2);
            g.setLineWidth(1);
            g.setLineDashes(4, 4);
            g.strokeLine(x, 0, x, h);
            g.setLineDashes((double[]) null);
        });
    }

    private void curva(GraphicsContext g, double w, FuncionReal funcion, Color color) {
        double hasta = w * Math.max(0, Math.min(1, progreso.get()));
        if (hasta <= 0) {
            return;
        }
        List<Double> asintotas = new ArrayList<>();
        datos.asintotas().forEach(a -> asintotas.add(FuncionReal.aDouble(a)));
        asintotas.addAll(periodicas());
        asintotas.addAll(datos.extra().verticales());
        List<List<double[]>> trazos = TrazadoCurva.trazos(funcion, ventana.xMin(), planoX(hasta),
                (int) Math.max(2, hasta / PX_POR_MUESTRA), asintotas,
                ventana.yMin() - ventana.alto() * 4, ventana.yMax() + ventana.alto() * 4);
        g.setStroke(color);
        g.setLineWidth(2);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (List<double[]> trazo : trazos) {
            g.beginPath();
            g.moveTo(px(trazo.getFirst()[0]), py(trazo.getFirst()[1]));
            for (double[] punto : trazo.subList(1, trazo.size())) {
                g.lineTo(px(punto[0]), py(punto[1]));
            }
            g.stroke();
        }
    }

    private void area(GraphicsContext g) {
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        if (calculo == null || calculo.area() == null) {
            return;
        }
        double a = Math.min(calculo.area().a(), calculo.area().b());
        double b = Math.max(calculo.area().a(), calculo.area().b());
        double desde = Math.max(px(a), 0);
        double hasta = Math.min(px(b), lienzo.getWidth() * Math.max(0, Math.min(1, progreso.get())));
        double cero = py(0);
        for (double sx = desde; sx <= hasta; sx += 1) {
            double y = funcion.valor(planoX(sx));
            if (!Double.isFinite(y)) {
                continue;
            }
            double sy = Math.max(-10, Math.min(lienzo.getHeight() + 10, py(y)));
            g.setFill(y >= 0 ? Color.color(0.15, 0.39, 0.92, 0.22) : Color.color(0.86, 0.15, 0.15, 0.22));
            g.fillRect(sx, Math.min(cero, sy), 1.2, Math.abs(sy - cero));
        }
        if (progreso.get() >= 1) {
            double medio = (a + b) / 2;
            double y = funcion.valor(medio);
            double sy = Double.isFinite(y) ? (py(y / 2) + cero) / 2 : cero - 20;
            etiqueta(g, px(medio) - 20, sy + 11, calculo.area().valor());
        }
        g.setStroke(EJE);
        g.setLineWidth(1);
        g.setLineDashes(4, 4);
        for (double v : new double[] {a, b}) {
            g.strokeLine(px(v), 0, px(v), lienzo.getHeight());
        }
        g.setLineDashes(null);
    }

    private void leyendaCurvas(GraphicsContext g) {
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        if (calculo == null || calculo.segunda() == null) {
            return;
        }
        g.setFont(FUENTE_ETIQUETA);
        double ancho = Math.max(calculo.nombre().length(), calculo.nombreSegunda().length()) * 6.6 + 40;
        g.setFill(Color.color(1, 1, 1, 0.9));
        g.fillRoundRect(8, 8, ancho, 44, 8, 8);
        g.setStroke(Color.web("#e2e8f0"));
        g.setLineWidth(1);
        g.strokeRoundRect(8, 8, ancho, 44, 8, 8);
        String[] nombres = {calculo.nombre(), calculo.nombreSegunda()};
        Color[] colores = {AZUL, NARANJA};
        for (int i = 0; i < 2; i++) {
            double y = 20 + i * 20;
            g.setStroke(colores[i]);
            g.setLineWidth(2.5);
            g.strokeLine(16, y, 34, y);
            g.setFill(TEXTO);
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(nombres[i], 40, y);
        }
    }

    private List<Double> periodicas() {
        List<Double> r = new ArrayList<>();
        for (DatosGrafica.Periodica p : datos.extra().periodicas()) {
            r.addAll(p.entre(ventana.xMin(), ventana.xMax()));
        }
        return r;
    }

    private void puntos(GraphicsContext g) {
        g.setFont(FUENTE_ETIQUETA);
        for (DatosGrafica.Marca m : datos.extra().marcas()) {
            punto(g, m.x(), 0, MARINO, m.etiqueta(), true);
        }
        for (Raiz r : datos.raices()) {
            double x = FuncionReal.aDouble(r.valor());
            String etiqueta = "(" + FormateadorTexto.numero(r.valor()) + ", 0)"
                    + (r.multiplicidad() % 2 == 0 ? " raíz " + VistaPropiedades.multiplicidad(r.multiplicidad()) : "");
            punto(g, x, 0, MARINO, etiqueta, true);
        }
        datos.corteY().ifPresent(c -> {
            boolean esRaiz = c.esCero() && datos.raices().stream().anyMatch(r -> r.valor().esCero());
            if (!esRaiz) {
                punto(g, 0, FuncionReal.aDouble(c), MARINO, "(0, " + FormateadorTexto.numero(c) + ")", true);
            }
        });
        datos.vertice().ifPresent(v -> punto(g, FuncionReal.aDouble(v.x()), FuncionReal.aDouble(v.y()), AZUL,
                "V" + VistaPropiedades.punto(v), true));
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        if (calculo != null) {
            for (DatosGrafica.PuntoMarcado p : calculo.puntos()) {
                punto(g, p.x(), p.y(), ROJO, p.etiqueta(), true);
            }
        }
        for (Punto hueco : datos.huecos()) {
            double x = px(FuncionReal.aDouble(hueco.x()));
            double y = py(FuncionReal.aDouble(hueco.y()));
            g.setFill(Color.WHITE);
            g.fillOval(x - 5, y - 5, 10, 10);
            g.setStroke(AZUL);
            g.setLineWidth(2);
            g.strokeOval(x - 5, y - 5, 10, 10);
            etiqueta(g, x, y, "hueco " + VistaPropiedades.punto(hueco));
        }
    }

    private void punto(GraphicsContext g, double x, double y, Color color, String texto, boolean conEtiqueta) {
        if (!ventana.contiene(x, y)) {
            return;
        }
        double sx = px(x);
        double sy = py(y);
        g.setFill(color);
        g.fillOval(sx - 4.5, sy - 4.5, 9, 9);
        g.setStroke(Color.WHITE);
        g.setLineWidth(1.5);
        g.strokeOval(sx - 4.5, sy - 4.5, 9, 9);
        if (conEtiqueta) {
            etiqueta(g, sx, sy, texto);
        }
    }

    private void etiqueta(GraphicsContext g, double sx, double sy, String texto) {
        g.setFont(FUENTE_ETIQUETA);
        double ancho = texto.length() * 6.4 + 8;
        double x = Math.max(2, Math.min(sx + 7, lienzo.getWidth() - ancho - 2));
        double y = Math.max(2, Math.min(sy - 22, lienzo.getHeight() - 18));
        g.setFill(Color.color(1, 1, 1, 0.85));
        g.fillRoundRect(x, y, ancho, 16, 6, 6);
        g.setFill(TEXTO);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(texto, x + 4, y + 8);
    }

    private void leyendaArriba(GraphicsContext g, double w) {
        if (leyenda == null) {
            return;
        }
        double lw = Math.min(leyenda.getWidth(), w * 0.6);
        double lh = leyenda.getHeight() * lw / leyenda.getWidth();
        double x = w - lw - 12;
        g.setFill(Color.color(1, 1, 1, 0.9));
        g.fillRoundRect(x - 6, 39, lw + 12, lh + 8, 8, 8);
        g.setStroke(Color.web("#e2e8f0"));
        g.setLineWidth(1);
        g.strokeRoundRect(x - 6, 39, lw + 12, lh + 8, 8, 8);
        g.drawImage(leyenda, x, 43, lw, lh);
    }

    private void dibujarConsulta() {
        GraphicsContext g = consulta.getGraphicsContext2D();
        g.clearRect(0, 0, consulta.getWidth(), consulta.getHeight());
        cursor(g);
    }

    private void etiquetaConsulta(GraphicsContext g, double sx, double sy, String texto) {
        double ancho = Math.min(lienzo.getWidth() - 8, texto.length() * 6.5 + 18);
        double x = sx + 12;
        if (x + ancho > lienzo.getWidth() - 4) x = sx - ancho - 12;
        x = Math.max(4, Math.min(x, lienzo.getWidth() - ancho - 4));
        double y = sy - 42;
        if (y < 4) y = sy + 16;
        y = Math.max(4, Math.min(y, lienzo.getHeight() - 25));
        g.setFill(Color.color(1, 1, 1, 0.97));
        g.fillRoundRect(x, y, ancho, 23, 6, 6);
        g.setStroke(Color.web("#93c5fd"));
        g.setLineWidth(1);
        g.strokeRoundRect(x, y, ancho, 23, 6, 6);
        g.setFill(TEXTO);
        g.setFont(FUENTE_ETIQUETA);
        g.setTextAlign(TextAlignment.LEFT);
        g.setTextBaseline(VPos.CENTER);
        g.fillText(texto, x + 9, y + 11.5, ancho - 12);
    }

    private void cursor(GraphicsContext g) {
        if (Double.isNaN(cursorX) || progreso.get() < 1) {
            return;
        }
        double sx = px(cursorX);
        double sy = py(cursorY);
        DatosGrafica.Calculo calculo = datos.extra().calculo();
        String pendiente = null;
        if (calculo != null && calculo.pendiente() != null) {
            double m = calculo.pendiente().valor(cursorX);
            if (Double.isFinite(m)) {
                g.setStroke(NARANJA);
                g.setLineWidth(1.5);
                g.setLineDashes(6, 4);
                double x1 = ventana.xMin();
                double x2 = ventana.xMax();
                g.strokeLine(px(x1), py(cursorY + m * (x1 - cursorX)), px(x2), py(cursorY + m * (x2 - cursorX)));
                g.setLineDashes(null);
                pendiente = "pendiente " + aproximado(m);
            }
        }
        g.setStroke(AZUL);
        g.setLineWidth(1);
        g.strokeOval(sx - 6, sy - 6, 12, 12);
        String nombre = datos.extra().calculo() == null || datos.extra().calculo().segunda() == null
                ? "f(" + datos.variable() + ")" : nombreCursor;
        String texto = datos.variable() + " = " + aproximado(cursorX) + " · " + nombre + " = " + aproximado(cursorY);
        etiquetaConsulta(g, sx, sy, pendiente == null ? texto : texto + " · " + pendiente);
    }

    private static String aproximado(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.equals("-0.00")) {
            s = "0.00";
        }
        if (s.endsWith(".00")) s = s.substring(0, s.length() - 3);
        else if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s.startsWith("-") ? FormateadorTexto.MENOS + s.substring(1) : s;
    }
}
