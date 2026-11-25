package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class VistaDesarrollo extends VBox {

    private final List<ToggleButton> controles = new ArrayList<>();
    private final List<Node> detalles = new ArrayList<>();

    public VistaDesarrollo(Desarrollo d, double maximo, double minimo) {
        super(6);
        getStyleClass().add("pizarra-desarrollo");
        setMinWidth(0);
        for (Desarrollo.Renglon r : d.renglones()) {
            getChildren().add(switch (r) {
                case Desarrollo.Texto t -> texto(t.texto());
                case Desarrollo.Formula f -> formula(f.latex(), maximo, minimo);
                case Desarrollo.Definicion def -> definicion(def, maximo, minimo);
            });
        }
    }

    private static Label texto(String t) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.setMinWidth(0);
        l.getStyleClass().add("pizarra-explicacion");
        return l;
    }

    static FormulaAjustable formula(String latex, double maximo, double minimo) {
        FormulaAjustable f = Igualdades.formula(maximo, minimo);
        f.setLatex(latex);
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private Node definicion(Desarrollo.Definicion d, double maximo, double minimo) {
        VBox caja = new VBox(6);
        caja.setMinWidth(0);
        caja.getStyleClass().add("pizarra-definicion");
        if (d.texto() != null) {
            caja.getChildren().add(texto(d.texto()));
        }
        FormulaAjustable f = formula(d.latex(), maximo, minimo);
        f.setMaxWidth(Region.USE_PREF_SIZE);
        if (d.detalle().isEmpty()) {
            caja.getChildren().add(f);
            return caja;
        }
        ToggleButton ver = new ToggleButton(d.accion());
        ver.getStyleClass().add("pizarra-ver-detalle");
        ver.setMinWidth(Region.USE_PREF_SIZE);
        FlowPane fila = new FlowPane(14, 4, f, ver);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setRowValignment(javafx.geometry.VPos.CENTER);
        fila.setMinWidth(0);
        VBox exacto = new VBox(4);
        exacto.setMinWidth(0);
        exacto.getStyleClass().add("pizarra-definicion-exacta");
        for (String l : d.detalle()) {
            exacto.getChildren().add(formula(l, maximo, minimo));
        }
        mostrar(exacto, false);
        String ocultar = d.accion().replaceFirst("^Ver ", "Ocultar ");
        ver.selectedProperty().addListener((obs, antes, si) -> {
            ver.setText(si ? ocultar : d.accion());
            mostrar(exacto, si);
        });
        controles.add(ver);
        detalles.add(exacto);
        caja.getChildren().addAll(fila, exacto);
        return caja;
    }

    private static void mostrar(Node n, boolean si) {
        n.setVisible(si);
        n.setManaged(si);
    }

    public List<ToggleButton> controles() {
        return List.copyOf(controles);
    }

    public List<Node> detalles() {
        return List.copyOf(detalles);
    }
}
