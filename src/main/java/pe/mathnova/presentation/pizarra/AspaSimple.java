package pe.mathnova.presentation.pizarra;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.latex.TextoLatex;
import pe.mathnova.presentation.resolucion.FormulaAjustable;
import pe.mathnova.presentation.resolucion.Igualdades;

/**
 * F12 y F13: el aspa simple como en el cuaderno, con tipografía matemática.
 * <pre>
 *   x  ╲╱  −2      x·(−3) = −3x
 *   x  ╱╲  −3      x·(−2) = −2x
 *                  −3x − 2x = −5x ✓
 * </pre>
 * Debajo, la descomposición del término central con los productos cruzados, la agrupación y los factores,
 * con los "=" alineados: x² − 5x + 6 = x² − 2x − 3x + 6 = x(x − 2) − 3(x − 2) = (x − 2)(x − 3).
 */
public final class AspaSimple extends VBox {

    private static final double ANCHO_CRUCE = 56;
    private static final double ALTO_CRUCE = 52;

    private final ModeloAspa modelo;

    public AspaSimple(TrabajoIntermedio.AspaSimple trabajo) {
        modelo = ModeloAspa.de(trabajo);
        getStyleClass().addAll("pizarra", "aspa-simple");
        setSpacing(12);
        setMinWidth(0);

        GridPane aspa = new GridPane();
        aspa.getStyleClass().add("aspa-grilla");
        aspa.add(celda(modelo.izquierda().get(0), Pos.CENTER_RIGHT), 0, 0);
        aspa.add(celda(modelo.izquierda().get(1), Pos.CENTER_RIGHT), 0, 1);
        aspa.add(celda(modelo.derecha().get(0), Pos.CENTER_LEFT), 2, 0);
        aspa.add(celda(modelo.derecha().get(1), Pos.CENTER_LEFT), 2, 1);
        aspa.add(cruce(), 1, 0, 1, 2);
        aspa.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        List<String> cuentas = new ArrayList<>(modelo.productos());
        cuentas.add(modelo.suma());
        Igualdades productos = Igualdades.deTextos(cuentas, Pizarra.TAMANO - 2, Pizarra.TAMANO_MINIMO - 1);
        productos.getStyleClass().add("aspa-productos");

        // El aspa y sus productos lado a lado; si la pizarra se angosta, los productos bajan.
        javafx.scene.layout.FlowPane fila = new javafx.scene.layout.FlowPane(28, 10, aspa, productos);
        fila.setRowValignment(javafx.geometry.VPos.CENTER);
        fila.setMinWidth(0);
        getChildren().add(fila);

        List<String> cadena = cadena(trabajo);
        if (!cadena.isEmpty()) {
            getChildren().add(new Igualdades(cadena.stream().map(Igualdades.Renglon::deLatex).toList(),
                    Pizarra.TAMANO, Pizarra.TAMANO_MINIMO));
        }
    }

    public ModeloAspa modelo() {
        return modelo;
    }

    /**
     * La descomposición, la agrupación y los factores en LaTeX, tres renglones; vacía en el aspa homogénea.
     * Con (a₁x + c₁)(a₂x + c₂): a₁a₂x² + a₂c₁x + a₁c₂x + c₁c₂ = a₂x(a₁x + c₁) + c₂(a₁x + c₁).
     */
    public static List<String> cadena(TrabajoIntermedio.AspaSimple a) {
        if (a.segunda() != 0) {
            return List.of();
        }
        String x = String.valueOf(a.variable());
        Racional cuadrado = a.a1().multiplicar(a.a2());
        Racional b1 = a.a2().multiplicar(a.c1());
        Racional b2 = a.a1().multiplicar(a.c2());
        Racional c = a.c1().multiplicar(a.c2());
        String original = termino(cuadrado, x + "^{2}", true) + termino(b1.sumar(b2), x, false)
                + termino(c, "", false);
        String descompuesto = termino(cuadrado, x + "^{2}", true) + termino(b1, x, false) + termino(b2, x, false)
                + termino(c, "", false);
        String binomio1 = "(" + termino(a.a1(), x, true) + termino(a.c1(), "", false) + ")";
        String binomio2 = "(" + termino(a.a2(), x, true) + termino(a.c2(), "", false) + ")";
        String agrupado = termino(a.a2(), x, true) + binomio1 + termino(a.c2(), "", false, true) + binomio1;
        String factores = binomio1 + binomio2;
        return List.of(original + " = " + descompuesto, "= " + agrupado, "= " + factores);
    }

    /** "+ 3x", "− 2", "x²"; con {@code factor} un coeficiente ±1 se escribe solo con su signo: "− (x − 2)". */
    private static String termino(Racional c, String letra, boolean primero, boolean factor) {
        if (c.esCero()) {
            return "";
        }
        Racional abs = c.signo() < 0 ? c.negar() : c;
        String signo = c.signo() < 0 ? (primero ? "-" : " - ") : (primero ? "" : " + ");
        boolean uno = abs.equals(Racional.UNO);
        String coef = uno && (!letra.isEmpty() || factor) ? "" : ImpresoraLatex.imprimir(new Constante(abs));
        return signo + coef + letra;
    }

    private static String termino(Racional c, String letra, boolean primero) {
        return termino(c, letra, primero, false);
    }

    private static Node celda(String texto, Pos alineacion) {
        String latex = TextoLatex.convertir(texto);
        FormulaAjustable f = new FormulaAjustable(Pizarra.TAMANO, Pizarra.TAMANO);
        f.setLatex(latex == null ? texto : latex);
        f.setPartible(false);
        HBox caja = new HBox(f);
        caja.setAlignment(alineacion);
        caja.setMinHeight(ALTO_CRUCE / 2);
        caja.getStyleClass().add("aspa-celda");
        return caja;
    }

    /** Dos líneas cruzadas: de cada término de la izquierda al de la otra fila a la derecha. */
    private static Pane cruce() {
        Line a = new Line(6, 8, ANCHO_CRUCE - 6, ALTO_CRUCE - 8);
        Line b = new Line(6, ALTO_CRUCE - 8, ANCHO_CRUCE - 6, 8);
        a.getStyleClass().add("aspa-linea");
        b.getStyleClass().add("aspa-linea");
        Pane p = new Pane(a, b);
        p.setMinSize(ANCHO_CRUCE, ALTO_CRUCE);
        p.setPrefSize(ANCHO_CRUCE, ALTO_CRUCE);
        p.setMaxSize(ANCHO_CRUCE, ALTO_CRUCE);
        return p;
    }
}
