package pe.mathnova.presentation.pizarra;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;

public final class Sustitucion extends GridPane {

    public Sustitucion(TrabajoIntermedio.Sustitucion s) {
        getStyleClass().addAll("pizarra", "sustitucion");
        Label definicion = new Label("Con " + FormateadorTexto.pizarra(s.definicion()));
        definicion.getStyleClass().add("sustitucion-definicion");
        add(definicion, 0, 0, 2, 1);
        List<String[]> filas = List.of(
                new String[] {"1. Sustituir", s.sustituir()},
                new String[] {"2. Factorizar en " + s.variableNueva(), s.factorizar()},
                new String[] {"3. Regresar", s.regresar()});
        int f = 1;
        for (String[] fila : filas) {
            Label rotulo = new Label(fila[0]);
            rotulo.getStyleClass().add("rotulo");
            add(rotulo, 0, f);
            add(resaltar(FormateadorTexto.pizarra(fila[1]), s.variableNueva()), 1, f);
            f++;
        }
    }

    static TextFlow resaltar(String texto, String variable) {
        TextFlow flujo = new TextFlow();
        flujo.getStyleClass().add("pizarra-texto");
        StringBuilder normal = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            boolean coincide = texto.startsWith(variable, i)
                    && (i == 0 || !Character.isLetter(texto.charAt(i - 1)))
                    && (i + variable.length() >= texto.length() || !Character.isLetter(texto.charAt(i + variable.length())));
            if (coincide) {
                if (!normal.isEmpty()) {
                    flujo.getChildren().add(trozo(normal.toString(), "sustitucion-texto"));
                    normal.setLength(0);
                }
                flujo.getChildren().add(trozo(variable, "sustitucion-variable"));
                i += variable.length() - 1;
            } else {
                normal.append(texto.charAt(i));
            }
        }
        if (!normal.isEmpty()) {
            flujo.getChildren().add(trozo(normal.toString(), "sustitucion-texto"));
        }
        return flujo;
    }

    private static Text trozo(String t, String clase) {
        Text x = new Text(t);
        x.getStyleClass().add(clase);
        return x;
    }
}
