package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
 * Definición de terminado 1 del Sprint 4: las filas con método principal F01 a F30, A4, A5, P01 y
 * T01 a T05, en su campo. Q como en el Sprint 3; R y C por estructura con la comparación de la Parte E
 * (exacta, o numérica con 40 cifras si hay radicales anidados) y con la verificación del resultado;
 * A5 con las mismas raíces aproximadas; T01–T05 por estructura con cada función como una letra.
 */
class BancoSprint4Test {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizadorFunciones FUNCIONES = new FactorizadorFunciones(MOTOR);

    static List<BancoCsv.Fila> filas() {
        return BancoCsv.filas().stream().filter(BancoSprint4Test::esDelSprint).toList();
    }

    private static boolean esDelSprint(BancoCsv.Fila f) {
        String m = f.metodo();
        return m.equals("A4") || m.equals("A5") || m.equals("P01")
                || m.matches("F\\d\\d") && Integer.parseInt(m.substring(1)) <= 30
                || m.matches("T0[1-5]");
    }

    static List<BancoCsv.Fila> enQ() {
        return filas().stream().filter(f -> f.metodo().startsWith("F") && f.campo().equals("Q")).toList();
    }

    static List<BancoCsv.Fila> enRyC() {
        return filas().stream().filter(f -> f.metodo().startsWith("F") && !f.campo().equals("Q")).toList();
    }

    static List<BancoCsv.Fila> conMetodo(String m) {
        return filas().stream().filter(f -> f.metodo().equals(m)).toList();
    }

    static List<BancoCsv.Fila> irreducibles() {
        return conMetodo("A4");
    }

    static List<BancoCsv.Fila> aproximadas() {
        return conMetodo("A5");
    }

    static List<BancoCsv.Fila> fracciones() {
        return conMetodo("P01");
    }

    static List<BancoCsv.Fila> funciones() {
        return filas().stream().filter(f -> f.metodo().startsWith("T")).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("enQ")
    void factorizaEnQ(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        assertEquals(ComparadorFactorizacion.leer(f.resultadoSinNota()), ComparadorFactorizacion.leer(obtenido),
                () -> f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido);
        assertEquals(Polinomio.desde(Parser.parse(f.entrada())), Polinomio.desde(Parser.parse(obtenido)), f.id());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("enRyC")
    void factorizaEnRyC(BancoCsv.Fila f) {
        Campo campo = Campo.valueOf(f.campo());
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), campo);
        String obtenido = r.texto();
        assertAll(f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido + " (banco: " + f.resultadoSinNota() + ")",
                () -> assertTrue(ComparadorFactorizacion.igualesEnCampo(
                        ComparadorFactorizacion.leerEnCampo(f.resultadoSinNota()),
                        ComparadorFactorizacion.leerEnCampo(obtenido)), "estructura"),
                () -> assertTrue(VerificacionCampo.de(r).coincide(), "verificación (Parte E)"),
                () -> assertEquals(campo == Campo.R ? "PR" : "PC", r.extension().pasos().getFirst().codigoTecnica(),
                        "primero Q, luego pasar al campo"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("irreducibles")
    void certificaLasIrreducibles(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.Q);
        assertAll(f.id() + " " + f.entrada(),
                () -> assertTrue(r.pasos().isEmpty()),
                () -> assertEquals(1, r.irreducibles().size()),
                () -> assertTrue(r.irreducibles().getFirst().certificada()));
    }

    /** "Irreducible en Q; raíces reales ≈ -1.5185121528, 0.5084994847, 1.2435963906". */
    @ParameterizedTest(name = "{0}")
    @MethodSource("aproximadas")
    void aproximaLasRaices(BancoCsv.Fila f) {
        Resolucion r = MOTOR.resolver(Parser.parse(f.entrada()), Campo.valueOf(f.campo()));
        String obtenido = r.texto();
        assertAll(f.id() + " " + f.entrada() + " → " + obtenido,
                () -> assertTrue(obtenido.startsWith("Irreducible en Q; raíces reales ≈ ")),
                () -> assertEquals(raices(f.resultadoEsperado()), raices(obtenido)),
                () -> assertTrue(r.extension().aproximada()),
                () -> assertTrue(r.extension().pasos().stream().anyMatch(p -> p.codigoTecnica().equals("A5"))));
    }

    private static List<BigDecimal> raices(String texto) {
        String lista = texto.substring(texto.indexOf('≈') + 1);
        return Arrays.stream(lista.split(",")).map(String::trim).map(BigDecimal::new).toList();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fracciones")
    void simplificaLasFracciones(BancoCsv.Fila f) {
        String obtenido = MOTOR.factorizar(f.entrada(), Campo.Q);
        String[] esperado = f.resultadoEsperado().split(";", 2);
        String[] partes = obtenido.split(";", 2);
        assertAll(f.id() + " " + f.entrada() + " → " + obtenido,
                () -> assertEquals(ComparadorFactorizacion.leerFraccion(esperado[0].trim()),
                        ComparadorFactorizacion.leerFraccion(partes[0].trim())),
                () -> assertEquals(restricciones(esperado[1]), restricciones(partes.length > 1 ? partes[1] : "")));
    }

    private static Set<String> restricciones(String texto) {
        return Arrays.stream(texto.split(","))
                .map(s -> s.replace('−', '-').replaceAll("\\s+", ""))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /** T01–T05: cada función de x pasa a una letra y se compara como en Q. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("funciones")
    void factorizaConFunciones(BancoCsv.Fila f) {
        ResolucionFunciones r = FUNCIONES.resolver(Parser.parse(f.entrada()));
        String obtenido = r.formula();
        var letras = new java.util.HashMap<String, Character>();
        assertAll(f.id() + " " + f.metodo() + ": " + f.entrada() + " → " + obtenido + " (banco: " + f.resultadoSinNota() + ")",
                () -> assertTrue(r.factorizable(), () -> r.mensaje()),
                () -> assertEquals(ComparadorFactorizacion.leer(SustitucionFunciones.aLetras(Parser.parse(f.resultadoSinNota()), letras)),
                        ComparadorFactorizacion.leer(SustitucionFunciones.aLetras(Parser.parse(obtenido), letras))),
                () -> assertEquals(f.metodo(), r.codigo()));
    }

    /**
     * El enunciado dice 285, pero el CSV tiene 275 filas con esos métodos: 188 en Q (F01–F20), 40 en R y C
     * (F21–F30), 6 de A4, 4 de A5, 10 de P01 y 27 de T01–T05. Las 25 restantes son M01–M07.
     */
    @Test
    void sonDoscientasSetentaYCinco() {
        assertEquals(275, filas().size());
        assertEquals(188, enQ().size());
        assertEquals(40, enRyC().size());
        assertEquals(6, irreducibles().size());
        assertEquals(4, aproximadas().size());
        assertEquals(10, fracciones().size());
        assertEquals(27, funciones().size());
    }
}
