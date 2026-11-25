import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Labeled;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import pe.mathnova.MathNovaApplication;
import pe.mathnova.presentation.Navegador;
import pe.mathnova.presentation.editor.EditorMatematico;

/**
 * Arnés de capturas del manual. No forma parte de MathNova: abre la aplicación real,
 * ejecuta el guion que le pasa generar_capturas.py y guarda, por cada captura,
 * la imagen limpia (escena a 2x) y las cajas de los controles en píxeles lógicos.
 *
 * Guion (una orden por línea, campos separados por TAB):
 *   CAPTURA  id  escala
 *   ACCION   tipo  selector  valor
 *   CAJA     nombre  selector
 *   FIN
 * Selector: selector CSS de JavaFX, opcionalmente "selector|texto" para elegir el nodo
 * cuyo texto o texto accesible es exactamente "texto".
 */
public class ArnesCapturas {

    private static Path guion;
    private static Path salida;
    private static ConfigurableApplicationContext contexto;
    private static Stage ventana;

    public static void main(String[] args) throws Exception {
        guion = Path.of(args[0]).toAbsolutePath();
        salida = Path.of(args[1]).toAbsolutePath();
        Files.createDirectories(salida);
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".mathnova"));
        Locale.setDefault(Locale.forLanguageTag("es-PE"));
        Application.launch(App.class);
    }

    public static class App extends Application {
        @Override
        public void init() {
            contexto = new SpringApplicationBuilder(MathNovaApplication.class).headless(false).run();
        }

        @Override
        public void start(Stage stage) {
            iniciar(stage);
        }
    }

    private static void iniciar(Stage stage) {
        ventana = stage;
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        contexto.getBean(Navegador.class).iniciar(stage);
        stage.setX(0);
        stage.setY(0);
        stage.show();
        ajustarTamano();
        Thread t = new Thread(() -> {
            int codigo = 0;
            try {
                ejecutar();
            } catch (Throwable e) {
                System.out.println("ERROR " + e);
                e.printStackTrace(System.out);
                codigo = 1;
            }
            System.out.flush();
            Platform.exit();
            contexto.close();
            System.exit(codigo);
        }, "guion-capturas");
        t.setDaemon(true);
        t.start();
    }

    private static final double ANCHO = Double.parseDouble(System.getProperty("capturas.ancho", "1280"));
    private static final double ALTO = Double.parseDouble(System.getProperty("capturas.alto", "1080"));

    private static void ajustarTamano() {
        ventana.setWidth(ventana.getWidth() + ANCHO - ventana.getScene().getWidth());
        ventana.setHeight(ventana.getHeight() + ALTO - ventana.getScene().getHeight());
    }

    private static boolean tamanoCorrecto() {
        return Math.abs(ventana.getScene().getWidth() - ANCHO) < 1 && Math.abs(ventana.getScene().getHeight() - ALTO) < 1;
    }

    private static void ejecutar() throws Exception {
        esperarEditor();
        String id = null;
        int escala = 2;
        Map<String, String> cajas = new LinkedHashMap<>();
        for (String linea : Files.readAllLines(guion, StandardCharsets.UTF_8)) {
            if (linea.isBlank()) {
                continue;
            }
            String[] c = linea.split("\t", -1);
            switch (c[0]) {
                case "CAPTURA" -> {
                    id = c[1];
                    escala = Integer.parseInt(c[2]);
                    cajas.clear();
                    System.out.println("CAPTURA " + id);
                    enFx(() -> {
                        for (Window w : new ArrayList<>(Window.getWindows())) {
                            if (w != ventana && w instanceof Stage st && st.isShowing()) {
                                st.close();
                            }
                        }
                        return null;
                    });
                }
                case "ACCION" -> accion(c[1], c[2], c.length > 3 ? c[3] : "");
                case "CAJA" -> cajas.put(c[1], c[2]);
                case "FIN" -> guardar(id, escala, cajas);
                default -> throw new IllegalArgumentException("Orden desconocida: " + linea);
            }
        }
    }

    private static void accion(String tipo, String selector, String valor) throws Exception {
        switch (tipo) {
            case "ir" -> clic(".nav-item|" + valor);
            case "clic" -> clic(selector);
            case "escribir" -> escribir(valor);
            case "limpiar" -> escribir("");
            case "esperar" -> esperar(() -> visible(buscar(selector)), "no apareció " + selector,
                    valor.isBlank() ? 60 : Integer.parseInt(valor));
            case "esperar_resultado" -> esperarResultado();
            case "ver" -> {
                enFx(() -> {
                    Node n = buscar(selector);
                    if (n == null) {
                        throw new IllegalStateException("No hay control " + selector);
                    }
                    for (Node p = n.getParent(); p != null; p = p.getParent()) {
                        if (p instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
                            ventana.getScene().getRoot().applyCss();
                            ventana.getScene().getRoot().layout();
                            Bounds c = sp.getContent().getLayoutBounds();
                            Bounds b = sp.getContent().sceneToLocal(n.localToScene(n.getBoundsInLocal()));
                            double vista = sp.getViewportBounds().getHeight();
                            double sobra = c.getHeight() - vista;
                            if (sobra > 0) {
                                double objetivo = Math.min(sobra, Math.max(0, b.getMaxY() + 16 - vista));
                                sp.setVvalue(sp.getVmin() + (sp.getVmax() - sp.getVmin()) * objetivo / sobra);
                            }
                        }
                    }
                    return null;
                });
                Thread.sleep(600);
            }
            case "pausa" -> Thread.sleep(Long.parseLong(valor));
            case "texto" -> {
                enFx(() -> {
                    javafx.scene.control.TextInputControl t = (javafx.scene.control.TextInputControl) buscar(selector);
                    t.setText(valor);
                    return null;
                });
                Thread.sleep(800);
            }
            case "elegir" -> {
                enFx(() -> {
                    javafx.scene.control.ComboBox<?> cb = (javafx.scene.control.ComboBox<?>) buscar(selector);
                    for (int i = 0; i < cb.getItems().size(); i++) {
                        Object o = cb.getItems().get(i);
                        String txt = cb.getConverter() != null ? ((javafx.util.StringConverter<Object>) (javafx.util.StringConverter<?>) cb.getConverter()).toString(o) : String.valueOf(o);
                        if (valor.equals(txt) || valor.equals(String.valueOf(o))) {
                            cb.getSelectionModel().select(i);
                            return null;
                        }
                    }
                    throw new IllegalStateException("No hay opción " + valor + " en " + selector + ": " + cb.getItems());
                });
                Thread.sleep(800);
            }
            case "muestra" -> {
                pe.mathnova.domain.escritura.Muestra m =
                        pe.mathnova.infrastructure.escritura.ArchivoMuestras.leer(Path.of(valor).toAbsolutePath());
                enFx(() -> {
                    panelEscritura(ventana.getScene().getRoot()).cargarMuestra(m);
                    return null;
                });
            }
            case "volcar" -> enFx(() -> {
                for (Window w : Window.getWindows()) {
                    if (w.getScene() == null) {
                        continue;
                    }
                    for (Node n : w.getScene().getRoot().lookupAll(selector)) {
                        if (visible(n)) {
                            System.out.println("VOLCAR " + n.getClass().getSimpleName() + " " + n.getStyleClass()
                                    + " acc=" + n.getAccessibleText()
                                    + (n instanceof Labeled l ? " texto=" + l.getText() : ""));
                        }
                    }
                }
                return null;
            });
            default -> throw new IllegalArgumentException("Acción desconocida: " + tipo);
        }
    }

    private static void esperarEditor() throws Exception {
        esperar(() -> {
            WebView w = webView();
            return w != null && Boolean.TRUE.equals(
                    w.getEngine().executeScript("!!(window.editor && window.fuentesListas)"));
        }, "el editor no cargó", 90);
    }

    private static EditorMatematico editor() {
        return (EditorMatematico) ventana.getScene().getRoot().lookup(".editor-matematico");
    }

    private static WebView webView() {
        EditorMatematico e = editor();
        return e == null ? null : (WebView) e.lookup("WebView");
    }

    private static void escribir(String latex) throws Exception {
        enFx(() -> {
            editor().setLatex(latex);
            return null;
        });
        esperar(() -> latex.equals(editor().getLatex()), "el editor no tomó " + latex, 20);
        Thread.sleep(600);
    }

    private static void esperarResultado() throws Exception {
        Thread.sleep(200);
        esperar(() -> {
            Node b = buscar("#tarjetaProcedimiento .pizarra-resultado-bloque");
            Node error = buscar("#sinPasos");
            boolean fallo = visible(error) && error instanceof Labeled l && l.getText() != null
                    && l.getText().startsWith("No se pudo");
            return visible(b) || fallo;
        }, "no terminó de resolver", 120);
        Thread.sleep(1200);
    }

    private static void clic(String selector) throws Exception {
        enFx(() -> {
            Node n = buscar(selector);
            if (n == null) {
                throw new IllegalStateException("No hay control " + selector);
            }
            if (n instanceof ButtonBase b) {
                Platform.runLater(b::fire);
            } else {
                Bounds l = n.getLayoutBounds();
                double x = l.getCenterX();
                double y = l.getCenterY();
                javafx.geometry.Point2D s = n.localToScene(x, y);
                for (var t : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED)) {
                    Event.fireEvent(n, new MouseEvent(t, x, y, s.getX(), s.getY(), MouseButton.PRIMARY, 1,
                            false, false, false, false, true, false, false, true, false, true, null));
                }
            }
            return null;
        });
        Thread.sleep(500);
    }

    private static pe.mathnova.presentation.escritura.PanelEscritura panelEscritura(Node n) {
        if (n instanceof pe.mathnova.presentation.escritura.PanelEscritura p) {
            return p;
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                var r = panelEscritura(h);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    static Node buscar(String selector) {
        String css = selector;
        String texto = null;
        int barra = selector.indexOf('|');
        if (barra >= 0) {
            css = selector.substring(0, barra);
            texto = selector.substring(barra + 1);
        }
        for (Window w : Window.getWindows()) {
            if (!w.isShowing() || w.getScene() == null) {
                continue;
            }
            Parent raiz = w.getScene().getRoot();
            raiz.applyCss();
            for (Node n : raiz.lookupAll(css)) {
                if (texto == null && visible(n)) {
                    return n;
                }
                if (texto != null && visible(n) && (texto.equals(n.getAccessibleText())
                        || (n instanceof Labeled l && texto.equals(l.getText())))) {
                    return n;
                }
            }
        }
        return null;
    }

    static boolean visible(Node n) {
        if (n == null || n.getScene() == null) {
            return false;
        }
        for (Node p = n; p != null; p = p.getParent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return n.isManaged() || n.getParent() == null;
    }

    private static void guardar(String id, int escala, Map<String, String> cajas) throws Exception {
        enFx(() -> {
            WebView w = webView();
            if (w != null) {
                w.getEngine().executeScript("document.activeElement && document.activeElement.blur && document.activeElement.blur()");
            }
            ventana.getScene().getRoot().requestFocus();
            return null;
        });
        Thread.sleep(400);
        for (int i = 0; i < 20 && !Boolean.TRUE.equals(enFx(ArnesCapturas::tamanoCorrecto)); i++) {
            enFx(() -> {
                ajustarTamano();
                return null;
            });
            Thread.sleep(500);
        }
        StringBuilder json = new StringBuilder("{");
        List<String> faltan = new ArrayList<>();
        enFx(() -> {
            Parent raiz = ventana.getScene().getRoot();
            raiz.applyCss();
            raiz.layout();
            if (!tamanoCorrecto()) {
                System.out.println("ERROR la escena mide " + ventana.getScene().getWidth() + "x" + ventana.getScene().getHeight());
            }
            int i = 0;
            for (var e : cajas.entrySet()) {
                Node n = buscar(e.getValue());
                if (n == null) {
                    faltan.add(e.getKey() + " (" + e.getValue() + ")");
                    continue;
                }
                Bounds b = n.localToScene(n.getBoundsInLocal());
                if (n.getScene() != ventana.getScene()) {
                    Window w = n.getScene().getWindow();
                    double dx = w.getX() + n.getScene().getX() - ventana.getX() - ventana.getScene().getX();
                    double dy = w.getY() + n.getScene().getY() - ventana.getY() - ventana.getScene().getY();
                    b = new javafx.geometry.BoundingBox(b.getMinX() + dx, b.getMinY() + dy, b.getWidth(), b.getHeight());
                }
                json.append(i++ == 0 ? "" : ",").append('"').append(e.getKey()).append("\":[")
                        .append(String.format(Locale.ROOT, "%.1f,%.1f,%.1f,%.1f",
                                b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight()))
                        .append(']');
            }
            StringBuilder texto = new StringBuilder();
            for (Window w : Window.getWindows()) {
                if (w.isShowing() && w.getScene() != null && w instanceof Stage) {
                    volcarTexto(w.getScene().getRoot(), texto);
                }
            }
            try {
                Files.writeString(salida.resolve(id + ".texto.txt"), texto, StandardCharsets.UTF_8);
            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
            SnapshotParameters p = new SnapshotParameters();
            p.setTransform(Transform.scale(escala, escala));
            p.setViewport(new javafx.geometry.Rectangle2D(0, 0, ANCHO * escala, ALTO * escala));
            p.setFill(Color.WHITE);
            WritableImage img = raiz.snapshot(p, null);
            BufferedImage base = SwingFXUtils.fromFXImage(img, null);
            pegarVentanasEmergentes(base, escala);
            try {
                ImageIO.write(base, "png", new File(salida.toFile(), id + ".png"));
            } catch (java.io.IOException ex) {
                throw new java.io.UncheckedIOException(ex);
            }
            return null;
        });
        json.append('}');
        Files.writeString(salida.resolve(id + ".cajas.json"), json, StandardCharsets.UTF_8);
        if (!faltan.isEmpty()) {
            System.out.println("FALTA " + id + ": " + String.join(", ", faltan));
        }
        System.out.println("GUARDADA " + id);
    }

    private static void volcarTexto(Node n, StringBuilder sb) {
        if (!n.isVisible() || n.getOpacity() == 0) {
            return;
        }
        if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank()) {
            sb.append(l.getText()).append('\n');
        } else if (n instanceof javafx.scene.text.Text t && t.getText() != null && !t.getText().isBlank()) {
            sb.append(t.getText()).append('\n');
        } else if (n instanceof javafx.scene.control.TextInputControl t && !t.getText().isBlank()) {
            sb.append("[campo] ").append(t.getText()).append('\n');
        } else if (n instanceof pe.mathnova.presentation.resolucion.FormulaAjustable f && f.getLatex() != null) {
            sb.append("[latex] ").append(f.getLatex()).append('\n');
        } else if (n instanceof pe.mathnova.presentation.latex.VistaLatex v && v.getLatex() != null) {
            sb.append("[latex] ").append(v.getLatex()).append('\n');
        } else if (n instanceof EditorMatematico e) {
            sb.append("[editor] ").append(e.getLatex()).append('\n');
            return;
        }
        if (n instanceof javafx.scene.control.TitledPane tp && tp.isExpanded() && tp.getContent() != null) {
            volcarTexto(tp.getContent(), sb);
        }
        if (n instanceof Parent p) {
            for (Node h : p.getChildrenUnmodifiable()) {
                volcarTexto(h, sb);
            }
        }
    }

    private static void pegarVentanasEmergentes(BufferedImage base, int escala) {
        java.awt.Graphics2D g = base.createGraphics();
        for (Window w : Window.getWindows()) {
            if (w == ventana || !w.isShowing() || w.getScene() == null || !(w instanceof Stage)) {
                continue;
            }
            SnapshotParameters p = new SnapshotParameters();
            p.setTransform(Transform.scale(escala, escala));
            BufferedImage im = SwingFXUtils.fromFXImage(w.getScene().getRoot().snapshot(p, null), null);
            double x = w.getX() + w.getScene().getX() - ventana.getX() - ventana.getScene().getX();
            double y = w.getY() + w.getScene().getY() - ventana.getY() - ventana.getScene().getY();
            // Marco de la ventana del diálogo (borde y barra de título), que la escena no incluye
            int barra = 32 * escala;
            int fx = (int) Math.round(x * escala), fy = (int) Math.round(y * escala) - barra;
            int fw = im.getWidth(), fh = im.getHeight() + barra;
            g.setColor(new java.awt.Color(0, 0, 0, 40));
            g.fillRect(fx - 6 * escala + 4 * escala, fy + 6 * escala, fw + 4 * escala, fh + 4 * escala);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(fx, fy, fw, barra);
            g.setColor(new java.awt.Color(32, 32, 32));
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12 * escala));
            String titulo = w instanceof Stage st && st.getTitle() != null ? st.getTitle() : "";
            g.drawString(titulo, fx + 12 * escala, fy + 21 * escala);
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(fx, fy + barra, fw, im.getHeight());
            g.drawImage(im, fx, fy + barra, null);
            g.setColor(new java.awt.Color(140, 140, 140));
            g.setStroke(new java.awt.BasicStroke(escala));
            g.drawRect(fx, fy, fw - 1, fh - 1);
        }
        g.dispose();
    }

    private static void esperar(Supplier<Boolean> condicion, String mensaje, int segundos) throws Exception {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(segundos);
        while (!Boolean.TRUE.equals(enFx(condicion::get))) {
            if (System.nanoTime() > limite) {
                throw new IllegalStateException(mensaje);
            }
            Thread.sleep(100);
        }
    }

    private static <T> T enFx(Supplier<T> accion) throws Exception {
        FutureTask<T> tarea = new FutureTask<>(accion::get);
        Platform.runLater(tarea);
        return tarea.get(120, TimeUnit.SECONDS);
    }
}
