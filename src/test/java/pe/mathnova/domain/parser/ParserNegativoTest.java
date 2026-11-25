package pe.mathnova.domain.parser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ParserNegativoTest {

    private static final String RF62 = "Las sumatorias, productorias, integrales de línea y el valor absoluto "
            + "están fuera del alcance de MathNova";

    @ParameterizedTest(name = "[{0}] -> {1} (posición {2})")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "`2x +`  | Se esperaba un número, una variable o \"(\" y llegó el fin | 5",
            "`x^`    | Se esperaba un número, una variable o \"(\" y llegó el fin | 3",
            "`3ñ + 1`| 'ñ' en la posición 2 no es una letra permitida            | 2",
            "`sen`   | Se esperaba una variable y llegó el fin                     | 4",
            "`(x+1`  | Se esperaba \")\" y llegó el fin                            | 5",
            "`x # 2` | Carácter '#' no reconocido en la posición 3                 | 3",
            "`2 3`   | Sobra \"3\" al final                                        | 3",
            "`lim(x→a, x)` | El punto del límite debe ser un número, una fracción, un múltiplo de π o ±∞, no la letra 'a' | 7",
            "`lim(x→□, x)` | Completa la casilla vacía en la posición 7            | 7",
            "`d/dx()`      | Falta la expresión de la derivada                     | 6",
            "`d2/dx3(x)`   | El orden de arriba (2) y el de abajo (3) de la derivada no coinciden | 1",
            "`d/de(x)`     | 'e' es una constante; no puede ser la variable de la operación | 4",
            "`∫(x^2, π)`   | 'π' es una constante; no puede ser la variable de la operación | 8",
            "`∫(x^2)`      | Se esperaba \",\" y llegó \")\"                         | 6",
            "`∬(x y, x, 0, 1)` | ∬ necesita 2 variables con sus límites, como ∬(f, x, a, b, y, c, d) | 1",
            "`lim(x→∞+, x)`| Un límite al infinito no lleva lado (+ o -)            | 8",
            "`2 + lim(x→1, x)` | \"lim\" va sola: un límite, una derivada o una integral no se combina con otra expresión | 5",
            "`x + ∞`       | ∞ solo puede ir como punto de un límite              | 5",
            "`3X`          | 'X' en la posición 2 no es una letra permitida        | 2"
    })
    void rechaza(String entrada, String mensaje, int posicion) {
        ErrorSintaxis error = assertThrows(ErrorSintaxis.class, () -> Parser.parse(entrada));
        assertAll(
                () -> assertEquals(mensaje, error.getMessage()),
                () -> assertEquals(posicion, error.getPosicion()));
    }

    @ParameterizedTest(name = "[{0}] fuera de alcance (posición {1})")
    @CsvSource(delimiter = ';', quoteCharacter = '`', value = {
            "`Σ(x)`  ; 1",
            "`|x|`   ; 1",
            "`∏(x)`  ; 1",
            "`∮(x, x)`; 1",
            "`x + |x - 1|`; 5"
    })
    void rechazaLoQueEstaFueraDeAlcance(String entrada, int posicion) {
        ErrorSintaxis error = assertThrows(ErrorSintaxis.class, () -> Parser.parse(entrada));
        assertAll(
                () -> assertEquals(RF62, error.getMessage()),
                () -> assertEquals(posicion, error.getPosicion()));
    }
}
