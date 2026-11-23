package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;

/** RF66 (la tabla de propiedades como texto) y A.3 (la fórmula del paso se reduce hasta 14 px). */
class PropiedadesYFilaTest {

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    private VistaPropiedades propiedades(String texto) {
        return VistaPropiedades.de(motor.resolver(Parser.parse(texto), Campo.Q)).orElseThrow();
    }

    @Test
    void tablaDePropiedades() {
        VistaPropiedades v = propiedades("2x^4 - 5x^3 + 5x - 2");
        assertAll(
                () -> assertEquals(List.of(
                        new VistaPropiedades.Fila("Grado", "4"),
                        new VistaPropiedades.Fila("Coeficiente principal", "2"),
                        new VistaPropiedades.Fila("Raíces racionales", "x = −1, x = 1/2, x = 1, x = 2"),
                        new VistaPropiedades.Fila("Corte con el eje y", "(0, −2)")), v.filas()),
                () -> assertEquals(List.of("−1", "1/2", "1", "2"), v.marcas()),
                () -> assertEquals(List.of(1, -1, 1, -1, 1), v.signos()),
                () -> assertNull(v.avisoSignos()));
    }

    @Test
    void verticeYRaizDoble() {
        assertEquals(new VistaPropiedades.Fila("Vértice", "(5/2, −1/4)"), propiedades("x^2 - 5x + 6").filas().getLast());
        assertEquals("x = −2, x = 1 (doble)", propiedades("x^3 - 3x + 2").filas().get(2).valor());
    }

    @Test
    void laFormulaSeReduceHastaCatorcePxYLuegoSeDesplaza() {
        assertEquals(300, FilaPaso.anchoAjustado(300, 400), 1e-9, "cabe: su tamaño");
        assertEquals(340, FilaPaso.anchoAjustado(400, 340), 1e-9, "no cabe: se reduce hasta caber");
        assertEquals(400 * 14.0 / 18.0, FilaPaso.anchoAjustado(400, 200), 1e-9, "no más chica que 14 px: se desplaza");
    }
}
