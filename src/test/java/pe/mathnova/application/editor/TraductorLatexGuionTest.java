package pe.mathnova.application.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.parser.Parser;

/**
 * Las formas de LaTeX que MathLive puede dejar para cada construcción del guion de demostración (raíz,
 * integral, e, operadores con nombre, integrales múltiples), aunque el teclado arme solo una de ellas.
 * Cada texto debe además leerse con el parser.
 */
class TraductorLatexGuionTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            # a) raíz con cualquier contenido
            \\sqrt{1-x^2}                                         | √(1 - x^2)
            \\sqrt{\\left(1-x^2\\right)}                          | √(1 - x^2)
            \\sqrt{(1-x^2)}                                       | √(1 - x^2)
            \\sqrt{\\left(1-x^{2}\\right)}+1                      | √(1 - x^2) + 1
            \\sqrt[3]{x+1}                                        | ∛(x + 1)
            \\sqrt[2]{x}                                          | √x
            \\surd{1-x^2}                                         | √(1 - x^2)
            \\surd\\left(1-x^2\\right)                            | √(1 - x^2)
            \\surd(1-x^2)                                         | √(1 - x^2)
            # b) integral indefinida: \\, dx, \\mathrm{d}x, dx, límites vacíos o \\placeholder{}
            \\int x^2\\,dx                                        | ∫(x^2, x)
            \\int x^2\\mathrm{d}x                                 | ∫(x^2, x)
            \\int x^2\\,\\mathrm{d}x                              | ∫(x^2, x)
            \\int x^2dx                                           | ∫(x^2, x)
            \\int x^2\\differentialD x                            | ∫(x^2, x)
            \\int_{}^{}x^2\\,dx                                   | ∫(x^2, x)
            \\int_{\\placeholder{}}^{\\placeholder{}}x^2\\,dx     | ∫(x^2, x)
            \\int_{0}^{1}x^2\\,dx                                 | ∫(x^2, x, 0, 1)
            \\int_0^1x^2\\mathrm{d}x                              | ∫(x^2, x, 0, 1)
            # c) e: \\exponentialE, \\mathrm{e} y e suelta son la constante
            \\exponentialE^{2x}                                   | e^(2x)
            \\mathrm{e}^{2x}                                      | e^(2x)
            e^{2x}                                                | e^(2x)
            \\exponentialE^{2x}-3\\exponentialE^{x}+2             | e^(2x) - 3e^x + 2
            # d) operadores con nombre
            \\operatorname{extremos}\\left(x^3+y^3-3xy\\right)    | extremos(x^3 + y^3 - 3xy)
            \\operatorname{\\mathrm{extremos}}\\left(xy;x+y=10\\right) | extremos(xy; x + y = 10)
            \\operatorname{extremos}(x^2+y^2)                     | extremos(x^2 + y^2)
            \\operatorname{grad}\\left(x^2y\\right)               | grad(x^2y)
            \\operatorname{\\mathrm{grad}}(x^2y)                  | grad(x^2y)
            \\operatorname{Du}\\left(x^2y,\\left(1,0\\right),\\left(1,1\\right)\\right) | Du(x^2y, (1, 0), (1, 1))
            # e) integrales múltiples: la región como la imprime MathNova y la forma iterada de la tecla ∬
            \\iint_{[0,1]\\times[0,2]}xy\\,dx\\,dy                | ∬(xy, x, 0, 1, y, 0, 2)
            \\iint_{\\left[y,1\\right]\\times\\left[0,1\\right]}e^{x^2}\\,dx\\,dy | ∬(e^(x^2), x, y, 1, y, 0, 1)
            \\int_0^1\\int_y^1e^{x^2}\\,dx\\,dy                   | ∬(e^(x^2), x, y, 1, y, 0, 1)
            \\int_{0}^{1}\\int_{y}^{1}e^{x^{2}}\\,\\mathrm{d}x\\,\\mathrm{d}y | ∬(e^(x^2), x, y, 1, y, 0, 1)
            \\int_0^1\\int_0^2\\int_0^3xyz\\,dx\\,dy\\,dz         | ∭(xyz, x, 0, 3, y, 0, 2, z, 0, 1)
            \\iiint_{[0,3]\\times[0,2]\\times[0,1]}xyz\\,dx\\,dy\\,dz | ∭(xyz, x, 0, 3, y, 0, 2, z, 0, 1)
            """)
    void seTraduceYSeLee(String latex, String texto) {
        String obtenido = TraductorLatex.traducir(latex.strip());
        assertEquals(texto.strip(), obtenido, latex);
        Parser.parse(obtenido);
    }

    /** RN48: con un solo límite lleno se pide el otro; con los dos vacíos la integral es indefinida. */
    @Test
    void unSoloLimiteLlenoPideElOtro() {
        Optional<TraductorLatex.CasillaVacia> falta = TraductorLatex.casillaVacia("\\int_{1}^{\\placeholder{}}x\\,dx");
        assertTrue(falta.isPresent());
        assertEquals(TraductorLatex.OTRO_LIMITE, falta.get().mensaje());
        falta = TraductorLatex.casillaVacia("\\int_{\\placeholder{}}^{2}x\\,dx");
        assertEquals(TraductorLatex.OTRO_LIMITE, falta.orElseThrow().mensaje());
        assertTrue(TraductorLatex.completo("\\int_{\\placeholder{}}^{\\placeholder{}}x\\,dx"));
    }

    /** En la ∬ iterada los cuatro límites son obligatorios, y el integrando también. */
    @Test
    void enLaIntegralDobleLosLimitesSonObligatorios() {
        String plantilla = "\\int_{\\placeholder{}}^{\\placeholder{}}\\int_{\\placeholder{}}^{\\placeholder{}}"
                + "\\placeholder{}\\,dx\\,dy";
        TraductorLatex.CasillaVacia falta = TraductorLatex.casillaVacia(plantilla).orElseThrow();
        assertEquals(0, falta.indice());
        String sinIntegrando = "\\int_0^1\\int_y^1\\placeholder{}\\,dx\\,dy";
        assertEquals("Completa el integrando", TraductorLatex.casillaVacia(sinIntegrando).orElseThrow().mensaje());
        String sinUnLimite = "\\int_0^1\\int_{\\placeholder{}}^1e^{x^2}\\,dx\\,dy";
        assertEquals("Completa el límite inferior de x",
                TraductorLatex.casillaVacia(sinUnLimite).orElseThrow().mensaje());
    }
}
