package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.parser.Parser;

class PasoACampoTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    private static Extension.Paso pasoACampo(String texto, Campo campo) {
        Resolucion r = MOTOR.resolver(Parser.parse(texto), campo);
        Extension.Paso p = r.extension().pasos().getFirst();
        assertEquals(campo == Campo.R ? "PR" : "PC", p.codigoTecnica());
        return p;
    }

    @ParameterizedTest(name = "{0} en {1}")
    @CsvSource(delimiter = '|', value = {
            "x^2 - 3  | R | x^2 - 3 no se factoriza en Q porque Δ = 12 no es un cuadrado perfecto. En R sí, porque 3 = (√3)^2.",
            "x^2 - 3  | C | x^2 - 3 no se factoriza en Q porque Δ = 12 no es un cuadrado perfecto. En C sí, porque 3 = (√3)^2.",
            "x^2 + 4  | C | x^2 + 4 no tiene raíces reales (Δ = -16 < 0). En C sí: ±2i.",
            "x^2 - 2x - 1 | R | x^2 - 2x - 1 no se factoriza en Q porque Δ = 8 no es un cuadrado perfecto. En R sí, porque Δ > 0: √8 = 2√2."
    })
    void explicaElMotivoConElDato(String texto, Campo campo, String motivo) {
        assertEquals(motivo, pasoACampo(texto, campo).justificacion());
    }

    @Test
    void despuesEsElFactorQueSeParte() {
        Extension.Paso p = pasoACampo("x^3 - x^2 - 3x + 3", Campo.R);
        assertEquals("x^2 - 3", ImpresoraTexto.imprimir(p.despues()));
        assertTrue(ImpresoraTexto.imprimir(p.antes()).contains("x - 1"), ImpresoraTexto.imprimir(p.antes()));
        assertTrue(p.justificacion().startsWith("x^2 - 3 no se factoriza en Q"), p.justificacion());
    }

    @Test
    void sinNadaQueAgregarEnQElDespuesNoRepiteLaExpresion() {
        Extension.Paso p = pasoACampo("x^2 + 4", Campo.C);
        assertEquals("x^2 + 4", ImpresoraTexto.imprimir(p.despues()));
    }

    @Test
    void variosFactoresSeExplicanUnoPorUno() {
        Extension.Paso p = pasoACampo("(x^2 - 3)(x^2 + 4)", Campo.C);
        assertTrue(p.justificacion().contains("Δ = 12"), p.justificacion());
        assertTrue(p.justificacion().contains("±2i"), p.justificacion());
    }
}
