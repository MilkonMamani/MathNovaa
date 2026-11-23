package pe.mathnova.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assumptions;

/**
 * Lo que necesitan las pruebas de vista: arrancar JavaFX una vez, correr cosas en su hilo, esperar una
 * condición y mandar eventos del mouse. Mismo enfoque que PruebaHumoTest, sin TestFX.
 */
public final class Fx {

    private Fx() {
    }

    /** Arranca JavaFX (si no estaba) o aborta la prueba si no hay pantalla. */
    public static void arrancar() throws Exception {
        CountDownLatch arranque = new CountDownLatch(1);
        try {
            Platform.startup(arranque::countDown);
        } catch (IllegalStateException yaArrancado) {
            arranque.countDown();
        } catch (UnsupportedOperationException | Error sinPantalla) {
            Assumptions.abort("Sin pantalla para JavaFX: " + sinPantalla);
        }
        assertTrue(arranque.await(30, TimeUnit.SECONDS));
        // Cerrar la última ventana de una prueba no debe apagar JavaFX para las siguientes.
        Platform.setImplicitExit(false);
    }

    /** Una ventana invisible con la hoja de estilos de la app. */
    public static Stage ventana(Parent raiz, double ancho, double alto) {
        Scene escena = new Scene(raiz, ancho, alto);
        var css = Fx.class.getResource("/css/mathnova.css");
        if (css != null) {
            escena.getStylesheets().add(css.toExternalForm());
        }
        Stage s = new Stage();
        s.setScene(escena);
        s.setOpacity(0);
        s.show();
        return s;
    }

    public static <T> T enFx(Supplier<T> accion) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return accion.get();
        }
        FutureTask<T> tarea = new FutureTask<>(accion::get);
        Platform.runLater(tarea);
        return tarea.get(60, TimeUnit.SECONDS);
    }

    public static void enFx(Runnable accion) throws Exception {
        enFx(() -> {
            accion.run();
            return null;
        });
    }

    public static void esperar(Supplier<Boolean> condicion, String mensaje) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        while (!enFx(condicion)) {
            assertTrue(System.nanoTime() < limite, mensaje);
            Thread.sleep(30);
        }
    }

    /** Un trazo con el mouse sobre {@code n}, de punto en punto (coordenadas locales de n). En el hilo de FX. */
    public static void trazo(Node n, double[]... puntos) {
        for (int i = 0; i < puntos.length; i++) {
            var tipo = i == 0 ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_DRAGGED;
            raton(n, tipo, puntos[i][0], puntos[i][1]);
        }
        double[] ultimo = puntos[puntos.length - 1];
        raton(n, MouseEvent.MOUSE_RELEASED, ultimo[0], ultimo[1]);
    }

    private static void raton(Node n, javafx.event.EventType<MouseEvent> tipo, double x, double y) {
        Point2D escena = n.localToScene(x, y);
        boolean apretado = tipo != MouseEvent.MOUSE_RELEASED;
        Event.fireEvent(n, new MouseEvent(tipo, escena.getX(), escena.getY(), escena.getX(), escena.getY(),
                MouseButton.PRIMARY, 1, false, false, false, false, apretado, false, false, false, false, false,
                null));
    }
}
