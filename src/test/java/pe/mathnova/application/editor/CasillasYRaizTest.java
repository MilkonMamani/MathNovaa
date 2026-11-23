package pe.mathnova.application.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.parser.Parser;

/** \surd y √ pegados, las casillas opcionales de la integral (RF69) y qué casilla falta. */
class CasillasYRaizTest {

    // ------------------------------------------------------------------ \surd

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "\\surd{x}                    | √x",
            "\\surd{x+1}                  | √(x + 1)",
            "\\surd(1-x^2)                | √(1 - x^2)",
            "\\surd\\left(1-x^2\\right)   | √(1 - x^2)",
            "√(x+1)                       | √(x + 1)",
            "\\int\\surd(1-x^2)\\,dx      | ∫(√(1 - x^2), x)"
    })
    void surdConGrupoEsRaiz(String latex, String texto) {
        assertEquals(texto, TraductorLatex.traducir(latex));
        Parser.parse(TraductorLatex.traducir(latex));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"\\surd", "\\surd2", "\\surd x", "x+\\surd", "√"})
    void surdSueltoEsErrorClaro(String latex) {
        TraductorLatex.LatexNoValido e = assertThrows(TraductorLatex.LatexNoValido.class, () -> TraductorLatex.traducir(latex));
        assertEquals(TraductorLatex.RAIZ_SUELTA, e.getMessage());
        assertFalse(e.getMessage().contains("no reconoce"));
    }

    // ------------------------------------------------------------------ casillas

    @ParameterizedTest(name = "{0} → {2}")
    @CsvSource(delimiter = '|', value = {
            "x^{\\placeholder{}}                                              | 0 | Completa el exponente",
            "\\int_{\\placeholder{}}^{\\placeholder{}}\\placeholder{}\\,dx    | 2 | Completa el integrando",
            "\\lim_{x\\to\\placeholder{}}x^2                                  | 0 | Completa el punto del límite",
            "\\lim_{x\\to2}\\placeholder{}                                    | 0 | Completa la función del límite",
            "\\int_{1}^{\\placeholder{}}x\\,dx                                | 0 | Completa el otro límite o deja los dos vacíos",
            "\\int_{\\placeholder{}}^{2}x\\,dx                                | 0 | Completa el otro límite o deja los dos vacíos",
            "\\frac{x}{\\placeholder{}}                                       | 0 | Completa el denominador",
            "\\frac{\\placeholder{}}{\\placeholder{}}                         | 0 | Completa el numerador",
            "\\sqrt{\\placeholder{}}                                          | 0 | Completa el radicando",
            "\\sqrt[\\placeholder{}]{x}                                       | 0 | Completa el índice de la raíz",
            "\\frac{d}{dx}\\left(\\placeholder{}\\right)                      | 0 | Completa la función que se deriva",
            "\\operatorname{\\mathrm{sen}}\\left(\\placeholder{}\\right)      | 0 | Completa el argumento de la función",
            "\\log_{\\placeholder{}}\\left(x\\right)                          | 0 | Completa la base del logaritmo",
            "x+\\placeholder{}                                                | 0 | Completa la casilla resaltada"
    })
    void dicenQueCasillaFalta(String latex, int indice, String mensaje) {
        Optional<TraductorLatex.CasillaVacia> c = TraductorLatex.casillaVacia(latex);
        assertEquals(Optional.of(new TraductorLatex.CasillaVacia(indice, mensaje)), c);
        assertFalse(TraductorLatex.completo(latex));
        assertFalse(mensaje.contains("posición"));
    }

    /** El índice cuenta todas las casillas del LaTeX, también las opcionales que se saltan. */
    @Test
    void elIndiceCuentaLasCasillasOpcionales() {
        String latex = "\\int_{\\placeholder{}}^{\\placeholder{}}x^{\\placeholder{}}\\,dx";
        assertEquals(Optional.of(new TraductorLatex.CasillaVacia(2, "Completa el exponente")),
                TraductorLatex.casillaVacia(latex));
        // ∫₁^□: el 1 ya no es casilla; la única que queda, el límite superior, es la número 0
        assertEquals(0,TraductorLatex.casillaVacia("\\int_{1}^{\\placeholder{}}x\\,dx").orElseThrow().indice());
    }

    /** RF69: los dos límites vacíos se borran antes de traducir; la integral es indefinida y sin aviso. */
    @Test
    void integralConLimitesVaciosEsIndefinida() {
        String latex = "\\int_{\\placeholder{}}^{\\placeholder{}}e^{x}\\operatorname{\\mathrm{sen}}\\left(x\\right)\\,dx";
        assertTrue(TraductorLatex.casillaVacia(latex).isEmpty());
        assertTrue(TraductorLatex.completo(latex));
        String texto = TraductorLatex.traducir(latex);
        assertFalse(texto.contains(TraductorLatex.CASILLA), texto);
        assertEquals(Parser.parse("∫(e^x sen(x), x)"), Parser.parse(texto));
    }

    @Test
    void integralConLimitesLlenosEsDefinida() {
        assertEquals("∫(x^3 - x, x, -1, 1)", TraductorLatex.traducir("\\int_{-1}^1x^3-x\\,dx"));
        assertTrue(TraductorLatex.casillaVacia("\\int_{-1}^1x^3-x\\,dx").isEmpty());
    }

    @Test
    void sinCasillasNoFaltaNada() {
        assertTrue(TraductorLatex.casillaVacia("x^2-5x+6").isEmpty());
        assertTrue(TraductorLatex.casillaVacia("").isEmpty());
        assertTrue(TraductorLatex.casillaVacia(null).isEmpty());
    }
}
