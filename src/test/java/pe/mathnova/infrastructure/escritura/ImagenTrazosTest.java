package pe.mathnova.infrastructure.escritura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.escritura.Punto;
import pe.mathnova.domain.escritura.Trazo;

/** La imagen que va al modelo sale solo de los trazos: negro sobre blanco, recortada, con margen y sin deformar. */
class ImagenTrazosTest {

    private static Trazo raya(double x0, double y0, double x1, double y1) {
        List<Punto> ps = new ArrayList<>();
        for (int i = 0; i <= 20; i++) {
            ps.add(new Punto(x0 + (x1 - x0) * i / 20, y0 + (y1 - y0) * i / 20, i));
        }
        return new Trazo(ps);
    }

    /** Caja de la tinta: minX, minY, maxX, maxY (píxeles no blancos). */
    private static int[] caja(BufferedImage img) {
        int[] c = {Integer.MAX_VALUE, Integer.MAX_VALUE, -1, -1};
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int rgb = img.getRGB(x, y) & 0xFFFFFF;
                int r = rgb >> 16 & 0xFF;
                int g = rgb >> 8 & 0xFF;
                int b = rgb & 0xFF;
                assertEquals(r, g, "solo grises");
                assertEquals(g, b, "solo grises");
                if (rgb != 0xFFFFFF) {
                    c[0] = Math.min(c[0], x);
                    c[1] = Math.min(c[1], y);
                    c[2] = Math.max(c[2], x);
                    c[3] = Math.max(c[3], y);
                }
            }
        }
        return c;
    }

    @Test
    void sinTrazosNoHayImagen() {
        assertNull(ImagenTrazos.dibujar(List.of()));
        assertNull(ImagenTrazos.png(List.of()));
    }

    @Test
    void recortaALoEscritoConMargenYConservaLaProporcion() {
        // Un rectángulo de 300 × 100 dibujado lejos del origen del lienzo (en 400, 50).
        List<Trazo> trazos = List.of(raya(400, 50, 700, 50), raya(700, 50, 700, 150), raya(700, 150, 400, 150),
                raya(400, 150, 400, 50));
        BufferedImage img = ImagenTrazos.dibujar(trazos);
        int[] c = caja(img);
        double anchoTinta = c[2] - c[0];
        double altoTinta = c[3] - c[1];
        assertEquals(3.0, anchoTinta / altoTinta, 0.12, "la misma escala en x e y");
        assertEquals(ImagenTrazos.ALTO_TINTA, altoTinta, 12, "la tinta ocupa el alto previsto");
        // Margen blanco alrededor, igual a los dos lados.
        int izq = c[0];
        int der = img.getWidth() - 1 - c[2];
        int arriba = c[1];
        int abajo = img.getHeight() - 1 - c[3];
        assertTrue(izq >= ImagenTrazos.MARGEN_MINIMO && arriba >= ImagenTrazos.MARGEN_MINIMO, izq + " " + arriba);
        assertEquals(izq, der, 2);
        assertEquals(arriba, abajo, 2);
        // Esquina de la imagen: fondo blanco.
        assertEquals(0xFFFFFF, img.getRGB(0, 0) & 0xFFFFFF);
        // Tinta negra en el borde del rectángulo.
        assertEquals(0x000000, img.getRGB(c[0] + 1, (c[1] + c[3]) / 2) & 0xFFFFFF);
    }

    @Test
    void unaFormulaMuyAnchaNoPasaDelAnchoMaximo() {
        BufferedImage img = ImagenTrazos.dibujar(List.of(raya(0, 0, 5000, 100)));
        assertTrue(img.getWidth() <= ImagenTrazos.ANCHO_MAXIMO + 2 * 120, "ancho " + img.getWidth());
    }

    @Test
    void elPngSeLeeDeVuelta() throws Exception {
        byte[] png = ImagenTrazos.png(List.of(raya(10, 10, 60, 60)));
        BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        assertTrue(img.getWidth() > 0 && img.getHeight() > 0);
    }
}
