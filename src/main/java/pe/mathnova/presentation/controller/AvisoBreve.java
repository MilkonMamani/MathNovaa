package pe.mathnova.presentation.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Un mensaje corto abajo, al centro de la ventana, que se desvanece solo: "PDF guardado en …" (parte C),
 * "Guardado" (Configuración, parte E). Uno a la vez por ventana.
 */
public final class AvisoBreve {

    private static Popup actual;

    private AvisoBreve() {
    }

    /** No hace nada si el nodo no está en una ventana a la vista (pruebas). */
    public static void mostrar(Node origen, String texto, Duration duracion) {
        Window ventana = origen == null || origen.getScene() == null ? null : origen.getScene().getWindow();
        if (ventana == null || !ventana.isShowing()) {
            return;
        }
        if (actual != null) {
            actual.hide();
        }
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("aviso-breve");
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(Math.max(320, ventana.getWidth() - 160));
        StackPane caja = new StackPane(etiqueta);
        caja.getStylesheets().setAll(origen.getScene().getStylesheets());
        Popup p = new Popup();
        p.getContent().add(caja);
        p.setAutoHide(false);
        p.show(ventana);
        caja.applyCss();
        caja.layout();
        Bounds b = caja.getLayoutBounds();
        p.setX(ventana.getX() + (ventana.getWidth() - b.getWidth()) / 2);
        p.setY(ventana.getY() + ventana.getHeight() - b.getHeight() - 40);
        actual = p;
        FadeTransition salida = new FadeTransition(Duration.millis(400), caja);
        salida.setToValue(0);
        SequentialTransition t = new SequentialTransition(new PauseTransition(duracion), salida);
        t.setOnFinished(e -> p.hide());
        t.play();
    }
}
