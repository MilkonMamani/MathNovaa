package pe.mathnova.domain.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.parser.Parser;

class PolinomioBancoTest {

    private static final Pattern FUNCIONES = Pattern.compile("sen|cos|tan|ln|log|e\\^");

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas().stream()
                .filter(f -> f.campo().equals("Q"))
                .filter(f -> !FUNCIONES.matcher(f.entrada() + " " + f.resultadoEsperado()).find())
                .filter(f -> !f.metodo().equals("P01") && !f.metodo().equals("A4"))
                .toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filas")
    void expandirLaFactorizacionDaLaEntrada(BancoCsv.Fila f) {
        Polinomio entrada = Polinomio.desde(Parser.parse(f.entrada()));
        Polinomio esperado = Polinomio.desde(Parser.parse(f.resultadoSinNota()));
        assertEquals(entrada, esperado,
                () -> f.id() + " " + f.metodo() + ": " + f.entrada() + " ≠ " + f.resultadoSinNota());
    }

    @Test
    void cubreDoscientasTreceFilas() {
        assertEquals(213, filas().size());
    }
}
