package pe.mathnova.presentation.escritura;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import pe.mathnova.domain.escritura.Muestra;
import pe.mathnova.infrastructure.escritura.ArchivoMuestras;

final class BarraMuestras extends VBox {

    static final String PROPIEDAD = "mathnova.muestras";

    private final LienzoEscritura lienzo;
    private final ArchivoMuestras archivo;
    private final Runnable alCargar;
    private final TextField etiqueta = new TextField();
    private final Label estado = new Label();

    BarraMuestras(LienzoEscritura lienzo, ArchivoMuestras archivo, Runnable alCargar) {
        this.lienzo = lienzo;
        this.archivo = archivo;
        this.alCargar = alCargar;
        getStyleClass().add("barra-muestras");
        setSpacing(6);
        setStyle("-fx-border-color: #F59E0B; -fx-border-style: dashed; -fx-border-radius: 8;"
                + " -fx-padding: 8; -fx-background-color: #FFFBEB; -fx-background-radius: 8;");

        Label titulo = new Label("DESARROLLO · Muestras etiquetadas a mano (no es reconocimiento)");
        titulo.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #92400E;");

        etiqueta.setPromptText("Etiqueta correcta, p. ej. 2x");
        etiqueta.setAccessibleText("Etiqueta de la muestra");
        etiqueta.setPrefColumnCount(12);
        Button guardar = new Button("Guardar muestra");
        guardar.setOnAction(e -> guardar());
        Button cargar = new Button("Cargar muestra…");
        cargar.setOnAction(e -> elegirYCargar());
        FlowPane fila = new FlowPane(6, 6, etiqueta, guardar, cargar);
        fila.setAlignment(Pos.CENTER_LEFT);

        estado.setWrapText(true);
        estado.setStyle("-fx-font-size: 11px; -fx-text-fill: #78350F;");
        estado.setText("Carpeta: " + archivo.carpeta());

        getChildren().addAll(titulo, fila, estado);
    }

    Path guardar() {
        String texto = etiqueta.getText() == null ? "" : etiqueta.getText().strip();
        if (texto.isEmpty()) {
            estado.setText("Escribe la etiqueta correcta antes de guardar.");
            return null;
        }
        Muestra m = lienzo.muestra(texto);
        if (m.trazos().isEmpty()) {
            estado.setText("El lienzo está vacío: no hay nada que guardar.");
            return null;
        }
        try {
            Path guardado = archivo.guardar(m);
            estado.setText("Guardada «" + texto + "», " + m.trazos().size() + " trazos: " + guardado);
            return guardado;
        } catch (IOException ex) {
            estado.setText("No se pudo guardar: " + ex.getMessage());
            return null;
        }
    }

    boolean cargar(Path ruta) {
        try {
            Muestra m = ArchivoMuestras.leer(ruta);
            lienzo.cargar(m);
            etiqueta.setText(m.etiqueta());
            estado.setText("Cargada «" + m.etiqueta() + "» (etiqueta del desarrollador), "
                    + m.trazos().size() + " trazos: " + ruta.getFileName());
            alCargar.run();
            return true;
        } catch (IOException | RuntimeException ex) {
            estado.setText("No se pudo cargar: " + ex.getMessage());
            return false;
        }
    }

    private void elegirYCargar() {
        FileChooser elegir = new FileChooser();
        elegir.setTitle("Cargar muestra");
        elegir.getExtensionFilters().add(new FileChooser.ExtensionFilter("Muestras JSON", "*.json"));
        if (Files.isDirectory(archivo.carpeta())) {
            elegir.setInitialDirectory(archivo.carpeta().toFile());
        }
        var elegido = elegir.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (elegido != null) {
            cargar(elegido.toPath());
        }
    }

    TextField etiqueta() {
        return etiqueta;
    }

    Label estado() {
        return estado;
    }
}
