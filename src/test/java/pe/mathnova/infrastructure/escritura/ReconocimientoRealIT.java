package pe.mathnova.infrastructure.escritura;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import pe.mathnova.domain.escritura.Trazo;

/**
 * Inferencia REAL con UniMERNet Small (no es una prueba unitaria: necesita reconocedor/python y los pesos).
 * Se corre con {@code -Dmathnova.it.unimernet=true}. Solo mira qué LEE el modelo en cada imagen; lo que
 * hace MathNova con esa lectura lo prueba EscrituraRealIT.
 *
 * <p>Dos tipos de imagen, ambos técnicos (ninguno es escritura de una persona):
 * <ul>
 *   <li>tipográfica: la fórmula compuesta con jlatexmath;</li>
 *   <li>trazos simulados ({@link TrazosSimulados}) dibujados con {@link ImagenTrazos}, igual que el lienzo.</li>
 * </ul>
 * Deja las imágenes y un informe en {@code target/reconocimiento/}.
 */
@EnabledIfSystemProperty(named = "mathnova.it.unimernet", matches = "true")
class ReconocimientoRealIT {

    private static ReconocedorUniMERNet reconocedor;
    private static final Path SALIDA = Path.of("target", "reconocimiento");
    private static final StringBuilder INFORME = new StringBuilder();

    /** Caso, LaTeX para la imagen tipográfica, trazos simulados. */
    private record Caso(String nombre, String tipografico, BiFunction<Double, Long, List<Trazo>> trazos) {
    }

    private static final List<Caso> CASOS = List.of(
            new Caso("A", "3x^2+2x-1", TrazosSimulados::casoA),
            new Caso("B", "\\int 2x\\,dx", TrazosSimulados::casoB),
            new Caso("C", "\\int_0^1 2x\\,dx", TrazosSimulados::casoC),
            new Caso("D", "\\frac{x+1}{x-1}", TrazosSimulados::casoD),
            new Caso("E", "\\sqrt{x^2+1}", TrazosSimulados::casoE));

    @BeforeAll
    static void cargar() throws Exception {
        Files.createDirectories(SALIDA);
        reconocedor = new ReconocedorUniMERNet("", 0, 300, 120);
        long t = System.nanoTime();
        reconocedor.preparar();
        long ms = (System.nanoTime() - t) / 1_000_000;
        INFORME.append("carga del modelo (proceso): ").append(reconocedor.cargaMs()).append(" ms; desde Java: ")
                .append(ms).append(" ms\n");
        INFORME.append("caso\ttipo\tms\tlectura del modelo\n");
    }

    @AfterAll
    static void cerrar() throws Exception {
        if (reconocedor != null) {
            INFORME.append("memoria pico del proceso: ").append(memoriaPico(reconocedor.pid())).append('\n');
            reconocedor.destroy();
        }
        Files.writeString(SALIDA.resolve("lecturas.tsv"), INFORME.toString(), StandardCharsets.UTF_8);
    }

    /** PeakWorkingSet64 del proceso, en MB (Windows). */
    static String memoriaPico(long pid) {
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                    "$p=Get-Process -Id " + pid + "; '{0:N0} MB pico / {1:N0} MB actual' -f ($p.PeakWorkingSet64/1MB), ($p.WorkingSet64/1MB)")
                    .redirectErrorStream(true).start();
            return new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            return "?";
        }
    }

    static byte[] tipografica(String latex) throws Exception {
        TeXIcon icono = new TeXFormula(latex).createTeXIcon(TeXConstants.STYLE_DISPLAY, 60);
        int m = 30;
        BufferedImage img = new BufferedImage(icono.getIconWidth() + 2 * m, icono.getIconHeight() + 2 * m,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        icono.setForeground(Color.BLACK);
        icono.paintIcon(null, g, m, m);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static String leer(String caso, String tipo, byte[] png) throws Exception {
        Files.write(SALIDA.resolve(caso + "-" + tipo + ".png"), png);
        long t = System.nanoTime();
        String latex = reconocedor.reconocerPng(png);
        long ms = (System.nanoTime() - t) / 1_000_000;
        INFORME.append(caso).append('\t').append(tipo).append('\t').append(ms).append('\t').append(latex).append('\n');
        return latex;
    }

    @Test
    void leeLasImagenesTipograficasYLasDeTrazosSimulados() throws Exception {
        Map<String, String> lecturas = new LinkedHashMap<>();
        for (Caso c : CASOS) {
            lecturas.put(c.nombre() + " tipográfica", leer(c.nombre(), "tipografica", tipografica(c.tipografico())));
            for (long semilla = 1; semilla <= 3; semilla++) {
                byte[] png = ImagenTrazos.png(c.trazos().apply(45.0, semilla));
                lecturas.put(c.nombre() + " trazos-" + semilla, leer(c.nombre(), "trazos-" + semilla, png));
            }
        }
        // La misma imagen dos veces: el modelo ya cargado (tiempo en caliente).
        leer("B", "trazos-1-repetida", ImagenTrazos.png(TrazosSimulados.casoB(45.0, 1)));
        lecturas.values().forEach(l -> assertFalse(l == null));
        assertTrue(lecturas.size() == 20, "todas las imágenes tuvieron respuesta");
    }
}
