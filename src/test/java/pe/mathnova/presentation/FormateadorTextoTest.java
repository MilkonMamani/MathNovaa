package pe.mathnova.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A.4: del texto del dominio a escritura de pizarra. */
class FormateadorTextoTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "x^4 - 1                  | x⁴ − 1",
            "x^10 + x                 | x¹⁰ + x",
            "2*x*y                    | 2·x·y",
            "x >= 0, x <= 1, x != 2   | x ≥ 0, x ≤ 1, x ≠ 2",
            "-x^2 + 36                | -x² + 36",
            "(x - 1)(x + 1)           | (x − 1)(x + 1)",
            "x^2 - 2x + 1             | x² − 2x + 1",
            "x² − 1 ya formateado     | x² − 1 ya formateado"
    })
    void pizarra(String entrada, String esperado) {
        assertEquals(esperado, FormateadorTexto.pizarra(entrada));
    }

    /** HU37, HU38: el signo de un número negativo en soluciones y resultados. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "x₁ = -1/2 ≈ -0.5, x₂ = 2   | x₁ = −1/2 ≈ −0.5, x₂ = 2",
            "-3                         | −3",
            "2x + 1 = 0  →  x = -1/2    | 2x + 1 = 0  →  x = −1/2",
            "x = -1:  -2 = -2           | x = −1:  −2 = −2",
            "x^2 - 1 = 0                | x² − 1 = 0"
    })
    void pizarraConNegativos(String entrada, String esperado) {
        assertEquals(esperado, FormateadorTexto.pizarraConNegativos(entrada));
    }
}
