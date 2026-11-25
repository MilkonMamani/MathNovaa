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

public final class PanelAnalisis {

    private PanelAnalisis() {
    }

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

    public static void factorizacion(TablaAnalisis t, AnalisisFactorizacion a) {
        for (AnalisisFactorizacion.Fila f : a.filas()) {
            if (f.latex() != null) {
                t.formula(f.nombre(), f.latex());
            } else {
                t.frase(f.nombre(), f.texto());
            }
            if (AnalisisFactorizacion.RAICES.equals(f.nombre()) && a.notaRaices() != null) {
                t.nota(a.notaRaices());
            }
        }
        if (a.notaRaices() != null && a.filas().stream().noneMatch(f -> AnalisisFactorizacion.RAICES.equals(f.nombre()))) {
            t.nota(a.notaRaices());
        }
        a.recta().ifPresent(recta -> {
            t.seccion("Conjunto solución");
            RectaSolucion dibujo = new RectaSolucion(recta);
            FormulaAjustable cs = TablaAnalisis.formula(null);
            cs.getStyleClass().add("conjunto-solucion");
            javafx.scene.layout.FlowPane selector = new javafx.scene.layout.FlowPane(4, 4);
            selector.getStyleClass().add("selector-condicion");
            javafx.scene.control.ToggleGroup grupo = new javafx.scene.control.ToggleGroup();
            for (AnalisisFactorizacion.Condicion c : AnalisisFactorizacion.Condicion.values()) {
                javafx.scene.control.ToggleButton b = new javafx.scene.control.ToggleButton(c.texto());
                b.getStyleClass().add("condicion");
                b.setUserData(c);
                b.setToggleGroup(grupo);
                selector.getChildren().add(b);
            }
            grupo.selectedToggleProperty().addListener((obs, antes, ahora) -> {
                if (ahora == null) {
                    grupo.selectToggle(antes);
                    return;
                }
                AnalisisFactorizacion.Condicion c = (AnalisisFactorizacion.Condicion) ahora.getUserData();
                dibujo.mostrar(c);
                cs.setLatex(a.conjuntoSolucion(c));
            });
            grupo.selectToggle(grupo.getToggles().getFirst());
            t.bloque(selector).bloque(dibujo).bloque(cs);
        });
    }

    public static void tabla(TablaAnalisis t, TablaLimite tabla) {
        VBox caja = (VBox) tabla(tabla);
        caja.getChildren().removeFirst();
        t.seccion("Tabla de valores").bloque(caja);
    }

    public static void seccion(TablaAnalisis t, SeccionAnalisis s) {
        t.seccion(s.titulo());
        for (VistaPropiedades.Fila f : s.filas()) {
            t.valor(f.nombre(), f.valor());
        }
        if (!s.signos().isEmpty()) {
            t.fila(s.rotuloSignos() == null ? "Signo" : s.rotuloSignos(), new javafx.scene.layout.Region());
            t.bloque(new PropiedadesFuncion.RectaSignos(s.marcas(), s.signos()));
        }
        if (s.aviso() != null) {
            t.nota(s.aviso());
        }
    }

    public static void propiedades(TablaAnalisis t, VistaPropiedades v) {
        for (VistaPropiedades.Fila f : v.filas()) {
            t.valor(f.nombre(), f.valor());
        }
        if (!v.signos().isEmpty()) {
            t.fila("Signo de la función", new javafx.scene.layout.Region());
            t.bloque(new PropiedadesFuncion.RectaSignos(v.marcas(), v.signos()));
        } else if (v.avisoSignos() != null) {
            t.nota(v.avisoSignos());
        }
    }

    public static void asintotas(TablaAnalisis t, Asintotas a) {
        t.seccion("Asíntotas");
        boolean alguna = false;
        if (!a.verticales().isEmpty()) {
            t.valor("Verticales", String.join(", ", a.textoVerticales()));
            alguna = true;
        }
        if (a.textoHorizontal().isPresent()) {
            t.valor("Horizontal", a.textoHorizontal().get());
            alguna = true;
        }
        if (a.textoOblicua().isPresent()) {
            t.valor("Oblicua", a.textoOblicua().get());
            alguna = true;
        }
        if (!alguna) {
            t.nota("No tiene asíntotas: al simplificar queda un polinomio o el denominador no se anula.");
        }
        a.notas().forEach(t::nota);
        t.nota("En la gráfica van en rojo punteado.");
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
