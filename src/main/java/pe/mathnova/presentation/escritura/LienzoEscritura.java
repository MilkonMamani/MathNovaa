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
import javafx.scene.input.GestureEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import pe.mathnova.domain.escritura.Muestra;
import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;

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
    private Fuente fuente;
    private int toque = -1;
    private boolean borroAlgo;
    private Punto ultimoBorrador;
    private Herramienta herramienta = Herramienta.LAPIZ;
    private long origen;
    private double anchoCaptura;
    private double altoCaptura;
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
                empezar(Fuente.MOUSE, e.getX(), e.getY());
            }
            e.consume();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!e.isSynthesized() && fuente == Fuente.MOUSE) {
                seguir(punto(e.getX(), e.getY()));
            }
            e.consume();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (!e.isSynthesized() && fuente == Fuente.MOUSE) {
                terminar();
            }
            e.consume();
        });
        canvas.addEventHandler(TouchEvent.TOUCH_PRESSED, e -> {
            if (fuente == null) {
                TouchPoint t = e.getTouchPoint();
                toque = t.getId();
                empezar(Fuente.TOQUE, t.getX(), t.getY());
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
        canvas.addEventHandler(GestureEvent.ANY, e -> {
            if (e.isDirect()) {
                e.consume();
            }
        });
    }

    public void setHerramienta(Herramienta h) {
        herramienta = h;
    }

    public Herramienta herramienta() {
        return herramienta;
    }

    public void setOnEmpezar(Runnable r) {
        onEmpezar = r;
    }

    public void setOnTerminar(Runnable r) {
        onTerminar = r;
    }

    public List<Trazo> trazos() {
        return trazos;
    }

    public ReadOnlyBooleanProperty vacioProperty() {
        return estaVacio.getReadOnlyProperty();
    }

    public boolean isVacio() {
        return estaVacio.get();
    }

    public void deshacer() {
        if (!historial.isEmpty()) {
            trazos = historial.pop();
            redibujar();
        }
    }

    public void borrarTodo() {
        if (!trazos.isEmpty()) {
            historial.push(trazos);
            trazos = List.of();
        }
        redibujar();
    }

    public void reiniciar() {
        historial.clear();
        trazos = List.of();
        actual.clear();
        fuente = null;
        redibujar();
    }

    public Muestra muestra(String etiqueta) {
        double ancho = Math.max(anchoCaptura, canvas.getWidth());
        double alto = Math.max(altoCaptura, canvas.getHeight());
        return new Muestra(etiqueta, ancho, alto, trazos);
    }

    public void cargar(Muestra m) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double escala = 1;
        if (w > 0 && h > 0 && m.ancho() > 0 && m.alto() > 0) {
            escala = Math.min(1, Math.min(w / m.ancho(), h / m.alto()));
        }
        Muestra aqui = escala == 1 ? m : m.escalada(escala);
        historial.clear();
        List<Trazo> hasta = new ArrayList<>();
        for (Trazo t : aqui.trazos()) {
            historial.push(List.copyOf(hasta));
            hasta.add(t);
        }
        trazos = List.copyOf(hasta);
        actual.clear();
        fuente = null;
        anchoCaptura = aqui.ancho();
        altoCaptura = aqui.alto();
        long ultimo = trazos.stream().flatMap(t -> t.puntos().stream()).mapToLong(Punto::t).max().orElse(0);
        origen = System.nanoTime() - ultimo * 1_000_000L;
        redibujar();
    }

    private Punto punto(double x, double y) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double cx = w > 0 ? Math.max(0, Math.min(w, x)) : x;
        double cy = h > 0 ? Math.max(0, Math.min(h, y)) : y;
        return new Punto(cx, cy, (System.nanoTime() - origen) / 1_000_000L);
    }

    private void empezar(Fuente f, double x, double y) {
        fuente = f;
        onEmpezar.run();
        if (herramienta == Herramienta.LAPIZ && trazos.isEmpty()) {
            origen = System.nanoTime();
            anchoCaptura = 0;
            altoCaptura = 0;
        }
        anchoCaptura = Math.max(anchoCaptura, canvas.getWidth());
        altoCaptura = Math.max(altoCaptura, canvas.getHeight());
        Punto p = punto(x, y);
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
            historial.pop();
        }
        redibujar();
        onTerminar.run();
    }

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

    static double distancia(Punto p, Punto a, Punto b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double largo2 = dx * dx + dy * dy;
        double t = largo2 == 0 ? 0 : Math.max(0, Math.min(1, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / largo2));
        double cx = a.x() + t * dx - p.x();
        double cy = a.y() + t * dy - p.y();
        return Math.sqrt(cx * cx + cy * cy);
    }

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

    Canvas canvas() {
        return canvas;
    }
}
