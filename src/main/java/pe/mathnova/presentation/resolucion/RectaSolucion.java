package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

/**
 * El conjunto solución sobre la recta real: las raíces repartidas a distancias iguales con su valor debajo, los
 * tramos incluidos en azul (con flecha si llegan a ±∞), un punto lleno en cada raíz incluida y un círculo abierto
 * en cada raíz excluida. Cambia con {@link #mostrar} sin rehacerse.
 */
public final class RectaSolucion extends Pane {

    private static final PseudoClass INCLUIDA = PseudoClass.getPseudoClass("incluida");
    /** Una raíz excluida que limita un tramo azul: círculo abierto. Las demás excluidas son una marca neutra. */
    private static final PseudoClass EXTREMO = PseudoClass.getPseudoClass("extremo");
    private static final double Y = 18;
    private static final double MARGEN = 10;
    private static final double FLECHA = 7;

    private final AnalisisFactorizacion.Recta recta;
    private final Line eje = new Line();
    private final Polygon flechaEjeIzquierda = new Polygon();
    private final Polygon flechaEjeDerecha = new Polygon();
    private final List<Line> tramos = new ArrayList<>();
    private final Polygon flechaIzquierda = new Polygon();
    private final Polygon flechaDerecha = new Polygon();
    private final List<Circle> puntos = new ArrayList<>();
    private final List<FormulaAjustable> valores = new ArrayList<>();
    private boolean[] tramoIncluido = new boolean[0];
    private boolean[] raizIncluida = new boolean[0];

    public RectaSolucion(AnalisisFactorizacion.Recta recta) {
        this.recta = recta;
        getStyleClass().add("recta-solucion");
        eje.getStyleClass().add("recta-solucion-eje");
        flechaEjeIzquierda.getStyleClass().add("recta-solucion-eje-flecha");
        flechaEjeDerecha.getStyleClass().add("recta-solucion-eje-flecha");
        getChildren().addAll(eje, flechaEjeIzquierda, flechaEjeDerecha);
        for (int i = 0; i <= recta.raices().size(); i++) {
            Line t = new Line();
            t.getStyleClass().add("recta-solucion-tramo");
            tramos.add(t);
        }
        getChildren().addAll(tramos);
        flechaIzquierda.getStyleClass().add("recta-solucion-flecha");
        flechaDerecha.getStyleClass().add("recta-solucion-flecha");
        getChildren().addAll(flechaIzquierda, flechaDerecha);
        for (AnalisisFactorizacion.Raiz r : recta.raices()) {
            Circle c = new Circle(4.5);
            c.getStyleClass().add("recta-solucion-punto");
            puntos.add(c);
            FormulaAjustable v = new FormulaAjustable(13, 10);
            v.setPartible(false);
            v.getStyleClass().add("recta-solucion-valor");
            v.setLatex(r.latex());
            valores.add(v);
        }
        getChildren().addAll(valores);
        getChildren().addAll(puntos);
        setMinWidth(0);
    }

    /** Resalta lo que cumple la condición. */
    public void mostrar(AnalisisFactorizacion.Condicion c) {
        int n = recta.raices().size();
        tramoIncluido = new boolean[n + 1];
        raizIncluida = new boolean[n];
        for (AnalisisFactorizacion.Parte p : AnalisisFactorizacion.solucion(recta, c)) {
            switch (p) {
                case AnalisisFactorizacion.Intervalo i -> {
                    for (int t = i.desde() + 1; t <= i.hasta(); t++) {
                        tramoIncluido[t] = true;
                    }
                    for (int r = i.desde() + 1; r < i.hasta(); r++) {
                        raizIncluida[r] = true;
                    }
                    if (i.cerradoDesde()) {
                        raizIncluida[i.desde()] = true;
                    }
                    if (i.cerradoHasta()) {
                        raizIncluida[i.hasta()] = true;
                    }
                }
                case AnalisisFactorizacion.Puntos ps -> ps.indices().forEach(r -> raizIncluida[r] = true);
            }
        }
        for (int t = 0; t <= n; t++) {
            tramos.get(t).setVisible(tramoIncluido[t]);
        }
        flechaIzquierda.setVisible(tramoIncluido[0]);
        flechaDerecha.setVisible(tramoIncluido[n]);
        for (int r = 0; r < n; r++) {
            boolean extremo = extremoAbierto(r);
            puntos.get(r).pseudoClassStateChanged(INCLUIDA, raizIncluida[r]);
            puntos.get(r).pseudoClassStateChanged(EXTREMO, extremo);
            puntos.get(r).setRadius(raizIncluida[r] || extremo ? 4.5 : 2.5);
        }
        setAccessibleText("Recta real: " + c.texto());
        requestLayout();
    }

    /** Para las pruebas: qué tramos y qué raíces quedaron en azul. */
    boolean tramoIncluido(int t) {
        return tramoIncluido[t];
    }

    boolean raizIncluida(int r) {
        return raizIncluida[r];
    }

    /** La raíz va como círculo abierto: excluida y extremo de un tramo azul. */
    boolean extremoAbierto(int r) {
        return !raizIncluida[r] && (tramoIncluido[r] || tramoIncluido[r + 1]);
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double alto = 0;
        for (FormulaAjustable v : valores) {
            alto = Math.max(alto, v.prefHeight(-1));
        }
        return Y + 10 + Math.max(alto, 16) + 4;
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return computePrefHeight(ancho);
    }

    @Override
    protected double computePrefWidth(double alto) {
        return 320;
    }

    @Override
    protected void layoutChildren() {
        double ancho = getWidth();
        double izq = MARGEN;
        double der = ancho - MARGEN;
        eje.setStartX(izq);
        eje.setEndX(der);
        eje.setStartY(Y);
        eje.setEndY(Y);
        flecha(flechaEjeIzquierda, izq - 2, -1, FLECHA - 2);
        flecha(flechaEjeDerecha, der + 2, 1, FLECHA - 2);
        int n = puntos.size();
        double paso = (der - izq) / (n + 1);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = izq + paso * (i + 1);
            puntos.get(i).setCenterX(x[i]);
            puntos.get(i).setCenterY(Y);
            FormulaAjustable v = valores.get(i);
            double w = Math.min(v.prefWidth(-1), paso - 4);
            double h = v.prefHeight(w);
            v.resizeRelocate(x[i] - w / 2, Y + 9, w, h);
        }
        for (int t = 0; t <= n; t++) {
            Line l = tramos.get(t);
            double a = t == 0 ? izq + FLECHA : x[t - 1];
            double b = t == n ? der - FLECHA : x[t];
            l.setStartX(a);
            l.setEndX(b);
            l.setStartY(Y);
            l.setEndY(Y);
        }
        flecha(flechaIzquierda, izq, -1, FLECHA);
        flecha(flechaDerecha, der, 1, FLECHA);
    }

    /** Una punta de flecha en x que mira a la izquierda (−1) o a la derecha (+1). */
    private static void flecha(Polygon p, double x, int sentido, double largo) {
        p.getPoints().setAll(x, Y, x - sentido * largo, Y - largo * 0.6, x - sentido * largo, Y + largo * 0.6);
    }
}
