package pe.mathnova.domain.analisis;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;

/** RF66: propiedades exactas de la función. */
class AnalisisPolinomioTest {

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    private AnalisisPolinomio analizar(String texto) {
        return AnalisisPolinomio.de(motor.resolver(Parser.parse(texto), Campo.Q)).orElseThrow();
    }

    @Test
    void cuarticaConCuatroRaices() {
        AnalisisPolinomio a = analizar("2x^4 - 5x^3 + 5x - 2");
        assertAll(
                () -> assertEquals(4, a.grado()),
                () -> assertEquals(Racional.de(2), a.coeficientePrincipal()),
                () -> assertEquals(List.of(Racional.de(-1), Racional.de(1, 2), Racional.de(1), Racional.de(2)),
                        a.raices().stream().map(AnalisisPolinomio.Raiz::valor).toList()),
                () -> assertTrue(a.raices().stream().allMatch(r -> r.multiplicidad() == 1)),
                () -> assertEquals(Racional.de(-2), a.corteY(), "f(0) = −2"),
                () -> assertTrue(a.signosCompletos()),
                // (−∞, −1), (−1, 1/2), (1/2, 1), (1, 2), (2, +∞)
                () -> assertEquals(List.of(1, -1, 1, -1, 1),
                        a.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList()),
                () -> assertEquals(null, a.signos().getFirst().desde()),
                () -> assertEquals(Racional.de(-1), a.signos().getFirst().hasta()),
                () -> assertEquals(Racional.de(3, 4), a.signos().get(2).prueba(), "punto medio de 1/2 y 1"),
                () -> assertTrue(a.vertice().isEmpty()));
    }

    @Test
    void cuadraticaConVertice() {
        AnalisisPolinomio a = analizar("x^2 - 5x + 6");
        assertAll(
                () -> assertEquals(new AnalisisPolinomio.Punto(Racional.de(5, 2), Racional.de(-1, 4)),
                        a.vertice().orElseThrow()),
                () -> assertEquals(List.of(Racional.de(2), Racional.de(3)),
                        a.raices().stream().map(AnalisisPolinomio.Raiz::valor).toList()),
                () -> assertEquals(Racional.de(6), a.corteY()),
                () -> assertEquals(List.of(1, -1, 1), a.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList()));
    }

    @Test
    void multiplicidadYFactoresSinRaicesReales() {
        AnalisisPolinomio doble = analizar("x^3 - 3x + 2"); // (x − 1)²(x + 2)
        assertEquals(List.of(new AnalisisPolinomio.Raiz(Racional.de(-2), 1), new AnalisisPolinomio.Raiz(Racional.de(1), 2)),
                doble.raices());
        assertEquals(List.of(-1, 1, 1), doble.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList(),
                "en la raíz doble no cambia el signo");

        AnalisisPolinomio conCuadratico = analizar("x^4 - 16"); // (x + 2)(x − 2)(x² + 4)
        assertTrue(conCuadratico.signosCompletos(), "x² + 4 nunca se anula");
        assertEquals(List.of(1, -1, 1), conCuadratico.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList());

        AnalisisPolinomio sinRaices = analizar("x^2 + x + 1");
        assertEquals(List.of(1), sinRaices.signos().stream().map(AnalisisPolinomio.Tramo::signo).toList());
    }

    /** x² − 2 tiene raíces reales que no son racionales: la tabla de signos no se puede dar exacta. */
    @Test
    void conRaicesIrracionalesNoHayTablaDeSignos() {
        AnalisisPolinomio a = analizar("x^3 - 2x");
        assertAll(
                () -> assertFalse(a.signosCompletos()),
                () -> assertTrue(a.signos().isEmpty()),
                () -> assertEquals(List.of(Racional.CERO), a.raices().stream().map(AnalisisPolinomio.Raiz::valor).toList()));
    }

    @Test
    void variasVariablesNoSeAnaliza() {
        assertTrue(AnalisisPolinomio.de(motor.resolver(Parser.parse("a^2 - b^2"), Campo.Q)).isEmpty());
    }
}
