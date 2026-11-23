package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import pe.mathnova.domain.calculo.limites.Asintotas;
import pe.mathnova.domain.calculo.limites.TablaValores;
import pe.mathnova.presentation.FormateadorTexto;

/**
 * Sprint 5, pestaña Análisis: la tabla de valores de un límite (la forma numérica de "verlo") y las
 * asíntotas de una función racional (RF26).
 */
public final class PanelAnalisis {

    private PanelAnalisis() {
    }

    /** Dos tablas, x | f(x), que se acercan por la izquierda y por la derecha (o hacia ±∞). */
    public static Node tabla(TablaValores t, String conclusion) {
        VBox caja = new VBox(8);
        caja.getChildren().add(rotulo("Tabla de valores"));
        if (!t.izquierda().isEmpty()) {
            caja.getChildren().addAll(nota("Por la izquierda"), grilla(t.variable(), t.izquierda()));
        }
        caja.getChildren().addAll(nota(t.izquierda().isEmpty() ? "Hacia el infinito" : "Por la derecha"),
                grilla(t.variable(), t.derecha()));
        if (conclusion != null) {
            caja.getChildren().add(nota(conclusion));
        }
        return caja;
    }

    /**
     * Parte F: las dos columnas lado a lado, "Por la izquierda" y "Por la derecha" (x | f(x) cada una),
     * acercándose hacia abajo, y la fila final "→ 4" en verde. Al infinito, una sola columna.
     */
    public static Node tabla(TablaLimite t) {
        VBox caja = new VBox(8);
        caja.getChildren().add(rotulo("Tabla de valores"));
        String x = String.valueOf(t.variable());
        GridPane g = new GridPane();
        g.getStyleClass().add("tabla-valores");
        if (t.alInfinito()) {
            g.add(cabecera("Hacia el infinito"), 0, 0, 2, 1);
            g.add(cabecera(x), 0, 1);
            g.add(cabecera("f(" + x + ")"), 1, 1);
            for (int i = 0; i < t.derecha().size(); i++) {
                g.add(celda(t.derecha().get(i).x()), 0, i + 2);
                g.add(celda(t.derecha().get(i).valor()), 1, i + 2);
            }
        } else {
            g.add(cabecera("Por la izquierda"), 0, 0, 2, 1);
            g.add(cabecera("Por la derecha"), 2, 0, 2, 1);
            for (int c = 0; c < 4; c += 2) {
                g.add(cabecera(x), c, 1);
                g.add(cabecera("f(" + x + ")"), c + 1, 1);
            }
            int filas = Math.max(t.izquierda().size(), t.derecha().size());
            for (int i = 0; i < filas; i++) {
                if (i < t.izquierda().size()) {
                    g.add(celda(t.izquierda().get(i).x()), 0, i + 2);
                    g.add(celda(t.izquierda().get(i).valor()), 1, i + 2);
                }
                if (i < t.derecha().size()) {
                    g.add(celda(t.derecha().get(i).x()), 2, i + 2);
                    g.add(celda(t.derecha().get(i).valor()), 3, i + 2);
                }
            }
        }
        if (t.tiende() != null) {
            int fila = g.getRowCount();
            g.add(final_(t.tiende()), 1, fila);
            if (!t.alInfinito()) {
                g.add(final_(t.tiende()), 3, fila);
            }
        }
        caja.getChildren().add(g);
        if (t.conclusion() != null) {
            caja.getChildren().add(nota(t.conclusion()));
        }
        return caja;
    }

    private static Label final_(String t) {
        Label l = new Label(FormateadorTexto.pizarraConNegativos(t));
        l.getStyleClass().addAll("tabla-valores-celda", "tabla-valores-limite");
        return l;
    }

    private static GridPane grilla(char x, List<TablaValores.Fila> filas) {
        GridPane g = new GridPane();
        g.getStyleClass().add("tabla-valores");
        g.add(cabecera(String.valueOf(x)), 0, 0);
        g.add(cabecera("f(" + x + ")"), 1, 0);
        for (int i = 0; i < filas.size(); i++) {
            g.add(celda(filas.get(i).x()), 0, i + 1);
            g.add(celda(filas.get(i).valor()), 1, i + 1);
        }
        return g;
    }

    /** "Verticales: x = 1", "Oblicua: y = x + 1"…; "No tiene asíntotas" si no hay. */
    public static Node asintotas(Asintotas a) {
        VBox caja = new VBox(4);
        caja.getChildren().add(rotulo("Asíntotas"));
        List<String> lineas = new ArrayList<>();
        if (!a.verticales().isEmpty()) {
            lineas.add("Verticales: " + String.join(", ", a.textoVerticales()));
        }
        a.textoHorizontal().ifPresent(h -> lineas.add("Horizontal: " + h));
        a.textoOblicua().ifPresent(o -> lineas.add("Oblicua: " + o));
        if (lineas.isEmpty()) {
            lineas.add("No tiene asíntotas: al simplificar queda un polinomio o el denominador no se anula.");
        }
        lineas.addAll(a.notas());
        for (String l : lineas) {
            caja.getChildren().add(nota(FormateadorTexto.pizarraConNegativos(l)));
        }
        caja.getChildren().add(nota("En la gráfica van en rojo punteado."));
        return caja;
    }

    /** Sprint 6: título, tabla pequeña y, si hay, la recta de signos con su rótulo. */
    public static Node seccion(SeccionAnalisis s) {
        VBox caja = new VBox(6);
        caja.getChildren().add(rotulo(s.titulo()));
        if (!s.filas().isEmpty()) {
            GridPane tabla = new GridPane();
            tabla.getStyleClass().add("propiedades-tabla");
            int fila = 0;
            for (VistaPropiedades.Fila f : s.filas()) {
                Label nombre = new Label(f.nombre());
                nombre.getStyleClass().add("propiedad-nombre");
                nombre.setMinWidth(Label.USE_PREF_SIZE);
                Label valor = new Label(FormateadorTexto.pizarraConNegativos(f.valor()));
                valor.getStyleClass().add("propiedad-valor");
                valor.setWrapText(true);
                GridPane.setValignment(nombre, javafx.geometry.VPos.TOP);
                tabla.addRow(fila++, nombre, valor);
            }
            caja.getChildren().add(tabla);
        }
        if (!s.signos().isEmpty()) {
            Label rotulo = new Label(s.rotuloSignos() == null ? "Signo" : s.rotuloSignos());
            rotulo.getStyleClass().add("propiedad-nombre");
            caja.getChildren().addAll(rotulo, new PropiedadesFuncion.RectaSignos(s.marcas(), s.signos()));
        }
        if (s.aviso() != null) {
            Label aviso = nota(s.aviso());
            aviso.getStyleClass().add("descartado-motivo");
            caja.getChildren().add(aviso);
        }
        return caja;
    }

    private static Label rotulo(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("rotulo");
        return l;
    }

    private static Label nota(String t) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.getStyleClass().add("justificacion");
        return l;
    }

    private static Label cabecera(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("tabla-valores-cabecera");
        return l;
    }

    private static Label celda(String t) {
        Label l = new Label(FormateadorTexto.pizarraConNegativos(t));
        l.getStyleClass().add("tabla-valores-celda");
        return l;
    }
}
