package pe.mathnova.domain.ecuacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.NoEsPolinomioException;
import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion.PasoEcuacion;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion.Solucion;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

class ResolvedorEcuacionesTest {

    private static final ResolvedorEcuaciones RESOLVEDOR = new ResolvedorEcuaciones(new OrquestadorFactorizacion());

    private static ResolucionEcuacion resolver(String texto) {
        return RESOLVEDOR.resolver(assertInstanceOf(Ecuacion.class, Parser.parse(texto)));
    }

    private static List<String> valores(ResolucionEcuacion r) {
        return r.soluciones().stream().map(s -> s.valor().toString()).toList();
    }

    private static List<PasoEcuacion.Tipo> tipos(ResolucionEcuacion r) {
        return r.pasos().stream().map(PasoEcuacion::tipo).toList();
    }

    @Test
    void cuadraticaIgualadaACero() {
        ResolucionEcuacion r = resolver("x^2 - 5x + 6 = 0");
        assertEquals(List.of("2", "3"), valores(r));
        assertEquals("x₁ = 2, x₂ = 3", r.resumen());
        assertEquals(List.of(PasoEcuacion.Tipo.FACTORIZAR, PasoEcuacion.Tipo.IGUALAR_A_CERO, PasoEcuacion.Tipo.DESPEJAR),
                tipos(r));
        assertEquals(List.of("x - 3 = 0", "x - 2 = 0").stream().sorted().toList(),
                r.pasos().get(1).lineas().stream().sorted().toList());
    }

    @Test
    void cubicaConLaIncognitaALaDerecha() {
        ResolucionEcuacion r = resolver("x^3 = x");
        assertEquals(List.of("-1", "0", "1"), valores(r));
        assertEquals("x₁ = -1, x₂ = 0, x₃ = 1", r.resumen());
        PasoEcuacion primero = r.pasos().getFirst();
        assertEquals("Pasar todo a un lado", primero.titulo());
        assertEquals(List.of("x^3 - x = 0"), primero.lineas());
        assertEquals("x(x + 1)(x - 1) = 0", r.pasos().get(1).lineas().getFirst());
    }

    @Test
    void coeficientePrincipalDistintoDeUno() {
        ResolucionEcuacion r = resolver("2x^2 = 3x + 2");
        assertEquals(List.of("-1/2", "2"), valores(r));
        assertEquals("x₁ = -1/2, x₂ = 2", r.resumen());
        assertEquals("2x^2 - 3x - 2 = 0", r.pasos().getFirst().lineas().get(1));
        assertEquals("2x^2 - (3x + 2) = 0", r.pasos().getFirst().lineas().getFirst());
    }

    @Test
    void raizDobleSeMuestraUnaVez() {
        ResolucionEcuacion r = resolver("(x - 1)^2 = 0");
        assertEquals(List.of(new Solucion(Racional.UNO, 2)), r.soluciones());
        assertEquals("x = 1 (doble)", r.resumen());
        assertEquals("doble", r.soluciones().getFirst().nombreMultiplicidad());
        assertTrue(r.pasos().get(1).lineas().contains("x - 1 = 0 (doble)"), r.pasos().get(1).lineas().toString());
    }

    @Test
    void factorSinRaizEnQSeInforma() {
        ResolucionEcuacion r = resolver("x^3 + x = 0");
        assertEquals(List.of("0"), valores(r));
        assertEquals(1, r.sinSolucion().size());
        assertTrue(r.pasos().getLast().lineas().contains("x^2 + 1 = 0 no tiene solución en Q"),
                r.pasos().getLast().lineas().toString());
    }

    @Test
    void sinNingunaSolucionEnQ() {
        ResolucionEcuacion r = resolver("x^2 + 1 = 0");
        assertEquals(ResolucionEcuacion.Tipo.SIN_SOLUCION, r.tipo());
        assertEquals("No tiene solución en Q", r.resumen());
    }

    @Test
    void identidadYContradiccion() {
        assertEquals(ResolucionEcuacion.Tipo.IDENTIDAD, resolver("x + 1 = 1 + x").tipo());
        assertEquals(ResolucionEcuacion.Tipo.SIN_SOLUCION, resolver("x + 1 = x + 2").tipo());
    }

    @Test
    void lineal() {
        ResolucionEcuacion r = resolver("3x - 6 = 0");
        assertEquals(List.of("2"), valores(r));
        assertEquals("x = 2", r.resumen());
    }

    @Test
    void otraLetra() {
        assertEquals("t₁ = -2, t₂ = 2", resolver("t^2 = 4").resumen());
    }

    @Test
    void conMetodoForzado() {
        ResolucionEcuacion r = RESOLVEDOR.resolver((Ecuacion) Parser.parse("x^2 - 5x + 6 = 0"), "F13");
        assertEquals(List.of("2", "3"), valores(r));
        assertEquals("F13", r.factorizacion().pasos().getFirst().codigoTecnica());
    }

    @Test
    void dosIncognitasNoSeResuelven() {
        assertThrows(NoEsPolinomioException.class, () -> resolver("x + y = 1"));
    }

    @Test
    void noPolinomicaNoSeResuelve() {
        assertThrows(NoEsPolinomioException.class, () -> resolver("sen(x) = 0"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x^2 - 5x + 6 = 0 | x^2 - 5x + 6 = 0",
            "2x^2=3x+2        | 2x^2 = 3x + 2",
            "(x - 1)^2 = 0    | (x - 1)^2 = 0"
    })
    void elParserLeeLaEcuacion(String entrada, String impreso) {
        assertEquals(impreso, ImpresoraTexto.imprimir(Parser.parse(entrada)));
    }

    @Test
    void latexDeLaEcuacion() {
        assertEquals("x^{2} - 1 = 0", ImpresoraLatex.imprimir(Parser.parse("x^2 - 1 = 0")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "x = 1 = 2      | Una ecuación lleva un solo \"=\"",
            "x^2 =          | Falta el lado derecho de la ecuación",
            "x == 1         | Falta el lado derecho de la ecuación",
            "x = lim(x→1, x) | va sola"
    })
    void rechazaEcuacionesMalEscritas(String entrada, String mensaje) {
        ErrorSintaxis e = assertThrows(ErrorSintaxis.class, () -> Parser.parse(entrada));
        assertTrue(e.getMessage().contains(mensaje), e.getMessage());
    }

    @Test
    void elIgualNoVaDentroDeUnParentesis() {
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("(x = 1)"));
    }
}
