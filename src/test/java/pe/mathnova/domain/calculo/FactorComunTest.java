package pe.mathnova.domain.calculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.integrales.MotorIntegrales;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;

/**
 * Límites, derivadas e integrales de un cociente de polinomios con un factor común: primero se factoriza y se
 * cancela (con la restricción), después la operación sobre lo que queda. Por la misma ruta que "Resolver"
 * ({@link ResolverService}); se comprueba el paso elegido, la factorización, la cancelación y el resultado, no
 * solo el valor final.
 */
class FactorComunTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final ResolverService RESOLVER = new ResolverService(new FactorizacionService(MOTOR), MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static ResolucionDerivada derivar(String f) {
        return RESOLVER.resolverDerivada("d/dx(" + f + ")", null, a -> { });
    }

    private static ResolucionIntegral integrar(String f) {
        return RESOLVER.resolverIntegral("int(" + f + ", x)", null, a -> { });
    }

    private static ResolucionLimite limite(String f, String punto) {
        return RESOLVER.resolverLimite("lim(x->" + punto + ", " + f + ")", null, a -> { });
    }

    private static String p(pe.mathnova.domain.expresion.Expresion e) {
        return ImpresoraPizarra.imprimir(e);
    }

    private static List<String> codigos(List<PasoLimite> pasos) {
        return pasos.stream().map(PasoLimite::codigo).toList();
    }

    /** El paso que factoriza y cancela: qué factoriza, con qué métodos y dónde vale. */
    private static void cancela(PasoLimite paso, String codigo, String antes, String despues, List<String> metodos,
                                List<String> restricciones) {
        assertEquals(codigo, paso.codigo());
        assertEquals(antes, p(paso.antes()));
        assertEquals(despues, p(paso.despues()));
        assertEquals(metodos, paso.hijos().stream().map(PasoLimite::codigo).toList());
        if (codigo.equals(FactorComun.SIMPLIFICAR)) {
            List<String> lineas = ((TrabajoIntermedio.Agrupacion) paso.trabajo()).grupos();
            assertEquals(String.join(", ", restricciones), lineas.getLast());
        }
    }

    // ------------------------------------------------------------------ caso corto

    @Test
    void cortoEnElLimite() {
        ResolucionLimite r = limite("(x^2-9)/(x-3)", "3");
        // La evaluación directa da 0/0 (la función no está definida en 3) y por eso se factoriza
        assertEquals(List.of("L01", "L02", "L01"), codigos(r.pasos()));
        cancela(r.pasos().get(1), "L02", "(x² − 9)/(x − 3)", "x + 3", List.of("F05"), List.of());
        assertEquals("(x + 3)(x − 3)", p(r.pasos().get(1).hijos().getFirst().despues()));
        assertEquals("6", r.resultado().texto());
    }

    @Test
    void cortoEnLaDerivadaNoEmpiezaConLaReglaDelCociente() {
        ResolucionDerivada r = derivar("(x^2-9)/(x-3)");
        assertEquals(FactorComun.SIMPLIFICAR, r.pasos().getFirst().codigo());
        cancela(r.pasos().getFirst(), "P01", "(x² − 9)/(x − 3)", "x + 3", List.of("F05"), List.of("x ≠ 3"));
        assertFalse(codigos(r.pasos()).contains("D05"), "no debe usar la regla del cociente");
        assertEquals("x + 3", p(r.pasos().get(1).antes()), "se deriva la expresión simplificada");
        assertEquals("1", p(r.resultado()));
        assertEquals(List.of("x ≠ 3"), FactorComun.restricciones(r.pasos()));
    }

    @Test
    void cortoEnLaIntegralNoEmpiezaConLaDivision() {
        ResolucionIntegral r = integrar("(x^2-9)/(x-3)");
        cancela(r.pasos().getFirst(), "P01", "(x² − 9)/(x − 3)", "x + 3", List.of("F05"), List.of("x ≠ 3"));
        assertFalse(codigos(r.pasos()).contains(MotorIntegrales.DIVISION), "no debe dividir polinomios");
        assertEquals("∫ (x + 3) dx", p(r.pasos().get(1).antes()));
        assertEquals("x²/2 + 3x + C", r.texto());
        assertTrue(r.verificada());
    }

    // ------------------------------------------------------------------ caso largo

    @Test
    void largoEnElLimite() {
        ResolucionLimite r = limite("(x^3-3*x-2)/(x-2)", "2");
        assertEquals(List.of("L01", "L02", "L01"), codigos(r.pasos()));
        cancela(r.pasos().get(1), "L02", "(x³ − 3x − 2)/(x − 2)", "(x + 1)²", List.of("F16", "F07"), List.of());
        assertEquals("9", r.resultado().texto());
    }

    @Test
    void largoEnLaDerivada() {
        ResolucionDerivada r = derivar("(x^3-3*x-2)/(x-2)");
        cancela(r.pasos().getFirst(), "P01", "(x³ − 3x − 2)/(x − 2)", "(x + 1)²", List.of("F16", "F07"),
                List.of("x ≠ 2"));
        PasoLimite ruffini = r.pasos().getFirst().hijos().getFirst();
        TrabajoIntermedio.TablaRuffini t = (TrabajoIntermedio.TablaRuffini) ruffini.trabajo();
        var division = t.divisiones().getFirst();
        assertEquals("2", division.raiz().toString());
        assertEquals("[1, 0, -3, -2]", division.coeficientes().toString());
        assertEquals("[2, 4, 2]", division.productos().toString());
        assertEquals("[1, 2, 1, 0]", division.resultados().toString());
        assertEquals("(x − 2)(x² + 2x + 1)", p(ruffini.despues()));
        assertEquals("(x + 1)²", p(r.pasos().getFirst().hijos().get(1).despues()));
        assertFalse(codigos(r.pasos()).contains("D05"));
        assertEquals("2(x + 1)", p(r.resultado()));
    }

    @Test
    void largoEnLaIntegral() {
        ResolucionIntegral r = integrar("(x^3-3*x-2)/(x-2)");
        cancela(r.pasos().getFirst(), "P01", "(x³ − 3x − 2)/(x − 2)", "(x + 1)²", List.of("F16", "F07"),
                List.of("x ≠ 2"));
        assertFalse(codigos(r.pasos()).contains(MotorIntegrales.DIVISION));
        assertEquals("(x + 1)³/3 + C", r.texto());
        assertTrue(r.verificada());
    }

    // ------------------------------------------------------------------ otros casos

    @Test
    void cancelacionParcialConservaLasDosRestricciones() {
        ResolucionDerivada d = derivar("(x^2-9)/(x^2-x-6)");
        cancela(d.pasos().getFirst(), "P01", "(x² − 9)/(x² − x − 6)", "(x + 3)/(x + 2)", List.of("F05", "F12"),
                List.of("x ≠ −2", "x ≠ 3"));
        assertEquals("−1/(x + 2)²", p(d.resultado()));
        ResolucionIntegral i = integrar("(x^2-9)/(x^2-x-6)");
        cancela(i.pasos().getFirst(), "P01", "(x² − 9)/(x² − x − 6)", "(x + 3)/(x + 2)", List.of("F05", "F12"),
                List.of("x ≠ −2", "x ≠ 3"));
        assertEquals("x + ln|x + 2| + C", i.texto());
        ResolucionLimite l = limite("(x^2-9)/(x^2-x-6)", "3");
        assertEquals("(x + 3)/(x + 2)", p(l.pasos().get(1).despues()));
        assertEquals("6/5", l.resultado().texto());
    }

    @Test
    void sinFactorComunSeUsaElMetodoHabitual() {
        ResolucionDerivada d = derivar("(x^2+1)/(x-3)");
        assertEquals("D05", d.pasos().getFirst().codigo());
        assertTrue(FactorComun.restricciones(d.pasos()).isEmpty());
        ResolucionIntegral i = integrar("(x^2+1)/(x-3)");
        assertEquals(MotorIntegrales.DIVISION, i.pasos().getFirst().codigo());
        assertEquals("x²/2 + 3x + 10ln|x − 3| + C", i.texto());
        assertTrue(FactorComun.buscar(pe.mathnova.domain.parser.Parser.parse("(x^2+1)/(x-3)"), MOTOR).isEmpty());
    }

    @Test
    void siElAlumnoEligeUnMetodoSeRespeta() {
        // "Probar" fracciones parciales en Exploración: se divide como pidió, sin simplificar antes
        ResolucionIntegral i = RESOLVER.resolverIntegral("int((x^2-9)/(x-3), x)", MotorIntegrales.FRACCIONES, a -> { });
        assertNotEquals(FactorComun.SIMPLIFICAR, i.pasos().getFirst().codigo());
        assertEquals("x²/2 + 3x + C", i.texto());
        ResolucionDerivada d = RESOLVER.resolverDerivada("d/dx((x^2-9)/(x-3))",
                pe.mathnova.domain.calculo.derivadas.MotorDerivadas.COMO_PRODUCTO, a -> { });
        assertNotEquals(FactorComun.SIMPLIFICAR, d.pasos().getFirst().codigo());
    }

    @Test
    void unaExpresionYaFactorizadaMuestraLosFactoresQueSeCancelan() {
        ResolucionDerivada d = derivar("((x-3)(x+3))/(x-3)");
        PasoLimite primero = d.pasos().getFirst();
        assertEquals(FactorComun.SIMPLIFICAR, primero.codigo());
        assertEquals("x + 3", p(primero.despues()));
        assertEquals("1", p(d.resultado()));
    }
}
