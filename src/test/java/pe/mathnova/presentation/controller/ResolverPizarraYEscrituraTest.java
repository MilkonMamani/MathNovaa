package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

/**
 * La pantalla "Resolver ejercicio" completa (resolucion.fxml, el editor MathLive y la Pizarra): ya no hay
 * línea de resultado en vivo, las notas empiezan ocultas y "Escribir" llena el editor sin resolver.
 */
class ResolverPizarraYEscrituraTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    /** Cuántas veces se pidió el resumen en una línea (la antigua línea en vivo). */
    private static final AtomicInteger EN_VIVO = new AtomicInteger();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR, null);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR)) {
        @Override
        public Optional<String> enVivo(String texto, Campo campo) {
            EN_VIVO.incrementAndGet();
            return super.enVivo(texto, campo);
        }
    };

    /** Sin base de datos ni animaciones, con las explicaciones ocultas. */
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
        // El editor está listo cuando devuelve lo que se le puso.
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
            assertTrue(seVe(raiz.lookup(".pizarra-resultado-doble")), "el resultado al final de la pizarra");
            assertEquals("FACTORIZA", ((javafx.scene.control.Label) raiz.lookup(".pizarra-rotulo")).getText());
        });
        assertEquals(0, EN_VIVO.get());
    }

    @Test
    void escribirAManoLlenaElEditorPeroNoResuelve() throws Exception {
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        Node canvas = enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
        enFx(() -> {
            assertTrue(seVe(canvas));
            Fx.trazo(canvas, new double[] {20, 60}, new double[] {60, 100}, new double[] {100, 60});
        });
        Fx.esperar(() -> seVe(raiz.lookup(".tarjeta-entendi")), "no apareció \"Lo que entendí\"");

        enFx(() -> {
            Button por = (Button) raiz.lookupAll(".boton-alternativa-simbolo").stream()
                    .filter(n -> "×".equals(((Button) n).getText())).findFirst().orElseThrow();
            por.fire();
            ((Button) raiz.lookup(".boton-usar-escritura")).fire();
        });
        Fx.esperar(() -> editor.getLatex().contains("\\times"), "el editor no recibió el ejercicio");
        enFx(() -> {
            assertNotEquals(ReconocedorSimulado.LATEX, editor.getLatex());
            assertTrue(controlador.escritoAMano(), "falta \"✎ Escrito a mano\"");
        });
        Thread.sleep(1000);
        enFx(() -> {
            EstadoResolucion e = controlador.estado();
            assertNotEquals(EstadoResolucion.CALCULANDO, e, "no se resuelve solo");
            assertNotEquals(EstadoResolucion.RESUELTO, e, "no se resuelve solo");
            assertNull(controlador.ultimaResolucion());
        });

        // Al editar con el teclado la etiqueta se va; Resolver sigue siendo cosa del estudiante.
        enFx(() -> editor.setLatex(ReconocedorSimulado.LATEX));
        Fx.esperar(() -> !controlador.escritoAMano(), "\"Escrito a mano\" sigue tras editar");
        Fx.esperar(() -> !raiz.lookup("#botonResolver").isDisabled(), "Resolver no se activó");
        enFx(() -> assertNull(controlador.ultimaResolucion()));
        enFx(() -> ((Button) raiz.lookup("#botonResolver")).fire());
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO, "no se resolvió al pulsar Resolver");
    }
}
