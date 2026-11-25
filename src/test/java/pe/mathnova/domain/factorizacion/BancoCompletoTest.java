package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;

class BancoCompletoTest {

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filas")
    void pasa(BancoCsv.Fila f) {
        ComprobacionBanco.comprobar(f);
    }

    @Test
    void sonTrescientas() {
        assertEquals(300, filas().size());
        Map<String, Long> m = filas().stream().filter(f -> f.metodo().startsWith("M"))
                .collect(Collectors.groupingBy(BancoCsv.Fila::metodo, TreeMap::new, Collectors.counting()));
        assertEquals(Map.of("M01", 4L, "M02", 4L, "M03", 5L, "M04", 4L, "M05", 3L, "M06", 3L, "M07", 2L), m);
    }
}
