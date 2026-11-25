package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;

class BancoSprint2Test {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas().stream().filter(BancoSprint2Test::esDelSprint).toList();
    }

    private static boolean esDelSprint(BancoCsv.Fila f) {
        String m = f.metodo();
        return m.matches("F\\d\\d") && Integer.parseInt(m.substring(1)) <= 13;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filas")
    void factorizaComoElBanco(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.valueOf(f.campo()));
        assertEquals(ComparadorFactorizacion.leer(f.resultadoSinNota()), ComparadorFactorizacion.leer(obtenido),
                () -> f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido
                        + " (banco: " + f.resultadoSinNota() + ")");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filas")
    void elTextoSeVuelveALeer(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        Polinomio leido = assertDoesNotThrow(() -> Polinomio.desde(Parser.parse(obtenido)), obtenido);
        assertEquals(Polinomio.desde(Parser.parse(f.entrada())), leido, obtenido);
    }

    @Test
    void sonCientoTreintaYUna() {
        assertEquals(131, filas().size());
    }
}
