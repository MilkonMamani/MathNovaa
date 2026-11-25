package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.numeros.PolinomioAlgebraico;
import pe.mathnova.domain.numeros.RaicesAproximadas;
import pe.mathnova.domain.parser.Parser;

class VerificacionCampoTest {

    private static Polinomio p(String t) {
        return Polinomio.desde(Parser.parse(t));
    }

    private static FormaExtendida forma(Racional c, String... factores) {
        return new FormaExtendida(c, java.util.Arrays.stream(factores)
                .map(f -> new FormaExtendida.Factor(PolinomioAlgebraico.desde(Parser.parse(f), 'x'), 1)).toList());
    }

    @Test
    void exactaSinAnidados() {
        VerificacionCampo v = VerificacionCampo.de(p("x^4 + 1"), forma(Racional.UNO, "x^2 - √2·x + 1", "x^2 + √2·x + 1"));
        assertTrue(v.coincide());
        assertFalse(v.numerica());
    }

    @Test
    void exactaDetectaUnError() {
        VerificacionCampo v = VerificacionCampo.de(p("x^2 - 3"), forma(Racional.UNO, "x - √2", "x + √2"));
        assertFalse(v.coincide());
        assertFalse(v.numerica());
    }

    @Test
    void numericaConAnidados() {
        FormaExtendida bien = forma(Racional.UNO, "x + 1/2 - √(1 + 4√2)/2", "x + 1/2 + √(1 + 4√2)/2", "x^2 + x + √2");
        FormaExtendida mal = forma(Racional.UNO, "x + 1/2 - √(1 + 4√3)/2", "x + 1/2 + √(1 + 4√3)/2", "x^2 + x + √2");
        VerificacionCampo v = VerificacionCampo.de(p("x^4 + 2x^3 + x^2 - 2"), bien);
        assertTrue(v.coincide());
        assertTrue(v.numerica());
        assertFalse(VerificacionCampo.de(p("x^4 + 2x^3 + x^2 - 2"), mal).coincide());
    }

    @Test
    void comparadorEnRyC() {
        var a = ComparadorFactorizacion.leerEnCampo("(x + 1/2 + √3·i/2)(x + 1/2 - √3·i/2)");
        var b = ComparadorFactorizacion.leerEnCampo("(x + 1/2 - (√3/2)i)(x + 1/2 + (√3/2)i)");
        var c = ComparadorFactorizacion.leerEnCampo("(x + 1/2 - √3·i)(x + 1/2 + √3·i)");
        assertTrue(ComparadorFactorizacion.igualesEnCampo(a, b));
        assertFalse(ComparadorFactorizacion.igualesEnCampo(a, c));
        var d = ComparadorFactorizacion.leerEnCampo("2(x - 1 + √6/2)(x - 1 - √6/2)");
        var e = ComparadorFactorizacion.leerEnCampo("(2x - 2 + √6)(x - 1 - √6/2)");
        assertTrue(ComparadorFactorizacion.igualesEnCampo(d, e));
    }

    @Test
    void cuarentaCifras() {
        MathContext mc = EvaluacionNumerica.MC;
        ComplejoDecimal raiz2 = EvaluacionNumerica.evaluar(Parser.parse("√2"), Map.of(), mc);
        assertEquals(0, new BigDecimal("1.414213562373095048801688724209698078570").compareTo(raiz2.re()), raiz2::toString);
        ComplejoDecimal pi = EvaluacionNumerica.evaluar(Parser.parse("π"), Map.of(), mc);
        assertEquals(0, new BigDecimal("3.141592653589793238462643383279502884197").compareTo(pi.re()), pi::toString);
        ComplejoDecimal cos = EvaluacionNumerica.evaluar(Parser.parse("cos(π/3)"), Map.of(), mc);
        assertTrue(cos.cercano(ComplejoDecimal.de(Racional.de(1, 2), mc), 35, mc));
        ComplejoDecimal arccos = EvaluacionNumerica.evaluar(Parser.parse("arccos(-1/2)"), Map.of(), mc);
        assertTrue(arccos.cercano(EvaluacionNumerica.evaluar(Parser.parse("2π/3"), Map.of(), mc), 35, mc));
        ComplejoDecimal ln = EvaluacionNumerica.evaluar(Parser.parse("ln(e^3)"), Map.of(), mc);
        assertTrue(ln.cercano(ComplejoDecimal.de(Racional.de(3), mc), 35, mc));
    }

    @Test
    void durandKerner() {
        assertEquals(List.of("1.1673039783"), RaicesAproximadas.reales(List.of(Racional.de(-1), Racional.de(-1),
                Racional.CERO, Racional.CERO, Racional.CERO, Racional.UNO)).stream().map(RaicesAproximadas::texto).toList());
        assertEquals(5, RaicesAproximadas.raices(List.of(Racional.de(2), Racional.de(-4),
                Racional.CERO, Racional.CERO, Racional.CERO, Racional.UNO)).size());
    }
}
