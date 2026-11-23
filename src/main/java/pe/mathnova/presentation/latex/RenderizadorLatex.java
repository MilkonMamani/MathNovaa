package pe.mathnova.presentation.latex;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

/** Dibuja con jlatexmath el LaTeX de ImpresoraLatex como imagen con fondo transparente (HU02). */
public final class RenderizadorLatex {

    private final float tamano;

    public RenderizadorLatex(float tamano) {
        this.tamano = tamano;
    }

    /**
     * @throws org.scilab.forge.jlatexmath.ParseException si jlatexmath no entiende el texto
     */
    public BufferedImage dibujar(String latex) {
        TeXIcon icono = new TeXFormula(latex).createTeXIcon(TeXConstants.STYLE_DISPLAY, tamano);
        icono.setInsets(new Insets(4, 4, 4, 4));
        icono.setForeground(Color.BLACK);

        BufferedImage imagen = new BufferedImage(
                Math.max(1, icono.getIconWidth()), Math.max(1, icono.getIconHeight()),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            icono.paintIcon(null, g, 0, 0);
        } finally {
            g.dispose();
        }
        return imagen;
    }
}
