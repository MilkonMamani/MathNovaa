package pe.mathnova.presentation.pizarra;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.TextoLatex;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.Igualdades;

public final class TablaPartes extends VBox {

    static final String FORMULA_TEXTO = "uv − ∫v du";
    static final String FORMULA = "\\int u\\,dv = uv - \\int v\\,du";

    private static final Pattern FUNCION_SIN_PARENTESIS =
            Pattern.compile("\\b(ln|log|sen|sin|cos|tan|tg|arctan|arcsen|arcsin|arccos)\\s+([a-z])\\b");
    private static final Pattern UNO_SOBRE = Pattern.compile("^\\(?1/(.+?)\\)?\\s*d([a-z])$");

    public TablaPartes(TrabajoIntermedio.TablaPartes t) {
        getStyleClass().addAll("pizarra", "tabla-partes");
        setSpacing(10);
        setMinWidth(0);
        Label ilate = new Label(FormateadorTexto.pizarra(t.ilate().startsWith("ILATE") ? t.ilate() : "ILATE: " + t.ilate()));
        ilate.getStyleClass().add("pizarra-explicacion");
        ilate.setWrapText(true);
        ilate.setMinWidth(0);

        VBox izquierda = columna(new String[] {"u", t.u()}, new String[] {"du", t.du()});
        VBox derecha = columna(new String[] {"dv", t.dv()}, new String[] {"v", t.v()});
        FlowPane tabla = new FlowPane(48, 10, izquierda, derecha);
        tabla.getStyleClass().add("partes-grilla");
        tabla.setMinWidth(0);

        getChildren().addAll(ilate, tabla);
        List<Igualdades.Renglon> renglones = renglones(t.lineas());
        if (!renglones.isEmpty()) {
            getChildren().add(new Igualdades(renglones, Pizarra.TAMANO, Pizarra.TAMANO_MINIMO));
        }
    }

    static List<Igualdades.Renglon> renglones(List<String> lineas) {
        List<Igualdades.Renglon> r = new ArrayList<>();
        for (String l : lineas) {
            String linea = l;
            int k = linea.indexOf(" = " + FORMULA_TEXTO);
            if (k >= 0) {
                r.add(Igualdades.Renglon.deLatex(FORMULA));
                linea = linea.substring(0, k) + linea.substring(k + 3 + FORMULA_TEXTO.length());
            }
            String latex = latex(linea);
            r.add(latex == null ? Igualdades.Renglon.deTexto(linea) : Igualdades.Renglon.deLatex(latex));
        }
        return r;
    }

    private static VBox columna(String[] arriba, String[] abajo) {
        VBox c = new VBox(8, celda(arriba[0], arriba[1]), celda(abajo[0], abajo[1]));
        c.setMinWidth(Region.USE_PREF_SIZE);
        c.getStyleClass().add("partes-columna");
        return c;
    }

    private static FormulaAjustable celda(String simbolo, String valor) {
        String v = valor.strip();
        Matcher m = UNO_SOBRE.matcher(v);
        String latex;
        if (simbolo.equals("du") && m.matches() && !m.group(1).contains("/")) {
            String den = TextoLatex.convertir(parentesisEnFunciones(m.group(1)));
            latex = den == null ? null : "\\frac{d" + m.group(2) + "}{" + den + "}";
        } else {
            latex = latex(v);
        }
        FormulaAjustable f = new FormulaAjustable(Pizarra.TAMANO, Pizarra.TAMANO_MINIMO);
        f.setLatex(simbolo + " = " + (latex == null ? "\\text{" + v + "}" : latex));
        f.setPartible(false);
        f.getStyleClass().add("partes-celda");
        return f;
    }

    static String latex(String texto) {
        String l = TextoLatex.convertir(parentesisEnFunciones(texto));
        return TextoLatex.conDiferenciales(l);
    }

    static String parentesisEnFunciones(String texto) {
        return FUNCION_SIN_PARENTESIS.matcher(texto).replaceAll("$1($2)");
    }
}
