package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;

/**
 * Definición de terminado 1 del Sprint 3: todas las filas del grupo Q con método principal F01 a
 * F20, A4 o P01. Las factorizaciones se comparan por estructura (ComparadorFactorizacion); las
 * irreducibles deben quedar certificadas; las fracciones, con el mismo resultado y las mismas
 * restricciones (RN14).
 */
class BancoSprint3Test {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas().stream().filter(BancoSprint3Test::esDelSprint).toList();
    }

    static List<BancoCsv.Fila> factorizaciones() {
        return filas().stream().filter(f -> f.metodo().startsWith("F")).toList();
    }

    static List<BancoCsv.Fila> irreducibles() {
        return filas().stream().filter(f -> f.metodo().equals("A4")).toList();
    }

    static List<BancoCsv.Fila> fracciones() {
        return filas().stream().filter(f -> f.metodo().equals("P01")).toList();
    }

    private static boolean esDelSprint(BancoCsv.Fila f) {
        String m = f.metodo();
        if (!f.campo().equals("Q")) {
            return false;
        }
        return m.equals("A4") || m.equals("P01") || m.matches("F\\d\\d") && Integer.parseInt(m.substring(1)) <= 20;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("factorizaciones")
    void factorizaComoElBanco(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        assertEquals(ComparadorFactorizacion.leer(f.resultadoSinNota()), ComparadorFactorizacion.leer(obtenido),
                () -> f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido
                        + " (banco: " + f.resultadoSinNota() + ")");
        Polinomio leido = Polinomio.desde(Parser.parse(obtenido));
        assertEquals(Polinomio.desde(Parser.parse(f.entrada())), leido, () -> f.id() + ": HU18");
    }

    /** "Irreducible en Q: …": sin pasos, el resultado es la entrada y queda certificado. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("irreducibles")
    void certificaLasIrreducibles(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.Q);
        assertAll(f.id() + " " + f.entrada(),
                () -> assertTrue(r.pasos().isEmpty(), () -> "pasos: " + r.pasos()),
                () -> assertEquals(Polinomio.desde(Parser.parse(f.entrada())), r.resultado().expandir()),
                () -> assertEquals(1, r.irreducibles().size()),
                () -> assertTrue(r.irreducibles().stream().allMatch(Irreducibilidad::certificada),
                        () -> r.irreducibles().toString()));
    }

    /** "(x + 2)/(x + 3); x ≠ -3, x ≠ 2". */
    @ParameterizedTest(name = "{0}")
    @MethodSource("fracciones")
    void simplificaLasFracciones(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        String[] esperado = f.resultadoEsperado().split(";", 2);
        String[] partes = obtenido.split(";", 2);
        assertAll(f.id() + " " + f.entrada() + " → " + obtenido,
                () -> assertEquals(ComparadorFactorizacion.leerFraccion(esperado[0].trim()),
                        ComparadorFactorizacion.leerFraccion(partes[0].trim()), "fracción"),
                () -> assertEquals(restricciones(esperado[1]), restricciones(partes.length > 1 ? partes[1] : ""),
                        "restricciones"));
    }

    private static Set<String> restricciones(String texto) {
        return Arrays.stream(texto.split(","))
                .map(s -> s.replace('−', '-').replaceAll("\\s+", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /** El enunciado del sprint dice 214, pero el CSV tiene 204 filas en el grupo Q (F01–F20, A4, P01). */
    @Test
    void sonDoscientasCuatro() {
        assertEquals(204, filas().size());
        assertEquals(188, factorizaciones().size());
        assertEquals(6, irreducibles().size());
        assertEquals(10, fracciones().size());
    }
}
