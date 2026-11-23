package pe.mathnova.application.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.sugerencias.Sugerencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 6, Parte D: un polinomio sugiere la derivada y los puntos críticos; una fracción sugiere su
 * integral. RN20: solo queda activa si la plantilla se resuelve de verdad.
 */
class SugerenciasCalculoTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR, null);
    private static final ResolverService SERVICIO = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static List<Sugerencia> dePolinomio(String p) {
        return FACTORIZACION.sugerencias(FACTORIZACION.factorizar(p, null, Campo.Q, f -> { }));
    }

    private static List<Sugerencia> deFraccion(String q) {
        return FACTORIZACION.sugerencias(FACTORIZACION.simplificar(q, f -> { }));
    }

    @ParameterizedTest
    @ValueSource(strings = {"x^3 - 3x", "2x + 1", "x^2 + 1", "x^4 - 5x^2 + 4", "x^3 - 3x^2 - 9x + 5"})
    void polinomioSugiereDerivadaYPuntosCriticos(String p) {
        Sugerencia s = dePolinomio(p).stream().filter(x -> x.operacion().equals("PUNTOS_CRITICOS")).findFirst()
                .orElseThrow();
        assertEquals("Derivada y puntos críticos", s.titulo());
        assertTrue(s.disponible());
        var r = SERVICIO.resolverDerivada(s.plantilla(), null, f -> { });
        assertTrue(r.verificada());
        assertNotNull(r.analisis(), "la derivada trae los puntos críticos");
    }

    @ParameterizedTest(name = "{0} → {2}")
    @CsvSource(delimiter = ';', value = {
            "(3x + 5)/((x - 1)(x + 2)) ; RN16 ; = (8/3)ln|x − 1| + (1/3)ln|x + 2| + C",
            "3/(x - 1)                 ; RN16 ; = 3ln|x − 1| + C",
            "(x^3 + 1)/(x - 1)         ; RN17 ; = x³/3 + x²/2 + x + 2ln|x − 1| + C",
            "(x^2 - 4)/(x - 2)         ; RN17 ; = x²/2 + 2x + C",
            "1/(x^2 + 2x + 5)          ; RN16 ; = (1/2)arctan((x + 1)/2) + C",
            "x/(x^2 + 1)               ; RN16 ; = (1/2)ln(x² + 1) + C",
    })
    void fraccionSugiereSuIntegral(String q, String regla, String enVivo) {
        List<Sugerencia> integrales = deFraccion(q).stream().filter(x -> x.operacion().equals("INTEGRAR")).toList();
        assertEquals(1, integrales.size(), integrales.toString());
        Sugerencia s = integrales.getFirst();
        assertEquals(regla, s.regla());
        assertTrue(s.disponible());
        assertEquals(Optional.of(enVivo), SERVICIO.enVivo(s.plantilla()));
        assertTrue(SERVICIO.resolverIntegral(s.plantilla(), null, f -> { }).verificada());
    }

    /** RN20: si ningún método la resuelve, la tarjeta sale pero desactivada. */
    @ParameterizedTest
    @ValueSource(strings = {"1/(x^3 + 2)", "1/(x^2 - 2)", "1/(x^2 + 1)^2"})
    void unaIntegralQueNoSeResuelveQuedaDesactivada(String q) {
        Sugerencia s = deFraccion(q).stream().filter(x -> x.operacion().equals("INTEGRAR")).findFirst().orElseThrow();
        assertFalse(s.disponible());
        assertFalse(SERVICIO.resolverIntegral(s.plantilla(), null, f -> { }).resuelta());
    }
}
