package pe.mathnova.presentation.pizarra;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * F12 y F13: el aspa simple como en el cuaderno.
 * <pre>
 *   2x  ╲╱  -1
 *   3x  ╱╲   2
 * </pre>
 * Al costado, los productos cruzados y su suma; abajo, el resultado leído por filas.
 */
public final class AspaSimple extends VBox {

    private static final double ANCHO_CRUCE = 56;
    private static final double ALTO_CRUCE = 52;

    private final ModeloAspa modelo;

    public AspaSimple(TrabajoIntermedio.AspaSimple trabajo) {
        modelo = ModeloAspa.de(trabajo);
        getStyleClass().addAll("pizarra", "aspa-simple");
        setSpacing(10);

        GridPane aspa = new GridPane();
        aspa.getStyleClass().add("aspa-grilla");
        aspa.add(celda(modelo.izquierda().get(0), Pos.CENTER_RIGHT), 0, 0);
        aspa.add(celda(modelo.izquierda().get(1), Pos.CENTER_RIGHT), 0, 1);
        aspa.add(celda(modelo.derecha().get(0), Pos.CENTER_LEFT), 2, 0);
        aspa.add(celda(modelo.derecha().get(1), Pos.CENTER_LEFT), 2, 1);
        aspa.add(cruce(), 1, 0, 1, 2);

        VBox productos = new VBox(4);
        productos.getStyleClass().add("aspa-productos");
        for (String p : modelo.productos()) {
            Label l = new Label(p);
            l.getStyleClass().add("pizarra-texto");
            productos.getChildren().add(l);
        }
        Label suma = new Label(modelo.suma());
        suma.getStyleClass().addAll("pizarra-texto", "aspa-suma");
        productos.getChildren().add(suma);
        productos.setAlignment(Pos.CENTER_LEFT);

        HBox fila = new HBox(28, aspa, productos);
        fila.setAlignment(Pos.CENTER_LEFT);

        Label resultado = new Label(modelo.resultado());
        resultado.getStyleClass().addAll("pizarra-texto", "pizarra-resultado");
        getChildren().addAll(fila, resultado);
    }

    public ModeloAspa modelo() {
        return modelo;
    }

    private static Label celda(String texto, Pos alineacion) {
        Label l = new Label(texto);
        l.getStyleClass().add("aspa-celda");
        l.setMinHeight(ALTO_CRUCE / 2);
        l.setAlignment(alineacion);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    /** Dos líneas cruzadas: de cada término de la izquierda al de la otra fila a la derecha. */
    private static Pane cruce() {
        Line a = new Line(6, 8, ANCHO_CRUCE - 6, ALTO_CRUCE - 8);
        Line b = new Line(6, ALTO_CRUCE - 8, ANCHO_CRUCE - 6, 8);
        a.getStyleClass().add("aspa-linea");
        b.getStyleClass().add("aspa-linea");
        Pane p = new Pane(a, b);
        p.setMinSize(ANCHO_CRUCE, ALTO_CRUCE);
        p.setPrefSize(ANCHO_CRUCE, ALTO_CRUCE);
        p.setMaxSize(ANCHO_CRUCE, ALTO_CRUCE);
        return p;
    }
}
