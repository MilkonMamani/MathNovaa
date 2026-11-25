package pe.mathnova.presentation.latex;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.parser.Parser;

class RenderizadorLatexTest {

    private static final RenderizadorLatex RENDERIZADOR = new RenderizadorLatex(24f);

    @BeforeAll
    static void calentar() {
        System.setProperty("java.awt.headless", "true");
        RENDERIZADOR.dibujar("x");
    }

    static java.util.List<String> entradas() {
        return BancoCsv.filas().stream().map(BancoCsv.Fila::entrada).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entradas")
    void dibujaLasEntradasDelBanco(String texto) {
        String latex = ImpresoraLatex.imprimir(Parser.parse(texto));
        BufferedImage imagen = assertTimeout(Duration.ofMillis(100), () -> RENDERIZADOR.dibujar(latex));
        assertTrue(imagen.getWidth() > 10 && imagen.getHeight() > 10, latex);
        assertTrue(tieneTinta(imagen), latex);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "lim(x→2, (x^2 - 4)/(x - 2))", "lim(x->2+, 1/(x - 2))", "lim(x→∞, (2x^2 + 1)/(x^2 - 3))",
            "lim(x→-π/2, tan x)", "d/dx(x^2 sen x)", "d2/dx2(x^3)", "d/dt(a t^2)", "∂/∂y(x^2 y + 3x y^2)",
            "∂2/∂x∂y(x^2 y^3)", "∫(x e^x, x)", "∫(x^2, x, 0, 1)", "int(1/x, x, 1, e)",
            "∬(x^2 + y^2, x, 0, 1, y, 0, 2)", "∭(x y z, x, 0, 1, y, 0, 1, z, 0, 1)", "a^2 - b^2",
            "sen^2 θ + cos^2 θ", "2α + β", "√[4](x)", "m x + b",
            "α + β + γ + δ + ε + ζ + η + θ + ι + κ + λ + μ + ν + ξ + ο + ρ + ς + σ + τ + υ + φ + χ + ψ + ω"})
    void dibujaLasOperaciones(String texto) {
        String latex = ImpresoraLatex.imprimir(Parser.parse(texto));
        BufferedImage imagen = RENDERIZADOR.dibujar(latex);
        assertTrue(imagen.getWidth() > 10 && imagen.getHeight() > 10, latex);
        assertTrue(tieneTinta(imagen), latex);
    }

    private static boolean tieneTinta(BufferedImage imagen) {
        for (int x = 0; x < imagen.getWidth(); x++) {
            for (int y = 0; y < imagen.getHeight(); y++) {
                if ((imagen.getRGB(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
