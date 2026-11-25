package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.editor.EditorMatematico;
import pe.mathnova.presentation.resolucion.CadenaIgualdades;
import pe.mathnova.presentation.resolucion.ComparacionCampos;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.VistaDesarrollo;

class CardanoYExpansionTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static final class Preferencias extends ConfiguracionService {
        Preferencias() {
            super(null);
        }

        @Override
        public boolean animacionesActivas() {
            return false;
        }

        @Override
        public boolean mostrarCodigos() {
            return false;
        }

        @Override
        public boolean mostrarExplicaciones() {
            return false;
        }

        @Override
        public void guardarMostrarExplicaciones(boolean si) {
        }
    }

    private Stage ventana;
    private Parent raiz;
    private ResolucionController controlador;
    private SplitPane division;
    private Region herramientas;

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        enFx(() -> {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), new ReconocedorSimulado(0)));
                raiz = loader.load();
                controlador = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz, 1280, 900);
            division = (SplitPane) raiz.lookup("#division");
            herramientas = (Region) raiz.lookup("#panelHerramientas");
        });
        EditorMatematico editor = enFx(() -> (EditorMatematico) raiz.lookup(".editor-matematico"));
        enFx(() -> editor.setLatex("x"));
        Fx.esperar(() -> "x".equals(editor.getLatex()), "el editor no cargó");
        enFx(() -> editor.setLatex(""));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.VACIO, "el editor no quedó vacío");
        acomodar();
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    @Test
    void cubicoEnRConExploracionYSeparador() throws Exception {
        elegirCampo("R");
        resolver("3x^3 + 8x^2 - x + 10");

        enFx(() -> {
            VistaDesarrollo d = (VistaDesarrollo) raiz.lookup(".pizarra-desarrollo");
            assertTrue(d != null && seVe(d), "el paso de Cardano no está desarrollado");
            assertFalse(d.controles().isEmpty(), "falta Ver valor exacto de r");
            assertFalse(seVe(d.detalles().getFirst()), "el valor exacto empieza plegado");
            d.controles().getFirst().fire();
        });
        acomodar();
        enFx(() -> {
            VistaDesarrollo d = (VistaDesarrollo) raiz.lookup(".pizarra-desarrollo");
            assertTrue(seVe(d.detalles().getFirst()), "no se desplegó el valor exacto de r");
            legibles();
        });

        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaExploracion")).fire());
        ComparacionCampos comp = esperarComparacion();
        enFx(() -> comp.controles().getFirst().fire());
        double inicial = enFx(controlador::posicionSeparador);
        for (double dx : new double[] {150, -300}) {
            Node divisor = enFx(() -> division.lookup(".split-pane-divider"));
            enFx(() -> Fx.trazo(divisor, new double[] {divisor.getLayoutBounds().getWidth() / 2, 10},
                    new double[] {divisor.getLayoutBounds().getWidth() / 2 + dx, 10}));
            acomodar();
            double posicion = enFx(controlador::posicionSeparador);
            enFx(() -> {
                Bounds panel = herramientas.localToScene(herramientas.getLayoutBounds());
                assertEquals(3, comp.rotulos().size());
                for (Region r : comp.rotulos()) {
                    Bounds b = r.localToScene(r.getLayoutBounds());
                    assertTrue(seVe(r), "no se ve " + ((Labeled) r).getText());
                    assertTrue(b.getMinX() >= panel.getMinX() && b.getMaxX() <= panel.getMaxX() + 1,
                            "la etiqueta se sale del panel: " + ((Labeled) r).getText());
                    assertEquals(ComparacionCampos.class.cast(comp).rotulos().getFirst().getWidth(), r.getWidth(), 0.5,
                            "la columna de las etiquetas cambió de ancho");
                }
                for (Node t : raiz.lookupAll(".tarjeta-metodo")) {
                    if (seVe(t)) {
                        Bounds b = t.localToScene(t.getLayoutBounds());
                        assertTrue(b.getMaxX() <= panel.getMaxX() + 1, "una tarjeta se recorta: " + b);
                    }
                }
                Bounds c = comp.localToScene(comp.getLayoutBounds());
                assertTrue(c.getMaxX() <= panel.getMaxX() + 1, "la comparación ensancha el panel: " + c);
                legibles();
            });
            assertTrue(Math.abs(posicion - inicial) > 0.05, "el separador no se movió");
            inicial = posicion;
            assertEquals(posicion, enFx(controlador::posicionSeparador), 1e-9, "la comparación movió el separador");
        }
    }

    @Test
    void verExpansionDelGradoOcho() throws Exception {
        resolver("2x^8 - 12x^6 + 18x^4 - 8x^2");
        enFx(() -> {
            ToggleButton ver = (ToggleButton) raiz.lookup(".pizarra-ver-detalle");
            assertEquals("Ver comprobación", ver.getText());
            ver.fire();
        });
        acomodar();
        enFx(() -> {
            CadenaIgualdades c = (CadenaIgualdades) raiz.lookup(".pizarra-cadena");
            assertTrue(seVe(c), "no se ve la expansión");
            FormulaAjustable celeste = (FormulaAjustable) raiz.lookup(".pizarra-resultado-formula");
            assertEquals(celeste.getLatex(), c.expresiones().getFirst());
            assertEquals("2x^{8} - 12x^{6} + 18x^{4} - 8x^{2}", c.expresiones().getLast());
            assertEquals(7, c.expresiones().size(), "con el renglón distribuido antes de reducir");
            assertTrue(c.expresiones().stream().noneMatch(l -> l.equals("x^{2} = x^{2}")));
            List<Double> xs = new ArrayList<>();
            for (Node fila : c.lookupAll(".pizarra-cadena-fila")) {
                Node f = ((Parent) fila).getChildrenUnmodifiable().getFirst();
                xs.add(f.localToScene(0, 0).getX());
            }
            assertEquals(7, xs.size());
            xs.forEach(x -> assertEquals(xs.getFirst(), x, 0.5, "las expresiones no empiezan en la misma columna"));
        });
    }

    @Test
    void encabezadosEnPesoRegular() throws Exception {
        resolver("2x^8 - 12x^6 + 18x^4 - 8x^2");
        for (int i = 0; i < 4; i++) {
            int vuelta = i;
            enFx(() -> {
                if (vuelta % 2 == 1) {
                    ((ToggleButton) raiz.lookup(".interruptor-explicaciones")).fire();
                } else if (vuelta > 0) {
                    boton(vuelta == 2 ? "Paso a paso" : "Ver todo").fire();
                }
            });
            acomodar();
            enFx(() -> {
                List<Node> metodos = new ArrayList<>(raiz.lookupAll(".pizarra-metodo"));
                metodos.addAll(raiz.lookupAll(".pizarra-numero .label"));
                assertFalse(metodos.isEmpty());
                for (Node n : metodos) {
                    Font f = ((Labeled) n).getFont();
                    String estilo = f.getStyle().toLowerCase();
                    assertFalse(estilo.contains("bold") || estilo.contains("semibold") || estilo.contains("black"),
                            ((Labeled) n).getText() + ": " + f);
                }
            });
        }
    }

    private void legibles() {
        for (Node n : raiz.lookupAll(".formula-ajustable")) {
            if (n instanceof FormulaAjustable f && seVe(f) && f.getLatex() != null && f.getWidth() > 0
                    && isEnPizarra(f)) {
                assertTrue(f.disposicionActual().tamano() >= 11, "fórmula diminuta: " + f.getLatex());
            }
        }
    }

    private boolean isEnPizarra(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (x.getStyleClass().contains("pizarra-resolucion")) {
                return true;
            }
        }
        return false;
    }

    private ComparacionCampos esperarComparacion() throws Exception {
        Fx.esperar(() -> {
            Node c = raiz.lookup(".comparacion-campos");
            return c instanceof ComparacionCampos cc && cc.filas().stream().allMatch(f -> f.latex() != null);
        }, "no se calculó la comparación");
        acomodar();
        return enFx(() -> (ComparacionCampos) raiz.lookup(".comparacion-campos"));
    }

    private void resolver(String texto) throws Exception {
        enFx(() -> controlador.resolver(texto));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió " + texto);
        acomodar();
    }

    private void elegirCampo(String campo) throws Exception {
        enFx(() -> {
            ToggleButton b = (ToggleButton) raiz.lookup("#campo" + campo);
            if (!b.isSelected()) {
                b.fire();
            }
        });
    }

    private Button boton(String texto) {
        return raiz.lookupAll(".pizarra-control").stream().filter(n -> n instanceof Button b && texto.equals(b.getText()))
                .map(n -> (Button) n).findFirst().orElseThrow(() -> new AssertionError("no hay " + texto));
    }

    private void acomodar() throws Exception {
        Thread.sleep(150);
        enFx(() -> {
            raiz.applyCss();
            raiz.layout();
        });
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }
}
