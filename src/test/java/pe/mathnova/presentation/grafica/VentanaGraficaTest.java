package pe.mathnova.presentation.grafica;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.analisis.AnalisisPolinomio;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;

/** HU35: ventana inicial, puntos exactos y curva cortada en las asíntotas, sin abrir la ventana. */
class VentanaGraficaTest {

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    private DatosGrafica polinomio(String texto) {
        return DatosGrafica.de(motor.resolver(Parser.parse(texto), Campo.Q)).orElseThrow();
    }

    private DatosGrafica fraccion(String texto) {
        return DatosGrafica.de(new SimplificadorFracciones(motor).simplificar(Parser.parse(texto))).orElseThrow();
    }

    @Test
    void laVentanaInicialContieneLasRaicesYElVertice() {
        DatosGrafica d = polinomio("x^2 - 5x + 6");
        VentanaGrafica v = VentanaGrafica.inicial(d);
        AnalisisPolinomio.Punto vertice = d.vertice().orElseThrow();
        assertAll(
                () -> assertEquals(new AnalisisPolinomio.Punto(Racional.de(5, 2), Racional.de(-1, 4)), vertice),
                () -> assertTrue(v.contiene(2, 0), "raíz 2"),
                () -> assertTrue(v.contiene(3, 0), "raíz 3"),
                () -> assertTrue(v.contiene(2.5, -0.25), "vértice"),
                // de la raíz menor a la mayor con un 30 % de margen a cada lado
                () -> assertEquals(1.7, v.xMin(), 1e-9),
                () -> assertEquals(3.3, v.xMax(), 1e-9));
    }

    @Test
    void sinRaicesRealesVaDeMenosCincoACinco() {
        VentanaGrafica v = VentanaGrafica.inicial(polinomio("x^2 + 1"));
        assertEquals(-5, v.xMin(), 1e-9);
        assertEquals(5, v.xMax(), 1e-9);
    }

    @Test
    void enYSeRecortaAMasMenosCincuenta() {
        // 1/(x − 1) se dispara cerca de 1: la ventana no pasa de ±50 (más un pequeño aire).
        VentanaGrafica v = VentanaGrafica.inicial(fraccion("(x + 1)/(x^2 - 1)"));
        assertTrue(v.yMax() <= 50 * 1.25 && v.yMin() >= -50 * 1.25, v.toString());
    }

    @Test
    void puntosExactosDeLaPolinomica() {
        DatosGrafica d = polinomio("2x^4 - 5x^3 + 5x - 2");
        assertAll(
                () -> assertEquals(List.of(Racional.de(-1), Racional.de(1, 2), Racional.de(1), Racional.de(2)),
                        d.raices().stream().map(AnalisisPolinomio.Raiz::valor).toList()),
                () -> assertEquals(Racional.de(-2), d.corteY().orElseThrow()),
                () -> assertFalse(d.esRacional()));
        assertEquals(2, polinomio("x^3 - 3x + 2").raices().stream()
                .filter(r -> r.valor().equals(Racional.UNO)).findFirst().orElseThrow().multiplicidad(), "raíz doble");
    }

    /** (x² − 4)/(x² + x − 6) = (x + 2)/(x + 3): asíntota en −3 y hueco en x = 2. */
    @Test
    void racionalConAsintotaYHueco() {
        DatosGrafica d = fraccion("(x^2 - 4)/(x^2 + x - 6)");
        assertAll(
                () -> assertTrue(d.esRacional()),
                () -> assertEquals(List.of(Racional.de(-3)), d.asintotas()),
                () -> assertEquals(List.of(new AnalisisPolinomio.Punto(Racional.de(2), Racional.de(4, 5))), d.huecos()),
                () -> assertEquals(List.of(Racional.de(-2)), d.raices().stream().map(AnalisisPolinomio.Raiz::valor).toList()),
                () -> assertEquals(Racional.de(2, 3), d.corteY().orElseThrow()));
    }

    /** C.7: ningún trazo une las dos ramas de una asíntota. */
    @Test
    void laCurvaSeCortaEnLaAsintota() {
        DatosGrafica d = fraccion("(x + 1)/(x^2 - 1)"); // 1/(x − 1)
        List<List<double[]>> trazos = TrazadoCurva.trazos(d.funcion(), -3, 5, 801, List.of(1.0), -200, 200);
        assertTrue(trazos.size() >= 2, "al menos una rama a cada lado");
        for (List<double[]> t : trazos) {
            double primero = t.getFirst()[0];
            double ultimo = t.getLast()[0];
            assertFalse(primero < 1 && ultimo > 1, "un trazo cruza la asíntota x = 1");
        }
    }

    @Test
    void marcasRedondas() {
        assertEquals(1, VentanaGrafica.paso(10, 8), 1e-12);
        assertEquals(0.2, VentanaGrafica.paso(1.6, 8), 1e-12);
        assertEquals(10, VentanaGrafica.paso(80, 8), 1e-12);
    }

    @Test
    void acercarDejaFijoElCursor() {
        VentanaGrafica v = new VentanaGrafica(-5, 5, -5, 5).escalar(0.5, 2, 1);
        assertEquals(-1.5, v.xMin(), 1e-12);
        assertEquals(3.5, v.xMax(), 1e-12);
        assertEquals(-2, v.yMin(), 1e-12);
    }
}
