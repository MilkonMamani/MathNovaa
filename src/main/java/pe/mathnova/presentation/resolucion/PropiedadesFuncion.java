package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.css.PseudoClass;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

public final class PropiedadesFuncion extends VBox {

    public PropiedadesFuncion(VistaPropiedades v) {
        setSpacing(10);
        GridPane tabla = new GridPane();
        tabla.getStyleClass().add("propiedades-tabla");
        int fila = 0;
        for (VistaPropiedades.Fila f : v.filas()) {
            Label nombre = new Label(f.nombre());
            nombre.getStyleClass().add("propiedad-nombre");
            nombre.setMinWidth(USE_PREF_SIZE);
            Label valor = new Label(f.valor());
            valor.getStyleClass().add("propiedad-valor");
            valor.setWrapText(true);
            GridPane.setValignment(nombre, VPos.TOP);
            tabla.addRow(fila++, nombre, valor);
        }
        getChildren().add(tabla);
        if (!v.signos().isEmpty()) {
            Label rotulo = new Label("Signo de la función");
            rotulo.getStyleClass().add("propiedad-nombre");
            getChildren().addAll(rotulo, new RectaSignos(v.marcas(), v.signos()));
        } else if (v.avisoSignos() != null) {
            Label aviso = new Label(v.avisoSignos());
            aviso.getStyleClass().add("descartado-motivo");
            aviso.setWrapText(true);
            getChildren().add(aviso);
        }
    }

    static final class RectaSignos extends Pane {

        private static final PseudoClass POSITIVO = PseudoClass.getPseudoClass("positivo");
        private static final PseudoClass NEGATIVO = PseudoClass.getPseudoClass("negativo");
        private static final double Y_RECTA = 30;

        private final Line recta = new Line();
        private final List<Circle> puntos = new ArrayList<>();
        private final List<Label> marcas = new ArrayList<>();
        private final List<Label> signos = new ArrayList<>();

        RectaSignos(List<String> raices, List<Integer> signosDeTramos) {
            getStyleClass().add("recta-signos");
            recta.getStyleClass().add("recta-linea");
            getChildren().add(recta);
            for (String r : raices) {
                Circle c = new Circle(4);
                c.getStyleClass().add("recta-punto");
                Label l = new Label(r);
                l.getStyleClass().add("recta-raiz");
                puntos.add(c);
                marcas.add(l);
                getChildren().addAll(c, l);
            }
            for (int s : signosDeTramos) {
                Label l = new Label(s > 0 ? "+" : "−");
                l.getStyleClass().add("recta-signo");
                l.pseudoClassStateChanged(POSITIVO, s > 0);
                l.pseudoClassStateChanged(NEGATIVO, s < 0);
                signos.add(l);
                getChildren().add(l);
            }
            setAccessibleText("Tabla de signos");
        }

        @Override
        protected void layoutChildren() {
            double ancho = getWidth();
            recta.setStartX(4);
            recta.setEndX(ancho - 4);
            recta.setStartY(Y_RECTA);
            recta.setEndY(Y_RECTA);
            int n = puntos.size();
            double tramo = ancho / (n + 1);
            for (int i = 0; i < n; i++) {
                double x = tramo * (i + 1);
                puntos.get(i).setCenterX(x);
                puntos.get(i).setCenterY(Y_RECTA);
                Label m = marcas.get(i);
                m.autosize();
                m.relocate(x - m.getWidth() / 2, Y_RECTA + 8);
            }
            for (int i = 0; i < signos.size(); i++) {
                Label s = signos.get(i);
                s.autosize();
                s.relocate(tramo * (i + 0.5) - s.getWidth() / 2, Y_RECTA - 8 - s.getHeight());
            }
        }
    }
}
