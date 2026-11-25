package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.aplica;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.noAplica;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.p;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.parser.Parser;

class AgrupacionTest {

    @Test
    void f04DosMasDos() {
        var r = aplica(new AgrupacionDeTerminos(), "x^3 - 2x^2 - x + 2", "x - 2", "x^2 - 1");
        assertEquals("Los grupos (x³ − 2x²) y (−x + 2) comparten (x − 2)", r.paso().justificacion());
        var grupos = assertInstanceOf(TrabajoIntermedio.Agrupacion.class, r.paso().trabajo()).grupos();
        assertEquals(List.of("x³ − 2x² = x²(x − 2)", "−x + 2 = −(x − 2)"), grupos);
        aplica(new AgrupacionDeTerminos(), "2x^3 - x^2 - 16x + 8", "2x - 1", "x^2 - 8");
    }

    @Test
    void f04ConVariasLetras() {
        aplica(new AgrupacionDeTerminos(), "x^3 + x^2 + 2x + 2", "x + 1", "x^2 + 2");
        aplica(new AgrupacionDeTerminos(), "a x + b y + a y + b x", "x + y", "a + b");
    }

    @Test
    void f04TresMasUnoYUnoMasTres() {
        var r = aplica(new AgrupacionDeTerminos(), "x^2 + 2x y + y^2 - z^2", "x + y + z", "x + y - z");
        assertEquals("(x² + 2xy + y²) = (x + y)² y z² = (z)²: queda una diferencia de cuadrados",
                r.paso().justificacion());
        aplica(new AgrupacionDeTerminos(), "a^2 - x^2 + 2x y - y^2", "a + x - y", "a - x + y");
    }

    @Test
    void f04SeisTerminos() {
        aplica(new AgrupacionDeTerminos(), "a x + a y + a z + b x + b y + b z", "x + y + z", "a + b");
    }

    @Test
    void f04NoAplica() {
        noAplica(new AgrupacionDeTerminos(), "x^3 + x^2 + x + 2", "Ninguna agrupación (2+2, 3+1, 1+3) deja un factor común");
        noAplica(new AgrupacionDeTerminos(), "x^2 + x + 1", "Tiene 3 términos; la agrupación necesita 4 o más");
    }

    @Test
    void f03FactorComunPolinomio() {
        var r = new FactorComunPolinomio().detectar(Parser.parse("x^2(x - 3) + 5(x - 3)")).orElseThrow();
        assertEquals("F03", r.paso().codigoTecnica());
        assertEquals("Los términos x²(x − 3) y 5(x − 3) comparten (x − 3)", r.paso().justificacion());
        assertEquals(List.of(p("x - 3"), p("x^2 + 5")), r.factores().stream().map(Factor::polinomio).toList());
        assertEquals(p("x^3 - 3x^2 + 5x - 15"), r.paso().antes());

        var resta = new FactorComunPolinomio().detectar(Parser.parse("4x(x + 3) - 5(x + 3)")).orElseThrow();
        assertEquals(List.of(p("x + 3"), p("4x - 5")), resta.factores().stream().map(Factor::polinomio).toList());

        var potencia = new FactorComunPolinomio().detectar(Parser.parse("(x + 1)^2 + 3(x + 1)")).orElseThrow();
        assertEquals(List.of(p("x + 1"), p("x + 4")), potencia.factores().stream().map(Factor::polinomio).toList());
    }

    @Test
    void f03SoloSiVieneAgrupada() {
        assertTrue(new FactorComunPolinomio().detectar(Parser.parse("x^3 - 3x^2 + 5x - 15")).isEmpty());
        assertTrue(new FactorComunPolinomio().detectar(Parser.parse("x(x - 3) + 5(x + 3)")).isEmpty());
        assertTrue(new FactorComunPolinomio().detectar(Parser.parse("(x - 3)(x + 3)")).isEmpty());
    }
}
