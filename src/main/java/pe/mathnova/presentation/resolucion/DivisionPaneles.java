package pe.mathnova.presentation.resolucion;

import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Region;

/**
 * El SplitPane de "Resolver ejercicio" (herramientas | Pizarra). Un SplitPane común mide su alto con lo que
 * pediría cada panel sin saber su ancho; en un panel angosto el texto se parte en más líneas, el panel queda
 * más alto que el SplitPane y este lo centra, así que la tarjeta sobresale por arriba. Aquí el alto se mide
 * con el ancho real de cada panel.
 */
public class DivisionPaneles extends SplitPane {

    public DivisionPaneles() {
        // Cuando un panel cambia de ancho (al arrastrar), su alto puede cambiar: se vuelve a medir.
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
