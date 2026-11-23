package pe.mathnova.presentation.grafica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.presentation.resolucion.ContenidoExtremos;
import pe.mathnova.presentation.resolucion.ContenidoIntegralMultiple;

/**
 * Sprint 8, Parte D: CurvasNivel dibuja de verdad las curvas de nivel (de azul a naranja), los puntos críticos, la
 * restricción y la región sombreada, y la rueda acerca. Fuera de pantalla, sin abrir la aplicación.
 */
class CurvasNivelTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService SERVICIO = new ResolverService(new FactorizacionService(MOTOR, null), MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    @BeforeAll
    static void arrancar() throws Exception {
        CountDownLatch arranque = new CountDownLatch(1);
        try {
            Platform.startup(arranque::countDown);
        } catch (IllegalStateException yaArrancado) {
            arranque.countDown();
        } catch (UnsupportedOperationException sinPantalla) {
            Assumptions.abort("Sin pantalla para JavaFX: " + sinPantalla);
        }
        assertTrue(arranque.await(30, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    private static <T> T enFx(Supplier<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion::get);
        Platform.runLater(tarea);
        return tarea.get(30, TimeUnit.SECONDS);
    }

    private static CurvasNivel dibujar(DatosCurvasNivel d) {
        CurvasNivel g = new CurvasNivel(d);
        StackPane raiz = new StackPane(g);
        new Scene(raiz, 480, 340);
        raiz.applyCss();
        raiz.layout();
        return g;
    }

    private static int pixeles(WritableImage img, Color color, double tolerancia) {
        PixelReader r = img.getPixelReader();
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                Color c = r.getColor(x, y);
                if (Math.abs(c.getRed() - color.getRed()) < tolerancia && Math.abs(c.getGreen() - color.getGreen())
                        < tolerancia && Math.abs(c.getBlue() - color.getBlue()) < tolerancia) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    void curvasDeNivelPuntosYRestriccion() throws Exception {
        DatosCurvasNivel d = ContenidoExtremos.de(SERVICIO.resolverExtremos("extremos(x^2 + y^2; x + 2y = 5)",
                f -> { })).curvasNivel().orElseThrow();
        int[] n = enFx(() -> {
            WritableImage img = dibujar(d).snapshot(null, null);
            return new int[] {pixeles(img, Color.web("#2563eb"), 0.08), pixeles(img, Color.web("#ea580c"), 0.08),
                    pixeles(img, Color.web("#16a34a"), 0.08), pixeles(img, Color.web("#0f172a"), 0.08)};
        });
        assertTrue(n[0] > 50, "curvas azules: " + n[0]);
        assertTrue(n[1] > 50, "curvas naranjas: " + n[1]);
        assertTrue(n[2] > 30, "el mínimo en verde y la curva del óptimo: " + n[2]);
        assertTrue(n[3] > 100, "la restricción en negro: " + n[3]);
    }

    @Test
    void laRegionSeSombrea() throws Exception {
        var r = SERVICIO.resolverIntegralMultiple("∬(1, y, x^2, x, x, 0, 1)", null, f -> { });
        DatosCurvasNivel d = ContenidoIntegralMultiple.de(r, SERVICIO.caminos(r), false).curvasNivel().orElseThrow();
        int sombra = enFx(() -> {
            WritableImage img = dibujar(d).snapshot(null, null);
            // el azul al 18 % sobre blanco
            return pixeles(img, Color.color(1 - 0.18 * (1 - 0.145), 1 - 0.18 * (1 - 0.388), 1 - 0.18 * (1 - 0.922)),
                    0.04);
        });
        assertTrue(sombra > 300, "región sombreada: " + sombra);
    }

    @Test
    void laRuedaAcerca() throws Exception {
        DatosCurvasNivel d = ContenidoExtremos.de(SERVICIO.resolverExtremos("extremos(x^2 - y^2)", f -> { }))
                .curvasNivel().orElseThrow();
        double[] anchos = enFx(() -> {
            CurvasNivel g = dibujar(d);
            double antes = g.ventana().ancho();
            g.fireEvent(new ScrollEvent(ScrollEvent.SCROLL, 240, 170, 240, 170, false, false, false, false, false,
                    false, 0, 40, 0, 40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 0, null));
            double despues = g.ventana().ancho();
            g.restablecer();
            return new double[] {antes, despues, g.ventana().ancho()};
        });
        assertTrue(anchos[1] < anchos[0], "acerca: " + anchos[0] + " → " + anchos[1]);
        assertEquals(anchos[0], anchos[2], 1e-9);
    }
}
