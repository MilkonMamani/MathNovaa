package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.presentation.Fx.enFx;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorUniMERNet;
import pe.mathnova.infrastructure.escritura.TrazosSimulados;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.editor.EditorMatematico;

/**
 * Recorrido completo con el MODELO REAL en la pantalla real: trazos SIMULADOS (no son escritura de una
 * persona) llegan al lienzo como eventos de mouse, el reconocedor los lee, el resultado aparece en el campo
 * de arriba y "Resolver" lo manda a la Pizarra. Cada caso se compara con la misma entrada escrita con el
 * teclado. Se corre con {@code -Dmathnova.it.unimernet=true}; deja target/reconocimiento/mathnova.tsv.
 */
@EnabledIfSystemProperty(named = "mathnova.it.unimernet", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EscrituraRealIT {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR, null);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));
    private static final Path SALIDA = Path.of("target", "reconocimiento");
    private static final StringBuilder TABLA = new StringBuilder(
            "caso\tlectura del modelo\tcampo superior\ttexto MathNova\tsolución (escritura)\tsolución (teclado)\tigual\n");

    private static ReconocedorUniMERNet real;
    /** Lo que devolvió el modelo en cada lectura, tal cual. */
    private static final List<String> LECTURAS = new CopyOnWriteArrayList<>();

    private Stage ventana;
    private Parent raiz;
    private ResolucionController controlador;
    private EditorMatematico editor;

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

    @BeforeAll
    static void arrancar() throws Exception {
        Fx.arrancar();
        Files.createDirectories(SALIDA);
        real = new ReconocedorUniMERNet("", 0, 300, 120);
    }

    @AfterAll
    static void terminar() throws Exception {
        Files.writeString(SALIDA.resolve("mathnova.tsv"), TABLA.toString(), StandardCharsets.UTF_8);
        long pid = real.pid();
        real.destroy();
        if (pid > 0) {
            assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false),
                    "al cerrar, el proceso de inferencia termina");
        }
    }

    @BeforeEach
    void abrir() throws Exception {
        ReconocedorEscritura espia = new ReconocedorEscritura() {
            @Override
            public ResultadoReconocimiento reconocer(List<Trazo> trazos) {
                ResultadoReconocimiento r = real.reconocer(trazos);
                LECTURAS.add(r.latex());
                return r;
            }

            @Override
            public void preparar() {
                real.preparar();
            }

            @Override
            public boolean listo() {
                return real.listo();
            }
        };
        if (ventana != null) {
            enFx(() -> ventana.close());
        }
        enFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(), espia));
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
        enFx(() -> ((ToggleButton) raiz.lookup("#pestanaEscribir")).fire());
        // La primera vez el modelo tarda en cargar.
        Fx.esperar(real::listo, "el modelo no cargó");
        LECTURAS.clear();
    }

    private Node canvas() throws Exception {
        return enFx(() -> raiz.lookup(".lienzo-escritura-canvas"));
    }

    private void dibujar(List<Trazo> trazos) throws Exception {
        Node c = canvas();
        for (Trazo t : trazos) {
            double[][] ps = t.puntos().stream().map(p -> new double[] {p.x(), p.y()}).toArray(double[][]::new);
            enFx(() -> Fx.trazo(c, ps));
            Thread.sleep(40); // el estudiante levanta el dedo entre trazos
        }
    }

    private void resolverYEsperar() throws Exception {
        enFx(() -> ((Button) raiz.lookup("#botonResolver")).fire());
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO
                || controlador.estado() == EstadoResolucion.ERROR, "no terminó de resolver");
    }

    private String solucion() throws Exception {
        return enFx(() -> controlador.estado() == EstadoResolucion.RESUELTO ? controlador.latexSolucion()
                : "ERROR: " + ((javafx.scene.control.Label) raiz.lookup("#avisoEntrada")).getText());
    }

    /** Dibuja, espera la lectura, resuelve; después lo mismo con el teclado. */
    private void caso(String nombre, BiFunction<Double, Long, List<Trazo>> trazos, String teclado) throws Exception {
        dibujar(trazos.apply(45.0, 1L));
        Fx.esperar(() -> !LECTURAS.isEmpty() && !controlador.panelEscritura().leyendo(), "no se reconoció");
        String campo = enFx(() -> editor.getLatex());
        String texto = enFx(() -> controlador.texto());
        resolverYEsperar();
        String deEscritura = solucion();

        enFx(() -> editor.setLatex(teclado));
        Fx.esperar(() -> !controlador.escritoAMano(), "no se escribió con el teclado");
        resolverYEsperar();
        String deTeclado = solucion();
        TABLA.append(nombre).append('\t').append(LECTURAS.getLast()).append('\t').append(campo).append('\t')
                .append(texto).append('\t').append(deEscritura).append('\t').append(deTeclado).append('\t')
                .append(deEscritura.equals(deTeclado)).append('\n');
    }

    @Test
    @Order(1)
    void casoA() throws Exception {
        caso("A", TrazosSimulados::casoA, "3x^2+2x-1");
    }

    @Test
    @Order(2)
    void casoB() throws Exception {
        caso("B", TrazosSimulados::casoB, "\\int_{\\placeholder{}}^{\\placeholder{}}2x\\,dx");
    }

    @Test
    @Order(3)
    void casoC() throws Exception {
        caso("C", TrazosSimulados::casoC, "\\int_0^12x\\,dx");
    }

    @Test
    @Order(4)
    void casoD() throws Exception {
        caso("D", TrazosSimulados::casoD, "\\frac{x+1}{x-1}");
    }

    @Test
    @Order(5)
    void casoE() throws Exception {
        caso("E", TrazosSimulados::casoE, "\\sqrt{x^2+1}");
    }

    @Test
    @Order(6)
    void resolverJustoDespuesDelUltimoTrazo() throws Exception {
        dibujar(TrazosSimulados.casoB(45.0, 2L));
        enFx(() -> ((Button) raiz.lookup("#botonResolver")).fire()); // antes de la pausa
        Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO
                || controlador.estado() == EstadoResolucion.ERROR, "no terminó de resolver");
        assertEquals(1, LECTURAS.size(), "se leyó una vez, lo último escrito");
        String s = solucion();
        TABLA.append("B-inmediato\t").append(LECTURAS.getLast()).append('\t').append(enFx(() -> editor.getLatex()))
                .append('\t').append(enFx(() -> controlador.texto())).append('\t').append(s).append("\t\t\n");
        assertTrue(enFx(() -> controlador.texto()).startsWith("∫("), "se resolvió la integral escrita a mano");
    }

    @Test
    @Order(7)
    void limpiarMientrasReconoceNoPoneNada() throws Exception {
        enFx(() -> editor.setLatex("x^2-1"));
        Fx.esperar(() -> "x^2-1".equals(enFxSilencioso()), "no se escribió");
        dibujar(TrazosSimulados.casoB(45.0, 3L));
        // Esperar a que empiece la lectura (pasó la pausa) y borrar todo en ese momento.
        Thread.sleep(800);
        enFx(() -> raiz.lookupAll(".herramienta-escritura").stream().map(n -> (javafx.scene.control.ButtonBase) n)
                .filter(b -> "Borrar todo".equals(b.getAccessibleText())).findFirst().orElseThrow().fire());
        Thread.sleep(3000); // la lectura en curso termina en el proceso y se descarta
        assertEquals("x^2-1", enFx(() -> editor.getLatex()), "el campo conserva la última entrada");
        TABLA.append("limpiar-durante-lectura\t\t").append(enFx(() -> editor.getLatex())).append("\t\t\t\t\n");
    }

    private String enFxSilencioso() {
        return editor.getLatex();
    }

    @Test
    @Order(8)
    void corregirEnElEditorYMoverElSeparador() throws Exception {
        dibujar(TrazosSimulados.casoB(45.0, 1L));
        Fx.esperar(() -> !LECTURAS.isEmpty() && !controlador.panelEscritura().leyendo(), "no se reconoció");
        // El estudiante corrige el integrando en el campo: 2x → 3x.
        String corregido = enFx(() -> editor.getLatex()).replace("2x", "3x");
        enFx(() -> editor.setLatex(corregido));
        // Mover el separador con la fórmula escrita: los trazos y el campo se conservan.
        int antes = enFx(() -> controlador.panelEscritura().lookupAll(".lienzo-escritura").size());
        SplitPane division = enFx(() -> (SplitPane) raiz.lookup(".split-pane"));
        if (division != null) {
            enFx(() -> division.setDividerPositions(0.3));
            Thread.sleep(300);
            enFx(() -> division.setDividerPositions(0.55));
            Thread.sleep(300);
        }
        assertEquals(1, antes);
        assertEquals(corregido, enFx(() -> editor.getLatex()), "la corrección se conserva");
        resolverYEsperar();
        TABLA.append("B-corregido\t").append(LECTURAS.getLast()).append('\t').append(corregido).append('\t')
                .append(enFx(() -> controlador.texto())).append('\t').append(solucion()).append("\t\t\n");
        assertTrue(enFx(() -> controlador.texto()).contains("3x"), "se resolvió la corrección");
    }
}
