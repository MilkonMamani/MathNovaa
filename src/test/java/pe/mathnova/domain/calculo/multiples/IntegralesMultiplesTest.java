package pe.mathnova.domain.calculo.multiples;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

/** Sprint 8, Parte C (HU31, RF56): integral iterada (V09), cambio de orden y cambio de coordenadas (V10). */
class IntegralesMultiplesTest {

    private static final MotorIntegralesMultiples MOTOR = new MotorIntegralesMultiples(new OrquestadorFactorizacion());
    private static final MathContext MC = new MathContext(30);

    private static ResolucionIntegralMultiple resolver(String texto) {
        return MOTOR.resolver((IntegralMultiple) Parser.parse(texto));
    }

    /** El valor exacto coincide con el esperado (que puede llevar π o e) con 25 cifras. */
    private static void vale(String esperado, ResolucionIntegralMultiple r) {
        assertTrue(r.resuelta(), () -> "no se resolvió: " + r.motivo());
        BigDecimal a = EvaluacionNumerica.evaluar(Parser.parse(esperado), Map.of(), MC).re();
        BigDecimal b = EvaluacionNumerica.evaluar(r.valor(), Map.of(), MC).re();
        assertEquals(0, a.subtract(b).abs().compareTo(new BigDecimal("1e-25")) < 0 ? 0 : 1,
                () -> "esperado " + esperado + ", obtenido " + r.texto());
    }

    /** Simpson da lo mismo con 4 cifras. */
    private static void comprobado(ResolucionIntegralMultiple r, int n) {
        BigDecimal s = MotorIntegralesMultiples.simpson(r, n).orElseThrow();
        BigDecimal v = MotorIntegralesMultiples.decimal(r).orElseThrow();
        BigDecimal tolerancia = new BigDecimal("1e-4").multiply(v.abs().max(BigDecimal.ONE));
        assertTrue(s.subtract(v).abs().compareTo(tolerancia) < 0, () -> "Simpson " + s + " y " + v);
    }

    private static String lineas(PasoLimite p) {
        return String.join("\n", ((TrabajoIntermedio.Agrupacion) p.trabajo()).grupos());
    }

    // ------------------------------------------------------------------ gramática

    @Test
    void seLeeElCambioDeCoordenadas() {
        IntegralMultiple i = assertInstanceOf(IntegralMultiple.class,
                Parser.parse("∬(x^2 + y^2, r, 0, 1, θ, 0, 2π; coord=polares)"));
        assertAll(
                () -> assertEquals(IntegralMultiple.Coordenadas.POLARES, i.coordenadas()),
                () -> assertEquals(i, Parser.parse(ImpresoraTexto.imprimir(i))),
                () -> assertEquals(IntegralMultiple.Coordenadas.ESFERICAS, ((IntegralMultiple) Parser.parse(
                        "∭(1, ρ, 0, 2, φ, 0, π, θ, 0, 2π; coord=esféricas)")).coordenadas()),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("∬(1, r, 0, 1, θ, 0, 2π; coord=esfericas)")),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("∬(1, r, 0, 1, θ, 0, 2π; polares)")),
                () -> assertThrows(ErrorSintaxis.class, () -> Parser.parse("polares + 1")));
    }

    // ------------------------------------------------------------------ los ejemplos exactos

    @Test
    void rectangulo() {
        ResolucionIntegralMultiple r = resolver("∬(x y, x, 0, 2, y, 0, 3)");
        vale("9", r);
        assertEquals("9", r.texto());
        assertEquals(2, r.pasos().size());
        // cada integral interna lleva anidados los pasos del motor de una variable
        assertFalse(r.pasos().getFirst().hijos().isEmpty());
        assertTrue(r.pasos().getFirst().justificacion().contains("y se trata como constante"));
        comprobado(r, 20);
    }

    @Test
    void regionTriangular() {
        ResolucionIntegralMultiple r = resolver("∬(x + y, y, 0, x, x, 0, 1)");
        vale("1/2", r);
        assertEquals("1/2", r.texto());
        assertTrue(lineas(r.pasos().getFirst()).contains("F(x) − F(0)"), lineas(r.pasos().getFirst()));
        comprobado(r, 20);
    }

    @Test
    void areaEntreParabolaYRecta() {
        ResolucionIntegralMultiple r = resolver("∬(1, y, x^2, x, x, 0, 1)");
        vale("1/6", r);
        comprobado(r, 20);
    }

    @Test
    void laInternaNoEsElementalYSeInvierte() {
        ResolucionIntegralMultiple r = resolver("∬(e^(x^2), x, y, 1, y, 0, 1)");
        vale("(e - 1)/2", r);
        assertAll(
                () -> assertTrue(r.invertida()),
                () -> assertEquals(Parser.parse("∬(e^(x^2), y, 0, x, x, 0, 1)"), r.efectiva()),
                () -> assertEquals("Cambiar el orden de integración", r.pasos().getFirst().titulo()),
                () -> assertTrue(r.pasos().getFirst().justificacion().contains("no es elemental")));
        comprobado(r, 20);
    }

    @Test
    void polares() {
        ResolucionIntegralMultiple r = resolver("∬(x^2 + y^2, r, 0, 1, θ, 0, 2π; coord=polares)");
        vale("π/2", r);
        PasoLimite cambio = r.pasos().getFirst();
        assertAll(
                () -> assertEquals("V10", cambio.codigo()),
                () -> assertEquals("r³", ImpresoraPizarra.imprimir(r.integrando())),
                () -> assertEquals("r", ImpresoraPizarra.imprimir(r.jacobiano())),
                // RN40: el jacobiano como determinante y simplificado
                () -> assertTrue(lineas(cambio).contains("J = ∂(x, y)/∂(r, θ)"), lineas(cambio)),
                () -> assertTrue(lineas(cambio).contains("= r"), lineas(cambio)),
                () -> assertEquals("π/2", r.texto()));
        comprobado(r, 20);
    }

    @Test
    void cuboTriple() {
        ResolucionIntegralMultiple r = resolver("∭(x + y + z, x, 0, 1, y, 0, 1, z, 0, 1)");
        vale("3/2", r);
        assertEquals(3, r.pasos().size());
        comprobado(r, 16);
    }

    @Test
    void cilindricasBajoElParaboloide() {
        ResolucionIntegralMultiple r = resolver("∭(1, z, 0, 4 - r^2, r, 0, 2, θ, 0, 2π; coord=cilindricas)");
        vale("8π", r);
        assertEquals("r", ImpresoraPizarra.imprimir(r.jacobiano()));
        comprobado(r, 16);
    }

    @Test
    void esfericasVolumenDeLaEsfera() {
        ResolucionIntegralMultiple r = resolver("∭(1, ρ, 0, 2, φ, 0, π, θ, 0, 2π; coord=esfericas)");
        vale("32π/3", r);
        assertEquals("ρ²·sen φ", ImpresoraPizarra.imprimir(r.jacobiano()));
        assertTrue(lineas(r.pasos().getFirst()).contains("J = ∂(x, y, z)/∂(ρ, φ, θ)"));
        comprobado(r, 16);
    }

    // ------------------------------------------------------------------ Exploración

    @Test
    void forzarElCambioDeOrden() {
        IntegralMultiple in = (IntegralMultiple) Parser.parse("∬(x + y, y, 0, x, x, 0, 1)");
        ResolucionIntegralMultiple r = MOTOR.resolver(in, MotorIntegralesMultiples.ORDEN);
        vale("1/2", r);
        assertEquals(Parser.parse("∬(x + y, x, y, 1, y, 0, 1)"), r.efectiva());
    }

    @Test
    void forzarPolaresEnUnDisco() {
        IntegralMultiple in = (IntegralMultiple) Parser.parse(
                "∬(x^2 + y^2, y, -sqrt(1 - x^2), sqrt(1 - x^2), x, -1, 1)");
        ResolucionIntegralMultiple r = MOTOR.resolver(in, MotorIntegralesMultiples.POLARES);
        vale("π/2", r);
        assertEquals(IntegralMultiple.Coordenadas.POLARES, r.coordenadas());
        List<CaminoCalculo> caminos = MOTOR.caminos(MOTOR.resolver(in));
        assertTrue(caminos.stream().anyMatch(c -> c.codigo().equals("POLARES")
                && c.estado() == CaminoCalculo.Estado.SIRVE), caminos.toString());
    }

    @Test
    void forzarEsfericasEnLaBola() {
        IntegralMultiple in = (IntegralMultiple) Parser.parse("∭(1, z, -sqrt(4 - x^2 - y^2), sqrt(4 - x^2 - y^2), "
                + "y, -sqrt(4 - x^2), sqrt(4 - x^2), x, -2, 2)");
        vale("32π/3", MOTOR.resolver(in, MotorIntegralesMultiples.ESFERICAS));
    }

    @Test
    void enUnRectanguloPolaresNoAplica() {
        IntegralMultiple in = (IntegralMultiple) Parser.parse("∬(x y, x, 0, 2, y, 0, 3)");
        List<CaminoCalculo> caminos = MOTOR.caminos(MOTOR.resolver(in));
        CaminoCalculo polares = caminos.stream().filter(c -> c.codigo().equals("POLARES")).findFirst().orElseThrow();
        assertEquals(CaminoCalculo.Estado.NO_APLICA, polares.estado());
        assertNotNull(polares.texto());
        assertEquals(CaminoCalculo.Estado.USADO, caminos.getFirst().estado());
    }

    // ------------------------------------------------------------------ errores

    @Test
    void losLimitesSoloDependenDeLasDeAfuera() {
        assertThrows(IllegalArgumentException.class, () -> resolver("∬(1, y, 0, 1, x, 0, y)"));
        assertThrows(IllegalArgumentException.class, () -> resolver("∬(1, r, 0, 1, x, 0, 1; coord=polares)"));
    }

    @Test
    void elIntegrandoEnCoordenadasSeCambia() {
        Expresion e = MotorIntegralesMultiples.aCoordenadas(Parser.parse("x^2 + y^2 + z^2"),
                IntegralMultiple.Coordenadas.ESFERICAS);
        assertEquals("ρ²", ImpresoraPizarra.imprimir(e));
    }
}
