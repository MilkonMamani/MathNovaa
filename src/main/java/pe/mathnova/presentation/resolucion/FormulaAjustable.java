package pe.mathnova.presentation.resolucion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

import pe.mathnova.presentation.latex.Apariencia;
import pe.mathnova.presentation.latex.ImagenLatex;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/**
 * Una fórmula en LaTeX que nunca se corta ni pide barra de desplazamiento:
 *
 * <ol>
 *   <li>se dibuja al tamaño máximo y se reduce (de 2 en 2 px) hasta que quepa a lo ancho;</li>
 *   <li>si al tamaño mínimo tampoco cabe, pasa a dos líneas: arriba la pregunta y abajo "= resultado",
 *       alineadas a la izquierda (ver {@link #partir(String)});</li>
 *   <li>si aun así no cabe, sigue partiendo el renglón más largo (en un "=", un + o − de afuera o entre dos
 *       factores) hasta seis renglones;</li>
 *   <li>como último recurso se dibuja más chica, nunca se amplía ni se reduce un mapa de bits.</li>
 * </ol>
 *
 * Cada renglón se dibuja con los píxeles reales de la pantalla ({@link ImagenLatex#escala}), por eso se ve
 * tan nítida como el editor a 125 % o 150 %.
 *
 * Un nodo "lateral" (la etiqueta Verificado) va a la derecha de la fórmula si hay lugar, y si no, debajo.
 */
public final class FormulaAjustable extends Region {

    private static final double SEPARACION = 16;
    private static final double ENTRE_LINEAS = 4;
    private static final int MAX_RENGLONES = 6;

    private final double maximo;
    private final double minimo;
    private final List<ImageView> vistas = new ArrayList<>();
    private final Map<String, Image> imagenes = new HashMap<>();
    private Node lateral;
    private String latex;
    private String[] partes;
    private boolean partible = true;
    private boolean renglonesPrimero;
    private int maxRenglones = MAX_RENGLONES;

    /** @param maximo y minimo el tamaño de la letra en px (28 y 18 en la tarjeta, 18 y 14 en los pasos) */
    public FormulaAjustable(double maximo, double minimo) {
        this.maximo = maximo;
        this.minimo = minimo;
        getStyleClass().add("formula-ajustable");
        setMinWidth(0);
        sceneProperty().addListener((obs, antes, ahora) -> {
            imagenes.clear(); // pudo cambiar de contenedor (escalable o no)
            requestLayout();
        });
        Apariencia.version().addListener(new javafx.beans.WeakInvalidationListener(redibujar));
    }

    /** Al cambiar el tamaño de letra o el tema se dibuja otra vez (referencia fuerte para el listener débil). */
    private final javafx.beans.InvalidationListener redibujar = obs -> {
        imagenes.clear();
        requestLayout();
    };

    public void setLatex(String nuevo) {
        latex = nuevo;
        partes = nuevo == null || !partible ? null : partir(nuevo);
        imagenes.clear();
        requestLayout();
    }

    public String getLatex() {
        return latex;
    }

    /** false: no se parte en renglones (un bloque ya alineado); si no cabe, se dibuja más chico. */
    public void setPartible(boolean si) {
        partible = si;
        setLatex(latex);
    }

    /**
     * true (la Pizarra): si no cabe, primero se reparte en renglones al tamaño máximo y solo después se
     * reduce la letra; así una fórmula larga sigue legible cuando se angosta el panel.
     */
    public void setRenglonesPrimero(boolean si) {
        renglonesPrimero = si;
        maxRenglones = si ? 10 : MAX_RENGLONES;
        requestLayout();
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

    /** El ancho de la fórmula en una línea al tamaño máximo (sin márgenes ni etiqueta). */
    public double anchoNatural() {
        return latex == null ? 0 : ancho(latex, maximo);
    }

    /** El ancho de la fórmula en una línea al tamaño mínimo. */
    public double anchoMinimo() {
        return latex == null ? 0 : ancho(latex, minimo);
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
        int delimitador = -1; // el "(" de "\left(" ya se contó con el \left
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (s.startsWith("\\left", i)) {
                r[i] = n;
                n++;
                delimitador = i + 5;
                continue;
            }
            if (s.startsWith("\\right", i)) {
                n--;
                delimitador = i + 6;
            }
            // Paréntesis y corchetes sueltos también agrupan: no se parte dentro de "(8 + 3r)".
            boolean suelto = i != delimitador && (i == 0 || s.charAt(i - 1) != '\\');
            if (c == '}' || suelto && (c == ')' || c == ']')) {
                n--;
            }
            r[i] = n;
            if (c == '{' || suelto && (c == '(' || c == '[')) {
                n++;
            }
        }
        return r;
    }

    // ------------------------------------------------------------------ dibujo

    /** Lo que se dibuja: los renglones, su tamaño de letra y si la etiqueta va al costado. */
    private record Plan(List<String> renglones, double tamano, boolean lateralAlCostado) {
    }

    private Image imagen(String tex, double tamano) {
        double s = ImagenLatex.escala(this);
        // Sprint 9, parte E: dentro de la Pizarra, la hoja o Práctica crece con el tamaño de letra
        double t = tamano * Apariencia.factor(this);
        return imagenes.computeIfAbsent(s + "|" + t + "|" + tex,
                k -> ImagenLatex.de(new RenderizadorLatex((float) t), tex, s));
    }

    /** Ancho lógico (en px de la pantalla a 100 %) del renglón al tamaño dado. */
    private double ancho(String tex, double tamano) {
        return ImagenLatex.anchoLogico(imagen(tex, tamano), ImagenLatex.escala(this));
    }

    private double alto(Image i) {
        return ImagenLatex.altoLogico(i, ImagenLatex.escala(this));
    }

    private Plan plan(double disponible) {
        if (latex == null) {
            return new Plan(List.of(), maximo, true);
        }
        if (renglonesPrimero) {
            return planPorRenglones(disponible);
        }
        double una = ancho(latex, maximo);
        double dos = partes == null ? 0 : Math.max(ancho(partes[0], maximo), ancho(partes[1], maximo));
        double lat = lateral == null || !lateral.isManaged() ? 0 : lateral.prefWidth(-1);
        Disposicion d = elegir(una, dos, lat, disponible, maximo, minimo);
        List<String> renglones = new ArrayList<>(d.dosLineas() ? List.of(partes) : List.of(latex));
        if (d.escala() >= 1) {
            return new Plan(renglones, d.tamano(), d.lateralAlCostado());
        }
        // Más renglones: se parte el más largo mientras no quepa al tamaño mínimo.
        while (partible && renglones.size() < maxRenglones && masAncho(renglones, minimo) > disponible) {
            int k = indiceMasAncho(renglones, minimo);
            String[] p = partir(renglones.get(k));
            if (p == null) {
                break;
            }
            renglones.remove(k);
            renglones.add(k, p[1]);
            renglones.add(k, p[0]);
        }
        for (double s = maximo; s >= minimo - 1e-9; s -= 2) {
            if (masAncho(renglones, s) <= disponible) {
                return new Plan(renglones, s, false);
            }
        }
        // Último recurso: más chica, dibujada a ese tamaño (no se encoge la imagen).
        double factor = disponible / Math.max(1, masAncho(renglones, minimo));
        return new Plan(renglones, Math.max(6, Math.floor(minimo * factor * 0.98)), false);
    }

    /** Renglones primero: se parte al tamaño máximo; luego se reduce hasta el mínimo; nunca menos de 80 %. */
    private Plan planPorRenglones(double disponible) {
        List<String> renglones = new ArrayList<>(List.of(latex));
        if (disponible <= 1) {
            return new Plan(renglones, maximo, true); // todavía sin ancho: se mide en una línea
        }
        while (partible && renglones.size() < maxRenglones && masAncho(renglones, maximo) > disponible) {
            int k = indiceMasAncho(renglones, maximo);
            String[] p = partir(renglones.get(k));
            if (p == null) {
                break;
            }
            renglones.remove(k);
            renglones.add(k, p[1]);
            renglones.add(k, p[0]);
        }
        for (double s = maximo; s >= minimo - 1e-9; s -= 1) {
            if (masAncho(renglones, s) <= disponible) {
                return new Plan(renglones, s, renglones.size() == 1);
            }
        }
        double factor = disponible / Math.max(1, masAncho(renglones, minimo));
        return new Plan(renglones, Math.max(Math.floor(minimo * 0.8), Math.floor(minimo * factor)), false);
    }

    private double masAncho(List<String> renglones, double tamano) {
        double m = 0;
        for (String r : renglones) {
            m = Math.max(m, ancho(r, tamano));
        }
        return m;
    }

    private int indiceMasAncho(List<String> renglones, double tamano) {
        int k = 0;
        for (int i = 1; i < renglones.size(); i++) {
            if (ancho(renglones.get(i), tamano) > ancho(renglones.get(k), tamano)) {
                k = i;
            }
        }
        return k;
    }

    /** La disposición para el ancho actual (pruebas): el tamaño, si va partida y si la etiqueta va al costado. */
    public Disposicion disposicionActual() {
        Plan p = plan(getWidth() - snappedLeftInset() - snappedRightInset());
        return new Disposicion(p.renglones().size() > 1, p.tamano(), 1, p.lateralAlCostado());
    }

    /** Cuántos renglones se ven ahora (pruebas). */
    public int renglones() {
        return (int) vistas.stream().filter(v -> v.getImage() != null).count();
    }

    private void poner(Plan p) {
        while (vistas.size() < p.renglones().size()) {
            ImageView v = new ImageView();
            v.setPreserveRatio(false);
            v.setSmooth(true);
            vistas.add(v);
            getChildren().add(vistas.size() - 1, v);
        }
        double s = ImagenLatex.escala(this);
        for (int i = 0; i < vistas.size(); i++) {
            ImageView v = vistas.get(i);
            Image img = i < p.renglones().size() ? imagen(p.renglones().get(i), p.tamano()) : null;
            v.setImage(img);
            v.setFitWidth(ImagenLatex.anchoLogico(img, s));
            v.setFitHeight(ImagenLatex.altoLogico(img, s));
            v.setVisible(img != null);
        }
    }

    private double altoFormula(Plan p) {
        double h = 0;
        int n = 0;
        for (String r : p.renglones()) {
            Image img = imagen(r, p.tamano());
            if (img != null) {
                h += (n > 0 ? ENTRE_LINEAS : 0) + alto(img);
                n++;
            }
        }
        return h;
    }

    private double anchoFormula(Plan p) {
        return masAncho(p.renglones(), p.tamano());
    }

    @Override
    protected double computeMinWidth(double alto) {
        return snappedLeftInset() + snappedRightInset();
    }

    @Override
    protected double computePrefWidth(double alto) {
        Plan p = plan(Double.MAX_VALUE);
        double lat = lateral == null || !lateral.isManaged() ? 0 : lateral.prefWidth(-1) + SEPARACION;
        return snappedLeftInset() + snappedRightInset() + Math.ceil(anchoFormula(p)) + lat;
    }

    @Override
    protected double computePrefHeight(double ancho) {
        double disponible = ancho < 0 ? Double.MAX_VALUE : ancho - snappedLeftInset() - snappedRightInset();
        Plan p = plan(disponible);
        double h = altoFormula(p);
        if (lateral != null && lateral.isManaged()) {
            double hl = lateral.prefHeight(-1);
            h = p.lateralAlCostado() ? Math.max(h, hl) : h + ENTRE_LINEAS * 2 + hl;
        }
        return snappedTopInset() + snappedBottomInset() + Math.ceil(h);
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
        Plan p = plan(w);
        poner(p);
        double hf = altoFormula(p);
        boolean costado = lateral != null && lateral.isManaged() && p.lateralAlCostado();
        // Cada renglón empieza en un píxel entero de la pantalla: así la imagen no se remuestrea.
        double yy = snapPositionY(costado ? y + Math.max(0, (h - hf) / 2) : y);
        for (ImageView v : vistas) {
            if (v.getImage() == null) {
                continue;
            }
            v.relocate(snapPositionX(x), snapPositionY(yy));
            yy += v.getFitHeight() + ENTRE_LINEAS;
        }
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
