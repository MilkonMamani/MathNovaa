package pe.mathnova.presentation.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.presentation.Fx;

/**
 * "Escribir" con el DEDO, con eventos táctiles simulados como los entrega Windows (cada toque va seguido de
 * su evento de mouse sintetizado). No reemplaza la prueba con una pantalla táctil física: comprueba que el
 * lienzo trata bien esos eventos y que el trazo hecho con el dedo llega al reconocedor y al campo de arriba.
 */
class EscrituraTactilTest {

    private Stage ventana;
    private PanelEscritura panel;
    private final List<String> puestos = new CopyOnWriteArrayList<>();
    private final List<List<Trazo>> leidos = new CopyOnWriteArrayList<>();
    /** Eventos de desplazamiento que llegaron al contenedor de afuera (no deberían, si vienen del dedo). */
    private final AtomicInteger desplazamientosAfuera = new AtomicInteger();

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        panel = enFx(() -> {
            PanelEscritura p = new PanelEscritura(trazos -> {
                leidos.add(trazos);
                return new ResultadoReconocimiento("\\int 2 x d x", List.of());
            }, PanelEscritura.destino(puestos::add), Duration.millis(200), null);
            StackPane afuera = new StackPane(p);
            afuera.addEventHandler(ScrollEvent.ANY, e -> desplazamientosAfuera.incrementAndGet());
            ventana = Fx.ventana(afuera, 520, 640);
            return p;
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private javafx.scene.canvas.Canvas canvas() {
        return panel.lienzo().canvas();
    }

    @Test
    void unTrazoConElDedoQuedaUnaSolaVezAunqueLleguenLosEventosDeMouseSintetizados() throws Exception {
        enFx(() -> Fx.trazoTactil(canvas(), 1, new double[] {40, 60}, new double[] {60, 80}, new double[] {80, 100},
                new double[] {100, 120}));
        enFx(() -> {
            List<Trazo> t = panel.lienzo().trazos();
            assertEquals(1, t.size(), "un trazo, no dos");
            assertEquals(4, t.getFirst().puntos().size(), "cada punto una vez (sin los del mouse sintetizado)");
            Punto p = t.getFirst().puntos().getFirst();
            assertEquals(40, p.x(), 0.5, "coordenadas del lienzo");
            assertEquals(60, p.y(), 0.5);
            assertFalse(panel.lienzo().isVacio());
        });
    }

    @Test
    void levantarElDedoEntreSimbolosYEsperarLlevaLaLecturaAlCampo() throws Exception {
        // ∫ 2x dx con el dedo: cuatro trazos separados, levantando el dedo entre ellos.
        enFx(() -> Fx.trazoTactil(canvas(), 1, new double[] {40, 20}, new double[] {35, 100}, new double[] {30, 180}));
        Thread.sleep(60);
        enFx(() -> Fx.trazoTactil(canvas(), 2, new double[] {70, 80}, new double[] {90, 80}, new double[] {70, 120},
                new double[] {95, 120}));
        Thread.sleep(60);
        enFx(() -> Fx.trazoTactil(canvas(), 3, new double[] {110, 90}, new double[] {130, 120}));
        enFx(() -> Fx.trazoTactil(canvas(), 4, new double[] {130, 90}, new double[] {110, 120}));
        Fx.esperar(() -> !puestos.isEmpty(), "lo escrito con el dedo no llegó al campo");
        assertEquals(1, leidos.size(), "una sola lectura, después de la pausa");
        assertEquals(4, leidos.getFirst().size(), "los cuatro trazos del dedo");
        assertEquals(List.of("\\int 2 x d x"), puestos);
    }

    @Test
    void unSegundoDedoMientrasSeEscribeNoAgregaPuntos() throws Exception {
        enFx(() -> {
            var c = canvas();
            Fx.toque(c, TouchEvent.TOUCH_PRESSED, TouchPoint.State.PRESSED, 1, 40, 60);
            Fx.toque(c, TouchEvent.TOUCH_PRESSED, TouchPoint.State.PRESSED, 2, 200, 60); // otro dedo
            Fx.toque(c, TouchEvent.TOUCH_MOVED, TouchPoint.State.MOVED, 2, 220, 80);
            Fx.toque(c, TouchEvent.TOUCH_MOVED, TouchPoint.State.MOVED, 1, 60, 80);
            Fx.toque(c, TouchEvent.TOUCH_RELEASED, TouchPoint.State.RELEASED, 2, 220, 80);
            Fx.toque(c, TouchEvent.TOUCH_RELEASED, TouchPoint.State.RELEASED, 1, 60, 80);
        });
        enFx(() -> {
            assertEquals(1, panel.lienzo().trazos().size());
            assertEquals(2, panel.lienzo().trazos().getFirst().puntos().size(), "solo los puntos del primer dedo");
            panel.lienzo().trazos().getFirst().puntos().forEach(p -> assertTrue(p.x() < 100, "nada del segundo dedo"));
        });
    }

    @Test
    void laManoApoyadaMientrasSeEscribeConLapizNoDibuja() throws Exception {
        enFx(() -> {
            var c = canvas();
            Fx.raton(c, MouseEvent.MOUSE_PRESSED, 40, 60, false); // lápiz (Windows lo manda como mouse)
            Fx.toque(c, TouchEvent.TOUCH_PRESSED, TouchPoint.State.PRESSED, 7, 300, 150); // la palma
            Fx.toque(c, TouchEvent.TOUCH_MOVED, TouchPoint.State.MOVED, 7, 310, 160);
            Fx.raton(c, MouseEvent.MOUSE_DRAGGED, 80, 100, false);
            Fx.toque(c, TouchEvent.TOUCH_RELEASED, TouchPoint.State.RELEASED, 7, 310, 160);
            Fx.raton(c, MouseEvent.MOUSE_RELEASED, 80, 100, false);
        });
        enFx(() -> {
            assertEquals(1, panel.lienzo().trazos().size(), "solo el trazo del lápiz");
            panel.lienzo().trazos().getFirst().puntos().forEach(p -> assertTrue(p.x() < 100, "nada de la palma"));
        });
    }

    @Test
    void deslizarConElDedoNoMueveLaPaginaPeroLaRuedaSi() throws Exception {
        enFx(() -> {
            var c = canvas();
            javafx.geometry.Point2D s = c.localToScene(50, 50);
            // Del dedo (directo): se queda en el lienzo.
            javafx.event.Event.fireEvent(c, new ScrollEvent(ScrollEvent.SCROLL, s.getX(), s.getY(), s.getX(), s.getY(),
                    false, false, false, false, true, false, 0, -40, 0, -40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0, 1, null));
        });
        assertEquals(0, desplazamientosAfuera.get(), "el deslizamiento del dedo no sale del lienzo");
        enFx(() -> {
            var c = canvas();
            javafx.geometry.Point2D s = c.localToScene(50, 50);
            // De la rueda del mouse (no directo): sigue moviendo la pantalla.
            javafx.event.Event.fireEvent(c, new ScrollEvent(ScrollEvent.SCROLL, s.getX(), s.getY(), s.getX(), s.getY(),
                    false, false, false, false, false, false, 0, -40, 0, -40, ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.LINES, -1, 0, null));
        });
        assertEquals(1, desplazamientosAfuera.get(), "la rueda sí llega a la pantalla");
    }

    @Test
    void elBorradorYDeshacerFuncionanConElDedo() throws Exception {
        double[][] raya = new double[19][];
        for (int i = 0; i < 19; i++) {
            raya[i] = new double[] {20 + 10 * i, 100};
        }
        enFx(() -> Fx.trazoTactil(canvas(), 1, raya));
        ButtonBase borrador = enFx(() -> panel.lookupAll(".herramienta-escritura").stream().map(n -> (ButtonBase) n)
                .filter(b -> "Borrador".equals(b.getAccessibleText())).findFirst().orElseThrow());
        enFx(() -> borrador.fire());
        enFx(() -> Fx.trazoTactil(canvas(), 2, new double[] {110, 60}, new double[] {110, 140}));
        enFx(() -> assertEquals(2, panel.lienzo().trazos().size(), "el dedo con el borrador partió la raya"));
        ButtonBase deshacer = enFx(() -> panel.lookupAll(".herramienta-escritura").stream().map(n -> (ButtonBase) n)
                .filter(b -> "Deshacer".equals(b.getAccessibleText())).findFirst().orElseThrow());
        enFx(() -> deshacer.fire());
        enFx(() -> assertEquals(1, panel.lienzo().trazos().size()));
    }
}
