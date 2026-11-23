package pe.mathnova.presentation.resolucion;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.animacion.AnimadorResolucion;
import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.pizarra.Pizarra;

/**
 * Un paso del procedimiento (HU04): círculo numerado y nombre del método en una línea, y debajo, a
 * todo el ancho, el "Después" en LaTeX. Si la fórmula no cabe, se reduce de 18 px hasta 14 px y, si
 * aun así no cabe, pasa a dos líneas (ver {@link FormulaAjustable}); nunca se corta ni se desplaza. Con un clic, Enter o Espacio se despliega y
 * muestra Por qué aplica, Antes, Trabajo y Después.
 */
public final class FilaPaso {

    private static final PseudoClass ABIERTA = PseudoClass.getPseudoClass("abierta");
    /** Las fórmulas de los pasos se dibujan a 18 px y se reducen como mucho hasta 14 px. */
    static final double TAMANO = 18;
    static final double TAMANO_MINIMO = 14;

    private final VistaPaso paso;
    private final RenderizadorLatex renderizador;
    private final AnimadorResolucion animador;

    private final VBox nodo = new VBox();
    private final StackPane numero;
    private final SVGPath flecha = new SVGPath();
    private final VBox detalle = new VBox();
    private boolean abierta;
    private boolean detalleListo;

    public FilaPaso(VistaPaso paso, RenderizadorLatex renderizador, AnimadorResolucion animador) {
        this.paso = paso;
        this.renderizador = renderizador;
        this.animador = animador;

        numero = new StackPane(new Label(String.valueOf(paso.orden())));
        numero.getStyleClass().add("paso-numero");

        Label nombre = new Label(paso.titulo());
        nombre.getStyleClass().add("paso-nombre");
        nombre.setWrapText(true);
        nombre.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nombre, Priority.ALWAYS);
        if (paso.ayuda() != null && !paso.ayuda().equals(paso.titulo())) {
            nombre.setTooltip(new Tooltip(paso.ayuda()));
        }

        flecha.setContent("M0 0 L5 5 L0 10");
        flecha.getStyleClass().add("paso-flecha");
        StackPane contenedorFlecha = new StackPane(flecha);
        contenedorFlecha.setMinSize(16, 16);
        Tooltip.install(contenedorFlecha, new Tooltip("Ver el detalle del paso"));

        HBox titulo = new HBox(numero, nombre, contenedorFlecha);
        titulo.getStyleClass().add("paso-titulo");
        titulo.setAlignment(Pos.CENTER_LEFT);

        // Sin "después" (igualar a cero, despejar en una ecuación): las líneas van en la fila misma.
        Node cuerpo = paso.despues() != null ? formulaAjustable(paso.despues())
                : paso.trabajo() != null ? Pizarra.de(paso.trabajo()) : new VBox();
        VBox cabecera = new VBox(titulo, cuerpo);
        cabecera.getStyleClass().add("paso-fila");
        cabecera.setFocusTraversable(true);
        cabecera.setAccessibleRole(AccessibleRole.BUTTON);
        cabecera.setAccessibleText("Paso " + paso.orden() + ": " + paso.titulo());
        cabecera.setOnMouseClicked(e -> {
            cabecera.requestFocus();
            alternar();
        });
        cabecera.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                alternar();
                e.consume();
            }
        });

        detalle.getStyleClass().add("paso-detalle");
        detalle.setMinHeight(0);
        detalle.setVisible(false);
        detalle.setManaged(false);

        nodo.getStyleClass().add("paso");
        nodo.getChildren().addAll(cabecera, detalle);
        // Límites: la factorización (L02) o las derivadas (L07) plegadas bajo su rótulo.
        if (!paso.hijos().isEmpty()) {
            VBox filas = new VBox(6);
            for (VistaPaso h : paso.hijos()) {
                filas.getChildren().add(new FilaPaso(h, renderizador, animador).nodo());
            }
            String rotuloGrupo = paso.grupo() == null ? "Pasos internos" : paso.grupo();
            TitledPane anidados = new TitledPane(rotuloGrupo + " (" + paso.hijos().size()
                    + (paso.hijos().size() == 1 ? " paso)" : " pasos)"), filas);
            anidados.setExpanded(false);
            anidados.setAnimated(animador.activas());
            anidados.getStyleClass().addAll("seccion-plegable", "pasos-anidados");
            nodo.getChildren().add(anidados);
        }
    }

    /** La fila completa, para ponerla en la tarjeta y animarla. */
    public Node nodo() {
        return nodo;
    }

    /** El círculo numerado, que entra con un ScaleTransition. */
    public Node numero() {
        return numero;
    }

    /** La fórmula a todo el ancho de la fila: se reduce si no cabe y, si hace falta, pasa a dos líneas. */
    private Node formulaAjustable(Expresion e) {
        FormulaAjustable f = new FormulaAjustable(TAMANO, TAMANO_MINIMO);
        f.getStyleClass().add("paso-formula");
        f.setLatex(pe.mathnova.domain.expresion.ImpresoraLatex.imprimir(e));
        return f;
    }

    private void alternar() {
        if (!detalleListo) {
            llenarDetalle();
            detalleListo = true;
        }
        abierta = !abierta;
        nodo.pseudoClassStateChanged(ABIERTA, abierta);
        animador.desplegar(detalle, flecha, abierta);
    }

    /** El detalle se dibuja la primera vez que se abre. */
    private void llenarDetalle() {
        Label justificacion = new Label(FormateadorTexto.pizarra(paso.justificacion()));
        justificacion.setWrapText(true);
        justificacion.getStyleClass().add("justificacion");
        detalle.getChildren().addAll(rotulo("Por qué aplica"), justificacion);
        if (paso.antes() != null) {
            detalle.getChildren().addAll(rotulo("Antes"), imagen(paso.antes()));
        }
        if (paso.trabajo() != null && paso.despues() != null) {
            detalle.getChildren().addAll(rotulo("Trabajo"), Pizarra.de(paso.trabajo()));
        }
        if (paso.despues() != null) {
            detalle.getChildren().addAll(rotulo("Después"), imagen(paso.despues()));
        }
    }

    private static Label rotulo(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("rotulo");
        return l;
    }

    private ImageView imagen(Expresion e) {
        Image img = ImagenLatex.de(renderizador, e);
        ImageView vista = new ImageView(img);
        vista.setPreserveRatio(true);
        return vista;
    }
}
