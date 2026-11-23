package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * F19: el aspa doble especial en tres columnas (x² | x | término independiente). Primero los
 * extremos y cuánto falta para el término x²; ese resto se reparte en la columna central y se
 * verifican las dos aspas laterales (x³ y x).
 */
public final class AspaDobleEspecial extends VBox {

    private final ModeloAspaDoble modelo;

    public AspaDobleEspecial(TrabajoIntermedio.AspaDobleEspecial trabajo) {
        modelo = ModeloAspaDoble.de(trabajo);
        getStyleClass().addAll("pizarra", "aspa-doble");
        setSpacing(8);

        GridPane columnas = new GridPane();
        columnas.getStyleClass().add("aspa-grilla");
        List<String> titulos = List.of("x²", "x", "indep.");
        for (int c = 0; c < 3; c++) {
            Label t = new Label(titulos.get(c));
            t.getStyleClass().add("rotulo");
            columnas.add(t, c, 0);
            columnas.add(celda(modelo.fila1().get(c), c == 1), c, 1);
            columnas.add(celda(modelo.fila2().get(c), c == 1), c, 2);
        }

        getChildren().addAll(columnas,
                renglon("1. Extremos: " + String.join("   ", modelo.extremos())),
                renglon("2. " + modelo.seTiene()),
                renglon("3. Columna central: " + modelo.central()),
                renglon("4. Aspas laterales: " + modelo.laterales().get(0)),
                renglon("    " + modelo.laterales().get(1)),
                resultado(modelo.resultado()));
    }

    public ModeloAspaDoble modelo() {
        return modelo;
    }

    private static Label celda(String texto, boolean central) {
        Label l = new Label(texto);
        l.getStyleClass().add(central ? "aspa-celda-central" : "aspa-celda");
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
