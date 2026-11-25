package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;

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
    void laFormulaSeReduceHastaCatorcePxYLuegoPasaADosLineas() {
        FormulaAjustable.Disposicion cabe = FormulaAjustable.elegir(300, 200, 0, 400, 18, 14);
        assertEquals(new FormulaAjustable.Disposicion(false, 18, 1, false), cabe, "cabe: su tamaño");
        FormulaAjustable.Disposicion reducida = FormulaAjustable.elegir(400, 250, 0, 340, 18, 14);
        assertEquals(new FormulaAjustable.Disposicion(false, 14, 1, false), reducida, "no cabe: se reduce hasta caber");
        FormulaAjustable.Disposicion partida = FormulaAjustable.elegir(400, 250, 0, 200, 18, 14);
        assertEquals(new FormulaAjustable.Disposicion(true, 14, 1, false), partida, "a 14 px tampoco: dos líneas");
    }
}
