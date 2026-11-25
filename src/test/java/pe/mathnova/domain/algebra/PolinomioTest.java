package pe.mathnova.domain.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.domain.parser.Parser;

class PolinomioTest {

    private static Polinomio p(String texto) {
        return Polinomio.desde(Parser.parse(texto));
    }

    @Test
    void cuboDeUnBinomio() {
        Polinomio x = Polinomio.variable('x');
        Polinomio esperado = Polinomio.de(Map.of(
                Monomio.de('x', 3), Racional.de(1),
                Monomio.de('x', 2), Racional.de(3),
                Monomio.de('x', 1), Racional.de(3),
                Monomio.UNO, Racional.de(1)));
        assertEquals(esperado, x.sumar(Polinomio.UNO).potencia(3));
        assertEquals(esperado, p("(x + 1)^3"));
        assertEquals("x^3 + 3x^2 + 3x + 1", p("(x + 1)^3").toString());
    }

    @Test
    void productoConmutativo() {
        Polinomio a = p("2x^2 - 3xy + 1/2");
        Polinomio b = p("x - y^3 + z");
        assertEquals(a.multiplicar(b), b.multiplicar(a));
        assertEquals(p("(x - 2)(x + 3)"), p("(x + 3)(x - 2)"));
    }

    @Test
    void noGuardaTerminosConCoeficienteCero() {
        Polinomio cero = p("x^2 + 2x - x^2 - 2x");
        assertTrue(cero.esCero());
        assertEquals(Polinomio.CERO, cero);
        assertEquals(0, cero.terminos().size());
        assertEquals("0", cero.toString());
        assertEquals(p("x"), p("x + y - y"));
    }

    @Test
    void grado() {
        assertEquals(3, p("x^3 - x + 7").grado());
        assertEquals(5, p("x^2y^3 + x^4").grado());
        assertEquals(4, p("x^2y^3 + x^4").gradoEn('x'));
        assertEquals(3, p("x^2y^3 + x^4").gradoEn('y'));
        assertEquals(0, p("x^2y^3 + x^4").gradoEn('z'));
        assertEquals(0, p("7").grado());
        assertEquals(-1, Polinomio.CERO.grado());
    }

    @Test
    void evaluacion() {
        Polinomio q = p("x^3 - 2x + 1");
        assertEquals(Racional.de(5), q.evaluar(Map.of('x', Racional.de(2))));
        assertEquals(Racional.de(1, 8), q.evaluar(Map.of('x', Racional.de(1, 2))));
        assertEquals(Racional.de(-8), p("x^2 - y^2").evaluar(Map.of('x', Racional.de(1), 'y', Racional.de(-3))));
        assertThrows(IllegalArgumentException.class, () -> q.evaluar(Map.of('y', Racional.UNO)));
    }

    @Test
    void operacionesBasicas() {
        assertEquals(p("2x + 1"), p("x").sumar(p("x + 1")));
        assertEquals(p("-1"), p("x").restar(p("x + 1")));
        assertEquals(p("-x^2 + 3"), p("x^2 - 3").negar());
        assertEquals(Polinomio.UNO, p("x - 1").potencia(0));
        assertEquals(p("x/2 + 1/3"), p("(3x + 2)/6"));
        assertEquals(p("512"), p("2^3^2"));
        assertEquals(p("x^4"), p("x^(2 + 2)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sen(x)", "√2x", "e^x", "π", "x + i", "x^-1", "x^(1/2)", "1/x", "x^x", "ln(x) + 1",
            "x^101", "((x + 1)^20)^20"})
    void rechazaLoQueNoEsPolinomio(String texto) {
        assertThrows(NoEsPolinomioException.class, () -> p(texto));
    }

    @Test
    void divisionEntreCero() {
        assertThrows(ArithmeticException.class, () -> p("x/(2 - 2)"));
    }

    @Test
    void diferenciaDeCuadradosEnAyB() {
        assertEquals(p("a^2 - b^2"), p("(a - b)(a + b)"));
        assertEquals("a^2 - b^2", p("(a - b)(a + b)").toString());
        assertEquals(java.util.List.of('a', 'b'), java.util.List.copyOf(p("a^2 - b^2").variables()));
        assertEquals(2, p("a^2 - b^2").gradoEn('a'));
    }

    @Test
    void letrasGriegasYOrdenAlfabetico() {
        assertEquals("α^2 + 2αβ + β^2", p("(α + β)^2").toString());
        assertEquals("a^2 + 2ab + 2ax + b^2 + 2bx + x^2", p("(a + b + x)^2").toString());
        assertEquals("m^2x^2 + 2bmx + b^2", p("(m x + b)^2").toString());
        assertEquals(Racional.de(7), p("m x + b").evaluar(Map.of('m', Racional.de(2), 'x', Racional.de(3),
                'b', Racional.de(1))));
    }

    @Test
    void hashCodeCoherenteConEquals() {
        assertEquals(p("(x + y)^2").hashCode(), p("x^2 + 2xy + y^2").hashCode());
    }
}
