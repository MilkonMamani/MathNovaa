package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;
import pe.mathnova.presentation.editor.EditorMatematico;

/**
 * La barra lateral compacta y desplegada (principal.fxml) con la pantalla "Resolver ejercicio" real en
 * el centro: orden de las cinco opciones, un clic abre la sección, la selección no se pierde y alternar
 * el menú no toca el ejercicio. Con -Dmathnova.capturasBarra=carpeta guarda capturas de ambos estados.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BarraLateralTest {

    private static final PseudoClass SELECCIONADO = PseudoClass.getPseudoClass("seleccionado");
    private static final List<String> ORDEN = List.of("Resolver", "Historial", "Práctica", "Ejemplos",
            "Configuración");
    private static final String EJERCICIO = "x^3-2x^2-x+2";

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static final class SinAnimaciones extends ConfiguracionService {
        SinAnimaciones() {
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
    }

    /** Navegador de prueba: las dos acciones existentes, sin Spring. */
    private static final class NavegadorPrueba extends Navegador {
        final List<String> llamadas = new ArrayList<>();
        final StackPane configuracion = new StackPane();

        NavegadorPrueba() {
            super(null);
        }

        @Override
        public void mostrarEntrada() {
            llamadas.add("Resolver");
            principal.mostrar(resolucion);
            principal.seleccionar("Resolver");
        }

        @Override
        public void mostrarConfiguracion() {
            llamadas.add("Configuración");
            principal.mostrar(configuracion);
            principal.seleccionar("Configuración");
        }
    }

    private static final NavegadorPrueba NAVEGADOR = new NavegadorPrueba();
    private static PrincipalController principal;
    private static ResolucionController resolucionControlador;
    private static Parent resolucion;
    private static BorderPane raiz;
    private static Stage ventana;

    @BeforeAll
    static void abrir() throws Exception {
        Fx.arrancar();
        Fx.enFx(() -> {
            resolucion = cargar("/fxml/resolucion.fxml");
            raiz = (BorderPane) cargar("/fxml/principal.fxml");
            principal.mostrar(resolucion);
            ventana = Fx.ventana(raiz, 1280, 800);
        });
        EditorMatematico editor = Fx.enFx(() -> (EditorMatematico) raiz.lookup(".editor-matematico"));
        assertNotNull(editor);
        // El editor está listo cuando devuelve lo que se le puso.
        Fx.enFx(() -> editor.setLatex("x"));
        Fx.esperar(() -> "x".equals(editor.getLatex()), "El editor no cargó");
    }

    private static Parent cargar(String fxml) {
        FXMLLoader loader = new FXMLLoader(BarraLateralTest.class.getResource(fxml));
        loader.setControllerFactory(c -> c == PrincipalController.class ? new PrincipalController(NAVEGADOR)
                : new ResolucionController(FACTORIZACION, RESOLVER, new SinAnimaciones()));
        try {
            Parent p = loader.load();
            if (loader.getController() instanceof PrincipalController pc) {
                principal = pc;
            } else {
                resolucionControlador = loader.getController();
            }
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @AfterAll
    static void cerrar() throws Exception {
        if (ventana != null) {
            Fx.enFx(() -> ventana.close());
        }
    }

    private static Region barra() {
        return (Region) raiz.getLeft();
    }

    private static List<Button> opciones() {
        return barra().lookupAll(".nav-item").stream().map(Button.class::cast)
                .filter(b -> !b.getStyleClass().contains("nav-alternar")).toList();
    }

    private static Button alternar() {
        return (Button) barra().lookup(".nav-alternar");
    }

    private static void refrescar() {
        raiz.applyCss();
        raiz.layout();
    }

    private static double xIcono(Button b) {
        return b.getGraphic().localToScene(0, 0).getX();
    }

    @Test
    @Order(1)
    void alAbrirEsCompactaConLasCincoOpcionesEnOrden() throws Exception {
        Fx.enFx(() -> {
            refrescar();
            assertFalse(principal.estaDesplegado());
            assertEquals(56, barra().getWidth(), 0.5);
            assertNotNull(alternar());
            assertEquals(ORDEN, opciones().stream().map(Button::getText).toList());
            for (Button b : opciones()) {
                assertEquals(ContentDisplay.GRAPHIC_ONLY, b.getContentDisplay(), b.getText());
                assertEquals(44, b.getHeight(), 0.5, b.getText());
                assertEquals(56 - 1, b.getWidth(), 0.5, "toda la fila responde: " + b.getText());
                // ícono de 18 × 18 sobre una caja de 24, centrada en la barra
                assertEquals(24, ((Region) b.getGraphic()).getWidth(), 0.01);
                assertEquals(16, xIcono(b), 0.5, b.getText());
            }
            assertEquals(16, xIcono(alternar()), 0.5, "las tres rayas van sobre el mismo eje");
            assertTrue(opciones().get(0).getPseudoClassStates().contains(SELECCIONADO));
        });
    }

    @Test
    @Order(2)
    void unClicEnElIconoAbreLaSeccionDesdeCompacto() throws Exception {
        Fx.enFx(() -> {
            refrescar();
            Button config = opciones().get(4);
            Fx.trazo(config, new double[] {28, 22});
            assertEquals(List.of("Configuración"), NAVEGADOR.llamadas);
            assertTrue(config.getPseudoClassStates().contains(SELECCIONADO));
            Fx.trazo(opciones().get(0), new double[] {28, 22});
            assertEquals(List.of("Configuración", "Resolver"), NAVEGADOR.llamadas);
            assertTrue(principal.muestra(resolucion));
            // Desde el Sprint 9 (Historial, Práctica y Ejemplos) todas las opciones funcionan
            assertTrue(opciones().stream().noneMatch(Node::isDisabled));
        });
    }

    @Test
    @Order(3)
    void desplegarMuestraLosNombresSinMoverLosIconosYConservaLaSeleccion() throws Exception {
        Fx.enFx(() -> {
            principal.seleccionar("Configuración");
            alternar().fire();
            refrescar();
            assertTrue(principal.estaDesplegado());
            assertEquals(184, barra().getWidth(), 0.5);
            for (Button b : opciones()) {
                assertEquals(ContentDisplay.LEFT, b.getContentDisplay());
                Text texto = (Text) b.lookup(".text");
                assertEquals(b.getText(), texto.getText(), "nombre completo, sin recorte");
                assertEquals(16, xIcono(b), 0.5, "el ícono no se mueve: " + b.getText());
                double xTexto = texto.localToScene(texto.getLayoutBounds()).getMinX();
                assertEquals(16 + 24 + 12, xTexto, 1.5, "12 px entre ícono y nombre: " + b.getText());
            }
            assertTrue(opciones().get(4).getPseudoClassStates().contains(SELECCIONADO));
            alternar().fire();
            refrescar();
            assertEquals(56, barra().getWidth(), 0.5);
            assertTrue(opciones().get(4).getPseudoClassStates().contains(SELECCIONADO));
            principal.seleccionar("Resolver");
        });
    }

    @Test
    @Order(4)
    void alternarVariasVecesConservaElEjercicioResuelto() throws Exception {
        Fx.enFx(() -> resolucionControlador.resolver(EJERCICIO));
        Fx.esperar(() -> resolucionControlador.estado() == EstadoResolucion.RESUELTO, "No se resolvió");
        Fx.esperar(() -> resolucionControlador.texto().replace(" ", "").contains("x^3"), "El editor no muestra x^3");
        Object resultado = Fx.enFx(() -> resolucionControlador.ultimaResolucion());
        String texto = Fx.enFx(() -> resolucionControlador.texto());
        List<Node> pasos = Fx.enFx(() -> List.copyOf(pizarraPasos()));
        // un paso intermedio de la pizarra: "anterior" una vez
        String paso = Fx.enFx(() -> {
            Button anterior = (Button) resolucion.lookup(".pizarra-flecha-nav");
            if (!anterior.isDisabled()) {
                anterior.fire();
            }
            return etiquetaPaso();
        });
        assertNotNull(resultado);
        assertFalse(pasos.isEmpty());
        double centroCompacto = Fx.enFx(() -> ((Region) raiz.getCenter()).getWidth());

        capturar("barra-compacta.png");
        for (int i = 0; i < 5; i++) {
            Fx.enFx(() -> {
                alternar().fire();
                refrescar();
            });
        }
        assertTrue(principal.estaDesplegado());
        capturar("barra-desplegada.png");
        double centroDesplegado = Fx.enFx(() -> ((Region) raiz.getCenter()).getWidth());
        assertEquals(184 - 56, centroCompacto - centroDesplegado, 0.5, "el centro aprovecha el espacio");

        Fx.enFx(() -> {
            assertSame(resultado, resolucionControlador.ultimaResolucion());
            assertEquals(texto, resolucionControlador.texto());
            assertEquals(EstadoResolucion.RESUELTO, resolucionControlador.estado());
            assertEquals(pasos, pizarraPasos(), "los mismos pasos en la pizarra");
            assertEquals(paso, etiquetaPaso(), "el mismo paso visible");
            assertTrue(principal.muestra(resolucion));
            assertTrue(opciones().get(0).getPseudoClassStates().contains(SELECCIONADO));
            alternar().fire();
        });
    }

    @Test
    @Order(5)
    void enElAnchoMinimoNoSeDesborda() throws Exception {
        Fx.enFx(() -> {
            ventana.setWidth(860);
            ventana.setHeight(600);
        });
        Fx.esperar(() -> raiz.getWidth() <= 860, "La ventana no se achicó");
        for (boolean desplegada : new boolean[] {false, true}) {
            Fx.enFx(() -> {
                if (principal.estaDesplegado() != desplegada) {
                    alternar().fire();
                }
                refrescar();
                double centro = ((Region) raiz.getCenter()).getWidth();
                assertEquals(raiz.getWidth() - barra().getWidth(), centro, 1);
                assertTrue(principal.muestra(resolucion));
            });
            capturar(desplegada ? "barra-desplegada-860.png" : "barra-compacta-860.png");
        }
        Fx.enFx(() -> alternar().fire());
    }

    private static String etiquetaPaso() {
        return ((javafx.scene.control.Label) resolucion.lookup(".pizarra-etiqueta-pasos")).getText();
    }

    private static List<Node> pizarraPasos() {
        return resolucion.lookupAll(".pizarra-resolucion").stream().findFirst()
                .map(p -> ((pe.mathnova.presentation.resolucion.PizarraResolucion) p).filasPasos())
                .orElseGet(() -> List.copyOf(resolucion.lookupAll(".fila-paso")));
    }

    private static void capturar(String nombre) throws Exception {
        String carpeta = System.getProperty("mathnova.capturasBarra");
        if (carpeta == null) {
            return;
        }
        Thread.sleep(700); // que el WebView del editor pinte la fórmula
        var imagen = Fx.enFx(() -> raiz.snapshot(null, null));
        File archivo = new File(carpeta, nombre);
        archivo.getParentFile().mkdirs();
        ImageIO.write(SwingFXUtils.fromFXImage(imagen, null), "png", archivo);
    }
}
