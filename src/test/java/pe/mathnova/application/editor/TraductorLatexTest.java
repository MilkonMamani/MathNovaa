package pe.mathnova.application.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.factorizacion.FactorizadorFunciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

class TraductorLatexTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            // potencias, fracciones y multiplicación implícita
            "x^2                                   | x^2",
            "x^{2}-5x+6                            | x^2 - 5x + 6",
            "\\frac{1}{2}                          | 1/2",
            "\\frac12x                             | (1/2)x",
            "\\frac{x+1}{x-1}                      | (x + 1)/(x - 1)",
            "\\frac{2x}{3}                         | 2x/3",
            "\\frac{1}{2x}                         | 1/(2x)",
            "2\\frac{1}{2}                         | 2(1/2)",
            "x\\cdot\\frac{1}{2}                   | x·(1/2)",
            "\\frac{1}{2}+\\frac{1}{3}             | 1/2 + 1/3",
            "-\\frac{1}{2}                         | -1/2",
            "\\frac{-x}{2}                         | (-x)/2",
            "\\frac{x^{2}}{\\sqrt{x}}              | x^2/√x",
            "x^{n+1}                               | x^(n + 1)",
            "x^{-2}                                | x^-2",
            "e^{2x}                                | e^(2x)",
            "e^{x}x                                | e^x·x",
            "x_{1}                                 | x_1",
            "2xy                                   | 2xy",
            "2\\cdot3                              | 2·3",
            "2\\times3                             | 2·3",
            "3.5x                                  | 3.5x",
            "a\\left(b+c\\right)                   | a(b + c)",
            "\\left(x-1\\right)\\left(x+2\\right)  | (x - 1)(x + 2)",
            "\\left(x-1\\right)^2                  | (x - 1)^2",
            "\\left\\lbrack x+1\\right\\rbrack^2   | (x + 1)^2",
            // raíces
            "\\sqrt{x}                             | √x",
            "\\sqrt{x+1}                           | √(x + 1)",
            "\\sqrt[3]{x}                          | ∛x",
            "\\sqrt[4]{x}                          | √[4]x",
            "\\sqrt{2}x                            | √2·x",
            // integrales (RF69: límites opcionales)
            "3\\int_{3}^{6}3x\\,dx                 | 3·∫(3x, x, 3, 6)",
            "\\int x^2\\,dx                        | ∫(x^2, x)",
            "\\int_{\\placeholder{}}^{\\placeholder{}}x\\,dx | ∫(x, x)",
            "\\int_0^1\\left(x+1\\right)\\,dx      | ∫(x + 1, x, 0, 1)",
            "\\int_{0}^{\\pi}\\sin x\\,dx          | ∫(sen(x), x, 0, π)",
            "\\int_{\\placeholder{}}^{2}x\\,dx     | ∫(x, x, □, 2)",
            "\\iint_{[0,1]\\times[0,2]}\\left(x^2+y^2\\right)\\,dx\\,dy | ∬(x^2 + y^2, x, 0, 1, y, 0, 2)",
            "\\iiint_{[0,1]\\times[0,1]\\times[0,1]}xyz\\,dx\\,dy\\,dz  | ∭(xyz, x, 0, 1, y, 0, 1, z, 0, 1)",
            // límites
            "\\lim_{x\\to2}\\frac{x^2-4}{x-2}      | lim(x→2, (x^2 - 4)/(x - 2))",
            "\\lim_{x\\to0^{+}}\\frac{1}{x}        | lim(x→0+, 1/x)",
            "\\lim_{x\\to0^-}\\frac{1}{x}          | lim(x→0-, 1/x)",
            "\\lim_{x\\to\\infty}\\frac{1}{x}      | lim(x→∞, 1/x)",
            "\\lim_{x\\to-\\infty}\\left(x+1\\right) | lim(x→-∞, x + 1)",
            "\\lim_{x\\to\\frac{\\pi}{2}}\\sin x   | lim(x→π/2, sen(x))",
            // derivadas
            "\\frac{d}{dx}\\left(x^2\\right)       | d/dx(x^2)",
            "\\frac{d^2}{dx^2}x^3                  | d2/dx2(x^3)",
            "\\frac{\\mathrm{d}}{\\mathrm{d}x}x^2  | d/dx(x^2)",
            "\\frac{\\partial}{\\partial x}\\left(x^2y\\right) | ∂/∂x(x^2y)",
            "\\frac{\\partial^{2}}{\\partial x\\partial y}\\left(xy\\right) | ∂2/∂x∂y(xy)",
            "\\frac{\\partial^{2}}{\\partial x^{2}}\\left(xy\\right) | ∂2/∂x∂x(xy)",
            // funciones
            "\\sin x                               | sen(x)",
            "\\operatorname{sen}\\left(x\\right)   | sen(x)",
            "\\operatorname{\\mathrm{sen}}\\left(x\\right) | sen(x)",
            "\\operatorname{\\mathrm{sen}}^{-1}\\left(x\\right) | arcsen(x)",
            "\\frac{\\frac{1}{2}}{\\frac{3}{4}}    | (1/2)/(3/4)",
            "\\sin^{2}\\left(x\\right)             | sen^2(x)",
            "\\sin^{-1}x                           | arcsen(x)",
            "\\cos\\left(2x\\right)                | cos(2x)",
            "\\tan\\theta                          | tan(θ)",
            "\\ln x                                | ln(x)",
            "\\log_{2}\\left(x\\right)             | log_2(x)",
            "\\log_{10}x                           | log(x)",
            "\\log x                               | log(x)",
            "x\\sin x                              | x·sen(x)",
            "2\\sin x                              | 2sen(x)",
            // constantes y griegas
            "2\\pi r                               | 2πr",
            "\\alpha+\\beta                        | α + β",
            "\\theta^2                             | θ^2",
            "\\frac{\\pi}{2}                       | π/2",
            "\\infty                               | ∞",
            "\\exponentialE^{x}                    | e^x",
            // ecuaciones
            "x^2-5x+6=0                            | x^2 - 5x + 6 = 0",
            "2x^2=3x+2                             | 2x^2 = 3x + 2",
            // RN45: letras sueltas
            "ln                                    | l n",
            "sin                                   | si n",
            "pi                                    | p i",
            // casillas vacías
            "\\placeholder{}+1                     | □ + 1",
            "\\frac{\\placeholder{}}{\\placeholder{}} | □/□"
    })
    void traduce(String latex, String texto) {
        assertEquals(texto, TraductorLatex.traducir(latex));
    }

    @Test
    void vacioEsVacio() {
        assertEquals("", TraductorLatex.traducir(""));
        assertEquals("", TraductorLatex.traducir("  "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\\int x", "\\lim x", "\\iint x\\,dx", "\\foo"})
    void rechazaLoQueNoSabeEscribir(String latex) {
        assertThrows(TraductorLatex.LatexNoValido.class, () -> TraductorLatex.traducir(latex));
    }

    /** RN45: "l n" sigue siendo l·n para el parser, y \ln es logaritmo. */
    @Test
    void letrasSueltasSonVariables() {
        assertEquals(Parser.parse("l·n"), Parser.parse(TraductorLatex.traducir("ln")));
        assertEquals(Parser.parse("ln(x)"), Parser.parse(TraductorLatex.traducir("\\ln x")));
    }

    // ------------------------------------------------------------------ funciones del editor

    /**
     * Lo que deja el editor al teclear un nombre de función (\operatorname{sen}, \sin, \ln...), con o sin
     * exponente sobre el nombre, se lee igual que en la gramática: sen^2 x es (sen x)^2.
     */
    @ParameterizedTest(name = "{0} ≡ {1}")
    @CsvSource(delimiter = '|', value = {
            "\\operatorname{sen}x                         | sen x",
            "\\operatorname{\\mathrm{sen}}x                | sen x",
            "\\sin x                                      | sen x",
            "\\sin^{2}x                                   | sen^2 x",
            "\\sin^2x                                     | (sen x)^2",
            "\\operatorname{sen}^{2}x                     | sen^2 x",
            "\\operatorname{\\mathrm{sen}}^2x              | (sen(x))^2",
            "\\operatorname{sen}^{2}\\left(x\\right)       | sen^2(x)",
            "\\ln^{2}x                                    | ln^2 x",
            "\\ln^2x                                      | (ln x)^2",
            "\\tan^{2}x                                   | tan^2 x",
            "\\operatorname{\\mathrm{cosec}}x              | csc x",
            "\\operatorname{\\mathrm{arcsen}}x             | arcsen x",
            "2\\operatorname{\\mathrm{sen}}^2x-\\operatorname{\\mathrm{sen}}x-1 | 2sen^2 x - sen x - 1",
            "e^{(2x)}-3e^{x}+2                            | e^(2x) - 3e^x + 2",
            "\\ln^2x-4                                    | ln^2 x - 4",
            "\\tan^2x-1                                   | tan^2 x - 1",
            "\\cos(2x)+\\cos x                             | cos(2x) + cos x",
            "x+\\operatorname{\\mathrm{sen}}x              | x + sen x"
    })
    void funcionesComoEnLaGramatica(String latex, String gramatica) {
        assertEquals(Parser.parse(gramatica), Parser.parse(TraductorLatex.traducir(latex)), latex);
    }

    /** Del LaTeX del editor al orquestador: las seis expresiones con funciones del Sprint 4. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "2\\operatorname{\\mathrm{sen}}^2x-\\operatorname{\\mathrm{sen}}x-1 | (2sen(x) + 1)·(sen(x) - 1) | ",
            "e^{(2x)}-3e^{x}+2                 | (e^x - 1)·(e^x - 2)        | ",
            "\\ln^2x-4                         | (ln(x) + 2)·(ln(x) - 2)    | x > 0",
            "\\tan^2x-1                        | (tan(x) + 1)·(tan(x) - 1)  | x ≠ π/2 + kπ",
            "\\cos(2x)+\\cos x                  | (cos(x) + 1)·(2cos(x) - 1) | ",
            "x+\\operatorname{\\mathrm{sen}}x   | No se puede factorizar: no es polinómica en una sola función (RN32) | "
    })
    void funcionesDelEditorSeFactorizan(String latex, String formula, String restriccion) {
        var r = new FactorizadorFunciones(new OrquestadorFactorizacion())
                .resolver(Parser.parse(TraductorLatex.traducir(latex)));
        assertEquals(formula, r.formula());
        assertEquals(restriccion == null ? List.of() : List.of(restriccion), r.restricciones());
    }

    /** La casilla vacía llega al parser como □ y el parser pide completarla. */
    @Test
    void casillaVaciaLlegaAlParser() {
        ErrorSintaxis e = assertThrows(ErrorSintaxis.class,
                () -> Parser.parse(TraductorLatex.traducir("x^{\\placeholder{}}")));
        assertTrue(e.getMessage().contains("Completa la casilla vacía"), e.getMessage());
    }

    // ------------------------------------------------------------------ casillas (RF69)

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "x^2                                                  | true",
            "''                                                   | true",
            "x^{\\placeholder{}}                                  | false",
            "\\frac{\\placeholder{}}{2}                           | false",
            "\\int_{\\placeholder{}}^{\\placeholder{}}x\\,dx      | true",
            "\\int_{\\placeholder{}}^{\\placeholder{}}\\placeholder{}\\,dx | false",
            "\\int_{0}^{\\placeholder{}}x\\,dx                    | false",
            "\\int_{0}^{1}x\\,dx                                  | true",
            "\\int x\\,dx                                         | true",
            "\\iint_{[\\placeholder{},1]\\times[0,1]}x\\,dx\\,dy  | false",
            "\\lim_{x\\to\\placeholder{}}x                        | false"
    })
    void sabeSiEstaCompleto(String latex, boolean completo) {
        assertEquals(completo, TraductorLatex.completo(latex));
    }

    // ------------------------------------------------------------------ ida y vuelta

    /**
     * 100 entradas del banco. Quedan fuera las que ImpresoraLatex no puede devolver igual: con decimal
     * (0.5 se dibuja como \frac{1}{2}) o con sqrt(…) (se dibuja como √).
     */
    static Stream<String> entradasDelBanco() {
        Set<String> unicas = new LinkedHashSet<>();
        for (BancoCsv.Fila f : BancoCsv.filas()) {
            String e = f.entrada().strip();
            if (e.isEmpty() || e.contains(".") || e.contains("sqrt")) {
                continue;
            }
            try {
                Parser.parse(e);
                unicas.add(e);
            } catch (ErrorSintaxis ex) {
                // fila con entrada inválida a propósito
            }
        }
        List<String> todas = new ArrayList<>(unicas);
        assertTrue(todas.size() >= 100, "El banco tiene solo " + todas.size() + " entradas válidas");
        // 100 repartidas por todo el banco, no solo las primeras.
        return IntStream.range(0, 100).mapToObj(i -> todas.get(i * todas.size() / 100));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entradasDelBanco")
    void idaYVueltaConElBanco(String texto) {
        idaYVuelta(texto);
    }

    /** Las operaciones también vuelven igual. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "lim(x→2, (x^2 - 4)/(x - 2))", "lim(x→2+, 1/(x - 2))", "lim(x→-∞, x + 1)", "lim(x→3π/2, sen(x))",
            "d/dx(x^2)", "d2/dx2(x^3)", "∂/∂y(x^2 y)", "∂2/∂x∂y(x y)", "∂3/∂x∂x∂y(x^3 y)",
            "∫(x^2, x, 0, 1)", "∫(x + 1, x)", "∫(e^(2x), x, -1, 1)",
            "∬(x^2 + y^2, x, 0, 1, y, 0, 2)", "∭(x y z, x, 0, 1, y, 0, 1, z, 0, 1)",
            "2α + β", "sen θ", "d/dθ(sen θ)", "sen^2(x) + cos^2(x)", "log_2(x) - ln(x)", "√[4](x)",
            "(x + 1)/(x - 2)", "2x^2(x - 1)/3", "-(x + 1)^3", "x^-2 + x^(n + 1)", "(2/3)x^2 - x/5"
    })
    void idaYVueltaConOperaciones(String texto) {
        idaYVuelta(texto);
    }

    private static void idaYVuelta(String texto) {
        Expresion original = Parser.parse(texto);
        String latex = ImpresoraLatex.imprimir(original);
        String traducido = TraductorLatex.traducir(latex);
        assertEquals(original, Parser.parse(traducido), () -> texto + " → " + latex + " → " + traducido);
    }
}
