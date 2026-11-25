package pe.mathnova.presentation.animacion;

import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableStringValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public final class AnimadorResolucion {

    public static final Duration MINIMO_CARGA = Duration.millis(600);
    static final Duration MINIMO_FASE = Duration.millis(200);
    static final Duration DURACION_FILA = Duration.millis(220);
    static final Duration ENTRE_FILAS = Duration.millis(140);
    static final Duration DURACION_SOLUCION = Duration.millis(260);
    static final Duration DURACION_DESPLIEGUE = Duration.millis(220);
    static final double DESPLAZAMIENTO_FILA = 12;

    private static final String ANIMACION_DESPLIEGUE = "animador.despliegue";

    private boolean activas = true;
    private Animation actual;
    private Timeline relojFases;
    private ObservableStringValue mensajeSeguido;
    private ChangeListener<String> oyenteFases;

    public boolean activas() {
        return activas;
    }

    public void setActivas(boolean activas) {
        this.activas = activas;
    }

    public void cancelar() {
        if (actual != null) {
            actual.stop();
            actual = null;
        }
        detenerFases();
    }

    public void seguirFases(Labeled etiqueta, ObservableStringValue mensaje, List<String> fases) {
        detenerFases();
        int[] alcanzada = {0};
        int[] mostrada = {0};
        etiqueta.setText(fases.getFirst());
        oyenteFases = (obs, antes, ahora) -> {
            int i = fases.indexOf(ahora);
            if (i < 0) {
                return;
            }
            alcanzada[0] = Math.max(alcanzada[0], i);
            if (!activas) {
                mostrada[0] = alcanzada[0];
                etiqueta.setText(fases.get(mostrada[0]));
            }
        };
        mensaje.addListener(oyenteFases);
        mensajeSeguido = mensaje;
        if (activas) {
            relojFases = new Timeline(new KeyFrame(MINIMO_FASE, e -> {
                if (mostrada[0] < alcanzada[0]) {
                    mostrada[0]++;
                    etiqueta.setText(fases.get(mostrada[0]));
                }
            }));
            relojFases.setCycleCount(Animation.INDEFINITE);
            relojFases.play();
        }
    }

    public void detenerFases() {
        if (relojFases != null) {
            relojFases.stop();
            relojFases = null;
        }
        if (mensajeSeguido != null) {
            mensajeSeguido.removeListener(oyenteFases);
            mensajeSeguido = null;
            oyenteFases = null;
        }
    }

    public void trasCargaMinima(long inicioNanos, Runnable accion) {
        double transcurrido = (System.nanoTime() - inicioNanos) / 1_000_000.0;
        double falta = MINIMO_CARGA.toMillis() - transcurrido;
        if (!activas || falta <= 0) {
            accion.run();
            return;
        }
        PauseTransition espera = new PauseTransition(Duration.millis(falta));
        espera.setOnFinished(e -> {
            actual = null;
            accion.run();
        });
        actual = espera;
        espera.play();
    }

    public void presentar(List<? extends Node> filas, List<? extends Node> numeros, List<? extends Node> solucion,
                          Node etiqueta, Runnable alTerminar) {
        cancelar();
        if (!activas) {
            estadoFinal(filas, numeros, solucion, etiqueta);
            alTerminar.run();
            return;
        }
        SequentialTransition todo = new SequentialTransition();

        if (!filas.isEmpty()) {
            ParallelTransition pasos = new ParallelTransition();
            for (int i = 0; i < filas.size(); i++) {
                Node fila = filas.get(i);
                fila.setOpacity(0);
                fila.setTranslateY(DESPLAZAMIENTO_FILA);

                FadeTransition aparecer = new FadeTransition(DURACION_FILA, fila);
                aparecer.setFromValue(0);
                aparecer.setToValue(1);
                TranslateTransition subir = new TranslateTransition(DURACION_FILA, fila);
                subir.setFromY(DESPLAZAMIENTO_FILA);
                subir.setToY(0);
                subir.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition entrada = new ParallelTransition(aparecer, subir);

                if (i < numeros.size()) {
                    Node numero = numeros.get(i);
                    numero.setScaleX(0.6);
                    numero.setScaleY(0.6);
                    ScaleTransition crecer = new ScaleTransition(DURACION_FILA, numero);
                    crecer.setFromX(0.6);
                    crecer.setFromY(0.6);
                    crecer.setToX(1);
                    crecer.setToY(1);
                    crecer.setInterpolator(Interpolator.EASE_OUT);
                    entrada.getChildren().add(crecer);
                }
                pasos.getChildren().add(new SequentialTransition(
                        new PauseTransition(ENTRE_FILAS.multiply(i)), entrada));
            }
            todo.getChildren().add(pasos);
        }

        if (!solucion.isEmpty()) {
            ParallelTransition formula = new ParallelTransition();
            for (Node n : solucion) {
                n.setOpacity(0);
                FadeTransition f = new FadeTransition(DURACION_SOLUCION, n);
                f.setFromValue(0);
                f.setToValue(1);
                formula.getChildren().add(f);
            }
            todo.getChildren().add(formula);
        }

        if (etiqueta != null) {
            etiqueta.setOpacity(0);
            etiqueta.setScaleX(0.8);
            etiqueta.setScaleY(0.8);
            FadeTransition aparecer = new FadeTransition(Duration.millis(140), etiqueta);
            aparecer.setFromValue(0);
            aparecer.setToValue(1);
            todo.getChildren().add(new ParallelTransition(aparecer,
                    new SequentialTransition(escala(etiqueta, 0.8, 1.05, 160), escala(etiqueta, 1.05, 1, 120))));
        }

        todo.setOnFinished(e -> {
            actual = null;
            estadoFinal(filas, numeros, solucion, etiqueta);
            alTerminar.run();
        });
        actual = todo;
        todo.play();
    }

    private static ScaleTransition escala(Node n, double desde, double hasta, double ms) {
        ScaleTransition s = new ScaleTransition(Duration.millis(ms), n);
        s.setFromX(desde);
        s.setFromY(desde);
        s.setToX(hasta);
        s.setToY(hasta);
        s.setInterpolator(Interpolator.EASE_OUT);
        return s;
    }

    private static void estadoFinal(List<? extends Node> filas, List<? extends Node> numeros,
                                    List<? extends Node> solucion, Node etiqueta) {
        for (Node f : filas) {
            f.setOpacity(1);
            f.setTranslateY(0);
        }
        for (Node n : numeros) {
            n.setScaleX(1);
            n.setScaleY(1);
        }
        for (Node n : solucion) {
            n.setOpacity(1);
        }
        if (etiqueta != null) {
            etiqueta.setOpacity(1);
            etiqueta.setScaleX(1);
            etiqueta.setScaleY(1);
        }
    }

    public void desplegar(Region detalle, Node flecha, boolean abrir) {
        Animation previa = (Animation) detalle.getProperties().remove(ANIMACION_DESPLIEGUE);
        if (previa != null) {
            previa.stop();
        }
        if (!activas) {
            detalle.setClip(null);
            detalle.setMaxHeight(Region.USE_COMPUTED_SIZE);
            detalle.setOpacity(1);
            detalle.setVisible(abrir);
            detalle.setManaged(abrir);
            flecha.setRotate(abrir ? 90 : 0);
            return;
        }
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(detalle.widthProperty());
        recorte.heightProperty().bind(detalle.heightProperty());
        detalle.setClip(recorte);

        double desde;
        double hasta;
        if (abrir) {
            desde = detalle.isManaged() ? detalle.getHeight() : 0;
            detalle.setVisible(true);
            detalle.setManaged(true);
            detalle.applyCss();
            hasta = detalle.prefHeight(anchoDisponible(detalle));
        } else {
            desde = detalle.getHeight();
            hasta = 0;
        }
        detalle.setMaxHeight(desde);

        Timeline alto = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(detalle.maxHeightProperty(), desde),
                        new KeyValue(detalle.opacityProperty(), detalle.getOpacity())),
                new KeyFrame(DURACION_DESPLIEGUE,
                        new KeyValue(detalle.maxHeightProperty(), hasta, Interpolator.EASE_BOTH),
                        new KeyValue(detalle.opacityProperty(), abrir ? 1 : 0, Interpolator.EASE_BOTH)));
        RotateTransition girar = new RotateTransition(DURACION_DESPLIEGUE, flecha);
        girar.setToAngle(abrir ? 90 : 0);

        ParallelTransition despliegue = new ParallelTransition(alto, girar);
        despliegue.setOnFinished(e -> {
            detalle.getProperties().remove(ANIMACION_DESPLIEGUE);
            detalle.setClip(null);
            detalle.setMaxHeight(Region.USE_COMPUTED_SIZE);
            if (!abrir) {
                detalle.setVisible(false);
                detalle.setManaged(false);
            }
        });
        detalle.getProperties().put(ANIMACION_DESPLIEGUE, despliegue);
        despliegue.play();
    }

    private static double anchoDisponible(Region detalle) {
        if (detalle.getParent() instanceof Region padre && padre.getWidth() > 0) {
            Insets i = padre.getInsets();
            return padre.getWidth() - i.getLeft() - i.getRight();
        }
        return -1;
    }

    static final Duration DURACION_CURVA = Duration.millis(500);

    public void presentarGrafica(Node tarjeta, javafx.beans.property.DoubleProperty progreso) {
        if (!activas) {
            tarjeta.setOpacity(1);
            if (progreso != null) {
                progreso.set(1);
            }
            return;
        }
        tarjeta.setOpacity(0);
        FadeTransition aparecer = new FadeTransition(DURACION_SOLUCION, tarjeta);
        aparecer.setFromValue(0);
        aparecer.setToValue(1);
        SequentialTransition todo = new SequentialTransition(aparecer);
        if (progreso != null) {
            progreso.set(0);
            todo.getChildren().add(new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progreso, 0)),
                    new KeyFrame(DURACION_CURVA, new KeyValue(progreso, 1, Interpolator.EASE_BOTH))));
        }
        todo.setOnFinished(e -> {
            actual = null;
            tarjeta.setOpacity(1);
            if (progreso != null) {
                progreso.set(1);
            }
        });
        actual = todo;
        todo.play();
    }
}
