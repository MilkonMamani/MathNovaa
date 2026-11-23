package pe.mathnova.presentation.escritura;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import pe.mathnova.domain.escritura.ReconocedorEscritura;
import pe.mathnova.domain.escritura.ResultadoReconocimiento;
import pe.mathnova.domain.escritura.SimboloDudoso;
import pe.mathnova.domain.escritura.TablaConfusiones;
import pe.mathnova.domain.escritura.Trazo;
import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/**
 * La pestaña "Escribir": el estudiante escribe el ejercicio a mano y el {@link ReconocedorEscritura}
 * lo pasa a LaTeX.
 *
 * <ol>
 *   <li>0,7 s después de levantar el lápiz sin empezar otro trazo, aparece "Leyendo tu escritura…" y
 *       se llama al reconocedor en un Task, fuera del hilo de la interfaz. Un trazo nuevo cancela la
 *       lectura en curso.</li>
 *   <li>"LO QUE ENTENDÍ" muestra la expresión; los símbolos dudosos van subrayados con puntos naranjas
 *       y debajo se pregunta "¿El símbolo es x ×?", con las alternativas como botones.</li>
 *   <li>"Usar este ejercicio" manda el LaTeX al editor. Nunca se resuelve solo.</li>
 * </ol>
 */
public final class PanelEscritura extends VBox {

    static final Duration ESPERA = Duration.millis(700);
    static final String LEYENDO = "Leyendo tu escritura…";
    static final String NO_SE_PUDO = "No pude leer tu escritura. Intenta otra vez, con letra más grande.";

    private final ReconocedorEscritura reconocedor;
    private final Consumer<String> usar;
    private final RenderizadorLatex latex = new RenderizadorLatex(22f);

    private final LienzoEscritura lienzo = new LienzoEscritura();
    private final ToggleButton lapiz;
    private final ToggleButton borrador;
    private final Label estadoLectura = new Label();
    private final VBox tarjeta = new VBox(10);
    private final StackPane expresion = new StackPane();
    private final VBox preguntas = new VBox(6);
    private final PauseTransition espera = new PauseTransition(ESPERA);

    private Task<ResultadoReconocimiento> lectura;
    /** Cambia con cada trazo: una lectura de trazos anteriores se descarta. */
    private int version;
    private ResultadoReconocimiento resultado;

    public PanelEscritura(ReconocedorEscritura reconocedor, Consumer<String> usar) {
        this.reconocedor = reconocedor;
        this.usar = usar;
        getStyleClass().add("panel-escritura");
        setSpacing(12);

        Label indicacion = new Label("Escribe con el dedo, lápiz o mouse");
        indicacion.getStyleClass().add("subtitulo-tarjeta");

        ToggleGroup grupo = new ToggleGroup();
        lapiz = herramienta("M3 17 L4 13 L14 3 L17 6 L7 16 Z M12 5 L15 8", "Lápiz", grupo);
        borrador = herramienta("M7 17 L2 12 L11 3 L18 10 L11 17 Z M6 8 L13 15 M11 17 H18", "Borrador", grupo);
        lapiz.setSelected(true);
        grupo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
            if (ahora == null) {
                antes.setSelected(true); // siempre hay una herramienta
                return;
            }
            lienzo.setHerramienta(ahora == lapiz ? LienzoEscritura.Herramienta.LAPIZ
                    : LienzoEscritura.Herramienta.BORRADOR);
        });
        Button deshacer = accion("M5 8 H13 A4 4 0 0 1 13 16 H8 M8 4 L4 8 L8 12", "Deshacer");
        deshacer.setOnAction(e -> {
            lienzo.deshacer();
            despuesDeCambiar();
        });
        Button borrarTodo = accion("M3 5 H17 M8 5 V3 H12 V5 M5 5 L6 17 H14 L15 5", "Borrar todo");
        borrarTodo.setOnAction(e -> {
            lienzo.borrarTodo();
            despuesDeCambiar();
        });
        HBox herramientas = new HBox(6, lapiz, borrador, deshacer, borrarTodo);
        herramientas.setAlignment(Pos.CENTER_LEFT);
        herramientas.getStyleClass().add("barra-escritura");

        lienzo.setOnEmpezar(this::cancelarLectura);
        lienzo.setOnTerminar(this::despuesDeCambiar);
        espera.setOnFinished(e -> leer());

        estadoLectura.getStyleClass().add("escritura-estado");
        estadoLectura.setWrapText(true);
        mostrarNodo(estadoLectura, false);

        armarTarjeta();

        getChildren().addAll(indicacion, herramientas, lienzo, estadoLectura, tarjeta);
    }

    // ------------------------------------------------------------------ lectura

    /** Después de un trazo, de deshacer o de borrar: a los 0,7 s se lee lo que haya. */
    private void despuesDeCambiar() {
        cancelarLectura();
        if (lienzo.isVacio()) {
            resultado = null;
            mostrarNodo(tarjeta, false);
            return;
        }
        espera.playFromStart();
    }

    private void cancelarLectura() {
        espera.stop();
        version++;
        if (lectura != null) {
            lectura.cancel();
            lectura = null;
        }
        mostrarNodo(estadoLectura, false);
    }

    private void leer() {
        List<Trazo> trazos = List.copyOf(lienzo.trazos());
        if (trazos.isEmpty()) {
            return;
        }
        int esta = ++version;
        estadoLectura.setText(LEYENDO);
        estadoLectura.getStyleClass().remove("mensaje-error");
        mostrarNodo(estadoLectura, true);
        Task<ResultadoReconocimiento> tarea = new Task<>() {
            @Override
            protected ResultadoReconocimiento call() {
                return reconocedor.reconocer(trazos);
            }
        };
        // Los manejadores del Task corren en el hilo de JavaFX (Platform.runLater por dentro).
        tarea.setOnSucceeded(e -> {
            if (esta == version) {
                lectura = null;
                mostrarNodo(estadoLectura, false);
                mostrarResultado(tarea.getValue());
            }
        });
        tarea.setOnFailed(e -> {
            if (esta == version) {
                lectura = null;
                estadoLectura.setText(NO_SE_PUDO);
                if (!estadoLectura.getStyleClass().contains("mensaje-error")) {
                    estadoLectura.getStyleClass().add("mensaje-error");
                }
            }
        });
        lectura = tarea;
        Thread hilo = new Thread(tarea, "lectura-escritura");
        hilo.setDaemon(true);
        hilo.start();
    }

    // ------------------------------------------------------------------ "Lo que entendí"

    private void armarTarjeta() {
        Label titulo = new Label("LO QUE ENTENDÍ");
        titulo.getStyleClass().add("entendi-titulo");
        expresion.setAlignment(Pos.CENTER_LEFT);
        expresion.getStyleClass().add("entendi-expresion");

        Button usarEste = new Button("Usar este ejercicio");
        usarEste.getStyleClass().addAll("boton-primario", "boton-usar-escritura");
        usarEste.setOnAction(e -> {
            if (resultado != null && !resultado.latex().isBlank()) {
                usar.accept(resultado.latex());
            }
        });
        Button volver = new Button("Volver a escribir");
        volver.getStyleClass().add("boton-alternativa");
        volver.setOnAction(e -> volverAEscribir());
        usarEste.setMinWidth(Region.USE_PREF_SIZE);
        volver.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane botones = new FlowPane(8, 8, usarEste, volver);
        botones.setAlignment(Pos.CENTER_LEFT);

        tarjeta.getStyleClass().add("tarjeta-entendi");
        tarjeta.getChildren().addAll(titulo, expresion, preguntas, botones);
        mostrarNodo(tarjeta, false);
    }

    private void mostrarResultado(ResultadoReconocimiento r) {
        resultado = r;
        if (r == null || r.latex().isBlank()) {
            estadoLectura.setText(NO_SE_PUDO);
            mostrarNodo(estadoLectura, true);
            mostrarNodo(tarjeta, false);
            return;
        }
        expresion.getChildren().setAll(dibujar(r));

        preguntas.getChildren().clear();
        for (SimboloDudoso d : r.dudosos()) {
            FlowPane fila = new FlowPane(6, 6);
            fila.setAlignment(Pos.CENTER_LEFT);
            Label pregunta = new Label("¿El símbolo es");
            pregunta.getStyleClass().add("entendi-pregunta");
            fila.getChildren().add(pregunta);
            for (String alternativa : d.alternativas()) {
                Button b = new Button(TablaConfusiones.aLaVista(alternativa));
                b.getStyleClass().add("boton-alternativa-simbolo");
                b.setAccessibleText("El símbolo es " + TablaConfusiones.aLaVista(alternativa));
                b.setOnAction(e -> mostrarResultado(resultado.elegir(d, alternativa)));
                fila.getChildren().add(b);
            }
            Label cierre = new Label("?");
            cierre.getStyleClass().add("entendi-pregunta");
            fila.getChildren().add(cierre);
            preguntas.getChildren().add(fila);
        }
        mostrarNodo(preguntas, !r.dudosos().isEmpty());
        mostrarNodo(tarjeta, true);
    }

    /**
     * La expresión con los símbolos dudosos subrayados con puntos: se dibuja por tramos. Si algún tramo
     * no se puede dibujar solo (un tramo que corta un \frac, por ejemplo), va entera y sin subrayados;
     * las preguntas de abajo siguen ahí.
     */
    private Node dibujar(ResultadoReconocimiento r) {
        List<Node> tramos = new ArrayList<>();
        int desde = 0;
        boolean bien = true;
        for (SimboloDudoso d : r.dudosos()) {
            if (d.indice() > desde) {
                bien &= agregar(tramos, r.latex().substring(desde, d.indice()), false);
            }
            int fin = d.indice() + d.elegido().length();
            bien &= agregar(tramos, r.latex().substring(d.indice(), fin), true);
            desde = fin;
        }
        if (desde < r.latex().length()) {
            bien &= agregar(tramos, r.latex().substring(desde), false);
        }
        if (bien) {
            HBox caja = new HBox(tramos.toArray(Node[]::new));
            caja.setAlignment(Pos.BASELINE_LEFT);
            return caja;
        }
        return new ImageView(ImagenLatex.de(latex, r.latex()));
    }

    private boolean agregar(List<Node> tramos, String tramo, boolean dudoso) {
        Image img = ImagenLatex.de(latex, tramo);
        if (img == null) {
            return false;
        }
        ImageView vista = new ImageView(img);
        if (!dudoso) {
            tramos.add(vista);
            return true;
        }
        StackPane marca = new StackPane(vista);
        marca.getStyleClass().add("simbolo-dudoso");
        marca.setAccessibleText("Símbolo dudoso: " + TablaConfusiones.aLaVista(tramo));
        Tooltip.install(marca, new Tooltip("No estoy seguro de este símbolo"));
        tramos.add(marca);
        return true;
    }

    private void volverAEscribir() {
        cancelarLectura();
        lienzo.reiniciar();
        resultado = null;
        mostrarNodo(tarjeta, false);
    }

    // ------------------------------------------------------------------ apoyo

    private static ToggleButton herramienta(String icono, String nombre, ToggleGroup grupo) {
        ToggleButton b = new ToggleButton(null, svg(icono));
        b.setToggleGroup(grupo);
        estilo(b, nombre);
        return b;
    }

    private static Button accion(String icono, String nombre) {
        Button b = new Button(null, svg(icono));
        estilo(b, nombre);
        return b;
    }

    private static void estilo(ButtonBase b, String nombre) {
        b.getStyleClass().add("herramienta-escritura");
        b.setAccessibleText(nombre);
        b.setTooltip(new Tooltip(nombre));
    }

    private static SVGPath svg(String contenido) {
        SVGPath s = new SVGPath();
        s.setContent(contenido);
        s.getStyleClass().add("herramienta-icono");
        return s;
    }

    private static void mostrarNodo(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    // ------------------------------------------------------------------ para las pruebas

    LienzoEscritura lienzo() {
        return lienzo;
    }

    ResultadoReconocimiento resultado() {
        return resultado;
    }

    Node tarjeta() {
        return tarjeta;
    }

    Label estadoLectura() {
        return estadoLectura;
    }
}
