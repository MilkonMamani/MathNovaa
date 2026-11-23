package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AnalisisExpresionServiceTest {

    private final AnalisisExpresionService servicio = new AnalisisExpresionService();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(delimiter = '|', value = {
            "x^3 - 2x + 1       | Variables: x · Tipo: polinomio · Grado 3",
            "x^2y + 3y          | Variables: x, y · Tipo: polinomio · Grado 3",
            "(x + 1)(x - 1)     | Variables: x · Tipo: polinomio · Grado 2",
            "7                  | Variables: ninguna · Tipo: polinomio · Grado 0",
            "x - x              | Variables: x · Tipo: polinomio",
            "sen x cos x        | Variables: x · Tipo: trascendente",
            "e^x - 1            | Variables: x · Tipo: trascendente",
            "√x + 1             | Variables: x · Tipo: algebraica",
            "1/x                | Variables: x · Tipo: algebraica",
            "a^2 - b^2          | Variables: a, b · Tipo: polinomio · Grado 2",
            "2α + β             | Variables: α, β · Tipo: polinomio · Grado 1",
            "lim(x→2, a x)      | Operación: límite cuando x → 2 · Variable: x · Constantes: a",
            "lim(x->2+, 1/(x - 2)) | Operación: límite cuando x → 2⁺ · Variable: x",
            "lim(x→-π/2, tan x) | Operación: límite cuando x → -π/2 · Variable: x",
            "lim(x→∞, 1/x)      | Operación: límite cuando x → ∞ · Variable: x",
            "d/dt(a t^2)        | Operación: derivada respecto de t · Variable: t · Constantes: a",
            "d2/dx2(x^3)        | Operación: derivada de orden 2 respecto de x · Variable: x",
            "∂2/∂x∂y(x^2 y^3)   | Operación: derivada parcial de orden 2 respecto de x, y · Variables: x, y",
            "∫(m x + b, x)      | Operación: integral indefinida respecto de x · Variable: x · Constantes: b, m",
            "∫(x^2, x, 0, 1)    | Operación: integral definida de 0 a 1 respecto de x · Variable: x",
            "∬(x^2 + y^2, x, 0, 1, y, 0, 2) | Operación: integral doble respecto de x, y · Variables: x, y"
    })
    void resumen(String texto, String esperado) {
        var valida = assertInstanceOf(AnalisisExpresion.Valida.class, servicio.analizar(texto));
        assertEquals(esperado, valida.resumen());
    }

    @Test
    void entregaElLatexDeLaImpresora() {
        var valida = assertInstanceOf(AnalisisExpresion.Valida.class, servicio.analizar("x^2/2"));
        assertEquals(pe.mathnova.domain.expresion.ImpresoraLatex.imprimir(
                pe.mathnova.domain.parser.Parser.parse("x^2/2")), valida.latex());
    }

    @Test
    void errorDeSintaxisConMensajeYPosicion() {
        var invalida = assertInstanceOf(AnalisisExpresion.Invalida.class, servicio.analizar("3ñ + 1"));
        assertEquals("'ñ' en la posición 2 no es una letra permitida", invalida.mensaje());
        assertEquals(2, invalida.posicion());
    }

    @Test
    void divisionEntreCeroNoRevientaLaPantalla() {
        var invalida = assertInstanceOf(AnalisisExpresion.Invalida.class, servicio.analizar("x/(2 - 2)"));
        assertEquals(0, invalida.posicion());
    }

    @Test
    void casillaVacia() {
        var invalida = assertInstanceOf(AnalisisExpresion.Invalida.class, servicio.analizar("lim(x→□, □)"));
        assertEquals("Completa la casilla vacía en la posición 7", invalida.mensaje());
        assertEquals(7, invalida.posicion());
    }

    @Test
    void campoVacio() {
        assertInstanceOf(AnalisisExpresion.Vacia.class, servicio.analizar("   "));
        assertInstanceOf(AnalisisExpresion.Vacia.class, servicio.analizar(null));
    }
}
