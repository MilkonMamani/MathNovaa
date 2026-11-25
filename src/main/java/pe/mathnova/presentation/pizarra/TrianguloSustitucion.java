package pe.mathnova.presentation.pizarra;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.TextoLatex;
import pe.mathnova.presentation.resolucion.Igualdades;

public final class TrianguloSustitucion extends VBox {

    private static final double ANCHO = 170;
    private static final double ALTO = 100;
    private static final double MARGEN = 30;

    public TrianguloSustitucion(TrabajoIntermedio.TrianguloSustitucion t) {
        getStyleClass().addAll("pizarra", "triangulo-sustitucion");
        setSpacing(8);
        setMinWidth(0);
        getChildren().add(renglones(java.util.List.of("Con " + t.sustitucion())));

        double x0 = MARGEN;
        double y0 = MARGEN + ALTO;
        double x1 = MARGEN + ANCHO;
        double y1 = MARGEN;
        Polygon triangulo = new Polygon(x0, y0, x1, y0, x1, y1);
        triangulo.getStyleClass().add("triangulo-lado");
        Rectangle recto = new Rectangle(x1 - 10, y0 - 10, 10, 10);
        recto.getStyleClass().add("triangulo-recto");
        Arc angulo = new Arc(x0, y0, 22, 22, 0, Math.toDegrees(Math.atan2(ALTO, ANCHO)));
        angulo.setType(ArcType.OPEN);
        angulo.getStyleClass().add("triangulo-angulo");

        Label theta = etiqueta("θ", x0 + 26, y0 - 22);
        Label adyacente = etiqueta(t.adyacente(), x0 + ANCHO / 2 - 20, y0 + 4);
        Label opuesto = etiqueta(t.opuesto(), x1 + 6, y1 + ALTO / 2 - 10);
        Label hipotenusa = etiqueta(t.hipotenusa(), x0 + ANCHO / 2 - 50, y1 + ALTO / 2 - 26);
        Pane dibujo = new Pane(triangulo, recto, angulo, theta, adyacente, opuesto, hipotenusa);
        dibujo.setMinSize(ANCHO + 2 * MARGEN + 80, ALTO + 2 * MARGEN);
        dibujo.setPrefSize(ANCHO + 2 * MARGEN + 80, ALTO + 2 * MARGEN);

        java.util.List<String> lista = new java.util.ArrayList<>();
        lista.add(t.angulo());
        lista.addAll(t.razones());
        javafx.scene.Node razones = renglones(lista);
        javafx.scene.layout.FlowPane fila = new javafx.scene.layout.FlowPane(20, 8, dibujo, razones);
        fila.setRowValignment(javafx.geometry.VPos.CENTER);
        fila.setMinWidth(0);
        getChildren().add(fila);
        if (!t.lineas().isEmpty()) {
            getChildren().add(renglones(t.lineas()));
        }
    }

    private static Label etiqueta(String texto, double x, double y) {
        Label l = new Label(FormateadorTexto.pizarra(texto));
        l.getStyleClass().add("triangulo-etiqueta");
        l.setLayoutX(x);
        l.setLayoutY(y);
        return l;
    }

    private static javafx.scene.Node renglones(java.util.List<String> lineas) {
        java.util.List<Igualdades.Renglon> r = new java.util.ArrayList<>();
        for (String l : lineas) {
            for (Igualdades.Renglon x : Igualdades.Renglon.partidos(l)) {
                r.add(x.latex() == null ? x : new Igualdades.Renglon(TextoLatex.conDiferenciales(x.latex()),
                        x.texto(), x.marca()));
            }
        }
        Igualdades i = new Igualdades(r, Pizarra.TAMANO, Pizarra.TAMANO_MINIMO);
        i.setMaxWidth(Double.MAX_VALUE);
        return i;
    }
}
