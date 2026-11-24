package pe.mathnova.presentation.editor;

import java.util.List;
import java.util.function.Supplier;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Teclado en pantalla (HU33, RF72) para un {@link EditorMatematico}. Pestañas en forma de píldora
 * 123 | f(x) | ABC | #&¬ y "•••" a la derecha, que abre la configuración. Las teclas no toman el
 * foco: el editor lo conserva y, con las plantillas, el cursor queda en la primera casilla.
 */
public class TecladoMatematico extends VBox {

    /** El ancho de partida de cada columna de teclas, antes de repartir el ancho del panel. */
    private static final double ANCHO_COLUMNA = 64;

    private final EditorMatematico editor;
    private final StackPane contenido = new StackPane();
    private final ToggleGroup grupo = new ToggleGroup();
    private Runnable onConfiguracion = () -> { };
    private Supplier<String> respuestaAnterior = () -> null;
    private boolean mayuscula;
    /** true mientras se arma ABC: solo sus letras cambian con ⇧. */
    private boolean armandoLetras;
    private Node letras;
    private Node griegas;
    private final java.util.List<Button> botonesDeLetra = new java.util.ArrayList<>();
    /** El botón de cada tecla (el primero, si la tecla está en dos pestañas). */
    private final java.util.Map<Teclas.Tecla, Button> botones = new java.util.IdentityHashMap<>();

    public TecladoMatematico(EditorMatematico editor) {
        this.editor = editor;
        getStyleClass().add("teclado-mn");
        setSpacing(12);

        HBox pestanas = new HBox(6);
        pestanas.getStyleClass().add("teclado-pestanas");
        pestanas.setAlignment(Pos.CENTER_LEFT);
        for (Teclas.Pestana p : Teclas.PESTANAS) {
            armandoLetras = p == Teclas.LETRAS;
            Node vista = vista(p);
            armandoLetras = false;
            if (p == Teclas.LETRAS) {
                letras = vista;
                griegas = vista(Teclas.GRIEGAS);
            }
            ToggleButton b = new ToggleButton(p.titulo());
            b.getStyleClass().add("teclado-pildora");
            b.setToggleGroup(grupo);
            b.setFocusTraversable(false);
            b.setOnAction(e -> {
                if (!b.isSelected()) {
                    b.setSelected(true);
                }
                contenido.getChildren().setAll(vista);
                editor.enfocar();
            });
            pestanas.getChildren().add(b);
        }
        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);
        Button mas = new Button("•••");
        mas.getStyleClass().add("teclado-mas");
        mas.setFocusTraversable(false);
        mas.setTooltip(new Tooltip("Configuración"));
        mas.setOnAction(e -> onConfiguracion.run());
        pestanas.getChildren().addAll(hueco, mas);

        getChildren().addAll(pestanas, contenido);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        ((ToggleButton) pestanas.getChildren().getFirst()).fire();
    }

    /** Lo que hace "•••". */
    public void setOnConfiguracion(Runnable accion) {
        onConfiguracion = accion == null ? () -> { } : accion;
    }

    /** LaTeX de la respuesta anterior para "ans"; null si todavía no hay. */
    public void setRespuestaAnterior(Supplier<String> proveedor) {
        respuestaAnterior = proveedor == null ? () -> null : proveedor;
    }

    // ------------------------------------------------------------------ armado

    private Node vista(Teclas.Pestana p) {
        if (p.derecha() == null) {
            return grilla(p.izquierda());
        }
        GridPane izq = grilla(p.izquierda());
        GridPane der = grilla(p.derecha());
        HBox.setHgrow(izq, Priority.ALWAYS);
        HBox.setHgrow(der, Priority.ALWAYS);
        HBox caja = new HBox(18, izq, der);
        caja.setFillHeight(true);
        return caja;
    }

    private GridPane grilla(Teclas.Bloque b) {
        GridPane g = new GridPane();
        g.getStyleClass().add("teclado-grilla");
        g.setHgap(6);
        g.setVgap(6);
        int columnas = b.columnas();
        for (int c = 0; c < columnas; c++) {
            // Todas las columnas parten del mismo ancho y crecen o se encogen por igual; al achicar el panel,
            // la columna de "Enter" o de una plantilla se detiene en lo que mide su texto y las demás siguen.
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPrefWidth(ANCHO_COLUMNA);
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            g.getColumnConstraints().add(cc);
        }
        List<List<Teclas.Tecla>> filas = b.filas();
        for (int f = 0; f < filas.size(); f++) {
            List<Teclas.Tecla> fila = filas.get(f);
            // Una fila más corta se estira: la última tecla ocupa lo que sobra (por ejemplo "espacio").
            int c = 0;
            for (int k = 0; k < fila.size(); k++) {
                Teclas.Tecla t = fila.get(k);
                int ancho = 1;
                if (t == Teclas.ESPACIO) {
                    ancho = columnas - fila.size() + 1;
                }
                g.add(boton(t), c, f, ancho, 1);
                c += ancho;
            }
        }
        return g;
    }

    private Button boton(Teclas.Tecla t) {
        Button b = new Button(t.etiqueta());
        b.getStyleClass().add("teclado-tecla");
        switch (t.tipo()) {
            case ESPECIAL -> b.getStyleClass().add("teclado-especial");
            case ENTER -> b.getStyleClass().add("teclado-enter");
            case CONSTANTE -> b.getStyleClass().add("teclado-constante");
            case DESACTIVADA -> b.getStyleClass().add("teclado-desactivada");
            case NORMAL -> { }
        }
        if (t.ayuda() != null) {
            Tooltip tip = new Tooltip(t.ayuda());
            tip.setShowDelay(javafx.util.Duration.millis(300));
            b.setTooltip(tip);
        }
        b.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        // Nunca "Ent…": la tecla no baja de lo que mide su texto; el panel no deja achicar más el teclado.
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setMinHeight(40);
        // Sin foco propio: el editor conserva el cursor.
        b.setFocusTraversable(false);
        b.setOnAction(e -> pulsar(t, b));
        botones.putIfAbsent(t, b);
        if (armandoLetras && t.accion() == Teclas.Accion.ESCRIBIR && t.valor().length() == 1 && Character.isLetter(t.valor().charAt(0))
                && t.valor().charAt(0) < 128) {
            botonesDeLetra.add(b);
        }
        return b;
    }

    /** Pulsa la tecla como con el mouse (pruebas). */
    void pulsar(Teclas.Tecla t) {
        Button b = botones.get(t);
        if (b == null) {
            throw new IllegalArgumentException("No hay botón para " + t.etiqueta());
        }
        b.fire();
    }

    private void pulsar(Teclas.Tecla t, Button b) {
        switch (t.accion()) {
            case INSERTAR -> editor.insertar(t.valor());
            case ESCRIBIR -> {
                String v = t.valor();
                if (mayuscula && botonesDeLetra.contains(b)) {
                    v = v.toUpperCase();
                    ponerMayuscula(false);
                }
                editor.escribir(v);
            }
            case BORRAR -> editor.borrar();
            case IZQUIERDA -> editor.moverIzquierda();
            case DERECHA -> editor.moverDerecha();
            case ENTER -> editor.pulsarEnter();
            case ANS -> {
                String ans = respuestaAnterior.get();
                if (ans != null && !ans.isBlank()) {
                    editor.insertar("\\left(" + ans + "\\right)");
                } else {
                    editor.enfocar();
                }
            }
            case MAYUSCULA -> {
                ponerMayuscula(!mayuscula);
                editor.enfocar();
            }
            case CAMBIAR_CAPA -> {
                Node actual = contenido.getChildren().isEmpty() ? null : contenido.getChildren().getFirst();
                contenido.getChildren().setAll(actual == letras ? griegas : letras);
                editor.enfocar();
            }
            case NADA -> editor.enfocar();
        }
    }

    private void ponerMayuscula(boolean si) {
        mayuscula = si;
        for (Button b : botonesDeLetra) {
            String l = b.getText();
            b.setText(si ? l.toUpperCase() : l.toLowerCase());
        }
    }
}
