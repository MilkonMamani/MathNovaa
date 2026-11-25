package pe.mathnova.domain.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.expresion.IntegralMultiple;

class IntegralMultipleFormasTest {

    @ParameterizedTest(name = "{1} = {0}")
    @CsvSource(delimiter = '|', textBlock = """
            ∬(e^(x^2), x, y, 1, y, 0, 1)                  | ∬(e^(x^2), x, [y, 1], y, [0, 1])
            ∬(xy, x, 0, 1, y, 0, 2)                       | ∬(xy, x, [0, 1], y, [0, 2])
            ∬(xy, x, 0, 1, y, 0, 2)                       | ∬(xy, x, [0, 1], y, 0, 2)
            ∭(xyz, x, 0, 3, y, 0, 2, z, 0, 1)             | ∭(xyz, x, [0, 3], y, [0, 2], z, [0, 1])
            ∬(r, r, 0, 1, θ, 0, 2π; coord=polares)        | ∬(r, r, [0, 1], θ, [0, 2π]; coord=polares)
            """)
    void lasDosFormasDanElMismoArbol(String conComas, String conCorchetes) {
        IntegralMultiple a = assertInstanceOf(IntegralMultiple.class, Parser.parse(conComas));
        IntegralMultiple b = assertInstanceOf(IntegralMultiple.class, Parser.parse(conCorchetes));
        assertEquals(a, b);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            ∬(xy, x, [0, 1, y, [0, 2])
            ∬(xy, x, [0], y, [0, 2])
            """)
    void unCorcheteMalCerradoEsUnError(String texto) {
        assertThrows(ErrorSintaxis.class, () -> Parser.parse(texto));
    }
}
