package pe.mathnova.application.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Lo que leyó UniMERNet Small (cadenas copiadas de ejecuciones reales, target/reconocimiento/lecturas.tsv)
 * pasa por el adaptador y por {@link TraductorLatex}, el mismo traductor que usa el teclado.
 */
class LatexReconocidoTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            // lectura real del modelo                         | LaTeX para el editor            | texto del parser
            "3 x ^ { 2 } + 2 x - 1                             | 3x^{2}+2x-1                     | 3x^2 + 2x - 1",
            "3 \\times ^ { 2 } + 2 x - 1                       | 3x^{2}+2x-1                     | 3x^2 + 2x - 1",
            "\\int 2 x \\, d x                                 | \\int 2xdx                      | ∫(2x, x)",
            "\\int 2 x d x                                     | \\int 2xdx                      | ∫(2x, x)",
            "\\int _ { 0 } ^ { 1 } \\, 2 x \\, d x             | \\int_{0}^{1}2xdx               | ∫(2x, x, 0, 1)",
            "\\frac { x + 1 } { x - 1 }                        | \\frac{x+1}{x-1}                | (x + 1)/(x - 1)",
            "\\frac { ( x + 1 ) } { ( x - 1 ) }                | \\frac{(x+1)}{(x-1)}            | (x + 1)/(x - 1)",
            "\\sqrt { x ^ { 2 } + 1 }                          | \\sqrt{x^{2}+1}                 | √(x^2 + 1)",
            "\\sqrt { X ^ { 2 } + 1 }                          | \\sqrt{x^{2}+1}                 | √(x^2 + 1)",
            "\\Delta X + Y                                     | \\Delta x+y                     | ",
            "\\int \\mathrm { d } x \\, 2 x .                  | \\int dx2x                      | ",
            "\\displaystyle \\int \\limits _ { 0 } ^ { 1 } x \\mathrm { d } x | \\int_{0}^{1}xdx | ∫(x, x, 0, 1)",
    })
    void adaptaYTraduce(String leido, String editor, String texto) {
        String adaptado = LatexReconocido.adaptar(leido);
        assertEquals(editor, adaptado);
        if (texto != null) {
            assertEquals(texto, TraductorLatex.traducir(adaptado));
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\\left. x \\right.  | \\left.x\\right.",
            "x \\cdot y          | x\\cdot y",
            "\\pi 2              | \\pi 2",
            "a \\  b             | ab",
    })
    void noRompeComandosNiDelimitadores(String leido, String esperado) {
        assertEquals(esperado, LatexReconocido.adaptar(leido));
    }
}
