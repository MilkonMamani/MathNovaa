package pe.mathnova.presentation.pizarra;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

/** Elige el dibujo de pizarra (HU06) de cada trabajo intermedio. */
public final class Pizarra {

    private Pizarra() {
    }

    public static Node de(TrabajoIntermedio trabajo) {
        return switch (trabajo) {
            case TrabajoIntermedio.TablaRuffini t -> new TablaRuffini(t);
            case TrabajoIntermedio.AspaSimple a -> new AspaSimple(a);
            case TrabajoIntermedio.AspaDobleEspecial a -> new AspaDobleEspecial(a);
            case TrabajoIntermedio.SistemaCoeficientes s -> new SistemaCoeficientes(s);
            case TrabajoIntermedio.Sustitucion s -> new Sustitucion(s);
            case TrabajoIntermedio.Agrupacion a -> renglones(a.grupos().toArray(String[]::new));
            case TrabajoIntermedio.Comparacion c -> renglones(c.detalle());
            case TrabajoIntermedio.TablaPartes p -> new TablaPartes(p);
            case TrabajoIntermedio.TrianguloSustitucion t -> new TrianguloSustitucion(t);
            case TrabajoIntermedio.AspaDoble a -> new AspaDoble(a);
            case TrabajoIntermedio.ArbolCadena a -> new ArbolCadena(a);
        };
    }

    /** Renglones de texto, uno debajo de otro. */
    static VBox renglones(String... lineas) {
        VBox caja = new VBox();
        caja.getStyleClass().addAll("trabajo", "pizarra");
        for (String l : lineas) {
            Label etiqueta = new Label(FormateadorTexto.pizarra(l));
            etiqueta.setWrapText(true);
            caja.getChildren().add(etiqueta);
        }
        return caja;
    }
}
