package pe.mathnova.presentation.controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pe.mathnova.application.pdf.DocumentoPdf;
import pe.mathnova.application.service.ExportacionPdfService;

/**
 * "Exportar PDF" de la Pizarra y de la hoja del Historial (Sprint 9, parte C), el mismo recorrido en las dos:
 * se arma el documento (fuera del hilo de la interfaz), el diálogo pregunta si se incluye la gráfica, el
 * FileChooser propone mathnova-&lt;operación&gt;-&lt;fecha&gt;.pdf, se escribe el archivo en otro hilo y abajo
 * aparece "PDF guardado en &lt;ruta&gt;" unos segundos.
 */
public final class ExportarPdf {

    private static final Logger LOG = LoggerFactory.getLogger(ExportarPdf.class);

    static final String GUARDADO = "PDF guardado en ";
    static final String SIN_GRAFICA = "Esta resolución no tiene gráfica.";
    static final Duration DURACION_AVISO = Duration.seconds(4);

    /** El diálogo y el FileChooser; las pruebas ponen respuestas fijas. */
    public interface Preguntas {

        /** @return si se incluye la gráfica; null si se canceló */
        Boolean incluirGrafica(Window ventana, boolean hayGrafica);

        /** @return dónde guardar; null si se canceló */
        Path destino(Window ventana, String nombrePropuesto);
    }

    private final ExportacionPdfService servicio;
    private Preguntas preguntas = new PreguntasReales();
    /** Para las pruebas: corre las tareas (por omisión, en un hilo aparte). */
    private Consumer<Task<?>> ejecutor = t -> {
        Thread hilo = new Thread(t, "exportar-pdf");
        hilo.setDaemon(true);
        hilo.start();
    };
    private Supplier<LocalDate> hoy = LocalDate::now;
    private boolean ocupado;
    private String ultimoAviso;

    public ExportarPdf(ExportacionPdfService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    /**
     * Empieza la exportación.
     *
     * @param origen  el botón (para la ventana y para ubicar el aviso)
     * @param armar   arma el documento; corre fuera del hilo de la interfaz
     * @param grafica la gráfica en PNG, en el hilo de la interfaz; null si no hay gráfica
     */
    public void exportar(Node origen, java.util.function.Function<LocalDate, DocumentoPdf> armar,
                         Supplier<byte[]> grafica) {
        if (ocupado) {
            return;
        }
        ocupado = true;
        LocalDate fecha = hoy.get();
        Task<DocumentoPdf> preparar = new Task<>() {
            @Override
            protected DocumentoPdf call() {
                return armar.apply(fecha);
            }
        };
        preparar.setOnSucceeded(e -> preguntar(origen, preparar.getValue(), fecha, grafica));
        preparar.setOnFailed(e -> fallo(origen, preparar.getException()));
        ejecutor.accept(preparar);
    }

    private void preguntar(Node origen, DocumentoPdf documento, LocalDate fecha, Supplier<byte[]> grafica) {
        Window ventana = origen.getScene() == null ? null : origen.getScene().getWindow();
        Boolean conGrafica = preguntas.incluirGrafica(ventana, grafica != null);
        if (conGrafica == null) {
            ocupado = false;
            return;
        }
        Path destino = preguntas.destino(ventana, ExportacionPdfService.nombreArchivo(documento.verbo(), fecha));
        if (destino == null) {
            ocupado = false;
            return;
        }
        Path archivo = conExtension(destino);
        byte[] png = null;
        if (conGrafica && grafica != null) {
            try {
                png = grafica.get();
            } catch (RuntimeException ex) {
                LOG.warn("No se pudo dibujar la gráfica para el PDF", ex);
            }
        }
        DocumentoPdf final_ = documento.conGrafica(png);
        Task<Path> escribir = new Task<>() {
            @Override
            protected Path call() throws IOException {
                servicio.exportar(final_, archivo);
                return archivo;
            }
        };
        escribir.setOnSucceeded(e -> {
            ocupado = false;
            avisar(origen, GUARDADO + archivo.toAbsolutePath());
        });
        escribir.setOnFailed(e -> fallo(origen, escribir.getException()));
        ejecutor.accept(escribir);
    }

    private void fallo(Node origen, Throwable causa) {
        ocupado = false;
        LOG.warn("No se pudo exportar el PDF", causa);
        String motivo = causa == null || causa.getMessage() == null ? "error desconocido" : causa.getMessage();
        avisar(origen, "No se pudo guardar el PDF: " + motivo);
    }

    /** "ejercicio" → "ejercicio.pdf"; "ejercicio.PDF" se deja. */
    static Path conExtension(Path p) {
        String nombre = p.getFileName().toString();
        return nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") ? p : p.resolveSibling(nombre + ".pdf");
    }

    // ---------------------------------------------------------------- aviso abajo

    /** El mensaje corto abajo, al centro de la ventana; se desvanece solo. */
    private void avisar(Node origen, String texto) {
        ultimoAviso = texto;
        AvisoBreve.mostrar(origen, texto, DURACION_AVISO);
    }

    // ---------------------------------------------------------------- gráfica

    /**
     * Dibuja un nodo nuevo fuera de la pantalla (la pestaña Gráfica puede estar oculta) y lo devuelve en PNG, al
     * doble de resolución para que se imprima bien.
     */
    static byte[] png(Parent grafica, double ancho, double alto, java.util.List<String> estilos) {
        // En papel no hay con qué pulsar: fuera los botones de zoom y "Restablecer vista".
        grafica.applyCss();
        for (Node control : grafica.lookupAll(".grafica-control")) {
            control.setVisible(false);
        }
        StackPane fondo = new StackPane(grafica);
        fondo.setStyle("-fx-background-color: white;");
        Scene escena = new Scene(fondo, ancho, alto);
        escena.getStylesheets().setAll(estilos);
        fondo.applyCss();
        fondo.layout();
        SnapshotParameters p = new SnapshotParameters();
        p.setTransform(Transform.scale(2, 2));
        var imagen = fondo.snapshot(p, null);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(SwingFXUtils.fromFXImage(imagen, null), "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------- diálogo y FileChooser

    /** El diálogo "Exportar PDF" con "Incluir gráfica" y el FileChooser del sistema. */
    static final class PreguntasReales implements Preguntas {

        @Override
        public Boolean incluirGrafica(Window ventana, boolean hayGrafica) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Exportar PDF");
            d.setHeaderText(null);
            if (ventana != null) {
                d.initOwner(ventana);
                if (ventana.getScene() != null) {
                    d.getDialogPane().getStylesheets().setAll(ventana.getScene().getStylesheets());
                }
            }
            Label titulo = new Label("Exportar PDF");
            titulo.getStyleClass().add("titulo-tarjeta");
            Label texto = new Label("Se guardan los pasos con sus explicaciones, el resultado y la comprobación, "
                    + "en una hoja A4.");
            texto.getStyleClass().add("subtitulo-tarjeta");
            texto.setWrapText(true);
            CheckBox grafica = new CheckBox("Incluir gráfica");
            grafica.getStyleClass().add("exportar-grafica");
            grafica.setDisable(!hayGrafica);
            VBox caja = new VBox(12, titulo, texto, grafica);
            if (!hayGrafica) {
                Label sin = new Label(SIN_GRAFICA);
                sin.getStyleClass().add("subtitulo-tarjeta");
                caja.getChildren().add(sin);
            }
            caja.setPrefWidth(400);
            caja.getStyleClass().add("dialogo-exportar");
            d.getDialogPane().setContent(caja);
            ButtonType guardar = new ButtonType("Elegir dónde guardar", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
            d.getDialogPane().getButtonTypes().setAll(cancelar, guardar);
            d.getDialogPane().lookupButton(guardar).getStyleClass().add("boton-primario");
            d.getDialogPane().lookupButton(cancelar).getStyleClass().add("boton-secundario");
            return d.showAndWait().filter(b -> b == guardar).map(b -> grafica.isSelected()).orElse(null);
        }

        @Override
        public Path destino(Window ventana, String nombrePropuesto) {
            FileChooser fc = new FileChooser();
            fc.setTitle("Guardar PDF");
            fc.setInitialFileName(nombrePropuesto);
            fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("Documento PDF (*.pdf)", "*.pdf"));
            File documentos = new File(System.getProperty("user.home"), "Documents");
            fc.setInitialDirectory(documentos.isDirectory() ? documentos : new File(System.getProperty("user.home")));
            File f = fc.showSaveDialog(ventana);
            return f == null ? null : f.toPath();
        }
    }

    ExportacionPdfService servicio() {
        return servicio;
    }

    // ---------------------------------------------------------------- pruebas

    void setPreguntas(Preguntas preguntas) {
        this.preguntas = preguntas;
    }

    void setEjecutor(Consumer<Task<?>> ejecutor) {
        this.ejecutor = ejecutor;
    }

    void setHoy(Supplier<LocalDate> hoy) {
        this.hoy = hoy;
    }

    /** El último mensaje de abajo ("PDF guardado en …"); null si no hubo. */
    String ultimoAviso() {
        return ultimoAviso;
    }
}
