package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.pdf.DocumentoPdf;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.infrastructure.pdf.ExportadorPdfBox;

/**
 * Sprint 9, parte C: el PDF de una resolución real, de Resolver y del Historial, escrito con el exportador de
 * PDFBox (el puerto con su implementación de infraestructura). Base de prueba de target/; se deshace al terminar.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class ExportacionPdfServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 24);
    /** "2.1 → Diferencia de cuadrados": el encabezado de un paso en el texto del PDF. */
    private static final Pattern ENCABEZADO = Pattern.compile("(?<![\\d.])\\d+(\\.\\d+)* → ");

    @Autowired
    private ExportacionPdfService exportacion;
    @Autowired
    private HistorialService historial;

    @TempDir
    Path carpeta;

    @Test
    void elNombrePropuestoLlevaLaOperacionYLaFecha() {
        assertEquals("mathnova-factorizar-2026-11-24.pdf", ExportacionPdfService.nombreArchivo("Factorizar", HOY));
        assertEquals("mathnova-calcular-limite-2026-11-24.pdf",
                ExportacionPdfService.nombreArchivo("Calcular límite", HOY));
        assertEquals("mathnova-integral-multiple-2026-11-24.pdf",
                ExportacionPdfService.nombreArchivo("Integral múltiple", HOY));
        assertEquals("24 de noviembre de 2026", ExportacionPdfService.fecha(HOY));
    }

    @Test
    void desdeResolverConSusPasosYSuComprobacion() throws IOException {
        DocumentoPdf d = exportacion.desdeResolver("x^3 - 2x^2 - x + 2", Campo.Q, null, HOY);
        assertEquals("Factorizar", d.verbo());
        assertEquals("24 de noviembre de 2026", d.fecha());
        assertTrue(d.pasos().size() >= 2);
        assertEquals("(x + 1)(x - 1)(x - 2)", d.resultadoTexto());
        assertNotNull(d.comprobacion());
        assertEquals("Por expansión", d.comprobacion().metodo());
        assertTrue(d.comprobacion().coincide());
        assertNull(d.graficaPng());

        Path pdf = carpeta.resolve(ExportacionPdfService.nombreArchivo(d.verbo(), HOY));
        exportacion.exportar(d, pdf);
        assertTrue(Files.exists(pdf));
        Map<String, Integer> formulas = formulas(pdf);
        assertEquals(d.pasos().size(), formulas.getOrDefault("paso", 0), "cada paso con su fórmula");
        assertEquals(1, formulas.getOrDefault("resultado", 0));
        assertEquals(d.comprobacion().lineasLatex().size(), formulas.getOrDefault("comprobacion", 0));
    }

    @Test
    void elHistorialSacaElMismoPdf() {
        DocumentoPdf deResolver = exportacion.desdeResolver("x^3 - 2x^2 - x + 2", Campo.Q, null, HOY);
        int id = historial.listar(HistorialService.Filtro.todo(), 0).filas().stream()
                .filter(f -> f.texto().equals("x^3 - 2x^2 - x + 2")).findFirst().orElseThrow().id();
        DocumentoPdf deHistorial = exportacion.desdeHistorial(id, HOY);
        assertEquals(deResolver.pasos(), deHistorial.pasos());
        assertEquals(deResolver.resultadoLatex(), deHistorial.resultadoLatex());
        assertEquals(deResolver.comprobacion(), deHistorial.comprobacion());
    }

    @Test
    void unaIntegralSeCompruebaDerivandoYUnLimiteNoLleva() {
        DocumentoPdf integral = exportacion.desdeResolver("∫((3x + 5)/((x - 1)(x + 2)), x)", null, null, HOY);
        assertEquals("Derivando el resultado", integral.comprobacion().metodo());
        assertTrue(integral.comprobacion().coincide());
        assertEquals(1, integral.comprobacion().lineasLatex().size());

        DocumentoPdf limite = exportacion.desdeResolver("lim(x→2, (x^2 - 4)/(x - 2))", null, null, HOY);
        assertEquals("Calcular límite", limite.verbo());
        assertNull(limite.comprobacion());
    }

    @Test
    void unEjercicioRealDeDiezPasosOcupaMasDeUnaPaginaSinCortarPasos() throws IOException {
        DocumentoPdf d = exportacion.desdeResolver("grad(x^2 + y^2, (1, 2))", null, null, HOY);
        assertEquals(10, d.pasos().size());
        Path pdf = carpeta.resolve("gradiente.pdf");
        exportacion.exportar(d, pdf);
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertTrue(doc.getNumberOfPages() > 1, "páginas: " + doc.getNumberOfPages());
            PDFTextStripper s = new PDFTextStripper();
            int encabezados = 0;
            int formulasPaso = 0;
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                s.setStartPage(i);
                s.setEndPage(i);
                Matcher m = ENCABEZADO.matcher(s.getText(doc).replaceAll("\\s+", " "));
                int enPagina = 0;
                while (m.find()) {
                    enPagina++;
                }
                int deFormula = formulas(doc.getPage(i - 1)).getOrDefault("paso", 0);
                // Cada fórmula está en la página de su encabezado
                assertEquals(enPagina, deFormula, "página " + i);
                encabezados += enPagina;
                formulasPaso += deFormula;
            }
            assertEquals(10, encabezados);
            assertEquals(10, formulasPaso);
        }
    }

    private static Map<String, Integer> formulas(Path pdf) throws IOException {
        try (PDDocument d = Loader.loadPDF(pdf.toFile())) {
            Map<String, Integer> total = new HashMap<>();
            for (PDPage p : d.getPages()) {
                formulas(p).forEach((k, v) -> total.merge(k, v, Integer::sum));
            }
            return total;
        }
    }

    private static Map<String, Integer> formulas(PDPage p) throws IOException {
        Map<String, Integer> r = new HashMap<>();
        for (COSName n : p.getResources().getXObjectNames()) {
            if (p.getResources().getXObject(n) instanceof PDFormXObject f) {
                r.merge(String.valueOf(f.getCOSObject().getString(ExportadorPdfBox.ROL)), 1, Integer::sum);
            }
        }
        return r;
    }
}
