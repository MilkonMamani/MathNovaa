package pe.mathnova.presentation.latex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

/**
 * Dibuja con jlatexmath el LaTeX de ImpresoraLatex como imagen con fondo transparente (HU02).
 *
 * <p>jlatexmath solo produce mapas de bits. Para que una fórmula se vea tan nítida como el editor (que es
 * vectorial), la imagen se dibuja con los píxeles reales de la pantalla: a {@code tamano × escala}, donde
 * {@code escala} es la escala de Windows (1,25, 1,5, 2…). Quien la muestra la reduce a su tamaño lógico
 * ({@link ImagenLatex#anchoLogico}), así cada píxel de la imagen cae en un píxel de la pantalla. Antes se
 * dibujaba a escala 1 y JavaFX la ampliaba: de ahí las fórmulas borrosas de los pasos.
 */
public final class RenderizadorLatex {

    private static final int MARGEN = 4;

    private final float tamano;

    public RenderizadorLatex(float tamano) {
        this.tamano = tamano;
    }

    public float tamano() {
        return tamano;
    }

    /**
     * A escala 1.
     *
     * @throws org.scilab.forge.jlatexmath.ParseException si jlatexmath no entiende el texto
     */
    public BufferedImage dibujar(String latex) {
        return dibujar(latex, 1);
    }

    /**
     * Con {@code escala} píxeles por píxel lógico.
     *
     * @throws org.scilab.forge.jlatexmath.ParseException si jlatexmath no entiende el texto
     */
    public BufferedImage dibujar(String latex, double escala) {
        double s = escala > 0 ? escala : 1;
        TeXIcon icono = new TeXFormula(latex).createTeXIcon(TeXConstants.STYLE_DISPLAY, (float) (tamano * s));
        int m = (int) Math.round(MARGEN * s);
        icono.setInsets(new Insets(m, m, m, m));
        icono.setForeground(Color.BLACK);

        BufferedImage imagen = new BufferedImage(
                Math.max(1, icono.getIconWidth()), Math.max(1, icono.getIconHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            icono.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return imagen;
    }
}
