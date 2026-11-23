package pe.mathnova.domain.calculo.integrales;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sprint 6, Parte B: los ejemplos exactos de integrales (HU13, RF29–RF32). */
class IntegralesTest {

    private static final MotorIntegrales MOTOR = new MotorIntegrales(new OrquestadorFactorizacion());

    private static ResolucionIntegral resolver(String texto) {
        return MOTOR.resolver(Parser.parse(texto));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "∫(3x^2 - 4x + 5, x)                  ; x³ − 2x² + 5x + C                              ; I01",
            "∫(2x(x^2 + 1)^5, x)                  ; (x² + 1)⁶/6 + C                                ; I02",
            "∫(x e^(x^2), x)                      ; e^(x²)/2 + C                                   ; I02",
            "∫((3x + 5)/((x - 1)(x + 2)), x)      ; (8/3)ln|x − 1| + (1/3)ln|x + 2| + C            ; I06",
            "∫(1/(x^2 - 1), x)                    ; (1/2)ln|x − 1| − (1/2)ln|x + 1| + C            ; I06",
            "∫((x + 3)/(x - 1)^2, x)              ; ln|x − 1| − 4/(x − 1) + C                      ; I06",
            "∫((x + 1)/(x^2 + 1), x)              ; (1/2)ln(x² + 1) + arctan x + C                 ; I06",
            "∫((2x^2 + 3)/(x(x^2 + 1)), x)        ; 3ln|x| − (1/2)ln(x² + 1) + C                   ; I06",
            "∫((x^3 + 1)/(x - 1), x)              ; x³/3 + x²/2 + x + 2ln|x − 1| + C               ; P02",
            "∫(x^2, x, 0, 1)                      ; 1/3                                            ; I01",
            "∫(1/x, x, 1, e)                      ; 1                                              ; I01",
            "3∫(3x, x, 3, 6)                      ; 243/2                                          ; I01",
    })
    void ejemplosExactos(String entrada, String esperado, String metodo) {
        ResolucionIntegral r = resolver(entrada);
        assertEquals(esperado, r.texto());
        assertEquals(metodo, r.metodo());
        assertTrue(r.verificada(), () -> "no verifica: " + r.verificacion());
    }

    @Test
    void laDivisionVaPrimeroEnUnaImpropia() {
        ResolucionIntegral r = resolver("∫((x^3 + 1)/(x - 1), x)");
        assertEquals(MotorIntegrales.DIVISION, r.pasos().getFirst().codigo());
    }

    @Test
    void sustitucionMuestraUduYLaIntegralEnU() {
        ResolucionIntegral r = resolver("∫(2x(x^2 + 1)^5, x)");
        PasoLimite s = r.pasos().getFirst();
        List<String> t = ((TrabajoIntermedio.Agrupacion) s.trabajo()).grupos();
        assertEquals("u = x² + 1", t.get(0));
        assertEquals("du = 2x dx", t.get(1));
        assertEquals("∫2x(x² + 1)⁵ dx = ∫u⁵ du", t.get(2));
        assertEquals("∫u⁵ du = u⁶/6", t.get(3));
        assertEquals("I01", s.hijos().getFirst().codigo());
    }

    @Test
    void fraccionesParcialesConFactorizacionAnidadaYSistema() {
        ResolucionIntegral r = resolver("∫((3x + 5)/((x - 1)(x + 2)), x)");
        assertEquals(List.of("Factorizar el denominador", "Plantear las fracciones parciales", "Resolver el sistema",
                "Integrar cada fracción"), r.pasos().stream().map(PasoLimite::titulo).toList());
        assertFalse(r.pasos().getFirst().hijos().isEmpty(), "la factorización del denominador va anidada");
        TrabajoIntermedio.SistemaCoeficientes sistema = (TrabajoIntermedio.SistemaCoeficientes) r.pasos().get(2).trabajo();
        assertEquals(List.of("A = 8/3", "B = 1/3"), sistema.solucion());
        assertEquals("A + B = 3", sistema.ecuaciones().get(0).ecuacion());
        assertEquals("2A − B = 5", sistema.ecuaciones().get(1).ecuacion());
    }

    @Test
    void barrowSustituyePasoAPaso() {
        ResolucionIntegral r = resolver("∫(x^2, x, 0, 1)");
        PasoLimite barrow = r.pasos().getLast();
        assertEquals("I07", barrow.codigo());
        assertEquals(List.of("F(x) = x³/3", "F(1) = 1³/3 = 1/3", "F(0) = 0³/3 = 0", "F(1) − F(0) = 1/3 − 0 = 1/3"),
                ((TrabajoIntermedio.Agrupacion) barrow.trabajo()).grupos());
    }

    @Test
    void elDelVideoMultiplicaAlFinal() {
        ResolucionIntegral r = resolver("3∫(3x, x, 3, 6)");
        assertEquals(List.of("I01", "I07", "K"), r.pasos().stream().map(PasoLimite::codigo).toList());
        assertEquals(new BigDecimal("121.5"), r.aproximado().stripTrailingZeros());
    }

    @Test
    void sinMetodoNoInventaNada() {
        // ∫x·eˣ ya se resuelve por partes (Sprint 7); e^(x²) no tiene antiderivada elemental
        ResolucionIntegral r = resolver("∫(e^(x^2), x)");
        assertFalse(r.resuelta());
        assertEquals(MotorIntegrales.SIN_METODO, r.texto());
        assertNull(r.antiderivada());
        assertFalse(r.verificada());
    }

    @Test
    void simpsonSeAcercaAlValorExacto() {
        ResolucionIntegral r = resolver("∫(x^2, x, 0, 1)");
        BigDecimal s = MotorIntegrales.simpson(r, 100).orElseThrow();
        assertTrue(s.subtract(new BigDecimal("0.333333333333")).abs().compareTo(new BigDecimal("1e-10")) < 0);
    }

    @Test
    void unaImpropiaNoSeCalcula() {
        ResolucionIntegral r = resolver("∫(1/x, x, -1, 1)");
        assertFalse(r.resuelta());
        assertTrue(r.texto().contains("impropia"), r.texto());
    }

    @Test
    void exploracionSustitucionFrenteAExpandir() {
        ResolucionIntegral r = resolver("∫(2x(x^2 + 1)^5, x)");
        List<CaminoCalculo> c = MOTOR.caminos(r);
        assertEquals(CaminoCalculo.Estado.USADO, c.getFirst().estado());
        assertEquals("I02", c.getFirst().codigo());
        assertTrue(c.stream().anyMatch(x -> x.codigo().equals(MotorIntegrales.EXPANDIR)
                && x.estado() == CaminoCalculo.Estado.SIRVE), c.toString());
        ResolucionIntegral expandida = MOTOR.resolver(Parser.parse("∫(2x(x^2 + 1)^5, x)"), MotorIntegrales.EXPANDIR);
        assertEquals(MotorIntegrales.EXPANDIR, expandida.metodo());
        assertTrue(expandida.verificada());
    }
}
