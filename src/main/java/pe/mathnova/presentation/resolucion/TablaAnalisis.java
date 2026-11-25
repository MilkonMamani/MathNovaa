package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.TextoLatex;

/**
 * La pestaña Análisis como una sola lista compacta sobre blanco: títulos de sección pequeños y filas de dos
 * columnas, la etiqueta a la izquierda y el valor a la derecha (una fórmula o una frase). Las etiquetas de todas
 * las secciones comparten la misma columna, así los valores quedan alineados.
 *
 * <p>Si el panel se angosta tanto que el valor no cabe al lado de la etiqueta, cada valor pasa debajo de su
 * etiqueta, con una pequeña sangría; las fórmulas se ajustan al ancho que les queda.
 */
public final class TablaAnalisis extends Region {

    /** Tamaño de las fórmulas: el de la interfaz, como el texto de las etiquetas. */
    static final double TAMANO = 15;
    static final double TAMANO_MINIMO = 11;
    /** Ancho mínimo del valor al lado de la etiqueta; con menos, va debajo. */
    static final double VALOR_MINIMO = 170;
    private static final double SEPARACION = 16;
    private static final double ENTRE_FILAS = 6;
    private static final double ANTES_DE_SECCION = 10;
    private static final double SANGRIA = 12;

    private sealed interface Entrada permits Seccion, Fila, Bloque {
    }

    private record Seccion(Label titulo) implements Entrada {
    }

    private record Fila(Label nombre, Node valor) implements Entrada {
    }

    /** Ocupa todo el ancho (una recta, una tabla de valores, una nota). */
    private record Bloque(Node nodo, boolean sangria) implements Entrada {
    }

    private final List<Entrada> entradas = new ArrayList<>();

    public TablaAnalisis() {
        getStyleClass().add("tabla-analisis");
    }

    /** Un título de sección pequeño ("Crecimiento", "Conjunto solución"). */
    public TablaAnalisis seccion(String titulo) {
        Label l = new Label(titulo);
        l.getStyleClass().add("analisis-seccion");
        l.setWrapText(true);
        agregar(new Seccion(l), l);
        return this;
    }

    /** Una fila con el valor como fórmula LaTeX. */
    public TablaAnalisis formula(String nombre, String latex) {
        return fila(nombre, formula(latex));
    }

    /**
     * Una fila con un valor del motor escrito como texto de pizarra: si es matemática ("(−∞, 0) ∪ (0, ∞)",
     * "x = −1") se dibuja como fórmula; si es una frase, como texto.
     */
    public TablaAnalisis valor(String nombre, String valor) {
        String latex = matematica(valor) ? TextoLatex.convertir(valor) : null;
        return latex == null ? frase(nombre, valor) : formula(nombre, latex);
    }

    /** Lleva notación matemática (un número, un signo, una letra sola…); "ningún intervalo" o "sí, exacto" no. */
    static boolean matematica(String valor) {
        if (valor == null || valor.isBlank()) {
            return false;
        }
        String v = valor.strip();
        return v.matches(".*[0-9∞∪∩=≠≤≥<>^²³√π±∅{}()|/+−'′-].*") || v.matches("[a-zA-Z]");
    }

    /** Una fila con el valor como frase normal. */
    public TablaAnalisis frase(String nombre, String texto) {
        return fila(nombre, texto(texto, "analisis-valor"));
    }

    /** Una fila con cualquier nodo como valor (el selector de condición). */
    public TablaAnalisis fila(String nombre, Node valor) {
        Label l = new Label(nombre);
        l.getStyleClass().add("analisis-nombre");
        l.setWrapText(true);
        agregar(new Fila(l, valor), l, valor);
        return this;
    }

    /** Una nota en gris debajo del último valor, alineada con él. */
    public TablaAnalisis nota(String texto) {
        Label l = texto(texto, "analisis-nota");
        agregar(new Bloque(l, true), l);
        return this;
    }

    /** Un nodo a todo el ancho. */
    public TablaAnalisis bloque(Node n) {
        agregar(new Bloque(n, false), n);
        return this;
    }

    /** Un nodo alineado con la columna de los valores (la fórmula del conjunto solución). */
    public TablaAnalisis bloqueValor(Node n) {
        agregar(new Bloque(n, true), n);
        return this;
    }

    public boolean vacia() {
        return entradas.isEmpty();
    }

    static FormulaAjustable formula(String latex) {
        FormulaAjustable f = new FormulaAjustable(TAMANO, TAMANO_MINIMO);
        f.getStyleClass().add("analisis-formula");
        f.setLatex(latex);
        return f;
    }

    private static Label texto(String t, String clase) {
        Label l = new Label(FormateadorTexto.pizarraConNegativos(t));
        l.getStyleClass().add(clase);
        l.setWrapText(true);
        l.setMinWidth(0);
        return l;
    }

    private void agregar(Entrada e, Node... nodos) {
        entradas.add(e);
        getChildren().addAll(nodos);
    }

    // ------------------------------------------------------------------ disposición

    /** El ancho de la columna de etiquetas: la etiqueta más larga, sin pasar de la mitad del panel. */
    private double columna(double ancho) {
        double max = 0;
        for (Entrada e : entradas) {
            if (e instanceof Fila f) {
                max = Math.max(max, f.nombre().prefWidth(-1));
            }
        }
        return Math.min(Math.ceil(max), Math.max(0, ancho * 0.5));
    }

    /** Si los valores van debajo de su etiqueta. */
    boolean apilada(double ancho) {
        return ancho - columna(ancho) - SEPARACION < VALOR_MINIMO;
    }

    @Override
    protected double computePrefWidth(double alto) {
        Insets in = getInsets();
        return in.getLeft() + in.getRight() + 360;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double a = ancho < 0 ? computePrefWidth(-1) : ancho;
        Insets in = getInsets();
        return in.getTop() + in.getBottom() + recorrer(a - in.getLeft() - in.getRight(), false);
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        recorrer(getWidth() - in.getLeft() - in.getRight(), true);
    }

    /** Mide (y si {@code ubicar}, ubica) todo con el ancho dado; devuelve el alto. */
    private double recorrer(double ancho, boolean ubicar) {
        Insets in = getInsets();
        double x0 = in.getLeft();
        double y = in.getTop();
        double col = columna(ancho);
        boolean apilada = apilada(ancho);
        double xValor = apilada ? SANGRIA : col + SEPARACION;
        double anchoValor = Math.max(0, ancho - xValor);
        boolean primero = true;
        for (Entrada e : entradas) {
            switch (e) {
                case Seccion s -> {
                    if (!primero) {
                        y += ANTES_DE_SECCION;
                    }
                    double h = s.titulo().prefHeight(ancho);
                    if (ubicar) {
                        s.titulo().resizeRelocate(x0, y, ancho, h);
                    }
                    y += h + ENTRE_FILAS;
                }
                case Fila f -> {
                    double anchoNombre = apilada ? ancho : col;
                    double hn = f.nombre().prefHeight(anchoNombre);
                    double hv = alto(f.valor(), anchoValor);
                    if (apilada) {
                        if (ubicar) {
                            f.nombre().resizeRelocate(x0, y, anchoNombre, hn);
                            f.valor().resizeRelocate(x0 + xValor, y + hn + 2, anchoValor, hv);
                        }
                        y += hn + 2 + hv + ENTRE_FILAS;
                    } else {
                        double h = Math.max(hn, hv);
                        if (ubicar) {
                            // La etiqueta, centrada con la primera línea del valor
                            double dy = Math.max(0, Math.min(hv, 26) - hn) / 2;
                            f.nombre().resizeRelocate(x0, y + dy, anchoNombre, hn);
                            f.valor().resizeRelocate(x0 + xValor, y, anchoValor, hv);
                        }
                        y += h + ENTRE_FILAS;
                    }
                }
                case Bloque b -> {
                    double x = b.sangria() ? xValor : 0;
                    double w = ancho - x;
                    double h = alto(b.nodo(), w);
                    if (ubicar) {
                        b.nodo().resizeRelocate(x0 + x, y, w, h);
                    }
                    y += h + ENTRE_FILAS;
                }
            }
            primero = false;
        }
        return y - in.getTop() - (entradas.isEmpty() ? 0 : ENTRE_FILAS);
    }

    private static double alto(Node n, double ancho) {
        return n.getContentBias() == javafx.geometry.Orientation.HORIZONTAL || n instanceof Label
                || n instanceof FormulaAjustable ? n.prefHeight(ancho) : n.prefHeight(-1);
    }
}
