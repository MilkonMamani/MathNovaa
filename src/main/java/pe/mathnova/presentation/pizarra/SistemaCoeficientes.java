package pe.mathnova.presentation.pizarra;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/** F20: el producto planteado, la tabla "coeficiente de xⁿ = …" y la solución encontrada. */
public final class SistemaCoeficientes extends VBox {

    public SistemaCoeficientes(TrabajoIntermedio.SistemaCoeficientes s) {
        getStyleClass().addAll("pizarra", "sistema-coeficientes");
        setSpacing(8);

        Label planteo = texto("Se plantea " + s.planteo());
        GridPane tabla = new GridPane();
        tabla.getStyleClass().add("sistema-tabla");
        Label c1 = new Label("Coeficiente de");
        Label c2 = new Label("Ecuación");
        c1.getStyleClass().add("rotulo");
        c2.getStyleClass().add("rotulo");
        tabla.addRow(0, c1, c2);
        int fila = 1;
        for (TrabajoIntermedio.SistemaCoeficientes.Ecuacion e : s.ecuaciones()) {
            tabla.addRow(fila++, texto(e.potencia()), texto(e.ecuacion()));
        }
        Label solucion = texto("Solución: " + String.join(", ", s.solucion()));
        solucion.getStyleClass().add("sistema-solucion");
        Label resultado = texto(s.resultado());
        resultado.getStyleClass().add("pizarra-resultado");
        getChildren().addAll(planteo, tabla, solucion, resultado);
    }

    private static Label texto(String t) {
        Label l = new Label(FormateadorTexto.pizarra(t));
        l.getStyleClass().add("pizarra-texto");
        l.setWrapText(true);
        return l;
    }
}
