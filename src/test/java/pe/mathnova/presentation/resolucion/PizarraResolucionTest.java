package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/** La Pizarra en una ventana de verdad: notas ocultas, el "?", el interruptor y paso a paso. */
class PizarraResolucionTest {

    /** x³ − 2x² − x + 2 en cuatro pasos, como en el wireframe. */
    private static final List<VistaPaso> PASOS = List.of(
            new VistaPaso(1, "F04", "Agrupación de términos", "Agrupación de términos",
                    "Los dos primeros términos tienen x² en común y los dos últimos, −1.",
                    Parser.parse("x^3 - 2x^2 - x + 2"),
                    new TrabajoIntermedio.Agrupacion(List.of("(x³ − 2x²) + (−x + 2)")),
                    Parser.parse("x^2(x - 2) - (x - 2)")),
            new VistaPaso(2, "F03", "Factor común polinomio", "Factor común polinomio",
                    "Los dos grupos tienen el factor (x − 2).", Parser.parse("x^2(x - 2) - (x - 2)"), null,
                    Parser.parse("(x - 2)(x^2 - 1)")),
            new VistaPaso(3, "F05", "Diferencia de cuadrados", "Diferencia de cuadrados",
                    "x² − 1 es una diferencia de cuadrados: x² − 1².", Parser.parse("x^2 - 1"), null,
                    Parser.parse("(x - 2)(x + 1)(x - 1)")),
            new VistaPaso(4, "F00", "Ordenar factores", "Ordenar factores",
                    "Se ordenan los factores por su raíz.", Parser.parse("(x - 2)(x + 1)(x - 1)"), null,
                    Parser.parse("(x + 1)(x - 1)(x - 2)")));

    private Stage ventana;
    private PizarraResolucion pizarra;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        pizarra = enFx(() -> {
            AnimadorResolucion animador = new AnimadorResolucion();
            animador.setActivas(false);
            PizarraResolucion p = new PizarraResolucion(new RenderizadorLatex(18f), animador, false);
            p.mostrar(new PizarraResolucion.Contenido("FACTORIZA", "x^{3}-2x^{2}-x+2", PASOS,
                    "\\left(x+1\\right)\\left(x-1\\right)\\left(x-2\\right)", "(x + 1)(x − 1)(x − 2) = x³ − 2x² − x + 2"));
            ventana = Fx.ventana(new StackPane(p), 1000, 1200);
            return p;
        });
        enFx(this::acomodar);
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private void acomodar() {
        ventana.getScene().getRoot().applyCss();
        ventana.getScene().getRoot().layout();
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> notasVisibles() {
        List<Integer> r = new ArrayList<>();
        for (int i = 1; i <= PASOS.size(); i++) {
            if (seVe(pizarra.nota(i))) {
                r.add(i);
            }
        }
        return r;
    }

    @Test
    void alResolverNingunaNotaSeVeYCadaPasoTieneSoloElBotonDeAyuda() throws Exception {
        enFx(() -> {
            assertEquals(List.of(), notasVisibles());
            for (int i = 1; i <= PASOS.size(); i++) {
                var b = pizarra.botonNota(i);
                assertTrue(seVe(b), "falta el \"?\" del paso " + i);
                assertTrue(b.getText() == null || b.getText().isEmpty(), "el \"?\" no lleva texto");
                assertEquals("Ver explicación del paso " + i, b.getAccessibleText());
                Tooltip t = b.getTooltip();
                assertNotNull(t);
                assertEquals("Ver explicación", t.getText());
                assertEquals(36, b.getWidth(), 0.5);
                assertEquals(36, b.getHeight(), 0.5);
            }
        });
    }

    @Test
    void elBotonDelPaso2AbreSoloSuNotaSinMoverLaFormula() throws Exception {
        enFx(() -> {
            Bounds formulaAntes = pizarra.formula(2).getBoundsInParent();
            Bounds columnaAntes = pizarra.nota(2).getParent().getBoundsInParent();
            pizarra.botonNota(2).fire();
            acomodar();
            assertEquals(List.of(2), notasVisibles());
            assertTrue(seVe(pizarra.ocultar(2)), "una nota abierta a mano lleva \"Ocultar\"");
            assertFalse(seVe(pizarra.botonNota(2)), "la nota va en el lugar del \"?\"");
            assertEquals(pizarra.botonNota(2).getParent(), pizarra.nota(2).getParent(), "en su misma columna");
            Bounds formulaDespues = pizarra.formula(2).getBoundsInParent();
            assertEquals(formulaAntes.getMinX(), formulaDespues.getMinX(), 0.01);
            assertEquals(formulaAntes.getMinY(), formulaDespues.getMinY(), 0.01);
            assertEquals(formulaAntes.getWidth(), formulaDespues.getWidth(), 0.01);
            Bounds columnaDespues = pizarra.nota(2).getParent().getBoundsInParent();
            assertEquals(columnaAntes.getMinX(), columnaDespues.getMinX(), 0.01);
            assertEquals(PizarraResolucion.ANCHO_NOTA, columnaDespues.getWidth(), 0.01);

            pizarra.ocultar(2).fire();
            acomodar();
            assertEquals(List.of(), notasVisibles());
            assertTrue(seVe(pizarra.botonNota(2)));
        });
    }

    @Test
    void elInterruptorMuestraLasCuatroNotasSinOcultar() throws Exception {
        List<Boolean> guardado = new ArrayList<>();
        enFx(() -> {
            pizarra.setOnExplicaciones(guardado::add);
            pizarra.botonNota(3).fire(); // se olvida al cambiar el interruptor
            pizarra.interruptor().fire();
            acomodar();
            assertEquals(List.of(1, 2, 3, 4), notasVisibles());
            for (int i = 1; i <= PASOS.size(); i++) {
                assertFalse(seVe(pizarra.ocultar(i)), "con el interruptor no hay \"Ocultar\" en el paso " + i);
                assertFalse(seVe(pizarra.botonNota(i)));
            }
            pizarra.interruptor().fire();
            acomodar();
            assertEquals(List.of(), notasVisibles());
        });
        assertEquals(List.of(true, false), guardado, "cada cambio se guarda");
    }

    @Test
    void elInterruptorSeUsaConEnterYEspacio() throws Exception {
        enFx(() -> {
            var b = pizarra.interruptor();
            b.requestFocus();
            javafx.event.Event.fireEvent(b, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                    false, false, false, false));
            assertTrue(pizarra.explicaciones(), "Enter");
            javafx.event.Event.fireEvent(b, new KeyEvent(KeyEvent.KEY_PRESSED, " ", " ", KeyCode.SPACE,
                    false, false, false, false));
            javafx.event.Event.fireEvent(b, new KeyEvent(KeyEvent.KEY_RELEASED, " ", " ", KeyCode.SPACE,
                    false, false, false, false));
            assertFalse(pizarra.explicaciones(), "Espacio");
            assertNotNull(b.getAccessibleText());
        });
    }

    @Test
    void pasoAPasoMuestraLosQueFaltanComoCirculosGrises() throws Exception {
        enFx(() -> {
            assertEquals("Todos los pasos", pizarra.etiquetaPasos().getText());
            pizarra.botonModo().fire();
            acomodar();
            assertEquals("Paso 1 de 4", pizarra.etiquetaPasos().getText());
            assertEquals("Ver todo", pizarra.botonModo().getText());
            pizarra.siguiente().fire();
            acomodar();
            assertEquals("Paso 2 de 4", pizarra.etiquetaPasos().getText());
            for (int i = 1; i <= 2; i++) {
                assertTrue(seVe(pizarra.formula(i)));
                assertFalse(pizarra.numero(i).getPseudoClassStates().stream().anyMatch(p -> p.getPseudoClassName()
                        .equals("pendiente")));
            }
            for (int i = 3; i <= 4; i++) {
                assertTrue(seVe(pizarra.numero(i)), "el círculo del paso " + i + " se ve");
                assertFalse(seVe(pizarra.formula(i)), "sin fórmula en el paso " + i);
                int paso = i;
                assertTrue(pizarra.numero(i).getPseudoClassStates().stream()
                        .anyMatch(p -> p.getPseudoClassName().equals("pendiente")), () -> "gris: paso " + paso);
            }
            assertFalse(seVe(pizarra.recuadroResultado()), "el resultado espera al último paso");
            assertFalse(seVe(pizarra.comprobacion()));

            pizarra.siguiente().fire();
            pizarra.siguiente().fire();
            acomodar();
            assertEquals("Paso 4 de 4", pizarra.etiquetaPasos().getText());
            assertTrue(pizarra.siguiente().isDisabled());
            assertTrue(seVe(pizarra.recuadroResultado()));
            assertTrue(seVe(pizarra.comprobacion()));

            pizarra.anterior().fire();
            assertEquals("Paso 3 de 4", pizarra.etiquetaPasos().getText());
            pizarra.botonModo().fire();
            acomodar();
            assertEquals("Todos los pasos", pizarra.etiquetaPasos().getText());
            assertTrue(seVe(pizarra.formula(4)));
            assertTrue(seVe(pizarra.recuadroResultado()));
        });
    }

    @Test
    void cadaPasoLlevaLaFlechaConElNombreDelMetodo() throws Exception {
        enFx(() -> {
            var metodos = pizarra.lookupAll(".pizarra-metodo").stream()
                    .map(n -> ((javafx.scene.control.Label) n).getText()).toList();
            assertEquals(PASOS.stream().map(VistaPaso::titulo).toList(), metodos);
            pizarra.lookupAll(".pizarra-paso-flecha").forEach(n -> assertTrue(seVe(n)));
        });
    }
}
