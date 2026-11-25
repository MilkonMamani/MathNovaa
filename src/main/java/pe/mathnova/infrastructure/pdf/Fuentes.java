package pe.mathnova.infrastructure.pdf;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/**
 * Las fuentes del texto del PDF (no de las fórmulas, que dibuja jlatexmath). Se usa la fuente de la app
 * (Segoe UI en Windows) incrustada solo con los glifos usados, para que "→", "≠" o "√" salgan bien; si no está,
 * Arial o DejaVu Sans, y en último caso Helvetica. Un carácter que la fuente no tiene se cambia por uno
 * parecido ("→" por "->") en vez de romper la exportación.
 */
final class Fuentes {

    private static final String[][] NORMAL = {
            {"C:/Windows/Fonts/segoeui.ttf"}, {"C:/Windows/Fonts/arial.ttf"},
            {"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"}, {"/usr/share/fonts/dejavu/DejaVuSans.ttf"},
            {"/System/Library/Fonts/Supplemental/Arial.ttf"}, {"/Library/Fonts/Arial.ttf"}};
    private static final String[][] NEGRITA = {
            {"C:/Windows/Fonts/segoeuib.ttf"}, {"C:/Windows/Fonts/arialbd.ttf"},
            {"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"}, {"/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf"},
            {"/System/Library/Fonts/Supplemental/Arial Bold.ttf"}, {"/Library/Fonts/Arial Bold.ttf"}};

    private static final Map<Integer, String> PARECIDOS = Map.ofEntries(
            Map.entry((int) '→', "->"), Map.entry((int) '≠', "!="), Map.entry((int) '≤', "<="),
            Map.entry((int) '≥', ">="), Map.entry((int) '−', "-"), Map.entry((int) '·', "*"),
            Map.entry((int) '✓', "OK"), Map.entry((int) '✗', "X"), Map.entry((int) '≈', "~"),
            Map.entry((int) '√', "raiz"), Map.entry((int) '∞', "inf"), Map.entry((int) 'π', "pi"),
            Map.entry((int) '²', "^2"), Map.entry((int) '³', "^3"), Map.entry((int) '\u00a0', " "));

    private final PDFont normal;
    private final PDFont negrita;
    private final Map<PDFont, Map<Integer, Boolean>> glifos = new HashMap<>();

    private Fuentes(PDFont normal, PDFont negrita) {
        this.normal = normal;
        this.negrita = negrita;
    }

    static Fuentes cargar(PDDocument pdf) {
        PDFont n = buscar(pdf, NORMAL);
        PDFont b = buscar(pdf, NEGRITA);
        if (n == null) {
            n = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
        if (b == null) {
            b = n instanceof PDType0Font ? n : new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        }
        return new Fuentes(n, b);
    }

    private static PDFont buscar(PDDocument pdf, String[][] rutas) {
        for (String[] r : rutas) {
            File f = new File(r[0]);
            if (f.isFile()) {
                try {
                    return PDType0Font.load(pdf, f);
                } catch (IOException | RuntimeException e) {
                    // sigue con la siguiente
                }
            }
        }
        return null;
    }

    PDFont normal() {
        return normal;
    }

    PDFont negrita() {
        return negrita;
    }

    /** El texto con solo los caracteres que la fuente puede dibujar. */
    String seguro(PDFont f, String t) {
        if (t == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(t.length());
        t.codePoints().forEach(cp -> {
            if (cp == '\n' || cp == '\r' || cp == '\t') {
                sb.append(' ');
            } else if (tiene(f, cp)) {
                sb.appendCodePoint(cp);
            } else {
                String otro = PARECIDOS.get(cp);
                sb.append(otro != null && otro.codePoints().allMatch(c -> tiene(f, c)) ? otro : "?");
            }
        });
        return sb.toString();
    }

    private boolean tiene(PDFont f, int cp) {
        return glifos.computeIfAbsent(f, k -> new HashMap<>()).computeIfAbsent(cp, c -> {
            try {
                f.encode(new String(Character.toChars(c)));
                return true;
            } catch (IOException | IllegalArgumentException e) {
                return false;
            }
        });
    }

    float ancho(PDFont f, float tamano, String t) {
        try {
            return f.getStringWidth(seguro(f, t)) / 1000 * tamano;
        } catch (IOException e) {
            return t.length() * tamano * 0.5f;
        }
    }

    /** Parte el texto en renglones de a lo más {@code ancho} puntos, por palabras. */
    List<String> partir(PDFont f, float tamano, String t, float ancho) {
        List<String> renglones = new ArrayList<>();
        String limpio = t == null ? "" : t.strip().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return renglones;
        }
        StringBuilder actual = new StringBuilder();
        for (String palabra : limpio.split(" ")) {
            String prueba = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (ancho(f, tamano, prueba) <= ancho) {
                actual.setLength(0);
                actual.append(prueba);
                continue;
            }
            if (!actual.isEmpty()) {
                renglones.add(actual.toString());
                actual.setLength(0);
            }
            // Una palabra más larga que el renglón se corta por caracteres.
            String resto = palabra;
            while (ancho(f, tamano, resto) > ancho && resto.length() > 1) {
                int k = resto.length() - 1;
                while (k > 1 && ancho(f, tamano, resto.substring(0, k)) > ancho) {
                    k--;
                }
                renglones.add(resto.substring(0, k));
                resto = resto.substring(k);
            }
            actual.append(resto);
        }
        if (!actual.isEmpty()) {
            renglones.add(actual.toString());
        }
        return renglones;
    }

    void escribir(PDPageContentStream cs, PDFont f, float tamano, Color color, String t, float x, float base)
            throws IOException {
        String s = seguro(f, t);
        if (s.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setFont(f, tamano);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, base);
        cs.showText(s);
        cs.endText();
    }
}
