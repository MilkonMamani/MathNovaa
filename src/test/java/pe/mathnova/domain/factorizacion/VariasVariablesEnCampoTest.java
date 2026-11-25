package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.parser.Parser;

class VariasVariablesEnCampoTest {

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    @ParameterizedTest(name = "{0} en {1}")
    @CsvSource(delimiter = '|', textBlock = """
            x^2 + 3xy + 2y^2 + 4x + 5y + 3 | R
            x^2 + 3xy + 2y^2 + 4x + 5y + 3 | C
            x^2 - y^2                      | R
            x^2 - y^2                      | C
            x^2y - y                       | R
            x^2 + xy + x                   | C
            """)
    void daLoMismoQueEnQ(String entrada, Campo campo) {
        Resolucion enQ = motor.resolver(Parser.parse(entrada), Campo.Q);
        Resolucion enCampo = motor.resolver(Parser.parse(entrada), campo);
        assertEquals(enQ.texto(), enCampo.texto());
    }
}
