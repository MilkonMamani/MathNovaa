package pe.mathnova.presentation.pizarra;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * I03: la tabla de la integración por partes como en el cuaderno.
 * <pre>
 *   u  = ln x        dv = x dx
 *   du = (1/x) dx    v  = x²/2
 * </pre>
 * Arriba, por qué u es esa (ILATE); abajo, uv − ∫v du con los valores.
 */
public final class TablaPartes extends VBox {

    public TablaPartes(TrabajoIntermedio.TablaPartes t) {
        getStyleClass().addAll("pizarra", "tabla-partes");
        setSpacing(8);
        Label ilate = new Label(FormateadorTexto.pizarra(t.ilate()));
        ilate.getStyleClass().add("pizarra-texto");
        ilate.setWrapText(true);

        GridPane tabla = new GridPane();
        tabla.getStyleClass().add("partes-grilla");
        tabla.add(celda("u = " + t.u()), 0, 0);
        tabla.add(celda("dv = " + t.dv()), 1, 0);
        tabla.add(celda("du = " + t.du()), 0, 1);
        tabla.add(celda("v = " + t.v()), 1, 1);
        getChildren().addAll(ilate, tabla);
        for (String l : t.lineas()) {
            Label r = new Label(FormateadorTexto.pizarra(l));
            r.getStyleClass().add("pizarra-texto");
            r.setWrapText(true);
            getChildren().add(r);
        }
    }

    private static Label celda(String texto) {
        Label l = new Label(FormateadorTexto.pizarra(texto));
        l.getStyleClass().add("partes-celda");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }
}
