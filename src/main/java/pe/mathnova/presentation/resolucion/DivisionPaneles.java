package pe.mathnova.presentation.resolucion;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;

public class DivisionPaneles extends SplitPane {

    public DivisionPaneles() {
        getItems().addListener((javafx.collections.ListChangeListener<Node>) c -> {
            while (c.next()) {
                for (Node n : c.getAddedSubList()) {
                    if (n instanceof Region r) {
                        r.widthProperty().addListener((obs, antes, ahora) -> requestLayout());
                    }
                }
            }
        });
    }

    @Override
    protected double computePrefHeight(double width) {
        double alto = 0;
        for (Node n : getItems()) {
            alto = Math.max(alto, n.prefHeight(anchoDe(n)));
        }
        return snappedTopInset() + alto + snappedBottomInset();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    private static double anchoDe(Node n) {
        return n instanceof Region r && r.getWidth() > 0 ? r.getWidth() : -1;
    }
}
