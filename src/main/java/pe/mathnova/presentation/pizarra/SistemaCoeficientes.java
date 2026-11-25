package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.TextoLatex;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.Igualdades;

/**
 * F20 y fracciones parciales: el planteo, una ecuación por coeficiente (con qué coeficiente se igualó, en gris al
 * costado), la solución y lo que queda con los valores encontrados; todo con tipografía matemática.
 */
public final class SistemaCoeficientes extends VBox {

    public SistemaCoeficientes(TrabajoIntermedio.SistemaCoeficientes s) {
        getStyleClass().addAll("pizarra", "sistema-coeficientes");
        setSpacing(8);
        setMinWidth(0);

        getChildren().add(Igualdades.deTextos(List.of(s.planteo()), Pizarra.TAMANO, Pizarra.TAMANO_MINIMO));

        GridPane tabla = new GridPane();
        tabla.getStyleClass().add("sistema-tabla");
        tabla.setHgap(18);
        tabla.setVgap(6);
        int fila = 0;
        for (TrabajoIntermedio.SistemaCoeficientes.Ecuacion e : s.ecuaciones()) {
            Label que = new Label(FormateadorTexto.pizarra(e.potencia()));
            que.getStyleClass().add("pizarra-explicacion");
            tabla.addRow(fila++, formula(e.ecuacion()), que);
        }
        getChildren().add(tabla);

        // A = 2,  B = −1
        getChildren().add(formula(String.join(", ", s.solucion())));
        getChildren().add(Igualdades.deTextos(List.of(s.resultado()), Pizarra.TAMANO, Pizarra.TAMANO_MINIMO));
    }

    private static FormulaAjustable formula(String texto) {
        String latex = TextoLatex.convertir(texto);
        FormulaAjustable f = new FormulaAjustable(Pizarra.TAMANO, Pizarra.TAMANO_MINIMO);
        f.setLatex(latex == null ? "\\text{" + texto + "}" : latex.replace(", ", ",\\quad "));
        return f;
    }
}
