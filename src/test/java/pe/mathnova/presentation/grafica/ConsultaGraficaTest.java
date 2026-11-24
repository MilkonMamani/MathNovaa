package pe.mathnova.presentation.grafica;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.latex.RenderizadorLatex;

class ConsultaGraficaTest {
    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    private static GraficaFuncion grafica(DatosGrafica datos, int ancho) {
        GraficaFuncion g = new GraficaFuncion(datos, new RenderizadorLatex(14f));
        StackPane raiz = new StackPane(g);
        new Scene(raiz, ancho, 340);
        raiz.applyCss();
        raiz.layout();
        return g;
    }

    private static void mover(GraficaFuncion g, double x, double y) {
        g.fireEvent(new MouseEvent(MouseEvent.MOUSE_MOVED, x, y, x, y, MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null));
    }

    @Test
    void consultarNoMueveLosEjesYElZoomSoloRespondeALosControles() throws Exception {
        DatosGrafica d = DatosGrafica.de(MOTOR.resolver(Parser.parse("x^3 - 2*x^2 - x + 2"), Campo.Q))
                .orElseThrow();
        assertEquals(List.of(Racional.de(-1), Racional.de(1), Racional.de(2)),
                d.raices().stream().map(r -> r.valor()).toList());
        assertEquals(Racional.de(2), d.corteY().orElseThrow());
        Fx.enFx(() -> {
            GraficaFuncion g = grafica(d, 480);
            VentanaGrafica inicial = g.ventana();
            assertNull(g.getOnScroll(), "la rueda debe llegar al ScrollPane");
            for (double[] p : new double[][] {{-1, 0}, {1, 0}, {2, 0}, {0, 2}}) {
                mover(g, (p[0] - inicial.xMin()) / inicial.ancho() * g.getWidth(),
                        (inicial.yMax() - p[1]) / inicial.alto() * g.getHeight());
                assertEquals(inicial, g.ventana());
            }
            Canvas capa = (Canvas) g.getChildrenUnmodifiable().get(1);
            WritableImage visible = capa.snapshot(null, null);
            g.fireEvent(new MouseEvent(MouseEvent.MOUSE_EXITED, 0, 0, 0, 0, MouseButton.NONE, 0,
                    false, false, false, false, false, false, false, false, false, false, null));
            assertEquals(inicial, g.ventana());
            assertTrue(pixelesDistintos(visible, capa.snapshot(null, null)) > 20,
                    "la consulta debe dibujarse y ocultarse en una capa superpuesta");
            Button mas = (Button) ((javafx.scene.layout.HBox) g.getChildrenUnmodifiable().get(2)).getChildren().get(0);
            Button menos = (Button) ((javafx.scene.layout.HBox) g.getChildrenUnmodifiable().get(2)).getChildren().get(1);
            Button reset = (Button) ((javafx.scene.layout.HBox) g.getChildrenUnmodifiable().get(2)).getChildren().get(2);
            mas.fire();
            assertNotEquals(inicial, g.ventana());
            menos.fire();
            assertEquals(inicial.xMin(), g.ventana().xMin(), 1e-9);
            mas.fire();
            VentanaGrafica elegida = g.ventana();
            g.resize(260, 340);
            g.layout();
            assertEquals(elegida, g.ventana(), "cambiar el ancho conserva el zoom");
            mover(g, (2 - elegida.xMin()) / elegida.ancho() * g.getWidth(),
                    (elegida.yMax()) / elegida.alto() * g.getHeight());
            WritableImage borde = capa.snapshot(null, null);
            assertTrue(pixelesDistintos(borde, visible) > 20,
                    "la etiqueta se recoloca en el panel estrecho");
            reset.fire();
            assertEquals(inicial, g.ventana());
            return null;
        });
    }

    private static int pixelesDistintos(WritableImage antes, WritableImage despues) {
        int cantidad = 0;
        for (int y = 0; y < antes.getHeight(); y++) {
            for (int x = 0; x < antes.getWidth(); x++) {
                if (!antes.getPixelReader().getColor(x, y).equals(
                        despues.getPixelReader().getColor(x, y))) cantidad++;
            }
        }
        return cantidad;
    }

    @Test
    void discontinuidadEnUnoQuedaSeparada() {
        DatosGrafica d = DatosGrafica.de(new SimplificadorFracciones(MOTOR)
                .simplificar(Parser.parse("1/(x-1)"))).orElseThrow();
        assertEquals(List.of(Racional.UNO), d.asintotas());
        assertFalse(Double.isFinite(d.funcion().valor(1)));
        List<List<double[]>> trazos = TrazadoCurva.trazos(d.funcion(), -3, 5, 801,
                List.of(1.0), -200, 200);
        assertTrue(trazos.size() >= 2);
        assertTrue(trazos.stream().noneMatch(t -> t.getFirst()[0] < 1 && t.getLast()[0] > 1));
    }
}
