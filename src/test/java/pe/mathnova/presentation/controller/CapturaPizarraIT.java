package pe.mathnova.presentation.controller;

import static pe.mathnova.presentation.Fx.enFx;

import java.nio.file.Files;
import java.nio.file.Path;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.infrastructure.escritura.ReconocedorSimulado;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.editor.EditorMatematico;

/**
 * Capturas de la Pizarra (ancha y angosta) de los ejercicios de referencia, en target/capturas-pizarra.
 * Solo con {@code -Dmathnova.capturaPizarra=true}; además, -Dmathnova.capturaPizarra.casos="a;b" elige otros.
 */
@EnabledIfSystemProperty(named = "mathnova.capturaPizarra", matches = "true")
class CapturaPizarraIT {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);
    private static final ResolverService RESOLVER = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static final class Preferencias extends ConfiguracionService {
        private final boolean explicaciones;

        Preferencias(boolean explicaciones) {
            super(null);
            this.explicaciones = explicaciones;
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
            return explicaciones;
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
        boolean explicaciones = Boolean.getBoolean("mathnova.capturaPizarra.explicaciones");
        enFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
                loader.setControllerFactory(c -> new ResolucionController(FACTORIZACION, RESOLVER,
                        new Preferencias(explicaciones), new ReconocedorSimulado(0)));
                raiz = loader.load();
                controlador = loader.getController();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            ventana = Fx.ventana(raiz, 1400, 950);
            editor = (EditorMatematico) raiz.lookup(".editor-matematico");
        });
        enFx(() -> editor.setLatex("x"));
        Fx.esperar(() -> "x".equals(editor.getLatex()), "el editor no cargó");
    }

    @AfterEach
    void cerrar() throws Exception {
        enFx(() -> ventana.close());
    }

    @Test
    void capturar() throws Exception {
        String casos = System.getProperty("mathnova.capturaPizarra.casos",
                "x^2 - 5x + 6;lim(x→3, (x^2 - 9)/(x - 3));d/dx((3x^2 + 1)^4);∫(x ln x, x)");
        Path carpeta = Path.of("target", System.getProperty("mathnova.capturaPizarra.carpeta", "capturas-pizarra"));
        Files.createDirectories(carpeta);
        int i = 0;
        for (String caso : casos.split(";")) {
            i++;
            String c = caso.strip();
            Object antes = enFx(() -> raiz.lookup(".pizarra-filas") == null ? null
                    : ((javafx.scene.layout.VBox) raiz.lookup(".pizarra-filas")).getChildren().isEmpty() ? null
                    : ((javafx.scene.layout.VBox) raiz.lookup(".pizarra-filas")).getChildren().getFirst());
            enFx(() -> controlador.resolver(c));
            Fx.esperar(() -> {
                var f = (javafx.scene.layout.VBox) raiz.lookup(".pizarra-filas");
                return controlador.estado() == EstadoResolucion.ERROR || f != null && !f.getChildren().isEmpty()
                        && f.getChildren().getFirst() != antes;
            }, "no cambió la pizarra con " + c);
            Fx.esperar(() -> controlador.estado() == EstadoResolucion.RESUELTO
                    || controlador.estado() == EstadoResolucion.ERROR, "no se resolvió " + c);
            String estado = enFx(() -> controlador.estado() + " " + controlador.aviso());
            Files.writeString(carpeta.resolve("estado.txt"), String.format("%02d %s -> %s%n", i, c, estado),
                    java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            for (double pos : new double[] {0.30, 0.62}) {
                int n = i;
                String nombre = String.format("%02d-%s.png", n, pos < 0.5 ? "ancha" : "angosta");
                enFx(() -> {
                    SplitPane division = (SplitPane) raiz.lookup(".division-trabajo");
                    division.setDividerPositions(pos);
                    raiz.applyCss();
                    raiz.layout();
                });
                Thread.sleep(400);
                enFx(() -> {
                    raiz.applyCss();
                    raiz.layout();
                    Node pizarra = raiz.lookup(".pizarra-resolucion");
                    expandir(pizarra);
                    raiz.applyCss();
                    raiz.layout();
                    try {
                        javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(
                                pizarra.snapshot(null, null), null), "png", carpeta.resolve(nombre).toFile());
                    } catch (java.io.IOException ex) {
                        throw new java.io.UncheckedIOException(ex);
                    }
                });
            }
        }
    }

    /** Abre los pasos internos y el detalle de la comprobación para que salgan en la captura. */
    private static void expandir(Node pizarra) {
        if (!Boolean.getBoolean("mathnova.capturaPizarra.expandir")) {
            return;
        }
        pizarra.lookupAll(".titled-pane").forEach(n -> ((javafx.scene.control.TitledPane) n).setExpanded(true));
        pizarra.lookupAll(".pizarra-ver-desarrollo").forEach(n -> ((javafx.scene.control.ToggleButton) n).setSelected(true));
        pizarra.lookupAll(".pizarra-ver-subpasos").forEach(n -> ((javafx.scene.control.ToggleButton) n).setSelected(true));
        pizarra.lookupAll(".pizarra-ver-detalle").forEach(n -> {
            var t = (javafx.scene.control.ToggleButton) n;
            if (t.isVisible()) {
                t.setSelected(true);
            }
        });
    }
}
