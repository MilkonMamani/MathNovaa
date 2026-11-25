package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;

/** "Resolver" decide qué hacer (Parte D) y el resultado en vivo (HU37, RF71). */
class ResolverServiceTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService SERVICIO = new ResolverService(
            new FactorizacionService(MOTOR), MOTOR, new ResolvedorEcuaciones(MOTOR));

    @Test
    void decideQueHacer() {
        assertInstanceOf(ResolverService.Plan.Vacio.class, SERVICIO.decidir(" "));
        assertInstanceOf(ResolverService.Plan.Invalido.class, SERVICIO.decidir("x +"));
        assertInstanceOf(ResolverService.Plan.Factorizar.class, SERVICIO.decidir("x^2 - 5x + 6"));
        assertInstanceOf(ResolverService.Plan.Simplificar.class, SERVICIO.decidir("(x^2 - 1)/(x + 1)"));
        assertInstanceOf(ResolverService.Plan.ResolverEcuacion.class, SERVICIO.decidir("x^2 = 4"));
        assertInstanceOf(ResolverService.Plan.Derivar.class, SERVICIO.decidir("d/dx(x^2)"));
        assertInstanceOf(ResolverService.Plan.Derivar.class, SERVICIO.decidir("d/dx(x^2 + y^2 = 25)"));
        assertInstanceOf(ResolverService.Plan.Integrar.class, SERVICIO.decidir("∫(x, x)"));
        assertInstanceOf(ResolverService.Plan.Integrar.class, SERVICIO.decidir("3∫(3x, x, 3, 6)"));
        // Sprint 7: las parciales se resuelven; Sprint 8: también las integrales múltiples
        assertInstanceOf(ResolverService.Plan.Parciales.class, SERVICIO.decidir("∂/∂y(x^2 y)"));
        assertInstanceOf(ResolverService.Plan.Parciales.class, SERVICIO.decidir("grad(x y)"));
        assertInstanceOf(ResolverService.Plan.Parciales.class, SERVICIO.decidir("Du(x y, (1, 1), (1, 2))"));
        assertInstanceOf(ResolverService.Plan.Parciales.class, SERVICIO.decidir("d/dt(x^2 y); x = t^2, y = sen t"));
        assertInstanceOf(ResolverService.Plan.IntegralMultiple.class, SERVICIO.decidir("∬(x y, x, 0, 1, y, 0, 1)"));
        assertInstanceOf(ResolverService.Plan.LimiteDoble.class, SERVICIO.decidir("lim((x,y)→(0,0), x y)"));
        assertInstanceOf(ResolverService.Plan.Extremos.class, SERVICIO.decidir("extremos(xy; x + y = 10)"));
        assertInstanceOf(ResolverService.Plan.FactorizarFunciones.class, SERVICIO.decidir("sen(x)"));
        assertInstanceOf(ResolverService.Plan.FactorizarFunciones.class, SERVICIO.decidir("e^(2x) - 1"));
        assertInstanceOf(ResolverService.Plan.NoSoportado.class, SERVICIO.decidir("sen(y) + x"));
        assertInstanceOf(ResolverService.Plan.NoSoportado.class, SERVICIO.decidir("x + y = 1"));
        assertTrue(SERVICIO.decidir("x^2 = 4").resoluble());
        assertTrue(SERVICIO.decidir("d/dx(x^2)").resoluble());
        assertTrue(SERVICIO.decidir("∂/∂y(x^2 y)").resoluble());
        assertTrue(SERVICIO.decidir("∬(x y, x, 0, 1, y, 0, 1)").resoluble());
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "x^2 - 5x + 6        | = (x - 2)(x - 3)",
            "(x^2 - 1)/(x + 1)   | = x - 1",
            "x^2 - 5x + 6 = 0    | x₁ = 2, x₂ = 3",
            "2x^2 = 3x + 2       | x₁ = -1/2 ≈ -0.5, x₂ = 2",
            "(x - 1)^2 = 0       | x = 1 (doble)",
            "3x = 1              | x = 1/3 ≈ 0.3333",
            "2/3 + 1             | = 5/3 ≈ 1.6667",
            "2·3 + 4             | = 10",
            "x^2 + 1 = 0         | No tiene solución en Q",
            "lim(x→2, (x^2 - 4)/(x - 2))   | = 4",
            "lim(x→1, (x^3 - 1)/(x^2 - 1)) | = 3/2 ≈ 1.5",
            "lim(x→∞, (1 + 1/x)^x)         | = e ≈ 2.7183",
            "lim(x→2+, 1/(x - 2))          | = +∞",
            "lim(x→2, 1/(x - 2))           | No existe"
    })
    void resultadoEnVivo(String texto, String esperado) {
        assertEquals(Optional.of(esperado), SERVICIO.enVivo(texto));
    }

    /** Sprint 4: en R y C el resultado en vivo lleva radicales e i; las funciones, la sustitución. */
    @ParameterizedTest(name = "{0} en {1} → {2}")
    @CsvSource(delimiter = '|', value = {
            "x^2 - 2       | R | = (x - √2)(x + √2)",
            "x^2 + 4       | C | = (x - 2·i)(x + 2·i)",
            "x^2 + 1       | R | ",
            "x^5 - x - 1   | R | Irreducible en Q; raíces reales ≈ 1.1673039783",
            "e^(2x) - 1    | Q | = (e^x + 1)·(e^x - 1)",
    })
    void resultadoEnVivoEnCampo(String texto, Campo campo, String esperado) {
        assertEquals(Optional.ofNullable(esperado), SERVICIO.enVivo(texto, campo));
    }

    /** RF71: nada para operaciones que todavía no se resuelven, ni para entradas incompletas. */
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"∫(e^(x^2), x)", "x^2 +", "□ + 1", "x^2 + 1", "sen(x)", "x + sen(x)"})
    void sinResultadoEnVivo(String texto) {
        assertEquals(Optional.empty(), SERVICIO.enVivo(texto));
    }

    /** Sprint 6, Parte D: resultado en vivo de derivadas e integrales (exacto, y ≈ en las definidas no enteras). */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = ';', value = {
            "d/dx(x^2 sen x)          ; = 2x·sen x + x²·cos x",
            "d2/dx2(x^3 - 3x)         ; = 6x",
            "d/dx(x^2 + y^2 = 25)     ; dy/dx = −x/y",
            "∫(3x^2 - 4x + 5, x)      ; = x³ − 2x² + 5x + C",
            "∫(1/(x^2 - 1), x)        ; = (1/2)ln|x − 1| − (1/2)ln|x + 1| + C",
            "∫(x^2, x, 0, 1)          ; = 1/3 ≈ 0.3333",
            "∫(1/x, x, 1, e)          ; = 1",
            "3∫(3x, x, 3, 6)          ; = 243/2 ≈ 121.5",
            "∫(1/x, x, 1, 2)          ; = ln 2 ≈ 0.6931",
    })
    void resultadoEnVivoDeDerivadasEIntegrales(String texto, String esperado) {
        assertEquals(Optional.of(esperado), SERVICIO.enVivo(texto));
    }

    /**
     * Sprint 5, corrección 1: con un límite, el resultado en vivo es el valor del límite (nunca la
     * fracción simplificada) y coincide con lo que da el motor, para los 16 ejemplos de LimitesTest.
     */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "lim(x→2, (x^2 - 4)/(x - 2))     | = 4",
            "lim(x→1, (x^3 - 1)/(x^2 - 1))   | = 3/2 ≈ 1.5",
            "lim(x→∞, (2x^2 + 1)/(x^2 - 3))  | = 2",
            "lim(x→∞, (x^3 + 1)/x^2)         | = +∞",
            "lim(x→∞, (3x + 1)/(x^2 + 5))    | = 0",
            "lim(x→0, sen(x)/x)              | = 1",
            "lim(x→0, sen(3x)/x)             | = 3",
            "lim(x→0, (1 - cos x)/x^2)       | = 1/2 ≈ 0.5",
            "lim(x→0, ln(1 + x)/x)           | = 1",
            "lim(x→0, (e^x - 1 - x)/x^2)     | = 1/2 ≈ 0.5",
            "lim(x→2+, 1/(x - 2))            | = +∞",
            "lim(x→2-, 1/(x - 2))            | = −∞",
            "lim(x→2, 1/(x - 2))             | No existe",
            "lim(x→4, (√x - 2)/(x - 4))      | = 1/4 ≈ 0.25",
            "lim(x→∞, √(x^2 + x) - x)        | = 1/2 ≈ 0.5",
            "lim(x→∞, (1 + 1/x)^x)           | = e ≈ 2.7183"
    })
    void resultadoEnVivoDeLosLimitesEsElDelMotor(String texto, String esperado) {
        String motor = SERVICIO.limites().calcular(texto).resultado().texto();
        Optional<String> enVivo = SERVICIO.enVivo(texto, Campo.R);
        assertEquals(Optional.of(esperado), enVivo);
        // El mismo valor que el motor: "= 4" para 4, "No existe" para "no existe".
        String valor = enVivo.get().startsWith("= ") ? enVivo.get().substring(2).split(" ≈ ")[0] : enVivo.get();
        assertEquals(motor.toLowerCase(), valor.toLowerCase());
    }

    @Test
    void decimal() {
        assertEquals("1/8 ≈ 0.125", ResolverService.conDecimal(Racional.de(1, 8)));
        assertEquals("-2/3 ≈ -0.6667", ResolverService.conDecimal(Racional.de(-2, 3)));
        assertEquals("7", ResolverService.conDecimal(Racional.de(7)));
    }
}
