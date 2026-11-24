package pe.mathnova.infrastructure.escritura;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import javax.imageio.ImageIO;

import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;

/**
 * La imagen que se manda al reconocedor, hecha solo con los trazos (no con el lienzo): tinta negra sobre
 * fondo blanco, recortada a lo escrito, con margen alrededor y sin deformar (la misma escala en x e y).
 * La cuadrícula, el cursor, los botones y "Escribe aquí tu ejercicio" no están en los trazos, así que no
 * llegan a la imagen.
 *
 * <p>La escala lleva la altura de lo escrito a {@link #ALTO_TINTA} píxeles (sin pasar de
 * {@link #ANCHO_MAXIMO} de ancho) y el grosor de la tinta crece con ella, para que el trazo de 2,5 px del
 * lienzo se vea como un trazo de bolígrafo en la imagen que lee el modelo.
 */
public final class ImagenTrazos {

    /** Alto de la tinta en la imagen, en píxeles. */
    static final double ALTO_TINTA = 160;
    static final double ANCHO_MAXIMO = 1400;
    /** El margen, como fracción del alto de la tinta (y nunca menos de {@link #MARGEN_MINIMO} px). */
    static final double MARGEN = 0.18;
    static final int MARGEN_MINIMO = 24;
    /** Grosor de la tinta como fracción del alto de la tinta. */
    static final double GROSOR = 0.035;
    static final double GROSOR_MINIMO = 3;

    private ImagenTrazos() {
    }

    /** La imagen de los trazos; null si no hay ningún punto. */
    public static BufferedImage dibujar(List<Trazo> trazos) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Trazo t : trazos) {
            for (Punto p : t.puntos()) {
                minX = Math.min(minX, p.x());
                minY = Math.min(minY, p.y());
                maxX = Math.max(maxX, p.x());
                maxY = Math.max(maxY, p.y());
            }
        }
        if (minX == Double.MAX_VALUE) {
            return null;
        }
        // Un punto o una raya horizontal: la altura mínima evita escalas enormes.
        double ancho = Math.max(maxX - minX, 8);
        double alto = Math.max(maxY - minY, 24);
        double escala = Math.min(ALTO_TINTA / alto, ANCHO_MAXIMO / ancho);
        double altoTinta = alto * escala;
        float grosor = (float) Math.max(GROSOR_MINIMO, altoTinta * GROSOR);
        int margen = (int) Math.ceil(Math.max(MARGEN_MINIMO, altoTinta * MARGEN) + grosor);
        int w = (int) Math.ceil(ancho * escala) + 2 * margen;
        int h = (int) Math.ceil(altoTinta) + 2 * margen;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(grosor, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double dx = margen - minX * escala;
            double dy = margen - minY * escala;
            for (Trazo t : trazos) {
                List<Punto> ps = t.puntos();
                if (ps.isEmpty()) {
                    continue;
                }
                if (ps.size() == 1) {
                    Punto p = ps.getFirst();
                    g.fill(new Ellipse2D.Double(p.x() * escala + dx - grosor / 2.0,
                            p.y() * escala + dy - grosor / 2.0, grosor, grosor));
                    continue;
                }
                Path2D.Double camino = new Path2D.Double();
                camino.moveTo(ps.getFirst().x() * escala + dx, ps.getFirst().y() * escala + dy);
                for (int i = 1; i < ps.size(); i++) {
                    camino.lineTo(ps.get(i).x() * escala + dx, ps.get(i).y() * escala + dy);
                }
                g.draw(camino);
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /** La imagen como PNG; null si no hay ningún punto. */
    public static byte[] png(List<Trazo> trazos) {
        BufferedImage img = dibujar(trazos);
        if (img == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
