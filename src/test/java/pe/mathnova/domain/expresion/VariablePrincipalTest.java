package pe.mathnova.domain.expresion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.parser.Parser;

/** RN43 y RN44. */
class VariablePrincipalTest {

    private static List<Character> letras(String texto) {
        return texto.isBlank() ? List.of() : texto.chars().filter(c -> c != ' ' && c != ',')
                .mapToObj(c -> (char) c).toList();
    }

    @ParameterizedTest(name = "{0} -> variables [{1}], constantes [{2}]")
    @CsvSource(delimiter = '|', value = {
            // RN43: la variable es la que indica la operación
            "d/dt(a t^2)                    | t    | a",
            "∫(m x + b, x)                  | x    | b, m",
            "lim(x→2, a x^2)                | x    | a",
            "∂/∂y(x^2 y + 3x y^2)           | y    | x",
            "∂2/∂x∂y(a x y)                 | x, y | a",
            "∬(k x y, x, 0, 1, y, 0, a)     | x, y | a, k",
            "d/dθ(r sen θ)                  | θ    | r",
            // RN44: sin operación se factoriza y todas las letras son variables
            "a^2 - b^2                      | a, b | ''",
            "3ab + 2a                       | a, b | ''",
            "2α + β                         | α, β | ''",
            "e^x + π                        | x    | ''",
            "7                              | ''   | ''"
    })
    void reparteVariablesYConstantes(String texto, String variables, String constantes) {
        VariablePrincipal vp = VariablePrincipal.de(Parser.parse(texto));
        assertAll(
                () -> assertEquals(letras(variables), List.copyOf(vp.variables()), "variables"),
                () -> assertEquals(letras(constantes), List.copyOf(vp.constantes()), "constantes"));
    }

    @ParameterizedTest(name = "{0} -> variable [{1}], constantes [{2}]")
    @CsvSource(delimiter = '|', value = {
            "m x + b      | x | b, m",
            "a t^2 + b    | a | b, t",
            "θ^2 + 1      | θ | ''",
            "d/dt(a t^2)  | t | a",
            "5            | '' | ''"
    })
    void conUnaVariableUsaXOLaPrimeraLetra(String texto, String variable, String constantes) {
        VariablePrincipal vp = VariablePrincipal.conUnaVariable(Parser.parse(texto));
        assertAll(
                () -> assertEquals(letras(variable), List.copyOf(vp.variables()), "variable"),
                () -> assertEquals(letras(constantes), List.copyOf(vp.constantes()), "constantes"));
    }
}
