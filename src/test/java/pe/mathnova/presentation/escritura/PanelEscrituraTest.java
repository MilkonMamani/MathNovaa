package pe.mathnova.presentation.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;

/** La pestaña "Escribir" sola, con el reconocedor simulado. */
class PanelEscrituraTest {

    private Stage ventana;
    private PanelEscritura panel;
    private final List<String> usados = new CopyOnWriteArrayList<>();
    /** Lo que recibió el reconocedor en cada lectura, y en qué hilo. */
    private final List<List<Trazo>> leidos = new CopyOnWriteArrayList<>();
    private final List<Boolean> enHiloFx = new CopyOnWriteArrayList<>();

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        abrirCon(new ReconocedorSimulado(0));
    }

    private void abrirCon(ReconocedorEscritura base) throws Exception {
        ReconocedorEscritura espia = trazos -> {
            leidos.add(trazos);
            enHiloFx.add(javafx.application.Platform.isFxApplicationThread());
            return base.reconocer(trazos);
        };
        if (ventana != null) {
            enFx(() -> ventana.close());
        }
        panel = enFx(() -> {
            PanelEscritura p = new PanelEscritura(espia, usados::add);
            ventana = Fx.ventana(new StackPane(p), 520, 640);
            return p;
        });
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private void trazo(double... xy) throws Exception {
        double[][] puntos = new double[xy.length / 2][];
        for (int i = 0; i < puntos.length; i++) {
            puntos[i] = new double[] {xy[2 * i], xy[2 * i + 1]};
        }
        enFx(() -> Fx.trazo(panel.lienzo().canvas(), puntos));
    }

    private ButtonBase boton(String nombre) throws Exception {
        return enFx(() -> panel.lookupAll(".herramienta-escritura").stream()
                .map(n -> (ButtonBase) n).filter(b -> nombre.equals(b.getAccessibleText())).findFirst().orElseThrow());
    }

    @Test
    void vacioMuestraLaIndicacionYLaPrimeraHerramientaEsElLapiz() throws Exception {
        enFx(() -> {
            assertTrue(panel.lienzo().isVacio());
            assertTrue(panel.lookup(".lienzo-vacio").isVisible());
            assertEquals(LienzoEscritura.Herramienta.LAPIZ, panel.lienzo().herramienta());
            assertEquals(220, panel.lienzo().getHeight(), 0.5);
            assertFalse(panel.tarjeta().isVisible());
        });
        for (String nombre : List.of("Lápiz", "Borrador", "Deshacer", "Borrar todo")) {
            ButtonBase b = boton(nombre);
            enFx(() -> {
                assertNotNull(b.getTooltip(), nombre);
                assertEquals(nombre, b.getTooltip().getText());
                assertTrue(b.getText() == null || b.getText().isEmpty(), "solo ícono: " + nombre);
                assertEquals(40, b.getWidth(), 0.5);
            });
        }
    }

    @Test
    void unTrazoYUnaPausaMuestranLoQueEntendi() throws Exception {
        trazo(20, 60, 40, 80, 60, 100);
        enFx(() -> {
            assertFalse(panel.lienzo().isVacio());
            assertFalse(panel.tarjeta().isVisible(), "todavía no pasaron los 0,7 s");
        });
        Fx.esperar(() -> panel.tarjeta().isVisible(), "no apareció \"Lo que entendí\"");
        assertEquals(1, leidos.size());
        assertEquals(1, leidos.getFirst().size(), "un trazo");
        assertEquals(3, leidos.getFirst().getFirst().puntos().size(), "como lista de puntos");
        assertEquals(List.of(false), enHiloFx, "el reconocedor corre fuera del hilo de JavaFX");
        enFx(() -> {
            assertEquals(ReconocedorSimulado.LATEX, panel.resultado().latex());
            assertEquals(1, panel.lookupAll(".simbolo-dudoso").size(), "el símbolo dudoso va subrayado");
        });

        // "¿El símbolo es x ×?": elegir × cambia la expresión.
        enFx(() -> {
            Button por = (Button) panel.lookupAll(".boton-alternativa-simbolo").stream()
                    .filter(n -> "×".equals(((Button) n).getText())).findFirst().orElseThrow();
            por.fire();
            assertNotEquals(ReconocedorSimulado.LATEX, panel.resultado().latex());
            assertTrue(panel.resultado().latex().contains("\\times"), panel.resultado().latex());
            assertTrue(panel.resultado().dudosos().isEmpty());
            assertTrue(panel.lookupAll(".boton-alternativa-simbolo").isEmpty(), "ya no se pregunta");
        });

        enFx(() -> ((Button) panel.lookup(".boton-usar-escritura")).fire());
        assertEquals(1, usados.size());
        assertTrue(usados.getFirst().contains("\\times"));
    }

    @Test
    void unTrazoNuevoCancelaLaLecturaEnCurso() throws Exception {
        CountDownLatch soltar = new CountDownLatch(1);
        List<Boolean> interrumpidas = new CopyOnWriteArrayList<>();
        abrirCon(trazos -> {
            try {
                soltar.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrumpidas.add(true);
                throw new IllegalStateException(e);
            }
            return new ResultadoReconocimiento("x", List.of());
        });
        trazo(20, 60, 60, 100);
        Fx.esperar(() -> panel.estadoLectura().isVisible()
                && PanelEscritura.LEYENDO.equals(panel.estadoLectura().getText()), "no empezó a leer");
        trazo(80, 60, 120, 100); // un trazo nuevo mientras lee
        Fx.esperar(() -> !interrumpidas.isEmpty(), "la lectura en curso no se canceló");
        soltar.countDown();
        Fx.esperar(() -> panel.tarjeta().isVisible(), "la segunda lectura no terminó");
        assertEquals(2, leidos.size());
        assertEquals(2, leidos.get(1).size(), "la segunda lectura lleva los dos trazos");
    }

    @Test
    void deshacerQuitaElUltimoTrazoYBorrarTodoVuelveAlVacio() throws Exception {
        trazo(20, 60, 60, 100);
        trazo(100, 60, 140, 100);
        enFx(() -> assertEquals(2, panel.lienzo().trazos().size()));
        ButtonBase deshacer = boton("Deshacer");
        enFx(() -> deshacer.fire());
        enFx(() -> {
            assertEquals(1, panel.lienzo().trazos().size());
            assertEquals(20, panel.lienzo().trazos().getFirst().puntos().getFirst().x(), 1e-9,
                    "queda el primero");
        });

        trazo(100, 60, 140, 100);
        ButtonBase borrarTodo = boton("Borrar todo");
        enFx(() -> borrarTodo.fire());
        enFx(() -> {
            assertTrue(panel.lienzo().isVacio());
            assertTrue(panel.lookup(".lienzo-vacio").isVisible(), "vuelve la indicación");
            assertFalse(panel.tarjeta().isVisible());
        });
    }

    @Test
    void elBorradorCortaElTrazoYSeDeshace() throws Exception {
        // Una raya horizontal de 20 a 200 px; el borrador pasa vertical por x = 110.
        double[] raya = new double[38];
        for (int i = 0; i < 19; i++) {
            raya[2 * i] = 20 + 10 * i;
            raya[2 * i + 1] = 100;
        }
        trazo(raya);
        ButtonBase borrador = boton("Borrador");
        enFx(() -> borrador.fire());
        trazo(110, 60, 110, 140);
        enFx(() -> {
            assertEquals(2, panel.lienzo().trazos().size(), "la raya quedó en dos");
            for (Trazo t : panel.lienzo().trazos()) {
                t.puntos().forEach(p -> assertTrue(Math.abs(p.x() - 110) > LienzoEscritura.ANCHO_BORRADOR / 2));
            }
        });
        ButtonBase deshacer = boton("Deshacer");
        enFx(() -> deshacer.fire());
        enFx(() -> assertEquals(1, panel.lienzo().trazos().size()));
    }

    @Test
    void volverAEscribirDejaTodoVacio() throws Exception {
        trazo(20, 60, 60, 100);
        Fx.esperar(() -> panel.tarjeta().isVisible(), "no apareció \"Lo que entendí\"");
        enFx(() -> {
            Node volver = panel.lookupAll(".boton-alternativa").stream()
                    .filter(n -> "Volver a escribir".equals(((Button) n).getText())).findFirst().orElseThrow();
            ((Button) volver).fire();
            assertTrue(panel.lienzo().isVacio());
            assertFalse(panel.tarjeta().isVisible());
        });
        assertTrue(usados.isEmpty(), "volver a escribir no manda nada al editor");
    }
}
