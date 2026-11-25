package pe.mathnova.presentation.grafica;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.resolucion.ContenidoDerivada;
import pe.mathnova.presentation.resolucion.ContenidoIntegral;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GraficaCalculoTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService SERVICIO = new ResolverService(new FactorizacionService(MOTOR), MOTOR,
            new ResolvedorEcuaciones(MOTOR));
    private static final Color NARANJA = Color.web("#ea580c");

    @BeforeAll
    static void arrancar() throws Exception {
        CountDownLatch arranque = new CountDownLatch(1);
        try {
            Platform.startup(arranque::countDown);
        } catch (IllegalStateException yaArrancado) {
            arranque.countDown();
        } catch (UnsupportedOperationException | Error sinPantalla) {
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

    private static GraficaFuncion dibujar(DatosGrafica d) {
        GraficaFuncion g = new GraficaFuncion(d, new RenderizadorLatex(14f));
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
    void derivadaConSuCurvaNaranjaYLaTangente() throws Exception {
        ResolucionDerivada r = SERVICIO.resolverDerivada("d/dx(x^3 - 3x^2 - 9x + 5)", null, f -> { });
        DatosGrafica d = ContenidoDerivada.de(r, SERVICIO.caminos(r), false).grafica().orElseThrow();
        int[] naranja = enFx(() -> {
            GraficaFuncion g = dibujar(d);
            int antes = pixeles(g.snapshot(null, null), NARANJA, 0.08);
            VentanaGrafica v = g.ventana();
            double sx = (0 - v.xMin()) / v.ancho() * g.getWidth();
            double sy = (v.yMax() - 5) / v.alto() * g.getHeight();
            g.fireEvent(new MouseEvent(MouseEvent.MOUSE_MOVED, sx, sy, sx, sy, MouseButton.NONE, 0, false, false,
                    false, false, false, false, false, false, false, false, null));
            int despues = pixeles(g.snapshot(null, null), NARANJA, 0.08);
            return new int[] {antes, despues};
        });
        assertTrue(naranja[0] > 100, "la curva de f' en naranja: " + naranja[0]);
        assertTrue(naranja[1] > naranja[0] + 50, "la tangente agrega naranja: " + naranja[0] + " → " + naranja[1]);
    }

    @Test
    void definidaConElAreaSombreada() throws Exception {
        ResolucionIntegral r = SERVICIO.resolverIntegral("∫(x^2 - 1, x, -1, 2)", null, f -> { });
        DatosGrafica d = ContenidoIntegral.de(r, SERVICIO.caminos(r), false).grafica().orElseThrow();
        int[] colores = enFx(() -> {
            WritableImage img = dibujar(d).snapshot(null, null);
            Color azul = Color.color(1 - 0.22 * (1 - 0.15), 1 - 0.22 * (1 - 0.39), 1 - 0.22 * (1 - 0.92));
            Color rojo = Color.color(1 - 0.22 * (1 - 0.86), 1 - 0.22 * (1 - 0.15), 1 - 0.22 * (1 - 0.15));
            return new int[] {pixeles(img, azul, 0.04), pixeles(img, rojo, 0.04)};
        });
        assertTrue(colores[0] > 200, "área azul: " + colores[0]);
        assertTrue(colores[1] > 200, "área roja: " + colores[1]);
    }
}
