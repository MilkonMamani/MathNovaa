package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * F16 y F17: la tabla de Ruffini como en el cuaderno.
 * <pre>
 *       |  2   -3   -3    2
 *   x=2 |       4    2   -2
 *       |-------------------
 *       |  2    1   -1  | 0
 * </pre>
 * Encima, las fichas de los candidatos p/q (las que fallaron, plegadas). Las tablas se apilan: el
 * cociente de una es la fila de arriba de la siguiente. Al pasar el mouse por un número de la fila
 * del medio, una flecha muestra de dónde sale (resultado anterior × raíz).
 */
public final class TablaRuffini extends VBox {

    private static final PseudoClass CERO = PseudoClass.getPseudoClass("cero");
    private static final PseudoClass RELLENO = PseudoClass.getPseudoClass("relleno");
    /** Columnas de la grilla: 0 raíz, 1 línea vertical, 2… números. */
    private static final int PRIMERA = 2;

    private final ModeloRuffini modelo;
    private final Pane capa = new Pane();
    private final Line flecha = new Line();
    private final Polygon punta = new Polygon();
    private final Label origen = new Label();

    public TablaRuffini(TrabajoIntermedio.TablaRuffini trabajo) {
        modelo = ModeloRuffini.de(trabajo);
        getStyleClass().addAll("pizarra", "ruffini");
        setSpacing(10);
        getChildren().addAll(fichas(), tablas());
    }

    public ModeloRuffini modelo() {
        return modelo;
    }

    private Node fichas() {
        VBox caja = new VBox(6);
        FlowPane raices = new FlowPane(6, 6);
        for (String r : modelo.raices()) {
            raices.getChildren().add(ficha(r, "ficha-raiz"));
        }
        caja.getChildren().add(raices);
        if (!modelo.fallidos().isEmpty()) {
            FlowPane fallidos = new FlowPane(6, 6);
            for (String f : modelo.fallidos()) {
                fallidos.getChildren().add(ficha(f, "ficha-fallida"));
            }
            TitledPane plegable = new TitledPane("Candidatos que no son raíz (" + modelo.fallidos().size() + ")",
                    fallidos);
            plegable.setExpanded(false);
            plegable.getStyleClass().add("seccion-plegable");
            caja.getChildren().add(plegable);
        }
        return caja;
    }

    private static Label ficha(String texto, String clase) {
        Label l = new Label(texto);
        l.getStyleClass().addAll("ficha", clase);
        return l;
    }

    private Node tablas() {
        GridPane g = new GridPane();
        g.getStyleClass().add("ruffini-grilla");
        int fila = 0;
        List<ModeloRuffini.Tabla> tablas = modelo.tablas();
        int columnas = tablas.getFirst().arriba().size();

        for (int k = 0; k < tablas.size(); k++) {
            ModeloRuffini.Tabla t = tablas.get(k);
            if (k == 0) {
                for (int i = 0; i < t.arriba().size(); i++) {
                    ModeloRuffini.Celda c = t.arriba().get(i);
                    Label l = numero(c.texto());
                    l.pseudoClassStateChanged(RELLENO, c.relleno());
                    g.add(l, PRIMERA + i, fila);
                }
                fila++;
            }
            int filaMedio = fila;
            Label raiz = new Label(t.raiz());
            raiz.getStyleClass().add("ruffini-raiz");
            g.add(raiz, 0, filaMedio);
            Label[] medio = new Label[t.medio().size()];
            for (int i = 1; i < t.medio().size(); i++) {
                medio[i] = numero(t.medio().get(i));
                medio[i].getStyleClass().add("ruffini-producto");
                g.add(medio[i], PRIMERA + i, filaMedio);
            }
            Region horizontal = new Region();
            horizontal.getStyleClass().add("ruffini-horizontal");
            g.add(horizontal, PRIMERA, filaMedio + 1, t.arriba().size(), 1);

            int filaAbajo = filaMedio + 2;
            Label[] abajo = new Label[t.abajo().size()];
            for (int i = 0; i < t.abajo().size(); i++) {
                abajo[i] = numero(t.abajo().get(i));
                g.add(abajo[i], PRIMERA + i, filaAbajo);
            }
            Label residuo = numero(t.residuo());
            residuo.getStyleClass().add("ruffini-residuo");
            residuo.pseudoClassStateChanged(CERO, t.residuoCero());
            g.add(residuo, PRIMERA + t.abajo().size(), filaAbajo);

            // Flecha: el producto sale del resultado anterior × raíz.
            for (int i = 1; i < medio.length; i++) {
                Label destino = medio[i];
                Label fuente = i - 1 < abajo.length ? abajo[i - 1] : residuo;
                String texto = t.origenes().get(i);
                destino.setOnMouseEntered(e -> mostrarFlecha(fuente, destino, texto));
                destino.setOnMouseExited(e -> ocultarFlecha());
            }
            fila = filaAbajo + 1;
        }
        Region vertical = new Region();
        vertical.getStyleClass().add("ruffini-vertical");
        g.add(vertical, 1, 0, 1, fila);

        flecha.getStyleClass().add("ruffini-flecha");
        punta.getStyleClass().add("ruffini-flecha-punta");
        origen.getStyleClass().add("ruffini-origen");
        capa.getChildren().addAll(flecha, punta, origen);
        capa.setMouseTransparent(true);
        ocultarFlecha();

        StackPane conCapa = new StackPane(g, capa);
        conCapa.setAlignment(Pos.TOP_LEFT);
        conCapa.setMaxWidth(Region.USE_PREF_SIZE);
        g.setUserData(columnas);
        return conCapa;
    }

    private static Label numero(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("ruffini-numero");
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private void mostrarFlecha(Node fuente, Node destino, String texto) {
        Point2D a = centro(fuente);
        Point2D b = centro(destino);
        if (a == null || b == null) {
            return;
        }
        // De la celda de abajo (columna anterior) a la del medio: diagonal hacia arriba a la derecha.
        flecha.setStartX(a.getX());
        flecha.setStartY(a.getY() - 8);
        flecha.setEndX(b.getX() - 6);
        flecha.setEndY(b.getY() + 6);
        double ang = Math.atan2(flecha.getEndY() - flecha.getStartY(), flecha.getEndX() - flecha.getStartX());
        double x = flecha.getEndX();
        double y = flecha.getEndY();
        punta.getPoints().setAll(x, y,
                x - 8 * Math.cos(ang - 0.4), y - 8 * Math.sin(ang - 0.4),
                x - 8 * Math.cos(ang + 0.4), y - 8 * Math.sin(ang + 0.4));
        origen.setText(texto);
        origen.relocate(b.getX() + 10, b.getY() - 22);
        flecha.setVisible(true);
        punta.setVisible(true);
        origen.setVisible(true);
    }

    private void ocultarFlecha() {
        flecha.setVisible(false);
        punta.setVisible(false);
        origen.setVisible(false);
    }

    private Point2D centro(Node n) {
        Bounds enEscena = n.localToScene(n.getBoundsInLocal());
        if (enEscena == null) {
            return null;
        }
        Bounds enCapa = capa.sceneToLocal(enEscena);
        return enCapa == null ? null : new Point2D(enCapa.getCenterX(), enCapa.getCenterY());
    }
}
