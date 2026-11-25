package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.ArrayList;
import java.util.List;

import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
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

/**
 * El separador entre el panel de herramientas y la Pizarra: uno solo para las cinco pestañas, 40 / 60 al
 * abrir, se arrastra con el mouse y Resolver no lo mueve.
 */
class SeparadorPizarraTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));
    private static final List<String> PESTANAS = List.of("#pestanaTeclado", "#pestanaEscribir", "#pestanaGrafica",
            "#pestanaAnalisis", "#pestanaExploracion");

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
    private EditorMatematico editor;
    private SplitPane division;
    private Region herramientas;
    private Region pizarra;

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
            editor = (EditorMatematico) raiz.lookup(".editor-matematico");
            division = (SplitPane) raiz.lookup("#division");
            herramientas = (Region) raiz.lookup("#panelHerramientas");
            pizarra = (Region) raiz.lookup("#panelPizarra");
        });
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

    /** La prueba principal de aceptación, paso a paso. */
    @Test
    void elSeparadorSeArrastraNoLoMuevenLasPestanasNiResolverYConservaElPaso() throws Exception {
        // Al abrir, con la pizarra vacía: 40 % para las herramientas.
        enFx(() -> {
            assertEquals(ResolucionController.PROPORCION_HERRAMIENTAS, controlador.posicionSeparador(), 0.01);
            double util = herramientas.getWidth() + pizarra.getWidth();
            assertEquals(0.4, herramientas.getWidth() / util, 0.02, "40 % del ancho útil");
            assertTrue(raiz.lookup("#sinPasos").isVisible(), "la pizarra empieza vacía");
        });

        // Arrastre hacia la derecha: los dos paneles responden durante el arrastre.
        double[] antes = anchos();
        Node divisor = enFx(() -> division.lookup(".split-pane-divider"));
        Point2D inicio = enFx(() -> divisor.localToScene(divisor.getLayoutBounds().getWidth() / 2, 10));
        enFx(() -> raton(divisor, MouseEvent.MOUSE_PRESSED, inicio, 0));
        enFx(() -> assertTrue(division.getPseudoClassStates().stream().anyMatch(p -> p.getPseudoClassName().equals("arrastrando")),
                "la línea se resalta mientras se arrastra"));
        enFx(() -> raton(divisor, MouseEvent.MOUSE_DRAGGED, inicio, 80));
        acomodar();
        double[] aMitad = anchos();
        assertEquals(antes[0] + 80, aMitad[0], 2, "el panel de herramientas crece durante el arrastre");
        assertEquals(antes[1] - 80, aMitad[1], 2, "la pizarra se achica durante el arrastre");
        enFx(() -> raton(divisor, MouseEvent.MOUSE_DRAGGED, inicio, 160));
        enFx(() -> raton(divisor, MouseEvent.MOUSE_RELEASED, inicio, 160));
        acomodar();
        enFx(() -> assertFalse(division.getPseudoClassStates().stream().anyMatch(p -> p.getPseudoClassName().equals("arrastrando"))));
        double[] elegido = anchos();
        assertEquals(antes[0] + 160, elegido[0], 2);
        double posicion = enFx(controlador::posicionSeparador);

        // Las cinco pestañas usan el mismo ancho; terminamos en Teclado.
        List<String> recorrido = new ArrayList<>(PESTANAS.subList(1, PESTANAS.size()));
        recorrido.add(PESTANAS.get(0));
        for (String id : recorrido) {
            enFx(() -> ((ToggleButton) raiz.lookup(id)).fire());
            acomodar();
            assertEquals(posicion, enFx(controlador::posicionSeparador), 1e-9, id + " movió el separador");
            assertEquals(elegido[0], anchos()[0], 0.5, id + " cambió el ancho del panel");
        }

        // Resolver no mueve el separador; la pizarra muestra la solución y el teclado sigue ahí, con el ancho
        // elegido (Resolver abre Gráfica con la tarjeta Solución, como antes).
        Node teclado = enFx(() -> raiz.lookup(".teclado-mn"));
        enFx(() -> controlador.resolver("x^3 - 2x^2 - x + 2"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió");
        acomodar();
        enFx(() -> {
            // Al resolver puede aparecer la barra de desplazamiento vertical: ese ancho lo cede la Pizarra.
            assertEquals(elegido[0], herramientas.getWidth(), 0.5, "Resolver movió el separador");
            assertFalse(raiz.lookupAll(".pizarra-paso").isEmpty(), "la pizarra muestra la solución");
            Node primerPaso = raiz.lookup(".pizarra-paso");
            assertTrue(primerPaso.localToScene(primerPaso.getBoundsInLocal()).getMinX()
                    >= pizarra.localToScene(pizarra.getBoundsInLocal()).getMinX() - 1, "la solución está en la pizarra");
            ((ToggleButton) raiz.lookup("#pestanaTeclado")).fire();
        });
        acomodar();
        enFx(() -> {
            assertSame(teclado, raiz.lookup(".teclado-mn"), "el teclado se volvió a crear");
            assertTrue(seVe(teclado), "el teclado sigue disponible");
            assertEquals(elegido[0], herramientas.getWidth(), 0.5);
        });

        // Paso a paso, en el paso 2; luego se arrastra a la izquierda.
        enFx(() -> {
            boton("Paso a paso").fire();
            raiz.lookupAll(".pizarra-flecha-nav").stream().map(n -> (Button) n).reduce((a, b) -> b).orElseThrow().fire();
        });
        String paso = enFx(() -> ((Label) raiz.lookup(".pizarra-etiqueta-pasos")).getText());
        assertTrue(paso.startsWith("Paso 2"), paso);
        String latex = enFx(editor::getLatex);

        double[] resuelto = anchos();
        Node divisor2 = enFx(() -> division.lookup(".split-pane-divider"));
        enFx(() -> Fx.trazo(divisor2, new double[] {divisor2.getLayoutBounds().getWidth() / 2, 10},
                new double[] {divisor2.getLayoutBounds().getWidth() / 2 - 150, 10}));
        acomodar();
        double[] despues = anchos();
        assertEquals(resuelto[0] - 150, despues[0], 2, "las herramientas ceden espacio");
        assertEquals(resuelto[1] + 150, despues[1], 2, "la pizarra gana espacio");
        assertEquals(paso, enFx(() -> ((Label) raiz.lookup(".pizarra-etiqueta-pasos")).getText()), "cambió el paso");
        assertEquals(latex, enFx(editor::getLatex), "cambió el ejercicio escrito");
        assertEquals(EstadoResolucion.RESUELTO, enFx(controlador::estado));
    }

    /** En una ventana angosta el separador topa en los límites, y en el mínimo no se corta ninguna tecla. */
    @Test
    void enVentanaAngostaLosLimitesSeRespetanYElTecladoNoSeCorta() throws Exception {
        enFx(() -> ventana.setWidth(860));
        acomodar();
        Node divisor = enFx(() -> division.lookup(".split-pane-divider"));

        enFx(() -> Fx.trazo(divisor, new double[] {4, 10}, new double[] {-2000, 10}));
        acomodar();
        assertEquals(ResolucionController.ANCHO_HERRAMIENTAS_MIN, anchos()[0], 1, "mínimo de las herramientas");
        // Todas las pestañas del teclado, en el ancho mínimo: ninguna tecla con "…".
        List<Node> pildoras = enFx(() -> new ArrayList<>(raiz.lookupAll(".teclado-pildora")));
        assertFalse(pildoras.isEmpty());
        for (Node p : pildoras) {
            enFx(() -> ((ButtonBase) p).fire());
            acomodar();
            enFx(() -> {
                for (Node n : raiz.lookupAll(".teclado-tecla")) {
                    if (seVe(n)) {
                        sinCortar((Labeled) n);
                    }
                }
            });
        }
        enFx(() -> {
            for (String id : PESTANAS) {
                sinCortar((Labeled) raiz.lookup(id));
            }
            Node t = raiz.lookup(".teclado-mn");
            assertTrue(t.getBoundsInParent().getMaxX() <= ((Region) t.getParent()).getWidth() + 1, "el teclado se sale");
        });

        // La gráfica se adapta al ancho que queda.
        enFx(() -> controlador.resolver("x^2 - 1"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió");
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaGrafica")).fire());
        acomodar();
        enFx(() -> {
            Region zona = (Region) raiz.lookup("#zonaGrafica");
            Region tarjeta = (Region) raiz.lookup("#tarjetaGrafica");
            assertTrue(seVe(zona), "no se ve la gráfica");
            assertTrue(zona.getWidth() <= tarjeta.getWidth(), "la gráfica no cabe en su tarjeta");
            assertTrue(tarjeta.getWidth() <= herramientas.getWidth(), "la tarjeta no cabe en el panel");
        });

        enFx(() -> Fx.trazo(divisor, new double[] {4, 10}, new double[] {4000, 10}));
        acomodar();
        assertEquals(ResolucionController.ANCHO_PIZARRA_MIN, anchos()[1], 1, "mínimo de la pizarra");
        // La cabecera de la Pizarra pasa sus controles a otra línea: nada se corta ni se sale.
        enFx(() -> {
            Region cabecera = (Region) raiz.lookup(".pizarra-cabecera");
            List<Labeled> textos = new ArrayList<>();
            juntar(cabecera, textos);
            assertTrue(textos.size() >= 4, "faltan textos en la cabecera");
            // La tarjeta empieza arriba de su panel y cabe a lo alto: no sobresale ni se recorta.
            Region tarjeta = (Region) raiz.lookup("#tarjetaProcedimiento");
            var enPanel = pizarra.sceneToLocal(tarjeta.localToScene(tarjeta.getLayoutBounds()));
            assertTrue(enPanel.getMinY() >= 0, "la Pizarra sobresale por arriba: " + enPanel);
            assertTrue(enPanel.getMaxY() <= pizarra.getHeight() + 1, "la Pizarra se recorta abajo: " + enPanel);
            for (Labeled l : textos) {
                sinCortar(l);
                double derecha = l.localToScene(l.getLayoutBounds()).getMaxX();
                assertTrue(derecha <= cabecera.localToScene(cabecera.getLayoutBounds()).getMaxX() + 1,
                        "se sale de la cabecera: " + l.getText());
            }
        });
        enFx(() -> {
            Region zona = (Region) raiz.lookup("#zonaGrafica");
            Region tarjeta = (Region) raiz.lookup("#tarjetaGrafica");
            assertTrue(zona.getWidth() > 300, "la gráfica no creció con el panel: " + zona.getWidth());
            assertTrue(zona.getWidth() <= tarjeta.getWidth());
        });
    }

    // ------------------------------------------------------------------ ayudas

    private static void sinCortar(Labeled l) {
        Text t = (Text) l.lookup(".text");
        assertNotNull(t, "sin texto: " + l);
        assertEquals(l.getText(), t.getText(), "se cortó: " + l.getText());
    }

    private static void juntar(Node n, List<Labeled> textos) {
        if (n instanceof Labeled l && l.getText() != null && !l.getText().isEmpty() && seVe(l)) {
            textos.add(l);
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                juntar(h, textos);
            }
        }
    }

    private Button boton(String texto) {
        return raiz.lookupAll(".pizarra-control").stream().filter(n -> n instanceof Button b && texto.equals(b.getText()))
                .map(n -> (Button) n).findFirst().orElseThrow(() -> new AssertionError("no hay " + texto));
    }

    private double[] anchos() throws Exception {
        return enFx(() -> new double[] {herramientas.getWidth(), pizarra.getWidth()});
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

    /** Un evento del mouse sobre el divisor, {@code dx} px a la derecha de donde se pulsó. */
    private static void raton(Node n, javafx.event.EventType<MouseEvent> tipo, Point2D inicio, double dx) {
        Point2D p = new Point2D(inicio.getX() + dx, inicio.getY());
        Event.fireEvent(n, new MouseEvent(tipo, p.getX(), p.getY(), p.getX(), p.getY(), MouseButton.PRIMARY, 1,
                false, false, false, false, tipo != MouseEvent.MOUSE_RELEASED, false, false, false, false, false,
                null));
    }
}
