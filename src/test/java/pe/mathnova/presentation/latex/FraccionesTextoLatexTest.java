package pe.mathnova.presentation.latex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.scilab.forge.jlatexmath.TeXFormula;

/** En la Pizarra, "a/b" del motor se escribe como fracción vertical; nada se corta ni cambia de valor. */
class FraccionesTextoLatexTest {

    private static String lx(String s) {
        String r = TextoLatex.convertir(s);
        new TeXFormula(r);
        return r;
    }

    @Test
    void numeradorYDenominadorEntreParentesis() {
        assertEquals("\\frac{3^{2} - 9}{3 - 3} = \\frac{0}{0}", lx("(3² − 9)/(3 − 3) = 0/0"));
        assertEquals("\\frac{(x + 3)(x - 3)}{x - 3}", lx("((x + 3)(x − 3))/(x − 3)"));
    }

    @Test
    void potenciasYFactoresPegados() {
        assertEquals("\\frac{x^{2}}{2}", lx("x²/2"));
        assertEquals("\\frac{2x}{3}", lx("2x/3"));
        assertEquals("\\ln x\\cdot \\frac{x^{2}}{2}", lx("ln x·x²/2"));
    }

    @Test
    void laFuncionVaConSuArgumento() {
        assertEquals("\\frac{\\mathrm{sen}\\, u}{u}", lx("sen u/u"));
    }

    @Test
    void conEspaciosQuedaEnLinea() {
        assertEquals("a / b", lx("a / b"));
    }

    @Test
    void sqrtEsRaiz() {
        assertEquals("\\sqrt{x + 4} - 2", lx("sqrt(x + 4) - 2"));
    }

    @Test
    void espacioFinoAntesDelDiferencial() {
        assertEquals("\\int \\frac{x}{2}\\,dx", TextoLatex.conDiferenciales("\\int \\frac{x}{2}dx"));
        assertEquals("\\frac{d}{dx}", TextoLatex.conDiferenciales("\\frac{d}{dx}"));
        assertEquals("a\\cdot b", TextoLatex.conDiferenciales("a\\cdot b"));
    }
}
