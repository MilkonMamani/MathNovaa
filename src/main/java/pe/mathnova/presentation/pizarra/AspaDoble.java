package pe.mathnova.presentation.pizarra;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

public final class AspaDoble extends VBox {

    private final ModeloAspaDobleXY modelo;

    public AspaDoble(TrabajoIntermedio.AspaDoble trabajo) {
        modelo = ModeloAspaDobleXY.de(trabajo);
        getStyleClass().addAll("pizarra", "aspa-doble");
        setSpacing(8);

        GridPane columnas = new GridPane();
        columnas.getStyleClass().add("aspa-grilla");
        for (int c = 0; c < 3; c++) {
            Label t = new Label(modelo.titulos().get(c));
            t.getStyleClass().add("rotulo");
            columnas.add(t, c, 0);
            columnas.add(celda(modelo.fila1().get(c)), c, 1);
            columnas.add(celda(modelo.fila2().get(c)), c, 2);
        }
        getChildren().addAll(columnas,
                renglon("1. Extremos: " + String.join("   ", modelo.extremos())),
                renglon("2. Aspas: " + modelo.aspas().get(0)),
                renglon("    " + modelo.aspas().get(1)),
                renglon("    " + modelo.aspas().get(2)),
                resultado(modelo.resultado()));
    }

    public ModeloAspaDobleXY modelo() {
        return modelo;
    }

    private static Label celda(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("aspa-celda");
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static Label renglon(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("pizarra-texto");
        l.setWrapText(true);
        return l;
    }

    private static Label resultado(String texto) {
        Label l = renglon(texto);
        l.getStyleClass().add("pizarra-resultado");
        return l;
    }
}
