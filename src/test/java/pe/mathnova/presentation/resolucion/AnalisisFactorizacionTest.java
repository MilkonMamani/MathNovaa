package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.resolucion.AnalisisFactorizacion.Condicion;

/** La pestaña Análisis de una factorización: propiedades, raíces sin repetir y conjunto solución exacto. */
class AnalisisFactorizacionTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final String REFERENCIA = "2*x^8-12*x^6+18*x^4-8*x^2";

    private static AnalisisFactorizacion de(String p, Campo campo) {
        return AnalisisFactorizacion.de(MOTOR.resolver(Parser.parse(p), campo)).orElseThrow();
    }

    private static AnalisisFactorizacion de(String p) {
        return de(p, Campo.Q);
    }

    private static String sinEspacios(String s) {
        return s.replaceAll("\\s+", "");
    }

    @Test
    void propiedadesDelEjercicioDeReferencia() {
        AnalisisFactorizacion a = de(REFERENCIA);
        var filas = a.porNombre();
        assertEquals("8", filas.get("Grado").latex());
        assertEquals("2", filas.get("Coeficiente principal").latex());
        assertEquals("\\left(0, 0\\right)", filas.get("Corte con el eje y").latex());
        // Cada raíz distinta una vez, de menor a mayor, sin "(doble)"
        assertEquals("x \\in \\left\\{-2, -1, 0, 1, 2\\right\\}", filas.get(AnalisisFactorizacion.RAICES).latex());
        assertEquals("Las raíces −1, 0 y 1 aparecen dos veces cada una en la factorización.", a.notaRaices());
        assertTrue(a.filas().stream().noneMatch(f -> (f.latex() + f.texto()).contains("doble")));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "MAYOR       | \\left(-\\infty, -2\\right) \\cup \\left(2, +\\infty\\right)",
            "MAYOR_IGUAL | \\left(-\\infty, -2\\right] \\cup \\left\\{-1, 0, 1\\right\\} \\cup \\left[2, +\\infty\\right)",
            "MENOR       | \\left(-2, -1\\right) \\cup \\left(-1, 0\\right) \\cup \\left(0, 1\\right) \\cup \\left(1, 2\\right)",
            "MENOR_IGUAL | \\left[-2, 2\\right]",
            "IGUAL       | \\left\\{-2, -1, 0, 1, 2\\right\\}"
    })
    void lasCincoCondicionesDelEjercicioDeReferencia(Condicion c, String esperado) {
        assertEquals("\\text{C.S.} = " + esperado, de(REFERENCIA).conjuntoSolucion(c));
    }

    @Test
    void sinSolucionEsElConjuntoVacio() {
        AnalisisFactorizacion a = de("x^2 + 1");
        assertTrue(a.recta().isPresent(), "x² + 1 se puede resolver sobre la recta real");
        assertEquals("\\text{C.S.} = \\varnothing", a.conjuntoSolucion(Condicion.MENOR));
        assertEquals("\\text{C.S.} = \\varnothing", a.conjuntoSolucion(Condicion.IGUAL));
        assertEquals("\\text{C.S.} = \\left(-\\infty, +\\infty\\right)", a.conjuntoSolucion(Condicion.MAYOR));
        assertEquals("no tiene raíces reales", a.porNombre().get(AnalisisFactorizacion.RAICES).texto());
        assertNull(a.notaRaices());
    }

    @Test
    void sinRaicesRepetidasNoHayNota() {
        AnalisisFactorizacion a = de("x^2 - 5x + 6");
        assertNull(a.notaRaices());
        assertEquals("x \\in \\left\\{2, 3\\right\\}", a.porNombre().get(AnalisisFactorizacion.RAICES).latex());
        assertEquals("\\text{C.S.} = \\left(2, 3\\right)", a.conjuntoSolucion(Condicion.MENOR));
    }

    @Test
    void laNotaDiceCuantasVecesSegunLaMultiplicidad() {
        assertEquals("La raíz 1 aparece tres veces en la factorización.", de("(x - 1)^3 (x + 2)").notaRaices());
        assertEquals("La raíz −2 aparece dos veces y la raíz 1 aparece tres veces en la factorización.",
                de("(x - 1)^3 (x + 2)^2").notaRaices());
    }

    @Test
    void unaRaizDeMultiplicidadParNoCambiaElSigno() {
        // (x − 1)²(x + 2): ≥ 0 en [−2, +∞), y 1 es un punto donde vale 0 sin cambiar de signo
        AnalisisFactorizacion a = de("(x - 1)^2 (x + 2)");
        assertEquals("\\text{C.S.} = \\left(-2, 1\\right) \\cup \\left(1, +\\infty\\right)", a.conjuntoSolucion(Condicion.MAYOR));
        assertEquals("\\text{C.S.} = \\left(-\\infty, -2\\right] \\cup \\left\\{1\\right\\}",
                a.conjuntoSolucion(Condicion.MENOR_IGUAL));
    }

    @Test
    void enQConRaicesIrracionalesNoHayConjunto() {
        // x² − 2 no se parte en Q: no se conocen sus raíces reales, la sección no se muestra
        AnalisisFactorizacion a = de("x^2 - 2");
        assertTrue(a.recta().isEmpty());
        assertEquals("no tiene raíces racionales", a.porNombre().get("Raíces racionales").texto());
    }

    @Test
    void enRLasRaicesIrracionalesEntranEnLaRecta() {
        AnalisisFactorizacion a = de("x^2 - 2", Campo.R);
        String raices = a.porNombre().get(AnalisisFactorizacion.RAICES).latex();
        assertTrue(raices.contains("\\sqrt{2}") && raices.indexOf("-") < raices.lastIndexOf("\\sqrt{2}"), raices);
        assertTrue(a.recta().isPresent());
        assertTrue(sinEspacios(a.conjuntoSolucion(Condicion.MENOR)).matches(".*\\\\left\\(-\\\\sqrt\\{2\\},\\\\sqrt\\{2\\}\\\\right\\)"),
                a.conjuntoSolucion(Condicion.MENOR));
    }

    @Test
    void enCLasComplejasSeMuestranAparteYLaRectaUsaSoloLasReales() {
        AnalisisFactorizacion a = de("x^3 + x", Campo.C);
        var filas = a.porNombre();
        assertEquals("x \\in \\left\\{0\\right\\}", filas.get(AnalisisFactorizacion.RAICES).latex());
        String complejas = filas.get("Raíces complejas").latex();
        assertTrue(complejas.contains("i"), complejas);
        assertEquals(1, a.recta().orElseThrow().raices().size());
        assertEquals("\\text{C.S.} = \\left(0, +\\infty\\right)", a.conjuntoSolucion(Condicion.MAYOR));
        // Las raíces reales no se repiten en la fila de complejas
        assertFalse(complejas.contains("0,") || complejas.contains("{0"), complejas);
    }

    @Test
    void lasPartesDelConjuntoSalenDeLosSignos() {
        var recta = new AnalisisFactorizacion.Recta(List.of(new AnalisisFactorizacion.Raiz("-1", 1),
                new AnalisisFactorizacion.Raiz("1", 1)), List.of(1, -1, 1));
        assertEquals(List.of(new AnalisisFactorizacion.Intervalo(-1, 0, false, true),
                new AnalisisFactorizacion.Intervalo(1, 2, true, false)),
                AnalisisFactorizacion.solucion(recta, Condicion.MAYOR_IGUAL));
        assertEquals(List.of(), AnalisisFactorizacion.solucion(
                new AnalisisFactorizacion.Recta(List.of(), List.of(1)), Condicion.MENOR));
    }

    @Test
    void losValoresDelMotorSeEscribenComoFormulaSoloSiSonMatematica() {
        assertTrue(TablaAnalisis.matematica("(−∞, 0) ∪ (0, ∞)"));
        assertTrue(TablaAnalisis.matematica("x = −1"));
        assertFalse(TablaAnalisis.matematica("ningún intervalo"));
        assertFalse(TablaAnalisis.matematica("sí, exacto"));
    }
}
