package pe.mathnova.domain.calculo.derivadas;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parte A del Sprint 5: el derivador interno que usa L'Hôpital. */
class DerivadorTest {

    @ParameterizedTest(name = "({0})'' respecto de {1} = {2}")
    @CsvSource(delimiter = '|', value = {
            "x^2*sen(x)        | x | 2x*sen(x) + x^2*cos(x)",
            "e^x               | x | e^x",
            "ln(x)             | x | 1/x",
            "sen(3x)           | x | 3cos(3x)",
            "a*t^2             | t | 2a*t",
            "√x                | x | 1/(2√x)",
            "tan(x)            | x | sec(x)^2",
            "5                 | x | 0",
            "x                 | x | 1",
            "x^5               | x | 5x^4",
            "x^(1/2)           | x | x^(-1/2)/2",
            "3x^2 + 2x + 1     | x | 6x + 2",
            "x^3 - 4x          | x | 3x^2 - 4",
            "cos(x)            | x | -sen(x)",
            "(x + 1)/(x - 1)   | x | -2/(x - 1)^2",
            "e^(2x)            | x | 2e^(2x)",
            "ln(x^2 + 1)       | x | 2x/(x^2 + 1)",
            "2^x               | x | 2^x*ln(2)",
            "log_2 x           | x | 1/(x*ln(2))",
            "arctan(x)         | x | 1/(1 + x^2)",
            "arcsen(x)         | x | 1/√(1 - x^2)",
            "∛x                | x | 1/(3∛(x^2))",
            "sen(x)*cos(x)     | x | cos(x)^2 - sen(x)^2",
            "x^x               | x | x^x*(ln(x) + 1)",
            "sec(x)            | x | sec(x)*tan(x)",
    })
    void deriva(String entrada, char variable, String esperado) {
        Expresion derivada = Derivador.derivar(Parser.parse(entrada), variable).derivada();
        Expresion esperada = Simplificador.simplificar(Parser.parse(esperado));
        assertEquals(esperada, derivada, () -> "salió " + ImpresoraTexto.imprimir(derivada));
        // Comprobación numérica en cinco puntos (dos son negativos: x^x y x^(-1/2) no están definidas ahí).
        if (Expresiones.variables(esperada).size() <= 1 && !entrada.equals("x^x") && !entrada.equals("x^(1/2)")) {
            assertTrue(EvaluacionNumerica.iguales(derivada, esperada, variable));
        }
    }

    @Test
    void cadaReglaQuedaComoPaso() {
        List<String> codigos = Derivador.derivar(Parser.parse("x^2*sen(x)"), 'x').pasos().stream()
                .map(PasoDerivada::codigo).toList();
        assertEquals(List.of("D04", "D02", "D07"), codigos);

        PasoDerivada cadena = Derivador.derivar(Parser.parse("sen(3x)"), 'x').pasos().getFirst();
        assertEquals("D06", cadena.codigo());
        assertTrue(cadena.regla().contains("u = 3x"), cadena.regla());
        assertEquals(Parser.parse("sen(3x)"), cadena.antes());
    }

    @Test
    void lasOtrasLetrasSonConstantes() {
        PasoDerivada p = Derivador.derivar(Parser.parse("a*t^2"), 't').pasos().getFirst();
        assertTrue(p.regla().contains("a es constante respecto de t"), p.regla());
        assertEquals(Parser.parse("0"), Derivador.derivar(Parser.parse("a^2 + 3"), 't').derivada());
    }

    @Test
    void unaOperacionNoSeDeriva() {
        assertThrows(IllegalArgumentException.class,
                () -> Derivador.derivar(Parser.parse("lim(x→2, x^2)"), 'x'));
    }

    @Test
    void elSimplificadorReuneYMultiplica() {
        assertEquals(Simplificador.simplificar(Parser.parse("5x")), Simplificador.simplificar(Parser.parse("3x + 2x")));
        assertEquals(Parser.parse("x"), Simplificador.simplificar(Parser.parse("1*x^1")));
        assertEquals(Parser.parse("1"), Simplificador.simplificar(Parser.parse("x^0")));
        assertEquals(Parser.parse("0"), Simplificador.simplificar(Parser.parse("0*sen(x)")));
        assertEquals(Simplificador.simplificar(Parser.parse("x^3")), Simplificador.simplificar(Parser.parse("x*x^2")));
        assertEquals(Simplificador.simplificar(Parser.parse("6x")), Simplificador.simplificar(Parser.parse("2*3*x")));
    }
}
