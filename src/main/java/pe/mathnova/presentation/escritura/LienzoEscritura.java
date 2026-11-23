package pe.mathnova.presentation.escritura;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;

/**
 * El lienzo de la pestaña "Escribir", con la misma cuadrícula de la Pizarra. Cada trazo se guarda
 * como una lista de puntos (x, y, t), no como imagen: el dibujo se rehace con ellos.
 *
 * <ul>
 *   <li>Lápiz: trazo de 2,5 px en #0F172A con puntas redondeadas.</li>
 *   <li>Borrador: quita los puntos que toca, con un ancho de 14 px; un trazo cortado al medio queda
 *       en dos.</li>
 *   <li>Deshacer quita lo último que se hizo (un trazo o una pasada del borrador).</li>
 * </ul>
 * Acepta mouse, lápiz y pantalla táctil. JavaFX no dice si un evento viene del lápiz o del dedo: los
 * toques que llegan mientras hay un trazo con mouse o lápiz se ignoran (la mano apoyada).
 */
public final class LienzoEscritura extends Region {

    public enum Herramienta { LAPIZ, BORRADOR }

    static final double ALTO = 220;
    static final double GROSOR = 2.5;
    static final double ANCHO_BORRADOR = 14;
    private static final double CUADRO = 24;
    private static final Color TINTA = Color.web("#0F172A");
    private static final Color CUADRICULA = Color.web("#EEF0F3");

    private final Canvas canvas = new Canvas();
    private final VBox vacio;

    private List<Trazo> trazos = List.of();
    private final Deque<List<Trazo>> historial = new ArrayDeque<>();
    private final List<Punto> actual = new ArrayList<>();
    /** Un trazo (o una pasada del borrador) en curso, y con qué. */
    private Fuente fuente;
    private int toque = -1;
    private boolean borroAlgo;
    private Punto ultimoBorrador;
    private Herramienta herramienta = Herramienta.LAPIZ;
    private final ReadOnlyBooleanWrapper estaVacio = new ReadOnlyBooleanWrapper(true);

    private Runnable onEmpezar = () -> { };
    private Runnable onTerminar = () -> { };

    private enum Fuente { MOUSE, TOQUE }

    public LienzoEscritura() {
        getStyleClass().add("lienzo-escritura");
        canvas.getStyleClass().add("lienzo-escritura-canvas");
        setPrefHeight(ALTO);
        setMinHeight(ALTO);
        setMaxHeight(ALTO);
        setAccessibleText("Lienzo para escribir el ejercicio a mano");

        SVGPath lapiz = new SVGPath();
        lapiz.setContent("M3 17 L4 13 L14 3 L17 6 L7 16 Z M12 5 L15 8");
        lapiz.getStyleClass().add("lienzo-vacio-icono");
        Label titulo = new Label("Escribe aquí tu ejercicio");
        titulo.getStyleClass().add("lienzo-vacio-titulo");
        Label ayuda = new Label("Una sola línea, letra grande. Para borrar, tacha encima.");
        ayuda.getStyleClass().add("lienzo-vacio-ayuda");
        vacio = new VBox(6, lapiz, titulo, ayuda);
        vacio.setAlignment(Pos.CENTER);
        vacio.setMouseTransparent(true);
        vacio.getStyleClass().add("lienzo-vacio");

        getChildren().addAll(canvas, vacio);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (!e.isSynthesized() && fuente == null) {
                empezar(Fuente.MOUSE, punto(e.getX(), e.getY()));
            }
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isSynthesized() && fuente == Fuente.MOUSE) {
                seguir(punto(e.getX(), e.getY()));
            }
        });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!e.isSynthesized() && fuente == Fuente.MOUSE) {
                terminar();
            }
        });
        canvas.addEventHandler(TouchEvent.TOUCH_PRESSED, e -> {
            if (fuente == null) {
                TouchPoint t = e.getTouchPoint();
                toque = t.getId();
                empezar(Fuente.TOQUE, punto(t.getX(), t.getY()));
            }
            e.consume();
        });
        canvas.addEventHandler(TouchEvent.TOUCH_MOVED, e -> {
            TouchPoint t = e.getTouchPoint();
            if (fuente == Fuente.TOQUE && t.getId() == toque) {
                seguir(punto(t.getX(), t.getY()));
            }
            e.consume();
        });
        canvas.addEventHandler(TouchEvent.TOUCH_RELEASED, e -> {
            if (fuente == Fuente.TOQUE && e.getTouchPoint().getId() == toque) {
                terminar();
            }
            e.consume();
        });
    }

    // ------------------------------------------------------------------ API

    public void setHerramienta(Herramienta h) {
        herramienta = h;
    }

    public Herramienta herramienta() {
        return herramienta;
    }

    /** Al apoyar el lápiz (o el dedo, o el borrador). */
    public void setOnEmpezar(Runnable r) {
        onEmpezar = r;
    }

    /** Al levantarlo. */
    public void setOnTerminar(Runnable r) {
        onTerminar = r;
    }

    /** Los trazos que hay ahora, en el orden en que se dibujaron. */
    public List<Trazo> trazos() {
        return trazos;
    }

    public ReadOnlyBooleanProperty vacioProperty() {
        return estaVacio.getReadOnlyProperty();
    }

    public boolean isVacio() {
        return estaVacio.get();
    }

    /** Deshace el último trazo o la última pasada del borrador. */
    public void deshacer() {
        if (!historial.isEmpty()) {
            trazos = historial.pop();
            redibujar();
        }
    }

    /** Deja el lienzo vacío (se puede deshacer). */
    public void borrarTodo() {
        if (!trazos.isEmpty()) {
            historial.push(trazos);
            trazos = List.of();
        }
        redibujar();
    }

    /** Vacío y sin nada que deshacer ("Volver a escribir"). */
    public void reiniciar() {
        historial.clear();
        trazos = List.of();
        actual.clear();
        fuente = null;
        redibujar();
    }

    // ------------------------------------------------------------------ trazos

    private Punto punto(double x, double y) {
        return new Punto(x, y, System.currentTimeMillis());
    }

    private void empezar(Fuente f, Punto p) {
        fuente = f;
        onEmpezar.run();
        if (herramienta == Herramienta.LAPIZ) {
            actual.clear();
            actual.add(p);
        } else {
            historial.push(trazos);
            borroAlgo = false;
            ultimoBorrador = p;
            borrar(p, p);
        }
        redibujar();
    }

    private void seguir(Punto p) {
        if (herramienta == Herramienta.LAPIZ) {
            actual.add(p);
        } else {
            borrar(ultimoBorrador, p);
            ultimoBorrador = p;
        }
        redibujar();
    }

    private void terminar() {
        fuente = null;
        if (herramienta == Herramienta.LAPIZ) {
            if (!actual.isEmpty()) {
                historial.push(trazos);
                List<Trazo> nuevos = new ArrayList<>(trazos);
                nuevos.add(new Trazo(actual));
                trazos = List.copyOf(nuevos);
                actual.clear();
            }
        } else if (!borroAlgo) {
            historial.pop(); // la pasada no tocó nada: no hay qué deshacer
        }
        redibujar();
        onTerminar.run();
    }

    /** Quita los puntos a menos de medio ancho del borrador del segmento a–b; corta los trazos en dos. */
    private void borrar(Punto a, Punto b) {
        double radio = ANCHO_BORRADOR / 2;
        List<Trazo> quedan = new ArrayList<>();
        boolean cambio = false;
        for (Trazo t : trazos) {
            List<Punto> tramo = new ArrayList<>();
            for (Punto p : t.puntos()) {
                if (distancia(p, a, b) <= radio) {
                    cambio = true;
                    if (!tramo.isEmpty()) {
                        quedan.add(new Trazo(tramo));
                        tramo = new ArrayList<>();
                    }
                } else {
                    tramo.add(p);
                }
            }
            if (!tramo.isEmpty()) {
                quedan.add(new Trazo(tramo));
            }
        }
        if (cambio) {
            borroAlgo = true;
            trazos = List.copyOf(quedan);
        }
    }

    /** Distancia de p al segmento a–b. */
    static double distancia(Punto p, Punto a, Punto b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double largo2 = dx * dx + dy * dy;
        double t = largo2 == 0 ? 0 : Math.max(0, Math.min(1, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / largo2));
        double cx = a.x() + t * dx - p.x();
        double cy = a.y() + t * dy - p.y();
        return Math.sqrt(cx * cx + cy * cy);
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
            redibujar();
        }
        vacio.resizeRelocate(0, 0, w, h);
    }

    private void redibujar() {
        estaVacio.set(trazos.isEmpty() && actual.isEmpty());
        vacio.setVisible(estaVacio.get());

        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setStroke(CUADRICULA);
        g.setLineWidth(1);
        for (double x = CUADRO + 0.5; x < w; x += CUADRO) {
            g.strokeLine(x, 0, x, h);
        }
        for (double y = CUADRO + 0.5; y < h; y += CUADRO) {
            g.strokeLine(0, y, w, y);
        }

        g.setStroke(TINTA);
        g.setFill(TINTA);
        g.setLineWidth(GROSOR);
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (Trazo t : trazos) {
            dibujar(g, t.puntos());
        }
        dibujar(g, actual);
    }

    private static void dibujar(GraphicsContext g, List<Punto> puntos) {
        if (puntos.isEmpty()) {
            return;
        }
        if (puntos.size() == 1) {
            Punto p = puntos.getFirst();
            g.fillOval(p.x() - GROSOR / 2, p.y() - GROSOR / 2, GROSOR, GROSOR);
            return;
        }
        g.beginPath();
        g.moveTo(puntos.getFirst().x(), puntos.getFirst().y());
        for (int i = 1; i < puntos.size(); i++) {
            g.lineTo(puntos.get(i).x(), puntos.get(i).y());
        }
        g.stroke();
    }

    /** El canvas, para las pruebas (se le mandan los eventos). */
    Canvas canvas() {
        return canvas;
    }
}
