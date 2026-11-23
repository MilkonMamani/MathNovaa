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

/**
 * Editor visual de fórmulas (HU36, RF67 a RF70): un WebView con MathLive (resources/editor).
 *
 * <ul>
 *   <li>{@link #insertar(String)} pone una plantilla y deja el cursor en su primera casilla.</li>
 *   <li>{@link #completoProperty()} es false mientras quede una casilla obligatoria vacía; entonces se
 *       ve ⚠ a la izquierda. {@link #casillaVaciaProperty()} dice cuál falta ("Completa el exponente");
 *       el mensaje lo muestra la pantalla, y {@link #resaltarCasilla(int)} lleva el cursor a esa casilla
 *       y la marca con borde naranja.</li>
 *   <li>{@link EventoEditor#CAMBIO} y {@link EventoEditor#ENTER} avisan de cada cambio y de Enter.</li>
 *   <li>Al escribir lim, der, int, sen, cos, tan, ln, log, raiz o frac aparece una lista con la
 *       plantilla (RF70); Enter la inserta y Esc la cierra.</li>
 * </ul>
 *
 * <p>El WebView de JavaFX no decodifica WOFF2 y entrega las teclas sin {@code key}; editor.html trae
 * los dos arreglos (ver su comentario).
 */
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

    // ------------------------------------------------------------------ API

    /** Inserta una plantilla LaTeX ({@code #?} = casilla) y lleva el cursor a la primera casilla. */
    public void insertar(String plantillaLatex) {
        js("window.editor.insertar(" + Js.cadena(plantillaLatex) + ")");
        enfocar();
    }

    /** Escribe texto como si se tecleara ("7", "+", "x"). */
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

    /** La tecla Enter del teclado en pantalla: lo mismo que pulsar Enter en el campo. */
    public void pulsarEnter() {
        sugerencias.hide();
        js("window.editor.cerrarSugerencias()");
        fireEvent(new EventoEditor(EventoEditor.ENTER, getLatex()));
        enfocar();
    }

    /** Reemplaza todo el contenido (por ejemplo, al volver a una expresión del historial). */
    public void setLatex(String nuevo) {
        js("window.editor.poner(" + Js.cadena(nuevo == null ? "" : nuevo) + ")");
    }

    /** El LaTeX actual, tal como lo da MathLive. */
    public String getLatex() {
        if (listo) {
            Object v = engine.executeScript("window.editor.valor()");
            if (v instanceof String s) {
                return s;
            }
        }
        return latex.get();
    }

    /** El texto para el parser (sección 9). */
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

    /** La casilla obligatoria que falta, con su mensaje; null si no falta ninguna. */
    public ReadOnlyObjectProperty<TraductorLatex.CasillaVacia> casillaVaciaProperty() {
        return casillaVacia.getReadOnlyProperty();
    }

    /** Lleva el cursor a la casilla número {@code indice} (ver CasillaVacia) y la marca con borde naranja. */
    public void resaltarCasilla(int indice) {
        js("window.editor.resaltarCasilla(" + indice + ")");
        web.requestFocus();
    }

    /** Borde naranja en todo el campo: falta una casilla (o se quita con false). */
    public void marcarFalta(boolean si) {
        fila.pseudoClassStateChanged(FALTA, si);
    }

    /** Devuelve el foco al campo; el teclado lo llama después de cada tecla. */
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

    /** Para las pruebas: el WebView de dentro. */
    WebView webView() {
        return web;
    }

    boolean estaListo() {
        return listo;
    }

    // ------------------------------------------------------------------ puente con editor.html

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

    /** Lo llama editor.html. Debe ser público para que JavaScript lo vea. */
    public final class Puente {

        public void onCambio(String valor) {
            cambio(valor == null ? "" : valor);
        }

        public void onEnter(String valor) {
            sugerencias.hide();
            fireEvent(new EventoEditor(EventoEditor.ENTER, valor == null ? "" : valor));
        }

        public void onListo(String estado) {
            // Las fuentes cargaron; nada más que hacer.
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

    /** RF70: la lista que aparece bajo el cursor. No toma el foco; las teclas las maneja editor.html. */
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

    // ------------------------------------------------------------------ ubicación de editor.html

    /**
     * Desde las clases compiladas se usa el archivo tal cual. Desde el .jar de Spring Boot (URL
     * "jar:nested:"), el WebView no siempre resuelve las rutas relativas, así que se copia la carpeta
     * a un directorio temporal una sola vez.
     */
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
