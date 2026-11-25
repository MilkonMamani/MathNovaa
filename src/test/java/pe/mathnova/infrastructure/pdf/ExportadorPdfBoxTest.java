package pe.mathnova.infrastructure.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pe.mathnova.application.pdf.DocumentoPdf;

/**
 * Sprint 9, parte C: el PDF que escribe PDFBox. Se lee el archivo generado con PDFBox: las fórmulas son Form
 * XObjects marcados con su papel, sin ninguna imagen, y un paso nunca queda partido entre dos páginas.
 */
class ExportadorPdfBoxTest {

    private static final ExportadorPdfBox EXPORTADOR = new ExportadorPdfBox();

    @TempDir
    Path carpeta;

    // ---------------------------------------------------------------- documentos de prueba

    private static DocumentoPdf factorizacion() {
        List<DocumentoPdf.Paso> pasos = List.of(
                new DocumentoPdf.Paso("1", 0, "Agrupación de términos",
                        "x^{3} - 2x^{2} - x + 2 = x^{2}\\left(x - 2\\right) - \\left(x - 2\\right)",
                        "Se agrupan los dos primeros y los dos últimos términos."),
                new DocumentoPdf.Paso("2", 0, "Factor común polinomio",
                        "\\left(x - 2\\right)\\left(x^{2} - 1\\right)", "Se saca x − 2 de los dos grupos."),
                new DocumentoPdf.Paso("2.1", 1, "Diferencia de cuadrados",
                        "x^{2} - 1 = \\left(x + 1\\right)\\left(x - 1\\right)", "a² − b² = (a + b)(a − b) con a = x, b = 1."));
        return new DocumentoPdf("Factorizar", "x^{3} - 2x^{2} - x + 2", "x^3 - 2x^2 - x + 2", "24 de noviembre de 2026",
                pasos, "\\left(x + 1\\right)\\left(x - 1\\right)\\left(x - 2\\right)", "(x + 1)(x - 1)(x - 2)",
                List.of(), new DocumentoPdf.Comprobacion("Por expansión",
                        List.of("\\left(x + 1\\right)\\left(x - 1\\right) = x^{2} - 1"), true, "Coincide con la entrada"),
                null);
    }

    /** Diez pasos con fórmulas altas (fracciones) y explicaciones de varios renglones. */
    static DocumentoPdf diezPasos() {
        List<DocumentoPdf.Paso> pasos = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            String n = String.format("%02d", i);
            pasos.add(new DocumentoPdf.Paso(String.valueOf(i), 0, "Paso de prueba " + n,
                    "\\frac{\\frac{x^{" + i + "} - 1}{x - 1}}{\\frac{x^{2} + " + i + "x + 1}{x + " + i + "}} = "
                            + "\\sum_{k=0}^{" + i + "} x^{k}",
                    "Explicación larga del paso " + n + ": se reescribe la fracción compuesta, se multiplica por el "
                            + "recíproco del denominador y se simplifican los factores comunes que aparecen arriba "
                            + "y abajo; con eso queda la suma geométrica. Fin del paso " + n + "."));
        }
        return new DocumentoPdf("Simplificar", "\\frac{x^{10} - 1}{x - 1}", "(x^10 - 1)/(x - 1)",
                "24 de noviembre de 2026", pasos, "\\sum_{k=0}^{9} x^{k}", "1 + x + … + x^9", List.of("x ≠ 1"), null,
                null);
    }

    // ---------------------------------------------------------------- lectura del PDF

    /** Las fórmulas de cada página por papel: [{"paso": 3, "resultado": 1…}, …]. */
    static List<Map<String, Integer>> formulasPorPagina(Path pdf) throws IOException {
        List<Map<String, Integer>> r = new ArrayList<>();
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            for (PDPage p : d.getPages()) {
                Map<String, Integer> cuenta = new HashMap<>();
                PDResources res = p.getResources();
                for (COSName nombre : res.getXObjectNames()) {
                    PDXObject x = res.getXObject(nombre);
                    if (x instanceof PDFormXObject f) {
                        String rol = f.getCOSObject().getString(ExportadorPdfBox.ROL);
                        cuenta.merge(rol == null ? "?" : rol, 1, Integer::sum);
                    }
                }
                r.add(cuenta);
            }
        }
        return r;
    }

    /** Imágenes en todo el documento, también dentro de las fórmulas. */
    static int imagenes(Path pdf) throws IOException {
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            int n = 0;
            for (PDPage p : d.getPages()) {
                n += imagenes(p.getResources());
            }
            return n;
        }
    }

    private static int imagenes(PDResources res) throws IOException {
        if (res == null) {
            return 0;
        }
        int n = 0;
        for (COSName nombre : res.getXObjectNames()) {
            PDXObject x = res.getXObject(nombre);
            if (x instanceof PDImageXObject) {
                n++;
            } else if (x instanceof PDFormXObject f) {
                n += imagenes(f.getResources());
            }
        }
        return n;
    }

    static List<String> textoPorPagina(Path pdf) throws IOException {
        List<String> r = new ArrayList<>();
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            PDFTextStripper s = new PDFTextStripper();
            for (int i = 1; i <= d.getNumberOfPages(); i++) {
                s.setStartPage(i);
                s.setEndPage(i);
                r.add(s.getText(d).replaceAll("\\s+", " "));
            }
        }
        return r;
    }

    private static int total(List<Map<String, Integer>> porPagina, String rol) {
        return porPagina.stream().mapToInt(m -> m.getOrDefault(rol, 0)).sum();
    }

    // ---------------------------------------------------------------- pruebas

    @Test
    void seGeneraElArchivo() throws IOException {
        Path pdf = carpeta.resolve("sub/mathnova-factorizar-2026-11-24.pdf");
        EXPORTADOR.exportar(factorizacion(), pdf);
        assertTrue(Files.size(pdf) > 1000);
        assertEquals("%PDF", new String(Files.readAllBytes(pdf), 0, 4));
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, d.getNumberOfPages());
            // A4 vertical
            assertEquals(595.3, d.getPage(0).getMediaBox().getWidth(), 0.5);
            assertEquals(841.9, d.getPage(0).getMediaBox().getHeight(), 0.5);
        }
        String texto = textoPorPagina(pdf).getFirst();
        for (String parte : List.of("MathNova", "24 de noviembre de 2026", "FACTORIZAR", "Pasos",
                "Agrupación de términos", "Diferencia de cuadrados", "Resultado", "Comprobación", "Por expansión",
                "Coincide con la entrada", "Página 1 de 1")) {
            assertTrue(texto.contains(parte), () -> "falta \"" + parte + "\" en: " + texto);
        }
    }

    @Test
    void tieneTantasFormulasComoPasosYEnVectores() throws IOException {
        Path pdf = carpeta.resolve("formulas.pdf");
        DocumentoPdf d = factorizacion();
        EXPORTADOR.exportar(d, pdf);
        List<Map<String, Integer>> f = formulasPorPagina(pdf);
        assertEquals(d.pasos().size(), total(f, "paso"));
        assertEquals(1, total(f, "enunciado"));
        assertEquals(1, total(f, "resultado"));
        assertEquals(1, total(f, "comprobacion"));
        assertEquals(0, total(f, "?"), "ninguna forma sin papel");
        // Ni una imagen: las fórmulas son trazos
        assertEquals(0, imagenes(pdf));
    }

    @Test
    void diezPasosOcupanMasDeUnaPaginaSinCortarUnPaso() throws IOException {
        Path pdf = carpeta.resolve("diez.pdf");
        DocumentoPdf d = diezPasos();
        EXPORTADOR.exportar(d, pdf);
        List<String> paginas = textoPorPagina(pdf);
        List<Map<String, Integer>> formulas = formulasPorPagina(pdf);
        assertTrue(paginas.size() > 1, "páginas: " + paginas.size());
        assertEquals(10, total(formulas, "paso"));

        for (int i = 0; i < paginas.size(); i++) {
            String texto = paginas.get(i);
            // En cada página, tantas fórmulas de paso como títulos de paso: la fórmula va con su título
            long titulos = java.util.stream.IntStream.rangeClosed(1, 10)
                    .filter(k -> texto.contains("Paso de prueba " + String.format("%02d", k))).count();
            assertEquals(titulos, (long) formulas.get(i).getOrDefault("paso", 0), "página " + (i + 1));
            assertTrue(texto.contains("Página " + (i + 1) + " de " + paginas.size()));
        }
        for (int k = 1; k <= 10; k++) {
            String n = String.format("%02d", k);
            int titulo = pagina(paginas, "Paso de prueba " + n);
            int inicio = pagina(paginas, "Explicación larga del paso " + n);
            int fin = pagina(paginas, "Fin del paso " + n + ".");
            assertTrue(titulo >= 0, "falta el paso " + n);
            assertEquals(titulo, inicio, "la explicación del paso " + n + " empieza en otra página");
            assertEquals(titulo, fin, "el paso " + n + " quedó cortado");
        }
        // El título "Resultado" no queda solo al pie: va con su recuadro
        int resultado = pagina(paginas, "Resultado");
        assertEquals(paginas.size() - 1, resultado);
        assertEquals(1, formulas.get(resultado).getOrDefault("resultado", 0));
    }

    @Test
    void conLetraMuyGrandeOcupaMasYNoCortaPasos() throws IOException {
        Path normal = carpeta.resolve("normal.pdf");
        Path grande = carpeta.resolve("grande.pdf");
        EXPORTADOR.exportar(diezPasos(), normal);
        EXPORTADOR.exportar(diezPasos().conEscala(1.4), grande);
        List<String> n = textoPorPagina(normal);
        List<String> g = textoPorPagina(grande);
        assertTrue(g.size() > n.size(), n.size() + " → " + g.size());
        for (int k = 1; k <= 10; k++) {
            String num = String.format("%02d", k);
            assertEquals(pagina(g, "Paso de prueba " + num), pagina(g, "Fin del paso " + num + "."), "paso " + num);
        }
    }

    @Test
    void laGraficaSoloSiSePide() throws IOException {
        BufferedImage img = new BufferedImage(400, 240, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);

        Path sin = carpeta.resolve("sin.pdf");
        EXPORTADOR.exportar(factorizacion(), sin);
        Path con = carpeta.resolve("con.pdf");
        EXPORTADOR.exportar(factorizacion().conGrafica(png.toByteArray()), con);
        assertEquals(0, imagenes(sin));
        assertEquals(1, imagenes(con));
        assertTrue(String.join(" ", textoPorPagina(con)).contains("Gráfica"));
        assertTrue(!String.join(" ", textoPorPagina(sin)).contains("Gráfica"));
    }

    @Test
    void unLatexQueNoSeEntiendeNoRompeLaExportacion() throws IOException {
        Path pdf = carpeta.resolve("raro.pdf");
        DocumentoPdf d = new DocumentoPdf("Factorizar", "\\frac{x", "x^2 + 1", "24 de noviembre de 2026",
                List.of(new DocumentoPdf.Paso("1", 0, "Paso → con flecha ≠ y ✓", "\\left(", "x → ∞, √2 · π")),
                "\\badcommand{x}", "x^2 + 1", List.of(), null, null);
        EXPORTADOR.exportar(d, pdf);
        assertEquals(1, total(formulasPorPagina(pdf), "paso"));
    }

    private static int pagina(List<String> paginas, String texto) {
        for (int i = 0; i < paginas.size(); i++) {
            if (paginas.get(i).contains(texto)) {
                return i;
            }
        }
        return -1;
    }
}
