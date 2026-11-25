package pe.mathnova.domain.expresion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.parser.Parser;

class ImpresoraLatexTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "x^2 - 3x + 2    | x^{2} - 3x + 2",
            "x^2(x - 3)      | x^{2}\\left(x - 3\\right)",
            "(x + 1)/(x - 2) | \\frac{x + 1}{x - 2}",
            "√2x             | \\sqrt{2}x",
            "∛(x + 1)        | \\sqrt[3]{x + 1}",
            "sen^2(x)        | \\mathrm{sen}^{2}\\left(x\\right)",
            "cos x           | \\cos\\left(x\\right)",
            "log_2(x)        | \\log_{2}\\left(x\\right)",
            "e^(2x)          | e^{2x}",
            "0.5x            | \\frac{1}{2}x",
            "2·3             | 2 \\cdot 3",
            "πx              | `\\pi x`",
            "-(x + 1)        | -\\left(x + 1\\right)",
            "(x + 1)^3       | \\left(x + 1\\right)^{3}"
    })
    void convierteALatex(String entrada, String esperado) {
        assertEquals(esperado, ImpresoraLatex.imprimir(Parser.parse(entrada)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "lim(x→2, (x^2 - 4)/(x - 2))     | \\lim_{x \\to 2} \\frac{x^{2} - 4}{x - 2}",
            "lim(x->2+, 1/(x - 2))           | \\lim_{x \\to 2^{+}} \\frac{1}{x - 2}",
            "lim(x→2-, 1/(x - 2))            | \\lim_{x \\to 2^{-}} \\frac{1}{x - 2}",
            "lim(x→∞, 1/x)                   | \\lim_{x \\to \\infty} \\frac{1}{x}",
            "lim(x→-∞, x + 1)                | \\lim_{x \\to -\\infty} \\left(x + 1\\right)",
            "d/dx(x^2)                       | \\frac{d}{dx}\\left(x^{2}\\right)",
            "d2/dx2(x^3)                     | \\frac{d^{2}}{dx^{2}}\\left(x^{3}\\right)",
            "∂/∂y(x^2 y)                     | \\frac{\\partial}{\\partial y}\\left(x^{2}y\\right)",
            "∂2/∂x∂y(x y)                    | \\frac{\\partial^{2}}{\\partial x \\partial y}\\left(xy\\right)",
            "∂2/∂x2(x y)                     | \\frac{\\partial^{2}}{\\partial x^{2}}\\left(xy\\right)",
            "∫(x^2, x, 0, 1)                 | \\int_{0}^{1} x^{2} \\, dx",
            "∫(x + 1, x)                     | \\int \\left(x + 1\\right) \\, dx",
            "∬(x^2 + y^2, x, 0, 1, y, 0, 2)  | \\iint_{[0, 1] \\times [0, 2]} \\left(x^{2} + y^{2}\\right) \\, dx \\, dy",
            "∭(x y z, x, 0, 1, y, 0, 1, z, 0, 1) | \\iiint_{[0, 1] \\times [0, 1] \\times [0, 1]} xyz \\, dx \\, dy \\, dz",
            "√[4](x)                         | \\sqrt[4]{x}",
            "2α + β                          | `2\\alpha  + \\beta `",
            "sen θ                           | `\\mathrm{sen}\\left(\\theta \\right)`",
            "d/dθ(sen θ)                     | `\\frac{d}{d\\theta}\\left(\\mathrm{sen}\\left(\\theta \\right)\\right)`"
    })
    void dibujaLasOperaciones(String entrada, String esperado) {
        assertEquals(esperado, ImpresoraLatex.imprimir(Parser.parse(entrada)));
    }
}
