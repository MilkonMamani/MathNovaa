package pe.mathnova.presentation.latex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.scilab.forge.jlatexmath.TeXFormula;

/** Los renglones de pizarra del motor pasan a LaTeX sin cambiar su contenido. */
class TextoLatexTest {

    private static String lx(String s) {
        String r = TextoLatex.convertir(s);
        if (r != null) {
            new TeXFormula(r); // jlatexmath lo entiende
        }
        return r;
    }

    @Test
    void exponentesSignosYParentesis() {
        assertEquals("x^{6} - 6x^{4} + 9x^{2} = (x^{3} - 3x)^{2}", lx("x⁶ − 6x⁴ + 9x² = (x³ − 3x)²"));
        assertEquals("(x^{3} + 2x^{2}) \\cdot (x^{2} + 2x + 1) = x^{5} + 4x^{4}",
                lx("(x³ + 2x²) · (x² + 2x + 1) = x⁵ + 4x⁴"));
    }

    @Test
    void lasPalabrasVanComoTexto() {
        String r = lx("a² + 2ab + b² = (a + b)² con a = x y b = 1");
        assertTrue(r.contains("\\text{con}"), r);
        assertTrue(r.contains("\\text{y}"), r);
        assertTrue(r.contains("2ab"), "2ab es un producto: " + r);
    }

    @Test
    void radicalesYFunciones() {
        assertEquals("\\sqrt{2}x", lx("√2x"));
        assertEquals("\\sqrt{x + 1}", lx("√(x + 1)"));
        assertEquals("\\sqrt[3]{2}", lx("³√2"));
        assertTrue(lx("d/dx(sen x) = cos x").contains("\\cos"));
    }

    @Test
    void laProsaYLosSimbolosDesconocidosQuedanComoTexto() {
        assertNull(TextoLatex.convertir("Se evalúan la entrada y el resultado con 40 cifras:"));
        assertNull(TextoLatex.convertir("x ⊕ y"));
    }

    @Test
    void elIgualDeAfuera() {
        assertEquals(6, TextoLatex.igualDeAfuera("x^{2} = 1"));
        assertEquals(-1, TextoLatex.igualDeAfuera("\\frac{a=b}{2}"));
    }
}
