package pe.mathnova.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import pe.mathnova.application.service.ConfiguracionService;
import pe.mathnova.application.service.ExportacionPdfService;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.HistorialService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.presentation.Fx;
import pe.mathnova.presentation.Navegador;

/**
 * Sprint 9, parte C: "Exportar PDF" desde la Pizarra y desde la hoja del Historial, con el diálogo y el
 * FileChooser reemplazados por respuestas fijas. El PDF se escribe de verdad. La pantalla escribe en su propio
 * hilo (sin la transacción de la prueba): el historial se vacía antes y después.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class ExportarPdfPantallaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);

    @Autowired
    private ExportacionPdfService exportacion;
    @Autowired
    private HistorialService historial;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private FactorizacionService factorizacion;
    @Autowired
    private JdbcTemplate jdbc;

    @TempDir
    Path carpeta;

    private Stage ventana;

    /** El diálogo y el FileChooser: anotan lo que se les pidió y responden lo que diga la prueba. */
    private static final class Respuestas implements ExportarPdf.Preguntas {
        final List<String> pedidos = new ArrayList<>();
        Boolean grafica;
        Path destino;

        @Override
        public Boolean incluirGrafica(Window v, boolean hayGrafica) {
            pedidos.add("gráfica disponible: " + hayGrafica);
            return grafica;
        }

        @Override
        public Path destino(Window v, String nombre) {
            pedidos.add("nombre: " + nombre);
            return destino;
        }
    }

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
    }

    @BeforeEach
    void vaciarAntes() {
        vaciar();
    }

    @AfterEach
    void cerrar() throws Exception {
        if (ventana != null) {
            Fx.enFx(() -> ventana.close());
        }
        vaciar();
    }

    private void vaciar() {
        jdbc.update("DELETE FROM resolucion");
        jdbc.update("DELETE FROM ejercicio");
    }

    private static ExportarPdf preparar(ExportarPdf e, Respuestas r) {
        e.setPreguntas(r);
        e.setEjecutor(t -> t.run());
        e.setHoy(() -> HOY);
        return e;
    }

    private static int imagenes(Path pdf) throws Exception {
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            int n = 0;
            for (PDPage p : d.getPages()) {
                for (COSName c : p.getResources().getXObjectNames()) {
                    if (p.getResources().getXObject(c) instanceof PDImageXObject) {
                        n++;
                    }
                }
            }
            return n;
        }
    }

    // ---------------------------------------------------------------- Resolver

    @Test
    void desdeLaPizarraConLaGrafica() throws Exception {
        ResolucionController[] c = new ResolucionController[1];
        Parent[] raiz = new Parent[1];
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/resolucion.fxml"));
            loader.setControllerFactory(k -> {
                ResolucionController r = new ResolucionController(factorizacion, resolver, new SinAnimaciones());
                r.setExportacion(exportacion);
                return r;
            });
            try {
                raiz[0] = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            c[0] = loader.getController();
            ventana = Fx.ventana(raiz[0], 1280, 900);
        });
        Fx.esperar(() -> raiz[0].lookup(".editor-matematico") != null, "el editor no cargó");
        Button descargar = Fx.enFx(() -> (Button) raiz[0].lookup(".pizarra-exportar"));
        assertTrue(Fx.enFx(descargar::isDisabled), "sin resultado no hay nada que exportar");

        Fx.enFx(() -> c[0].resolver("x^3 - 2x^2 - x + 2"));
        Fx.esperar(() -> c[0].estado() == EstadoResolucion.RESUELTO && !descargar.isDisabled(), "no se resolvió");

        Fx.enFx(() -> {
            try {
                java.io.File f = new java.io.File("target/capturas/resolver-exportar.png");
                f.getParentFile().mkdirs();
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(
                        raiz[0].snapshot(null, null), null), "png", f);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
        Respuestas r = new Respuestas();
        r.grafica = true;
        r.destino = carpeta.resolve("mi-factorizacion");
        preparar(c[0].exportador(), r);
        Fx.enFx(descargar::fire);

        Path pdf = carpeta.resolve("mi-factorizacion.pdf");
        Fx.esperar(() -> c[0].exportador().ultimoAviso() != null, "no terminó de exportar");
        assertEquals(List.of("gráfica disponible: true", "nombre: mathnova-factorizar-2026-11-24.pdf"), r.pedidos);
        assertEquals("PDF guardado en " + pdf.toAbsolutePath(), c[0].exportador().ultimoAviso());
        assertTrue(Files.size(pdf) > 1000);
        assertEquals(1, imagenes(pdf), "la gráfica, porque se marcó Incluir gráfica");
        capturar(pdf, "pdf-resolver");
    }

    /** Cada página del PDF en target/capturas, para revisarlo a ojo. */
    private static void capturar(Path pdf, String nombre) throws Exception {
        java.io.File salida = new java.io.File("target/capturas");
        salida.mkdirs();
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            var render = new org.apache.pdfbox.rendering.PDFRenderer(d);
            for (int i = 0; i < d.getNumberOfPages(); i++) {
                javax.imageio.ImageIO.write(render.renderImageWithDPI(i, 80), "png",
                        new java.io.File(salida, nombre + "-" + (i + 1) + ".png"));
            }
        }
    }

    @Test
    void cancelarElDialogoNoGuardaNada() throws Exception {
        Respuestas r = new Respuestas();
        r.grafica = null; // Cancelar
        ExportarPdf e = preparar(new ExportarPdf(exportacion), r);
        Fx.enFx(() -> {
            Button b = new Button();
            ventana = Fx.ventana(new javafx.scene.layout.StackPane(b), 300, 200);
            e.exportar(b, hoy -> exportacion.desdeResolver("x^2 - 4", Campo.Q, null, hoy), null);
        });
        // El documento se arma en una tarea: el diálogo llega en un pulso posterior
        Fx.esperar(() -> !r.pedidos.isEmpty(), "no se abrió el diálogo");
        assertEquals(List.of("gráfica disponible: false"), r.pedidos);
        assertNull(Fx.enFx(e::ultimoAviso));
        try (var archivos = Files.list(carpeta)) {
            assertEquals(0, archivos.count());
        }
    }

    // ---------------------------------------------------------------- Historial

    @Test
    void desdeLaHojaDelHistorialSinGrafica() throws Exception {
        resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null);
        HistorialController[] c = new HistorialController[1];
        Parent[] raiz = new Parent[1];
        Fx.enFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/historial.fxml"));
            loader.setControllerFactory(k -> {
                HistorialController h = new HistorialController(historial, new Navegador(null));
                h.setExportacion(exportacion);
                return h;
            });
            try {
                raiz[0] = loader.load();
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            c[0] = loader.getController();
            c[0].setEjecutor(javafx.concurrent.Task::run);
            ventana = Fx.ventana(raiz[0], 1280, 800);
        });
        Button exportar = Fx.enFx(() -> (Button) raiz[0].lookup("#exportarPdf"));
        Fx.enFx(() -> raiz[0].lookupAll(".boton-icono-historial").stream().map(Button.class::cast)
                .filter(b -> "Ver la hoja con todos los pasos".equals(b.getAccessibleText())).findFirst()
                .orElseThrow().fire());
        Fx.esperar(() -> !exportar.isDisabled(), "Exportar PDF no se activó con la hoja");

        Respuestas r = new Respuestas();
        r.grafica = false;
        r.destino = carpeta.resolve("historial.pdf");
        preparar(c[0].exportador(), r);
        Fx.enFx(exportar::fire);
        Fx.esperar(() -> c[0].exportador().ultimoAviso() != null, "no terminó de exportar");

        assertEquals(List.of("gráfica disponible: false", "nombre: mathnova-factorizar-2026-11-24.pdf"), r.pedidos);
        Path pdf = carpeta.resolve("historial.pdf");
        assertEquals("PDF guardado en " + pdf.toAbsolutePath(), c[0].exportador().ultimoAviso());
        assertTrue(Files.size(pdf) > 1000);
        assertEquals(0, imagenes(pdf));
        assertFalse(Fx.enFx(exportar::isDisabled));
    }

    @Test
    void laExtensionSeAgregaSiFalta() {
        assertEquals(Path.of("a/b.pdf"), ExportarPdf.conExtension(Path.of("a/b")));
        assertEquals(Path.of("a/b.PDF"), ExportarPdf.conExtension(Path.of("a/b.PDF")));
    }
}
