package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * V04: el diagrama de árbol de la regla de la cadena.
 * <pre>
 *          f
 *        /   \
 *       x     y      ∂f/∂x, ∂f/∂y en las ramas de arriba
 *       |     |
 *       t     t      dx/dt, dy/dt en las de abajo
 * </pre>
 * Debajo, la suma df/dt = fx·x' + fy·y'.
 */
public final class ArbolCadena extends VBox {

    private static final double SEPARACION = 150;
    private static final double NIVEL = 70;

    public ArbolCadena(TrabajoIntermedio.ArbolCadena a) {
        getStyleClass().addAll("pizarra", "arbol-cadena");
        setSpacing(8);
        List<String> medias = a.intermedias();
        int n = medias.size();
        double ancho = Math.max(1, n) * SEPARACION;
        double raizX = ancho / 2;
        Pane dibujo = new Pane();
        dibujo.getChildren().add(nodo(a.funcion(), raizX, 0));
        for (int i = 0; i < n; i++) {
            double cx = SEPARACION / 2 + i * SEPARACION;
            dibujo.getChildren().addAll(linea(raizX, 22, cx, NIVEL - 2),
                    rotulo(a.ramasArriba().get(i), (raizX + cx) / 2 + (cx < raizX ? -90 : 6), NIVEL / 2 - 8),
                    nodo(medias.get(i), cx, NIVEL),
                    linea(cx, NIVEL + 22, cx, 2 * NIVEL - 2),
                    rotulo(a.ramasAbajo().get(i), cx + 6, NIVEL * 1.5 - 8),
                    nodo(a.parametro(), cx, 2 * NIVEL));
        }
        dibujo.setMinSize(ancho + 60, 2 * NIVEL + 30);
        dibujo.setPrefSize(ancho + 60, 2 * NIVEL + 30);
        Label suma = new Label(FormateadorTexto.pizarra(a.suma()));
        suma.getStyleClass().addAll("pizarra-texto", "pizarra-resultado");
        suma.setWrapText(true);
        getChildren().addAll(dibujo, suma);
    }

    private static Label nodo(String texto, double cx, double y) {
        Label l = new Label(texto);
        l.getStyleClass().add("arbol-nodo");
        l.setLayoutX(cx - 8);
        l.setLayoutY(y);
        return l;
    }

    private static Label rotulo(String texto, double x, double y) {
        Label l = new Label(FormateadorTexto.pizarra(texto));
        l.getStyleClass().add("arbol-rama");
        l.setLayoutX(x);
        l.setLayoutY(y);
        return l;
    }

    private static Line linea(double x0, double y0, double x1, double y1) {
        Line l = new Line(x0, y0, x1, y1);
        l.getStyleClass().add("aspa-linea");
        return l;
    }
}
