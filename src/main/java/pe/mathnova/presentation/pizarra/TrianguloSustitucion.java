package pe.mathnova.presentation.pizarra;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * I05: el triángulo rectángulo de la sustitución trigonométrica. θ está abajo a la izquierda, el
 * ángulo recto abajo a la derecha: el cateto de abajo es el adyacente, el de la derecha el opuesto y
 * la diagonal la hipotenusa. Al costado, las razones que se leen del triángulo para regresar a x.
 */
public final class TrianguloSustitucion extends VBox {

    private static final double ANCHO = 170;
    private static final double ALTO = 100;
    private static final double MARGEN = 30;

    public TrianguloSustitucion(TrabajoIntermedio.TrianguloSustitucion t) {
        getStyleClass().addAll("pizarra", "triangulo-sustitucion");
        setSpacing(8);
        getChildren().add(renglon("Con " + t.sustitucion()));

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

        VBox razones = new VBox(4);
        razones.getChildren().add(renglon(t.angulo()));
        t.razones().forEach(r -> razones.getChildren().add(renglon(r)));
        HBox fila = new HBox(20, dibujo, razones);
        getChildren().add(fila);
        t.lineas().forEach(l -> getChildren().add(renglon(l)));
    }

    private static Label etiqueta(String texto, double x, double y) {
        Label l = new Label(FormateadorTexto.pizarra(texto));
        l.getStyleClass().add("triangulo-etiqueta");
        l.setLayoutX(x);
        l.setLayoutY(y);
        return l;
    }

    private static Label renglon(String texto) {
        Label l = new Label(FormateadorTexto.pizarra(texto));
        l.getStyleClass().add("pizarra-texto");
        l.setWrapText(true);
        return l;
    }
}
