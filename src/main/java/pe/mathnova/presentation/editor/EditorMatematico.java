package pe.mathnova.presentation.editor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Popup;

import netscape.javascript.JSObject;

import pe.mathnova.application.editor.TraductorLatex;

public class EditorMatematico extends BorderPane {

    private static final String RECURSOS = "/editor/";
    private static final String[] ARCHIVOS = {"editor.html", "mathlive.min.js"};
    private static final String[] FUENTES = {"AMS-Regular", "Caligraphic-Bold", "Caligraphic-Regular",
            "Fraktur-Bold", "Fraktur-Regular", "Main-Bold", "Main-BoldItalic", "Main-Italic", "Main-Regular",
            "Math-BoldItalic", "Math-Italic", "SansSerif-Bold", "SansSerif-Italic", "SansSerif-Regular",
            "Script-Regular", "Size1-Regular", "Size2-Regular", "Size3-Regular", "Size4-Regular",
            "Typewriter-Regular"};
    private static final double ALTO_MINIMO = 56;
    private static final javafx.css.PseudoClass FALTA = javafx.css.PseudoClass.getPseudoClass("falta");

    private static Path copiaLocal;

    private final WebView web = new WebView();
    private final WebEngine engine = web.getEngine();
    private final Puente puente = new Puente();
    private final List<String> pendientes = new ArrayList<>();
    private final ReadOnlyBooleanWrapper completo = new ReadOnlyBooleanWrapper(this, "completo", true);
    private final ReadOnlyStringWrapper latex = new ReadOnlyStringWrapper(this, "latex", "");
    private final javafx.beans.property.ReadOnlyObjectWrapper<TraductorLatex.CasillaVacia> casillaVacia =
            new javafx.beans.property.ReadOnlyObjectWrapper<>(this, "casillaVacia");
    private final HBox fila;
    private final ObjectProperty<EventHandler<EventoEditor>> onCambio = new SimpleObjectProperty<>(this, "onCambio");
    private final ObjectProperty<EventHandler<EventoEditor>> onEnter = new SimpleObjectProperty<>(this, "onEnter");
    private final ListaSugerencias sugerencias = new ListaSugerencias();
    private boolean listo;

    public EditorMatematico() {
        getStyleClass().add("editor-matematico");

        web.setContextMenuEnabled(false);
        web.setPrefHeight(ALTO_MINIMO);
        web.setMinHeight(ALTO_MINIMO);
        web.setFocusTraversable(true);
        HBox.setHgrow(web, Priority.ALWAYS);

        Label icono = new Label("⚠");
        icono.getStyleClass().add("editor-aviso-icono");
        icono.visibleProperty().bind(completo.not());
        icono.managedProperty().bind(icono.visibleProperty());
        fila = new HBox(6, icono, web);
        fila.getStyleClass().add("editor-campo");
        fila.setFillHeight(true);
        setCenter(fila);

        onCambio.addListener((o, a, h) -> setEventHandler(EventoEditor.CAMBIO, h));
        onEnter.addListener((o, a, h) -> setEventHandler(EventoEditor.ENTER, h));

        engine.documentProperty().addListener((o, a, d) -> instalarPuente());
        engine.getLoadWorker().stateProperty().addListener((o, a, estado) -> {
            if (estado == Worker.State.SUCCEEDED) {
                instalarPuente();
                engine.executeScript("window.editor.configurarSugerencias(" + Js.cadena(Plantillas.sugerenciasJson()) + ")");
                engine.executeScript("window.editor.configurarPlantillas(" + Js.cadena(Plantillas.atajosJson()) + ")");
                listo = true;
                List<String> cola = List.copyOf(pendientes);
                pendientes.clear();
                cola.forEach(engine::executeScript);
            }
        });
        engine.load(urlDelEditor());
    }

    public void insertar(String plantillaLatex) {
        js("window.editor.insertar(" + Js.cadena(plantillaLatex) + ")");
        enfocar();
    }

    public void escribir(String texto) {
        js("window.editor.escribir(" + Js.cadena(texto) + ")");
        enfocar();
    }

    public void moverIzquierda() {
        js("window.editor.izquierda()");
        enfocar();
    }

    public void moverDerecha() {
        js("window.editor.derecha()");
        enfocar();
    }

    public void borrar() {
        js("window.editor.borrar()");
        enfocar();
    }

    public void limpiar() {
        js("window.editor.limpiar()");
        enfocar();
    }

    public void pulsarEnter() {
        sugerencias.hide();
        js("window.editor.cerrarSugerencias()");
        fireEvent(new EventoEditor(EventoEditor.ENTER, getLatex()));
        enfocar();
    }

    public void setLatex(String nuevo) {
        js("window.editor.poner(" + Js.cadena(nuevo == null ? "" : nuevo) + ")");
    }

    public String getLatex() {
        if (listo) {
            Object v = engine.executeScript("window.editor.valor()");
            if (v instanceof String s) {
                return s;
            }
        }
        return latex.get();
    }

    public String getTexto() {
        return TraductorLatex.traducir(getLatex());
    }

    public ReadOnlyStringProperty latexProperty() {
        return latex.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty completoProperty() {
        return completo.getReadOnlyProperty();
    }

    public boolean isCompleto() {
        return completo.get();
    }

    public ReadOnlyObjectProperty<TraductorLatex.CasillaVacia> casillaVaciaProperty() {
        return casillaVacia.getReadOnlyProperty();
    }

    public void resaltarCasilla(int indice) {
        js("window.editor.resaltarCasilla(" + indice + ")");
        web.requestFocus();
    }

    public void marcarFalta(boolean si) {
        fila.pseudoClassStateChanged(FALTA, si);
    }

    public void enfocar() {
        web.requestFocus();
        js("window.editor.enfocar()");
    }

    public ObjectProperty<EventHandler<EventoEditor>> onCambioProperty() {
        return onCambio;
    }

    public void setOnCambio(EventHandler<EventoEditor> h) {
        onCambio.set(h);
    }

    public ObjectProperty<EventHandler<EventoEditor>> onEnterProperty() {
        return onEnter;
    }

    public void setOnEnter(EventHandler<EventoEditor> h) {
        onEnter.set(h);
    }

    WebView webView() {
        return web;
    }

    boolean estaListo() {
        return listo;
    }

    private void js(String script) {
        if (listo) {
            engine.executeScript(script);
        } else {
            pendientes.add(script);
        }
    }

    private void instalarPuente() {
        if (engine.getDocument() == null) {
            return;
        }
        JSObject ventana = (JSObject) engine.executeScript("window");
        ventana.setMember("java", puente);
    }

    private void cambio(String nuevo) {
        latex.set(nuevo);
        TraductorLatex.CasillaVacia falta = TraductorLatex.casillaVacia(nuevo).orElse(null);
        casillaVacia.set(falta);
        completo.set(falta == null);
        fireEvent(new EventoEditor(EventoEditor.CAMBIO, nuevo));
    }

    public final class Puente {

        public void onCambio(String valor) {
            cambio(valor == null ? "" : valor);
        }

        public void onEnter(String valor) {
            sugerencias.hide();
            fireEvent(new EventoEditor(EventoEditor.ENTER, valor == null ? "" : valor));
        }

        public void onListo(String estado) {
        }

        public void onAltura(int alto) {
            double h = Math.max(ALTO_MINIMO, alto + 4);
            web.setPrefHeight(h);
            web.setMinHeight(h);
        }

        public void onSugerencias(String indices, Number x, Number y) {
            List<Integer> lista = new ArrayList<>();
            for (String s : indices.split(",")) {
                if (!s.isBlank()) {
                    lista.add(Integer.parseInt(s.strip()));
                }
            }
            Point2D punto = web.localToScreen(x.doubleValue(), y.doubleValue() + 4);
            if (punto != null && !lista.isEmpty()) {
                sugerencias.mostrar(lista, punto);
            }
        }

        public void onSeleccion(Number posicion) {
            sugerencias.seleccionar(posicion.intValue());
        }

        public void onCerrarSugerencias(String nada) {
            sugerencias.hide();
        }
    }

    private final class ListaSugerencias extends Popup {

        private final VBox caja = new VBox();
        private final List<Label> filas = new ArrayList<>();

        ListaSugerencias() {
            caja.getStyleClass().add("editor-sugerencias");
            getContent().add(caja);
            setAutoHide(true);
            setHideOnEscape(false);
        }

        void mostrar(List<Integer> indices, Point2D punto) {
            caja.getChildren().clear();
            filas.clear();
            for (int i = 0; i < indices.size(); i++) {
                Plantillas.Sugerencia s = Plantillas.SUGERENCIAS.get(indices.get(i));
                Label fila = new Label(s.etiqueta());
                fila.getStyleClass().add("editor-sugerencia");
                fila.setMaxWidth(Double.MAX_VALUE);
                int posicion = i;
                fila.setOnMouseClicked(e -> {
                    js("window.editor.aceptarSugerencia(" + posicion + ")");
                    enfocar();
                });
                filas.add(fila);
                caja.getChildren().add(fila);
            }
            seleccionar(0);
            if (getScene().getStylesheets().isEmpty() && EditorMatematico.this.getScene() != null) {
                getScene().getStylesheets().setAll(EditorMatematico.this.getScene().getStylesheets());
            }
            if (EditorMatematico.this.getScene() != null) {
                show(EditorMatematico.this.getScene().getWindow(), punto.getX(), punto.getY());
            }
        }

        void seleccionar(int posicion) {
            for (int i = 0; i < filas.size(); i++) {
                filas.get(i).pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), i == posicion);
            }
        }
    }

    private static synchronized String urlDelEditor() {
        URL url = Objects.requireNonNull(EditorMatematico.class.getResource(RECURSOS + "editor.html"),
                "Falta /editor/editor.html");
        if ("file".equals(url.getProtocol())) {
            return url.toExternalForm();
        }
        try {
            if (copiaLocal == null) {
                Path dir = Files.createTempDirectory("mathnova-editor");
                Files.createDirectories(dir.resolve("fonts"));
                for (String a : ARCHIVOS) {
                    copiar(RECURSOS + a, dir.resolve(a));
                }
                for (String f : FUENTES) {
                    String nombre = "KaTeX_" + f + ".woff";
                    copiar(RECURSOS + "fonts/" + nombre, dir.resolve("fonts").resolve(nombre));
                }
                dir.toFile().deleteOnExit();
                copiaLocal = dir;
            }
            return copiaLocal.resolve("editor.html").toUri().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo preparar el editor", e);
        }
    }

    private static void copiar(String recurso, Path destino) throws IOException {
        try (InputStream in = Objects.requireNonNull(EditorMatematico.class.getResourceAsStream(recurso),
                "Falta " + recurso)) {
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        destino.toFile().deleteOnExit();
    }
}
