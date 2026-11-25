package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.editor.EditorMatematico;
import pe.mathnova.presentation.grafica.GraficaFuncion;
import pe.mathnova.presentation.grafica.VentanaGrafica;

class ResolverPizarraYEscrituraTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final AtomicInteger EN_VIVO = new AtomicInteger();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR)) {
        @Override
        public Optional<String> enVivo(String texto, Campo campo) {
            EN_VIVO.incrementAndGet();
            return super.enVivo(texto, campo);
        }
    };

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

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
    }

    @BeforeEach
    void abrir() throws Exception {
        enFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), new ReconocedorSimulado(0)));
                raiz = loader.load();
                controlador = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz, 1280, 900);
            editor = (EditorMatematico) raiz.lookup(".editor-matematico");
        });
        enFx(() -> editor.setLatex("x"));
        Fx.esperar(() -> "x".equals(editor.getLatex()), "el editor no cargó");
        enFx(() -> editor.setLatex(""));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.VACIO, "el editor no quedó vacío");
        EN_VIVO.set(0);
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    private static boolean seVe(Node n) {
        for (Node x = n; x != null; x = x.getParent()) {
            if (!x.isVisible()) {
                return false;
            }
        }
        return true;
    }

    @Test
    void laGraficaCubicaAparecePrimeroYConservaSuZoomAlVolver() throws Exception {
        enFx(() -> controlador.resolver("x^3 - 2*x^2 - x + 2"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió el cúbico");
        enFx(() -> {
            javafx.scene.layout.VBox vista = (javafx.scene.layout.VBox) raiz.lookup("#vistaGrafica");
            assertEquals("tarjetaGrafica", vista.getChildren().getFirst().getId());
            assertNull(raiz.lookup("#tarjetaSolucion"));
            GraficaFuncion grafica = (GraficaFuncion) raiz.lookup(".grafica-funcion");
            assertNotNull(grafica);
            assertTrue(seVe(grafica));
            assertTrue(seVe(raiz.lookup(".pizarra-paso")));
            raiz.applyCss();
            raiz.layout();
            assertTrue(grafica.getWidth() > 200, "gráfica sin ancho al consultar: " + grafica.getWidth());
            assertTrue(grafica.getHeight() > 200, "gráfica sin alto al consultar: " + grafica.getHeight());
            assertEquals(1, grafica.progresoProperty().get(), 1e-9);
            VentanaGrafica inicial = grafica.ventana();
            double x = (1 - inicial.xMin()) / inicial.ancho() * grafica.getWidth();
            double y = inicial.yMax() / inicial.alto() * grafica.getHeight();
            grafica.getOnMouseMoved().handle(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_MOVED,
                    x, y, x, y, javafx.scene.input.MouseButton.NONE, 0, false, false, false, false,
                    false, false, false, false, false, false, null));
            try {
                java.lang.reflect.Field cursor = GraficaFuncion.class.getDeclaredField("cursorX");
                cursor.setAccessible(true);
                assertEquals(1, cursor.getDouble(grafica), 0.03);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError(ex);
            }
            assertEquals(inicial, grafica.ventana());
            if (Boolean.getBoolean("mathnova.capturaGrafica")) {
                try {
                    java.nio.file.Path destino = java.nio.file.Path.of("target", "grafica-cubica-punto-1-0.png");
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(
                            raiz.snapshot(null, null), null), "png", destino.toFile());
                } catch (java.io.IOException ex) {
                    throw new java.io.UncheckedIOException(ex);
                }
            }
            javafx.scene.layout.HBox controles = (javafx.scene.layout.HBox) grafica.getChildrenUnmodifiable().get(2);
            ((Button) controles.getChildren().getFirst()).fire();
            VentanaGrafica elegida = grafica.ventana();
            assertNotEquals(inicial, elegida);
            ((ToggleButton) raiz.lookup("#pestanaAnalisis")).fire();
            ((ToggleButton) raiz.lookup("#pestanaGrafica")).fire();
            assertEquals(elegida, grafica.ventana());
            assertSame(grafica, raiz.lookup(".grafica-funcion"));
        });
    }

    @Test
    void yaNoHayLineaDeResultadoBajoElEditorNiSeCalculaNada() throws Exception {
        enFx(() -> {
            assertNull(raiz.lookup("#resultadoEnVivo"));
            assertNull(raiz.lookup(".resultado-en-vivo"));
            editor.setLatex("x^3-2x^2-x+2");
        });
        Thread.sleep(2 * (long) ResolucionController.ESPERA_ERROR.toMillis() + 300);
        enFx(() -> {
            assertEquals(EstadoResolucion.ESCRIBIENDO, controlador.estado());
            assertFalse(raiz.lookup("#botonResolver").isDisabled());
        });
        assertEquals(0, EN_VIVO.get(), "se calculó un resultado que nadie muestra");
    }

    @Test
    void alResolverLasNotasEstanOcultasYCadaPasoTieneSuBotonDeAyuda() throws Exception {
        enFx(() -> controlador.resolver("x^3 - 2x^2 - x + 2"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió");
        enFx(() -> {
            var pasos = raiz.lookupAll(".pizarra-paso");
            assertFalse(pasos.isEmpty());
            assertEquals(pasos.size(), raiz.lookupAll(".boton-nota").stream().filter(ResolverPizarraYEscrituraTest::seVe)
                    .count(), "un \"?\" por paso");
            assertEquals(0, raiz.lookupAll(".pizarra-nota").stream().filter(ResolverPizarraYEscrituraTest::seVe).count(),
                    "ninguna nota a la vista");
            assertEquals(pasos.size(), raiz.lookupAll(".pizarra-metodo").stream()
                    .filter(ResolverPizarraYEscrituraTest::seVe).count(), "cada paso con su método en la flecha");
            assertTrue(seVe(raiz.lookup(".pizarra-resultado-final")), "el resultado al final de la pizarra");
            assertEquals("FACTORIZA", ((javafx.scene.control.Label) raiz.lookup(".pizarra-rotulo")).getText());
        });
        assertEquals(0, EN_VIVO.get());
    }

    @Test
    void escribirAManoLlenaElEditorSoloPeroNoResuelve() throws Exception {
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        Node canvas = enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
        enFx(() -> {
            assertTrue(seVe(canvas));
            Fx.trazo(canvas, new double[] {20, 60}, new double[] {60, 100}, new double[] {100, 60});
        });
        Fx.esperar(() -> ReconocedorSimulado.LATEX.equals(editor.getLatex()), "el editor no recibió el ejercicio");
        enFx(() -> assertTrue(controlador.escritoAMano(), "falta \"✎ Escrito a mano\""));
        Thread.sleep(1000);
        enFx(() -> {
            EstadoResolucion e = controlador.estado();
            assertNotEquals(EstadoResolucion.CALCULANDO, e, "no se resuelve solo");
            assertNotEquals(EstadoResolucion.RESUELTO, e, "no se resuelve solo");
            assertNull(controlador.ultimaResolucion());
        });

        enFx(() -> editor.setLatex("x^2-1"));
        Fx.esperar(() -> !controlador.escritoAMano(), "\"Escrito a mano\" sigue tras editar");
        Fx.esperar(() -> !raiz.lookup("#botonResolver").isDisabled(), "Resolver no se activó");
        enFx(() -> assertNull(controlador.ultimaResolucion()));
        enFx(() -> ((Button) raiz.lookup("#botonResolver")).fire());
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió al pulsar Resolver");
    }

    @Test
    void resolverJustoDespuesDelUltimoTrazoEsperaLaLectura() throws Exception {
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        enFx(() -> assertTrue(raiz.lookup("#botonResolver").isDisabled(), "sin nada escrito, Resolver está apagado"));
        Node canvas = enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
        enFx(() -> {
            Fx.trazo(canvas, new double[] {20, 60}, new double[] {60, 100}, new double[] {100, 60});
            assertFalse(raiz.lookup("#botonResolver").isDisabled(), "Resolver activo con escritura sin leer");
            ((Button) raiz.lookup("#botonResolver")).fire();
        });
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió");
        enFx(() -> {
            assertEquals(ReconocedorSimulado.LATEX, editor.getLatex(), "se resolvió lo último escrito a mano");
            assertNotNull(controlador.ultimaResolucion());
        });
    }

    @Test
    void cambiarDePestanaConservaElEjercicio() throws Exception {
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        Node canvas = enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
        enFx(() -> Fx.trazo(canvas, new double[] {20, 60}, new double[] {60, 100}));
        Fx.esperar(() -> ReconocedorSimulado.LATEX.equals(editor.getLatex()), "el editor no recibió el ejercicio");
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaTeclado")).fire());
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        Thread.sleep(500);
        enFx(() -> {
            assertEquals(ReconocedorSimulado.LATEX, editor.getLatex());
            assertFalse(controlador.panelEscritura().leyendo(), "volver a la pestaña no relee");
        });
    }

    @Test
    void conElDedoLlenaElCampoYResuelve() throws Exception {
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        Node canvas = enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
        enFx(() -> Fx.trazoTactil(canvas, 1, new double[] {20, 60}, new double[] {60, 100}, new double[] {100, 60}));
        enFx(() -> assertFalse(raiz.lookup("#botonResolver").isDisabled(), "Resolver activo con lo escrito con el dedo"));
        Fx.esperar(() -> ReconocedorSimulado.LATEX.equals(editor.getLatex()), "lo escrito con el dedo no llegó al campo");
        enFx(() -> ((Button) raiz.lookup("#botonResolver")).fire());
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió lo escrito con el dedo");
    }

    @Test
    void laComprobacionSoloApareceAlFactorizarYEmpiezaCerrada() throws Exception {
        enFx(() -> controlador.resolver("x^2 - 5x + 6"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se factorizó");
        enFx(() -> {
            Node seccion = raiz.lookup(".pizarra-comprobacion-seccion");
            assertTrue(seVe(seccion), "una factorización lleva Comprobación");
            ToggleButton ver = (ToggleButton) raiz.lookup(".pizarra-ver-detalle");
            assertEquals("Ver comprobación", ver.getText());
            assertFalse(ver.isSelected());
            assertFalse(seVe(raiz.lookup("#zonaSugerencias")), "sin sugerencias de derivar, integrar o límite");
            ver.fire();
            assertTrue(seVe(raiz.lookup(".pizarra-comprobacion-detalle")));
        });
        Node primero = enFx(() -> ((javafx.scene.layout.VBox) raiz.lookup(".pizarra-filas")).getChildren().getFirst());
        enFx(() -> controlador.resolver("lim(x->2, (x^3 - 3x - 2)/(x - 2))"));
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO
                && ((javafx.scene.layout.VBox) raiz.lookup(".pizarra-filas")).getChildren().getFirst() != primero,
                "no se calculó el límite");
        enFx(() -> {
            assertFalse(seVe(raiz.lookup(".pizarra-comprobacion-seccion")), "un límite termina en el resultado");
            assertTrue(raiz.lookupAll(".pizarra-metodo").stream()
                    .anyMatch(n -> ((javafx.scene.control.Label) n).getText().equals("Sustituir en el límite")));
        });
    }
}
