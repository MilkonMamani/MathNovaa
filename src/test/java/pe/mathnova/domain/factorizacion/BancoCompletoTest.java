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

/**
 * Definición de terminado 1 del Sprint 7: el banco completo, las 300 filas, cada una con su comprobación
 * (ComprobacionBanco). Corre con "mvn test", sin perfil.
 */
class BancoCompletoTest {

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filas")
    void pasa(BancoCsv.Fila f) {
        ComprobacionBanco.comprobar(f);
    }

    /** 275 de los Sprints 2–4 y 25 de varias variables (M01–M07). */
    @Test
    void sonTrescientas() {
        assertEquals(300, filas().size());
        Map<String, Long> m = filas().stream().filter(f -> f.metodo().startsWith("M"))
                .collect(Collectors.groupingBy(BancoCsv.Fila::metodo, TreeMap::new, Collectors.counting()));
        assertEquals(Map.of("M01", 4L, "M02", 4L, "M03", 5L, "M04", 4L, "M05", 3L, "M06", 3L, "M07", 2L), m);
    }
}
