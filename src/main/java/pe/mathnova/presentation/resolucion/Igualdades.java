package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import org.scilab.forge.jlatexmath.TeXFormula;

import pe.mathnova.presentation.FormateadorTexto;
import pe.mathnova.presentation.latex.TextoLatex;

/**
 * Renglones de operaciones escritos directamente sobre la pizarra, sin recuadro de fondo.
 *
 * <p>Si todos son igualdades y caben, los "=" quedan uno debajo del otro (un solo bloque {@code aligned}).
 * Si la pizarra se angosta, cada renglón va por su cuenta y, si hace falta, se parte en un "=", un + o −
 * de afuera o entre factores (ver {@link FormulaAjustable}); nunca se cortan exponentes, fracciones,
 * radicales ni paréntesis. Lo que no es matemática (una frase) queda como texto.
 */
public final class Igualdades extends Region {

    /**
     * Un renglón.
     *
     * @param latex la fórmula; null si es una frase
     * @param texto la frase (cuando no hay fórmula)
     * @param marca "✓" o "✗" al final, o null
     */
    public record Renglon(String latex, String texto, String marca) {

        /** Un renglón del motor en escritura de pizarra ("(x + 1)² = x² + 2x + 1"). */
        public static Renglon deTexto(String t) {
            String s = t == null ? "" : t.strip();
            String marca = null;
            if (s.endsWith("✓") || s.endsWith("✗")) {
                marca = s.substring(s.length() - 1);
                s = s.substring(0, s.length() - 1).strip();
            }
            String latex = TextoLatex.convertir(s);
            if (latex != null && !valido(latex)) {
                latex = null;
            }
            return new Renglon(latex, FormateadorTexto.pizarra(s), marca);
        }

        /**
         * Como {@link #deTexto}, pero si el renglón mezcla una frase con matemática ("Δ = 12256/2187 > 0: una
         * raíz real y dos complejas conjugadas") se separa en un renglón de fórmula y otro de texto; dos
         * igualdades separadas por ",  " van cada una en su renglón.
         */
        public static List<Renglon> partidos(String t) {
            String s = t == null ? "" : t.strip();
            // "Se regresa a x: (x² + 1)⁶/6": la frase como texto y la fórmula aparte.
            int dos = s.indexOf(": ");
            if (dos > 0 && s.substring(0, dos).strip().matches("[A-ZÁÉÍÓÚ][a-záéíóúñ]+( [a-záéíóúñ]+)+( [a-zA-Zθ][²³]?)?")) {
                Renglon der = deTexto(s.substring(dos + 2));
                if (der.latex() != null) {
                    return List.of(new Renglon(null, s.substring(0, dos + 1), null), der);
                }
            }
            Renglon r = deTexto(t);
            if (r.latex() != null || r.marca() != null || s.isEmpty()) {
                return List.of(r);
            }
            int k = s.indexOf(": ");
            if (k > 0) {
                String izq = s.substring(0, k).strip();
                String der = s.substring(k + 2).strip();
                Renglon a = deTexto(izq);
                List<Renglon> b = partidos(der);
                boolean hayFormula = a.latex() != null || b.stream().anyMatch(x -> x.latex() != null);
                if (hayFormula) {
                    List<Renglon> l = new ArrayList<>();
                    // Un rótulo corto ("y = mx:") queda como texto: nombra lo que sigue.
                    boolean rotulo = izq.length() <= 10;
                    l.add(a.latex() != null && !rotulo ? a : new Renglon(null, a.texto() + ":", null));
                    l.addAll(b);
                    return l;
                }
            }
            String[] partes = s.split(",\\s{2,}");
            if (partes.length > 1) {
                List<Renglon> l = new ArrayList<>();
                for (String p : partes) {
                    l.addAll(partidos(p));
                }
                if (l.stream().anyMatch(x -> x.latex() != null)) {
                    return l;
                }
            }
            return List.of(r);
        }

        public static Renglon deLatex(String latex) {
            return new Renglon(latex, null, null);
        }

        /** Una igualdad, o la continuación de la anterior ("= x + 3"). */
        boolean igualdad() {
            return latex != null && marca == null && TextoLatex.igualDeAfuera(latex) >= 0;
        }
    }

    private final double maximo;
    private final double minimo;
    private final FormulaAjustable alineado;
    private final VBox apilado = new VBox(6);

    public Igualdades(List<Renglon> renglones, double maximo, double minimo) {
        this.maximo = maximo;
        this.minimo = minimo;
        getStyleClass().add("pizarra-igualdades");
        setMinWidth(0);
        apilado.setMinWidth(0);
        for (Renglon r : renglones) {
            apilado.getChildren().add(nodo(r));
        }
        boolean alinear = renglones.size() >= 2 && alineables(renglones);
        if (alinear) {
            alineado = formula(maximo, minimo);
            alineado.setPartible(false);
            alineado.setLatex(aligned(renglones));
            getChildren().add(alineado);
        } else {
            alineado = null;
        }
        getChildren().add(apilado);
    }

    public static Igualdades deTextos(List<String> lineas, double maximo, double minimo) {
        return new Igualdades(lineas.stream().flatMap(l -> Renglon.partidos(l).stream()).toList(), maximo, minimo);
    }

    /**
     * Todas son igualdades (la primera con lado izquierdo) o la primera es una expresión sola y las demás la
     * continúan: "\lim (x − 2)(x + 1)²/(x − 2)" y debajo "= \lim (x + 1)²".
     */
    private static boolean alineables(List<Renglon> renglones) {
        Renglon primero = renglones.getFirst();
        if (primero.latex() == null || primero.marca() != null) {
            return false;
        }
        int k = TextoLatex.igualDeAfuera(primero.latex());
        List<Renglon> resto = renglones.subList(1, renglones.size());
        if (k > 0) {
            return resto.stream().allMatch(Renglon::igualdad);
        }
        return k < 0 && resto.stream().allMatch(r -> r.igualdad() && TextoLatex.igualDeAfuera(r.latex()) == 0);
    }

    /** {@code \begin{aligned} izq &= der \\ … \end{aligned}}. */
    static String aligned(List<Renglon> renglones) {
        List<String> filas = new ArrayList<>();
        for (Renglon r : renglones) {
            int k = TextoLatex.igualDeAfuera(r.latex());
            filas.add(k < 0 ? "&\\mathrel{\\phantom{=}} " + r.latex().strip()
                    : r.latex().substring(0, k).strip() + " &= " + r.latex().substring(k + 1).strip());
        }
        return "\\begin{aligned} " + String.join(" \\\\ ", filas) + " \\end{aligned}";
    }

    private Node nodo(Renglon r) {
        Node principal;
        if (r.latex() != null) {
            FormulaAjustable f = formula(maximo, minimo);
            f.setLatex(r.latex());
            principal = f;
        } else {
            Label l = new Label(r.texto());
            l.setWrapText(true);
            l.getStyleClass().add("pizarra-renglon-texto");
            principal = l;
        }
        if (r.marca() == null) {
            return principal;
        }
        Label marca = new Label(r.marca());
        marca.getStyleClass().addAll("pizarra-marca", "✓".equals(r.marca()) ? "pizarra-marca-ok" : "pizarra-marca-mal");
        marca.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(principal, Priority.SOMETIMES);
        HBox fila = new HBox(8, principal, marca);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setMinWidth(0);
        return fila;
    }

    /** Una fórmula de la Pizarra: se reparte en renglones antes de achicar la letra. */
    static FormulaAjustable formula(double maximo, double minimo) {
        FormulaAjustable f = new FormulaAjustable(maximo, minimo);
        f.setRenglonesPrimero(true);
        return f;
    }

    private static boolean valido(String latex) {
        try {
            new TeXFormula(latex);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ disposición

    /** Alineados si caben a su tamaño; si no, cada renglón por su cuenta (se reparte antes de achicar). */
    boolean alineados(double ancho) {
        return alineado != null && (ancho < 0 || alineado.anchoNatural() <= ancho - snappedLeftInset()
                - snappedRightInset());
    }

    private Region activo(double ancho) {
        return alineados(ancho) ? alineado : apilado;
    }

    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double alto) {
        return snappedLeftInset() + snappedRightInset() + activo(-1).prefWidth(-1);
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double w = ancho < 0 ? -1 : ancho - snappedLeftInset() - snappedRightInset();
        return snappedTopInset() + snappedBottomInset() + activo(ancho).prefHeight(w);
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return computePrefHeight(ancho);
    }

    @Override
    protected void layoutChildren() {
        double x = snappedLeftInset();
        double y = snappedTopInset();
        double w = getWidth() - x - snappedRightInset();
        Region a = activo(getWidth());
        for (Node n : getChildren()) {
            boolean si = n == a;
            n.setVisible(si);
        }
        a.resizeRelocate(x, y, w, a.prefHeight(w));
    }

    /** Los "=" van alineados ahora (pruebas). */
    public boolean alineadosAhora() {
        return alineado != null && alineado.isVisible();
    }
}
