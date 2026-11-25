package pe.mathnova.presentation.latex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

public final class RenderizadorLatex {

    private static final int MARGEN = 4;

    private final float tamano;
    private final boolean negra;

    public RenderizadorLatex(float tamano) {
        this(tamano, false);
    }

    private RenderizadorLatex(float tamano, boolean negra) {
        this.tamano = tamano;
        this.negra = negra;
    }

    public RenderizadorLatex siempreNegra() {
        return new RenderizadorLatex(tamano, true);
    }

    public float tamano() {
        return tamano;
    }

    public BufferedImage dibujar(String latex) {
        return dibujar(latex, 1);
    }

    public BufferedImage dibujar(String latex, double escala) {
        double s = escala > 0 ? escala : 1;
        TeXIcon icono = new TeXFormula(latex).createTeXIcon(TeXConstants.STYLE_DISPLAY, (float) (tamano * s));
        int m = (int) Math.round(MARGEN * s);
        icono.setInsets(new Insets(m, m, m, m));
        icono.setForeground(negra ? Color.BLACK : Apariencia.tinta());

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
