package pe.mathnova.presentation.resolucion;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
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
import pe.mathnova.presentation.latex.VistaLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;
import pe.mathnova.presentation.pizarra.Pizarra;

public final class FilaPaso {

    private static final PseudoClass ABIERTA = PseudoClass.getPseudoClass("abierta");
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

    public Node nodo() {
        return nodo;
    }

    public Node numero() {
        return numero;
    }

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
        return new VistaLatex(renderizador, pe.mathnova.domain.expresion.ImpresoraLatex.imprimir(e));
    }
}
