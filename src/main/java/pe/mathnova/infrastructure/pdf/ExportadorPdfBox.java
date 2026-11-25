package pe.mathnova.infrastructure.pdf;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.springframework.stereotype.Component;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import pe.mathnova.application.pdf.DocumentoPdf;
import pe.mathnova.application.pdf.ExportadorPdf;

/**
 * El PDF con PDFBox (Sprint 9, parte C): A4 vertical, en negro con azul.
 *
 * <p>Fórmulas en vectores: jlatexmath dibuja cada LaTeX sobre un {@link PdfBoxGraphics2D}, que convierte los
 * trazos y los glifos de sus fuentes en curvas de un Form XObject; no hay ningún mapa de bits, así que se
 * imprimen nítidas a cualquier tamaño. Cada fórmula es un Form XObject propio, marcado con su papel
 * ({@link #ROL}: enunciado, paso, resultado o comprobación).
 *
 * <p>Páginas: el documento es una lista de bloques que saben su alto. Un bloque que no cabe en lo que queda de
 * la página empieza la siguiente; así un paso (número, título, fórmula y explicación) nunca queda partido.
 */
@Component
public class ExportadorPdfBox implements ExportadorPdf {

    /** Clave propia en el diccionario de cada fórmula: qué es. Los lectores de PDF la ignoran. */
    public static final COSName ROL = COSName.getPDFName("MathNovaRol");

    static final PDRectangle HOJA = PDRectangle.A4;
    static final float MARGEN = 56;
    static final float MARGEN_ARRIBA = 50;
    static final float MARGEN_ABAJO = 56;
    static final float ANCHO = HOJA.getWidth() - 2 * MARGEN;

    static final Color AZUL = new Color(0x1d, 0x4e, 0xd8);
    static final Color AZUL_CLARO = new Color(0xef, 0xf6, 0xff);
    static final Color TINTA = new Color(0x0f, 0x17, 0x2a);
    static final Color GRIS = new Color(0x47, 0x55, 0x69);
    static final Color BORDE = new Color(0xcb, 0xd5, 0xe1);

    static final float TAMANO_ENUNCIADO = 17;
    static final float TAMANO_PASO = 13;
    static final float TAMANO_RESULTADO = 16;
    static final float TAMANO_COMPROBACION = 12;
    /** Sangría por nivel de un paso anidado. */
    static final float SANGRIA = 18;

    @Override
    public void exportar(DocumentoPdf d, Path destino) throws IOException {
        try (PDDocument pdf = new PDDocument()) {
            Fuentes fuentes = Fuentes.cargar(pdf);
            PDDocumentInformation info = pdf.getDocumentInformation();
            info.setTitle("MathNova · " + d.verbo());
            info.setCreator("MathNova");

            Maquetador m = new Maquetador(pdf, fuentes, d.fecha());
            try {
                for (Bloque b : bloques(pdf, fuentes, d)) {
                    m.colocar(b);
                }
            } finally {
                m.cerrar();
            }
            m.numerarPaginas();
            Path carpeta = destino.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            pdf.save(destino.toFile());
        }
    }

    // ================================================================== contenido

    private static List<Bloque> bloques(PDDocument pdf, Fuentes f, DocumentoPdf d) throws IOException {
        List<Bloque> r = new ArrayList<>();
        // Sprint 9, parte E: el tamaño de letra de Configuración (textos y fórmulas; los títulos no cambian)
        float k = (float) d.escalaLetra();
        // El verbo y el enunciado
        Bloque verbo = new Texto(f, f.negrita(), 10, AZUL, d.verbo().toUpperCase(java.util.Locale.ROOT), 0, 4);
        verbo.conSiguiente = true;
        r.add(verbo);
        r.add(new Formula(pdf, d.enunciadoLatex(), d.enunciadoTexto(), TAMANO_ENUNCIADO * k, "enunciado", 0, 18, f));

        // Los pasos
        if (!d.pasos().isEmpty()) {
            Bloque titulo = new Titulo(f, "Pasos");
            titulo.conSiguiente = true;
            r.add(titulo);
            for (DocumentoPdf.Paso p : d.pasos()) {
                r.add(new PasoBloque(pdf, f, p, k));
            }
        }

        // El resultado en su recuadro
        Bloque rotulo = new Titulo(f, "Resultado");
        rotulo.conSiguiente = true;
        r.add(rotulo);
        r.add(new Recuadro(new Formula(pdf, d.resultadoLatex(), d.resultadoTexto(), TAMANO_RESULTADO * k, "resultado",
                0, 0, f)));
        if (!d.restricciones().isEmpty()) {
            r.add(new Texto(f, f.normal(), 10 * k, GRIS, "Restricciones: " + String.join(", ", d.restricciones()), 0, 0));
        }

        // La comprobación
        DocumentoPdf.Comprobacion c = d.comprobacion();
        if (c != null) {
            Bloque titulo = new Titulo(f, "Comprobación");
            titulo.conSiguiente = true;
            r.add(titulo);
            if (c.metodo() != null && !c.metodo().isBlank()) {
                Bloque metodo = new Texto(f, f.normal(), 10.5f * k, GRIS, c.metodo(), 0, 6);
                metodo.conSiguiente = !c.lineasLatex().isEmpty();
                r.add(metodo);
            }
            for (String linea : c.lineasLatex()) {
                r.add(new Formula(pdf, linea, linea, TAMANO_COMPROBACION * k, "comprobacion", 0, 6, f));
            }
            r.add(new Texto(f, f.negrita(), 10.5f * k, c.coincide() ? AZUL : TINTA,
                    c.mensaje(), 0, 0));
        }

        // La gráfica, solo si se pidió
        if (d.graficaPng() != null && d.graficaPng().length > 0) {
            Bloque titulo = new Titulo(f, "Gráfica");
            titulo.conSiguiente = true;
            r.add(titulo);
            r.add(new Imagen(PDImageXObject.createFromByteArray(pdf, d.graficaPng(), "grafica")));
        }
        return r;
    }

    // ================================================================== páginas

    /** Pone los bloques de arriba abajo y abre página cuando el siguiente no cabe. */
    private static final class Maquetador {
        private final PDDocument pdf;
        private final Fuentes fuentes;
        private final String fecha;
        private PDPageContentStream cs;
        private float y;
        private final List<Bloque> pendientes = new ArrayList<>();

        Maquetador(PDDocument pdf, Fuentes fuentes, String fecha) throws IOException {
            this.pdf = pdf;
            this.fuentes = fuentes;
            this.fecha = fecha;
            nuevaPagina();
        }

        private float utilTotal() {
            return HOJA.getHeight() - MARGEN_ARRIBA - MARGEN_ABAJO - ALTO_ENCABEZADO;
        }

        private float abajo() {
            return MARGEN_ABAJO;
        }

        /**
         * Un bloque marcado "con el siguiente" (un título) espera al siguiente: los dos van juntos a la página
         * que les toque, para que un título no quede solo al pie.
         */
        void colocar(Bloque b) throws IOException {
            pendientes.add(b);
            if (b.conSiguiente) {
                return;
            }
            float alto = 0;
            for (Bloque p : pendientes) {
                alto += p.alto() + p.despues;
            }
            boolean vacia = y >= HOJA.getHeight() - MARGEN_ARRIBA - ALTO_ENCABEZADO - 0.5f;
            if (y - alto < abajo() && !vacia) {
                nuevaPagina();
            }
            for (Bloque p : pendientes) {
                // Un bloque más alto que una página entera (no debería pasar) se dibuja igual, desde arriba.
                p.dibujar(cs, MARGEN, y);
                y -= p.alto() + p.despues;
            }
            pendientes.clear();
        }

        private void nuevaPagina() throws IOException {
            if (cs != null) {
                cs.close();
            }
            PDPage pagina = new PDPage(HOJA);
            pdf.addPage(pagina);
            cs = new PDPageContentStream(pdf, pagina);
            encabezado();
            y = HOJA.getHeight() - MARGEN_ARRIBA - ALTO_ENCABEZADO;
        }

        static final float ALTO_ENCABEZADO = 44;

        /** "MathNova" en azul a la izquierda, la fecha a la derecha y una raya azul. */
        private void encabezado() throws IOException {
            float base = HOJA.getHeight() - MARGEN_ARRIBA - 14;
            fuentes.escribir(cs, fuentes.negrita(), 15, AZUL, "MathNova", MARGEN, base);
            float ancho = fuentes.ancho(fuentes.normal(), 10, fecha);
            fuentes.escribir(cs, fuentes.normal(), 10, GRIS, fecha, HOJA.getWidth() - MARGEN - ancho, base);
            cs.setStrokingColor(AZUL);
            cs.setLineWidth(1.2f);
            cs.moveTo(MARGEN, base - 10);
            cs.lineTo(HOJA.getWidth() - MARGEN, base - 10);
            cs.stroke();
        }

        void cerrar() throws IOException {
            if (!pendientes.isEmpty()) {
                List<Bloque> resto = new ArrayList<>(pendientes);
                pendientes.clear();
                for (Bloque b : resto) {
                    b.conSiguiente = false;
                    colocar(b);
                }
            }
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }

        /** "Página 1 de 3" al pie de cada página. */
        void numerarPaginas() throws IOException {
            int total = pdf.getNumberOfPages();
            int i = 0;
            for (PDPage p : pdf.getPages()) {
                i++;
                try (PDPageContentStream pie = new PDPageContentStream(pdf, p, PDPageContentStream.AppendMode.APPEND,
                        true, true)) {
                    String t = "Página " + i + " de " + total;
                    float ancho = fuentes.ancho(fuentes.normal(), 9, t);
                    fuentes.escribir(pie, fuentes.normal(), 9, GRIS, t, (HOJA.getWidth() - ancho) / 2, MARGEN_ABAJO / 2);
                }
            }
        }
    }

    // ================================================================== bloques

    /** Algo que se dibuja de una pieza: sabe su alto y no se parte entre páginas. */
    abstract static class Bloque {
        /** Espacio debajo. */
        float despues;
        /** Va en la misma página que el bloque que sigue (títulos). */
        boolean conSiguiente;

        abstract float alto();

        /** Dibuja con la esquina superior izquierda en (x, arriba). */
        abstract void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException;
    }

    /** "Pasos", "Resultado"… en negrita. */
    private static final class Titulo extends Bloque {
        private final Fuentes f;
        private final String texto;

        Titulo(Fuentes f, String texto) {
            this.f = f;
            this.texto = texto;
            this.despues = 8;
        }

        @Override
        float alto() {
            return 22;
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            f.escribir(cs, f.negrita(), 12.5f, TINTA, texto, x, arriba - 16);
        }
    }

    /** Un párrafo que se parte en renglones al ancho disponible. */
    private static final class Texto extends Bloque {
        private final org.apache.pdfbox.pdmodel.font.PDFont fuente;
        private final float tamano;
        private final Color color;
        private final float sangria;
        private final List<String> renglones;
        private final Fuentes f;

        Texto(Fuentes f, org.apache.pdfbox.pdmodel.font.PDFont fuente, float tamano, Color color, String texto,
              float sangria, float despues) {
            this.f = f;
            this.fuente = fuente;
            this.tamano = tamano;
            this.color = color;
            this.sangria = sangria;
            this.despues = despues;
            this.renglones = this.f.partir(fuente, tamano, texto, ANCHO - sangria);
        }

        float interlineado() {
            return tamano * 1.35f;
        }

        @Override
        float alto() {
            return renglones.size() * interlineado();
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            float base = arriba - tamano;
            for (String r : renglones) {
                f.escribir(cs, fuente, tamano, color, r, x + sangria, base);
                base -= interlineado();
            }
        }
    }

    /**
     * Una fórmula en vectores: jlatexmath la pinta en un PdfBoxGraphics2D y queda como Form XObject. Si es más
     * ancha que la hoja, se reduce (es vectorial: no pierde nitidez).
     */
    static final class Formula extends Bloque {
        private final PDFormXObject forma;
        private final float ancho;
        private final float alto;
        private final float escala;
        private final float sangria;

        Formula(PDDocument pdf, String latex, String textoAlterno, float tamano, String rol, float sangria,
                float despues, Fuentes f) throws IOException {
            this.sangria = sangria;
            this.despues = despues;
            TeXIcon icono = icono(latex, textoAlterno, tamano);
            int w = Math.max(1, icono.getIconWidth());
            int h = Math.max(1, icono.getIconHeight());
            PdfBoxGraphics2D g = new PdfBoxGraphics2D(pdf, w, h);
            try {
                icono.paintIcon(null, g, 0, 0);
            } finally {
                g.dispose();
            }
            forma = g.getXFormObject();
            forma.getCOSObject().setString(ROL, rol);
            this.ancho = w;
            this.alto = h;
            float disponible = ANCHO - sangria;
            float maxAlto = HOJA.getHeight() - MARGEN_ARRIBA - MARGEN_ABAJO - Maquetador.ALTO_ENCABEZADO - 120;
            this.escala = Math.min(1f, Math.min(disponible / w, maxAlto / h));
        }

        /** El LaTeX; si jlatexmath no lo entiende, el texto tal cual. */
        private static TeXIcon icono(String latex, String alterno, float tamano) {
            String l = latex == null || latex.isBlank() ? textoLatex(alterno) : latex;
            TeXIcon icono;
            try {
                icono = new TeXFormula(l).createTeXIcon(TeXConstants.STYLE_DISPLAY, tamano);
            } catch (RuntimeException e) {
                icono = new TeXFormula(textoLatex(alterno == null ? l : alterno))
                        .createTeXIcon(TeXConstants.STYLE_DISPLAY, tamano);
            }
            icono.setForeground(Color.BLACK);
            return icono;
        }

        private static String textoLatex(String t) {
            String s = t == null ? "" : t;
            return "\\text{" + s.replace("\\", "").replace("{", "").replace("}", "").replace("$", "")
                    .replace("%", "\\%").replace("&", "\\&").replace("#", "\\#").replace("_", "\\_") + "}";
        }

        float anchoFinal() {
            return ancho * escala;
        }

        @Override
        float alto() {
            return alto * escala;
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            cs.saveGraphicsState();
            cs.transform(new Matrix(escala, 0, 0, escala, x + sangria, arriba - alto * escala));
            cs.drawForm(forma);
            cs.restoreGraphicsState();
        }
    }

    /** "2.1 → Diferencia de cuadrados", la fórmula y la explicación: siempre en la misma página. */
    private static final class PasoBloque extends Bloque {
        private final Fuentes f;
        private final DocumentoPdf.Paso paso;
        private final float sangria;
        private final Formula formula;
        private final Texto explicacion;
        private final List<String> titulo;

        /** El tamaño del título del paso. */
        private final float t;

        PasoBloque(PDDocument pdf, Fuentes f, DocumentoPdf.Paso p, float k) throws IOException {
            this.f = f;
            this.t = 11 * k;
            this.paso = p;
            this.sangria = SANGRIA * Math.min(p.nivel(), 4);
            float anchoNumero = f.ancho(f.negrita(), t, p.numero() + "  ") + 4;
            this.titulo = f.partir(f.negrita(), t, "→  " + p.titulo(), ANCHO - sangria - anchoNumero);
            this.formula = p.tieneFormula()
                    ? new Formula(pdf, p.latex(), p.latex(), TAMANO_PASO * k, "paso", sangria + 4, 0, f) : null;
            this.explicacion = p.explicacion().isBlank() ? null
                    : new Texto(f, f.normal(), 10 * k, GRIS, p.explicacion(), sangria + 4, 0);
            this.despues = 14;
        }

        private float altoTitulo() {
            return titulo.size() * t * 1.35f;
        }

        @Override
        float alto() {
            float a = altoTitulo();
            if (formula != null) {
                a += 4 + formula.alto();
            }
            if (explicacion != null) {
                a += 4 + explicacion.alto();
            }
            return a;
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            float base = arriba - t;
            String numero = paso.numero();
            f.escribir(cs, f.negrita(), t, AZUL, numero, x + sangria, base);
            float dx = f.ancho(f.negrita(), t, numero + "  ") + 4;
            for (String r : titulo) {
                f.escribir(cs, f.negrita(), t, TINTA, r, x + sangria + dx, base);
                base -= t * 1.35f;
            }
            float y = arriba - altoTitulo();
            if (formula != null) {
                y -= 4;
                formula.dibujar(cs, x, y);
                y -= formula.alto();
            }
            if (explicacion != null) {
                y -= 4;
                // La línea azul a la izquierda de la explicación, como en la Pizarra.
                cs.setStrokingColor(AZUL);
                cs.setLineWidth(1.5f);
                cs.moveTo(x + sangria, y);
                cs.lineTo(x + sangria, y - explicacion.alto() + 2);
                cs.stroke();
                explicacion.dibujar(cs, x + 6, y);
            }
        }
    }

    /** El resultado: borde azul y fondo azul muy claro. */
    private static final class Recuadro extends Bloque {
        private static final float RELLENO = 10;
        private final Formula formula;

        Recuadro(Formula formula) {
            this.formula = formula;
            this.despues = 12;
        }

        @Override
        float alto() {
            return formula.alto() + 2 * RELLENO;
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            float ancho = Math.min(ANCHO, formula.anchoFinal() + 2 * RELLENO);
            cs.setNonStrokingColor(AZUL_CLARO);
            cs.setStrokingColor(AZUL);
            cs.setLineWidth(1.2f);
            cs.addRect(x, arriba - alto(), ancho, alto());
            cs.fillAndStroke();
            formula.dibujar(cs, x + RELLENO, arriba - RELLENO);
        }
    }

    /** La gráfica, al ancho de la hoja con su proporción. */
    private static final class Imagen extends Bloque {
        private final PDImageXObject imagen;
        private final float ancho;
        private final float alto;

        Imagen(PDImageXObject imagen) {
            this.imagen = imagen;
            float w = Math.min(ANCHO, imagen.getWidth());
            float h = w * imagen.getHeight() / imagen.getWidth();
            float max = 420;
            if (h > max) {
                w = w * max / h;
                h = max;
            }
            this.ancho = w;
            this.alto = h;
            this.despues = 8;
        }

        @Override
        float alto() {
            return alto;
        }

        @Override
        void dibujar(PDPageContentStream cs, float x, float arriba) throws IOException {
            cs.drawImage(imagen, x, arriba - alto, ancho, alto);
            cs.setStrokingColor(BORDE);
            cs.setLineWidth(0.8f);
            cs.addRect(x, arriba - alto, ancho, alto);
            cs.stroke();
        }
    }
}
