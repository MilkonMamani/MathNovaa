package pe.mathnova.presentation.resolucion;

import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class CadenaIgualdades extends VBox {

    public record Eslabon(String latex, String nota, String notaLatex) {
        public Eslabon(String latex) {
            this(latex, null, null);
        }
    }

    private final List<Eslabon> eslabones;

    public CadenaIgualdades(List<Eslabon> eslabones, double maximo, double minimo) {
        super(6);
        this.eslabones = List.copyOf(eslabones);
        getStyleClass().add("pizarra-cadena");
        setMinWidth(0);
        FormulaAjustable medida = VistaDesarrollo.formula("=", maximo, minimo);
        double ancho = Math.ceil(medida.anchoNatural()) + 2;
        for (int i = 0; i < this.eslabones.size(); i++) {
            Eslabon e = this.eslabones.get(i);
            if (e.nota() != null) {
                getChildren().add(nota(e, ancho, maximo, minimo));
            }
            FormulaAjustable f = VistaDesarrollo.formula((i == 0 ? "\\phantom{=}\\;" : "=\\;") + e.latex(),
                    maximo, minimo);
            f.setMinWidth(0);
            HBox.setHgrow(f, Priority.ALWAYS);
            HBox fila = new HBox(f);
            fila.setAlignment(Pos.TOP_LEFT);
            fila.setMinWidth(0);
            fila.getStyleClass().add("pizarra-cadena-fila");
            getChildren().add(fila);
        }
    }

    private static VBox nota(Eslabon e, double ancho, double maximo, double minimo) {
        Label texto = new Label(e.nota());
        texto.setWrapText(true);
        texto.setMinWidth(0);
        texto.getStyleClass().add("pizarra-cadena-nota");
        VBox caja = new VBox(2, texto);
        if (e.notaLatex() != null) {
            caja.getChildren().add(VistaDesarrollo.formula(e.notaLatex(), maximo - 2, minimo - 1));
        }
        caja.setPadding(new Insets(4, 0, 0, ancho + 8));
        caja.setMinWidth(0);
        caja.getStyleClass().add("pizarra-cadena-justificacion");
        return caja;
    }

    public List<String> expresiones() {
        return eslabones.stream().map(Eslabon::latex).toList();
    }
}
