package pe.mathnova.presentation.resolucion;

import java.util.HashMap;
import java.util.Map;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/**
 * Una fórmula en LaTeX que nunca se corta ni pide barra de desplazamiento:
 *
 * <ol>
 *   <li>se dibuja al tamaño máximo y se reduce (de 2 en 2 px) hasta que quepa a lo ancho;</li>
 *   <li>si al tamaño mínimo tampoco cabe, pasa a dos líneas: arriba la pregunta y abajo "= resultado",
 *       alineadas a la izquierda (ver {@link #partir(String)});</li>
 *   <li>si aun así no cabe, la imagen se encoge lo justo.</li>
 * </ol>
 *
 * Un nodo "lateral" (la etiqueta Verificado) va a la derecha de la fórmula si hay lugar, y si no, debajo.
 */
public final class FormulaAjustable extends Region {

    private static final double SEPARACION = 16;
    private static final double ENTRE_LINEAS = 4;

    private final double maximo;
    private final double minimo;
    private final ImageView arriba = new ImageView();
    private final ImageView abajo = new ImageView();
    private final Map<String, Image> imagenes = new HashMap<>();
    private Node lateral;
    private String latex;
    private String[] partes;

    /** @param maximo y minimo el tamaño de la letra en px (28 y 18 en la tarjeta, 18 y 14 en los pasos) */
    public FormulaAjustable(double maximo, double minimo) {
        this.maximo = maximo;
        this.minimo = minimo;
        getStyleClass().add("formula-ajustable");
        arriba.setPreserveRatio(true);
        abajo.setPreserveRatio(true);
        getChildren().addAll(arriba, abajo);
        setMinWidth(0);
    }

    public void setLatex(String nuevo) {
        latex = nuevo;
        partes = nuevo == null ? null : partir(nuevo);
        imagenes.clear();
        requestLayout();
    }

    public String getLatex() {
        return latex;
    }

    /** La etiqueta que va al costado (o debajo, si no cabe al costado). */
    public void setLateral(Node n) {
        if (lateral != null) {
            getChildren().remove(lateral);
        }
        lateral = n;
        if (n != null) {
            getChildren().add(n);
        }
        requestLayout();
    }

    // ------------------------------------------------------------------ elección

    /**
     * Cómo se dibuja en {@code disponible} px.
     *
     * @param dosLineas       la fórmula va partida en dos
     * @param tamano          el tamaño de la letra elegido
     * @param escala          lo que se encoge la imagen además (1 si cabe al tamaño elegido)
     * @param lateralAlCostado la etiqueta va a la derecha; si no, debajo
     */
    public record Disposicion(boolean dosLineas, double tamano, double escala, boolean lateralAlCostado) {
    }

    /**
     * @param ancho     ancho de la fórmula en una línea al tamaño {@code maximo}
     * @param anchoDos  ancho de la línea más larga partida en dos, al tamaño {@code maximo}; ≤ 0 si no se puede partir
     * @param lateral   ancho de la etiqueta (0 si no hay)
     */
    public static Disposicion elegir(double ancho, double anchoDos, double lateral, double disponible,
                                     double maximo, double minimo) {
        if (disponible <= 0) {
            return new Disposicion(false, maximo, 1, lateral > 0);
        }
        double alCostado = lateral > 0 ? disponible - lateral - SEPARACION : disponible;
        for (double s = maximo; s >= minimo - 1e-9; s -= 2) {
            if (ancho * s / maximo <= alCostado) {
                return new Disposicion(false, s, 1, lateral > 0);
            }
        }
        for (double s = maximo; s >= minimo - 1e-9; s -= 2) {
            if (ancho * s / maximo <= disponible) {
                return new Disposicion(false, s, 1, false);
            }
        }
        if (anchoDos > 0) {
            for (double s = maximo; s >= minimo - 1e-9; s -= 2) {
                if (anchoDos * s / maximo <= disponible) {
                    return new Disposicion(true, s, 1, false);
                }
            }
            return new Disposicion(true, minimo, disponible / (anchoDos * minimo / maximo), false);
        }
        return new Disposicion(false, minimo, disponible / (ancho * minimo / maximo), false);
    }

    /**
     * Dónde se parte una fórmula larga: en el primer "=" del nivel de afuera (arriba la pregunta, abajo
     * "= resultado"). Si a la izquierda del "=" hay muy poco ("x = 2, x = 3"), en la coma del medio; si no
     * hay "=", en el + o − del nivel de afuera más cerca del medio, o entre dos factores. null si no se
     * puede partir.
     */
    public static String[] partir(String latex) {
        if (latex == null) {
            return null;
        }
        int[] nivel = niveles(latex);
        int igual = -1;
        for (int i = 0; i < latex.length(); i++) {
            if (nivel[i] == 0 && latex.charAt(i) == '=' && i > 0) {
                igual = i;
                break;
            }
        }
        if (igual > 0 && latex.substring(0, igual).strip().length() > 2) {
            return new String[] {latex.substring(0, igual).strip(), latex.substring(igual).strip()};
        }
        int medio = latex.length() / 2;
        int mejor = -1;
        for (int i = 1; i < latex.length() - 1; i++) {
            char c = latex.charAt(i);
            boolean corte = nivel[i] == 0 && (c == ',' || ((c == '+' || c == '-') && igual < 0));
            if (corte && (mejor < 0 || Math.abs(i - medio) < Math.abs(mejor - medio))) {
                mejor = i;
            }
        }
        if (mejor > 0) {
            boolean coma = latex.charAt(mejor) == ',';
            String izq = latex.substring(0, coma ? mejor + 1 : mejor).strip();
            String der = latex.substring(coma ? mejor + 1 : mejor).strip();
            if (der.startsWith("\\quad")) {
                der = der.substring(5).strip();
            }
            return izq.isEmpty() || der.isEmpty() ? null : new String[] {izq, der};
        }
        // Entre dos factores: …\right)\left(…
        int factor = -1;
        for (int i = latex.indexOf("\\left"); i >= 0; i = latex.indexOf("\\left", i + 1)) {
            if (i > 0 && nivel[i] == 0 && (factor < 0 || Math.abs(i - medio) < Math.abs(factor - medio))) {
                factor = i;
            }
        }
        if (factor > 0) {
            return new String[] {latex.substring(0, factor).strip(), latex.substring(factor).strip()};
        }
        return null;
    }

    /** El nivel de anidamiento en cada posición: llaves y \left…\right. */
    private static int[] niveles(String s) {
        int[] r = new int[s.length()];
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (s.startsWith("\\left", i)) {
                r[i] = n;
                n++;
                continue;
            }
            if (s.startsWith("\\right", i)) {
                n--;
            }
            if (c == '}') {
                n--;
            }
            r[i] = n;
            if (c == '{') {
                n++;
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ dibujo

    private Image imagen(String tex, double tamano) {
        return imagenes.computeIfAbsent(tamano + "|" + tex,
                k -> ImagenLatex.de(new RenderizadorLatex((float) tamano), tex));
    }

    private static double ancho(Image i) {
        return i == null ? 0 : i.getWidth();
    }

    private Disposicion disposicion(double disponible) {
        if (latex == null) {
            return new Disposicion(false, maximo, 1, true);
        }
        double una = ancho(imagen(latex, maximo));
        double dos = partes == null ? 0 : Math.max(ancho(imagen(partes[0], maximo)), ancho(imagen(partes[1], maximo)));
        double lat = lateral == null || !lateral.isManaged() ? 0 : lateral.prefWidth(-1);
        return elegir(una, dos, lat, disponible, maximo, minimo);
    }

    /** La disposición para el ancho actual (pruebas). */
    public Disposicion disposicionActual() {
        return disposicion(getWidth() - snappedLeftInset() - snappedRightInset());
    }

    private void poner(Disposicion d) {
        if (latex == null) {
            arriba.setImage(null);
            abajo.setImage(null);
            return;
        }
        Image a = imagen(d.dosLineas() ? partes[0] : latex, d.tamano());
        Image b = d.dosLineas() ? imagen(partes[1], d.tamano()) : null;
        arriba.setImage(a);
        abajo.setImage(b);
        arriba.setFitWidth(a == null ? 0 : a.getWidth() * d.escala());
        abajo.setFitWidth(b == null ? 0 : b.getWidth() * d.escala());
    }

    private double altoFormula(Disposicion d) {
        double h = arriba.getImage() == null ? 0 : arriba.getImage().getHeight() * d.escala();
        if (d.dosLineas() && abajo.getImage() != null) {
            h += ENTRE_LINEAS + abajo.getImage().getHeight() * d.escala();
        }
        return h;
    }

    @Override
    protected double computeMinWidth(double alto) {
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double alto) {
        Disposicion d = disposicion(Double.MAX_VALUE);
        poner(d);
        double lat = lateral == null || !lateral.isManaged() ? 0 : lateral.prefWidth(-1) + SEPARACION;
        return snappedLeftInset() + snappedRightInset() + (arriba.getImage() == null ? 0 : arriba.getImage().getWidth()) + lat;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double disponible = ancho < 0 ? Double.MAX_VALUE : ancho - snappedLeftInset() - snappedRightInset();
        Disposicion d = disposicion(disponible);
        poner(d);
        double h = altoFormula(d);
        if (lateral != null && lateral.isManaged()) {
            double hl = lateral.prefHeight(-1);
            h = d.lateralAlCostado() ? Math.max(h, hl) : h + ENTRE_LINEAS * 2 + hl;
        }
        return snappedTopInset() + snappedBottomInset() + h;
    }

    @Override
    protected double computeMinHeight(double ancho) {
        return computePrefHeight(ancho);
    }

    @Override
    public javafx.geometry.Orientation getContentBias() {
        return javafx.geometry.Orientation.HORIZONTAL;
    }

    @Override
    protected void layoutChildren() {
        double x = snappedLeftInset();
        double y = snappedTopInset();
        double w = getWidth() - x - snappedRightInset();
        double h = getHeight() - y - snappedBottomInset();
        Disposicion d = disposicion(w);
        poner(d);
        double hf = altoFormula(d);
        double ha = arriba.getImage() == null ? 0 : arriba.getImage().getHeight() * d.escala();
        boolean costado = lateral != null && lateral.isManaged() && d.lateralAlCostado();
        double yFormula = costado ? y + Math.max(0, (h - hf) / 2) : y;
        arriba.relocate(x, yFormula);
        abajo.relocate(x, yFormula + ha + ENTRE_LINEAS);
        if (lateral != null && lateral.isManaged()) {
            double lw = lateral.prefWidth(-1);
            double lh = lateral.prefHeight(lw);
            if (costado) {
                layoutInArea(lateral, x + w - lw, y, lw, h, 0, HPos.RIGHT, VPos.CENTER);
            } else {
                lateral.resizeRelocate(x, y + hf + ENTRE_LINEAS * 2, lw, lh);
            }
        }
    }
}
