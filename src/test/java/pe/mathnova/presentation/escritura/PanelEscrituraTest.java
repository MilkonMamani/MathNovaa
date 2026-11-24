package pe.mathnova.presentation.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;

/**
 * La pestaña "Escribir" sola. El reconocedor de estas pruebas es falso (no lee los trazos): sirve para
 * probar la sincronización con el campo de arriba, no el reconocimiento.
 */
class PanelEscrituraTest {

    private static final Duration PAUSA = Duration.millis(250);

    private Stage ventana;
    private PanelEscritura panel;
    /** Lo que llegó al campo de arriba, en orden. */
    private final List<String> puestos = new CopyOnWriteArrayList<>();
    private final AtomicLong generacion = new AtomicLong();
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
        ReconocedorEscritura espia = new ReconocedorEscritura() {
            @Override
            public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
                leidos.add(trazos);
                enHiloFx.add(javafx.application.Platform.isFxApplicationThread());
                return base.reconocer(trazos);
            }

            @Override
            public void preparar() {
                base.preparar();
            }

            @Override
            public boolean listo() {
                return base.listo();
            }
        };
        PanelEscritura.Destino destino = new PanelEscritura.Destino() {
            @Override
            public long generacion() {
                return generacion.get();
            }

            @Override
            public String poner(String latex) {
                puestos.add(latex);
                return null;
            }
        };
        if (ventana != null) {
            enFx(() -> ventana.close());
        }
        leidos.clear();
        enHiloFx.clear();
        puestos.clear();
        panel = enFx(() -> {
            PanelEscritura p = new PanelEscritura(espia, destino, PAUSA, null);
            ventana = Fx.ventana(new StackPane(p), 520, 640);
            return p;
        });
    }

    /** Un reconocedor que devuelve "n trazos" según cuántos recibe, y espera a que se lo suelte si se pide. */
    private static ReconocedorEscritura porCantidad(Function<Integer, CountDownLatch> espera) {
        return trazos -> {
            CountDownLatch l = espera.apply(trazos.size());
            if (l != null) {
                try {
                    l.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    // una lectura cancelada: igual devuelve, y el panel debe descartarla
                }
            }
            return new ResultadoReconocimiento("t" + trazos.size(), List.of());
        };
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

    /** El aviso a la vista ("" si no hay). Se llama en el hilo de JavaFX (dentro de esperar o enFx). */
    private String aviso() {
        return panel.filaEstado().isVisible() ? panel.estadoLectura().getText() : "";
    }

    private String estado() throws Exception {
        return enFx(this::aviso);
    }

    @Test
    void vacioMuestraLaIndicacionYLaPrimeraHerramientaEsElLapiz() throws Exception {
        enFx(() -> {
            assertTrue(panel.lienzo().isVacio());
            assertTrue(panel.lookup(".lienzo-vacio").isVisible());
            assertEquals(LienzoEscritura.Herramienta.LAPIZ, panel.lienzo().herramienta());
            assertEquals(220, panel.lienzo().getHeight(), 0.5);
            assertFalse(panel.filaEstado().isVisible());
            assertTrue(panel.lookup(".boton-usar-escritura") == null, "ya no hay que pulsar \"Usar este ejercicio\"");
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
    void trasLaPausaLoReconocidoVaSoloAlCampoDeArriba() throws Exception {
        trazo(20, 60, 40, 80, 60, 100);
        enFx(() -> assertTrue(puestos.isEmpty(), "todavía no pasó la pausa"));
        Fx.esperar(() -> !puestos.isEmpty(), "lo reconocido no llegó al campo");
        assertEquals(List.of(ReconocedorSimulado.LATEX), puestos);
        assertEquals(1, leidos.size());
        assertEquals(1, leidos.getFirst().size(), "un trazo");
        assertEquals(3, leidos.getFirst().getFirst().puntos().size(), "como lista de puntos");
        assertEquals(List.of(false), enHiloFx, "el reconocedor corre fuera del hilo de JavaFX");
        assertEquals(PanelEscritura.LISTO, estado());

        // El símbolo dudoso del simulado: elegir × vuelve a poner la expresión en el campo.
        enFx(() -> {
            Button por = (Button) panel.lookupAll(".boton-alternativa-simbolo").stream()
                    .filter(n -> "×".equals(((Button) n).getText())).findFirst().orElseThrow();
            por.fire();
        });
        assertEquals(2, puestos.size());
        assertTrue(puestos.get(1).contains("\\times"), puestos.get(1));
    }

    @Test
    void levantarElDedoEntreSimbolosNoLeeHastaLaPausa() throws Exception {
        trazo(20, 60, 60, 100);
        Thread.sleep(80);
        trazo(80, 60, 120, 100);
        Thread.sleep(80);
        trazo(140, 60, 180, 100);
        Fx.esperar(() -> !puestos.isEmpty(), "no se leyó");
        assertEquals(1, leidos.size(), "una sola lectura, con los tres trazos");
        assertEquals(3, leidos.getFirst().size());
    }

    @Test
    void unaRespuestaTardiaDeTrazosViejosSeDescarta() throws Exception {
        CountDownLatch primera = new CountDownLatch(1);
        abrirCon(porCantidad(n -> n == 1 ? primera : null));
        trazo(20, 60, 60, 100); // "2x"
        Fx.esperar(() -> PanelEscritura.LEYENDO.equals(aviso()), "no empezó a leer");
        trazo(80, 60, 120, 100); // "+1" mientras lee
        Fx.esperar(() -> puestos.contains("t2"), "la lectura nueva no llegó");
        primera.countDown(); // ahora llega tarde la lectura de "2x"
        Thread.sleep(300);
        assertEquals(List.of("t2"), puestos, "el campo conserva la interpretación más reciente");
    }

    @Test
    void unaCorreccionEnElCampoNoLaPisaUnaLecturaAnterior() throws Exception {
        CountDownLatch soltar = new CountDownLatch(1);
        abrirCon(porCantidad(n -> soltar));
        trazo(20, 60, 60, 100);
        Fx.esperar(() -> PanelEscritura.LEYENDO.equals(aviso()), "no empezó a leer");
        generacion.incrementAndGet(); // el estudiante corrige un símbolo en el campo de arriba
        soltar.countDown();
        Fx.esperar(() -> PanelEscritura.CONSERVA.equals(aviso()), "no avisó que conserva la corrección");
        assertTrue(puestos.isEmpty(), "la lectura vieja no reemplazó la corrección");

        // Un trazo nuevo sí se lee y se pone.
        trazo(80, 60, 120, 100);
        Fx.esperar(() -> puestos.contains("t2"), "el trazo nuevo no se leyó");
    }

    @Test
    void borrarTodoMientrasLeeInvalidaLaLectura() throws Exception {
        CountDownLatch soltar = new CountDownLatch(1);
        abrirCon(porCantidad(n -> soltar));
        trazo(20, 60, 60, 100);
        Fx.esperar(() -> PanelEscritura.LEYENDO.equals(aviso()), "no empezó a leer");
        ButtonBase borrarTodo = boton("Borrar todo");
        enFx(() -> borrarTodo.fire());
        soltar.countDown();
        Thread.sleep(400);
        assertTrue(puestos.isEmpty(), "la lectura de antes de borrar no llegó al campo");
        enFx(() -> {
            assertTrue(panel.lienzo().isVacio());
            assertFalse(panel.leyendo());
        });
    }

    @Test
    void deshacerYBorrarUnSimboloVuelvenALeer() throws Exception {
        abrirCon(porCantidad(n -> null));
        trazo(20, 60, 60, 100);
        trazo(100, 60, 140, 100);
        Fx.esperar(() -> puestos.contains("t2"), "no leyó los dos trazos");
        ButtonBase deshacer = boton("Deshacer");
        enFx(() -> deshacer.fire());
        enFx(() -> assertEquals(1, panel.lienzo().trazos().size()));
        Fx.esperar(() -> puestos.size() == 2 && "t1".equals(puestos.get(1)), "deshacer no volvió a leer");

        // Borrador sobre el segundo símbolo.
        trazo(100, 60, 140, 100);
        Fx.esperar(() -> puestos.size() == 3, "no leyó el trazo nuevo");
        ButtonBase borrador = boton("Borrador");
        enFx(() -> borrador.fire());
        double[] pasada = new double[22];
        for (int i = 0; i < 11; i++) {
            pasada[2 * i] = 95 + 5 * i;
            pasada[2 * i + 1] = 60 + 4 * i;
        }
        trazo(pasada);
        Fx.esperar(() -> puestos.size() == 4, "borrar un símbolo no volvió a leer");
        assertEquals("t1", puestos.get(3), "queda solo el primer símbolo");
    }

    @Test
    void siFallaSeConservanLosTrazosYSePuedeReintentar() throws Exception {
        AtomicInteger intentos = new AtomicInteger();
        abrirCon(trazos -> {
            if (intentos.incrementAndGet() == 1) {
                throw new IllegalStateException("el proceso se cerró");
            }
            return new ResultadoReconocimiento("x+1", List.of());
        });
        trazo(20, 60, 60, 100);
        Fx.esperar(() -> PanelEscritura.NO_SE_PUDO.equals(aviso()), "no avisó el fallo");
        enFx(() -> {
            assertEquals(1, panel.lienzo().trazos().size(), "los trazos siguen");
            assertTrue(panel.botonReintentar().isVisible());
        });
        assertTrue(puestos.isEmpty(), "el campo conserva la última entrada válida");
        enFx(() -> panel.botonReintentar().fire());
        Fx.esperar(() -> puestos.contains("x+1"), "reintentar no volvió a leer");
    }

    @Test
    void alTerminarEsperaLaLecturaDeLoUltimoEscrito() throws Exception {
        abrirCon(porCantidad(n -> null));
        trazo(20, 60, 60, 100);
        List<String> orden = new CopyOnWriteArrayList<>();
        // "Resolver" justo después del último trazo, antes de la pausa.
        enFx(() -> {
            assertTrue(panel.leyendo());
            panel.alTerminar(() -> orden.add("resolver con " + puestos), () -> orden.add("falló"));
        });
        Fx.esperar(() -> !orden.isEmpty(), "no se resolvió");
        assertEquals(List.of("resolver con [t1]"), orden, "se resolvió después de poner lo último");

        // Sin nada pendiente, corre enseguida.
        List<String> otra = new CopyOnWriteArrayList<>();
        enFx(() -> panel.alTerminar(() -> otra.add("ya"), () -> otra.add("falló")));
        assertEquals(List.of("ya"), otra);
    }

    @Test
    void alMostrarPreparaElReconocedorUnaSolaVez() throws Exception {
        AtomicInteger preparaciones = new AtomicInteger();
        CountDownLatch soltar = new CountDownLatch(1);
        boolean[] listo = {false};
        abrirCon(new ReconocedorEscritura() {
            @Override
            public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
                return new ResultadoReconocimiento("x", List.of());
            }

            @Override
            public void preparar() {
                preparaciones.incrementAndGet();
                try {
                    soltar.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                listo[0] = true;
            }

            @Override
            public boolean listo() {
                return listo[0];
            }
        });
        enFx(() -> panel.alMostrar());
        enFx(() -> panel.alMostrar());
        assertEquals(PanelEscritura.PREPARANDO, estado());
        soltar.countDown();
        Fx.esperar(() -> !panel.filaEstado().isVisible(), "el aviso de preparación no se fue");
        assertEquals(1, preparaciones.get());
    }
}
