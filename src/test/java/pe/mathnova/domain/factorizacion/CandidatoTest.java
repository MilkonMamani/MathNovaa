package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.p;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;

/** esCandidato: solo se prueban (y se listan como descartados) los métodos que tienen sentido por la forma. */
class CandidatoTest {

    @Test
    void cubicaDeCuatroTerminos() {
        Polinomio cubica = p("2x^3 - 3x^2 - 3x + 2");
        assertAll(
                () -> assertFalse(new Reciproco().esCandidato(cubica), "recíproco: grado impar"),
                () -> assertFalse(new QuitaYPon().esCandidato(cubica), "quita y pon: no es de grado 4k"),
                () -> assertFalse(new CambioDeVariable().esCandidato(cubica), "cambio de variable: no es trinomio"),
                () -> assertFalse(new TrinomioCuadradoPerfecto().esCandidato(cubica), "TCP: no tiene 3 términos"),
                () -> assertTrue(new CuboPerfecto().esCandidato(cubica), "cubo perfecto: 4 términos y grado 3"),
                () -> assertTrue(new AgrupacionDeTerminos().esCandidato(cubica)),
                () -> assertTrue(new Ruffini().esCandidato(cubica)));
    }

    @Test
    void porForma() {
        assertAll(
                () -> assertFalse(new CuboPerfecto().esCandidato(p("x^4 + 3x^3 + 3x + 1")), "grado 4"),
                () -> assertFalse(new CuboPerfecto().esCandidato(p("x^3 + 1")), "2 términos"),
                () -> assertTrue(new QuitaYPon().esCandidato(p("x^4 + 4"))),
                () -> assertFalse(new QuitaYPon().esCandidato(p("x^4 + x^3 + x^2 + 1")), "4 términos"),
                () -> assertFalse(new QuitaYPon().esCandidato(p("x^6 + 1")), "grado 6"),
                () -> assertTrue(new Reciproco().esCandidato(p("x^4 + 3x^3 + 4x^2 + 3x + 1"))),
                () -> assertTrue(new TrinomioCuadradoPerfecto().esCandidato(p("x^2 + 2x + 1"))),
                () -> assertFalse(new TrinomioCuadradoPerfecto().esCandidato(p("x^2 - 1"))));
    }

    /** El orquestador no prueba ni lista como descartado lo que no es candidato. */
    @Test
    void losDescartadosSoloTraenCandidatos() {
        OrquestadorFactorizacion motor = new OrquestadorFactorizacion();
        Resolucion r = motor.resolver(Parser.parse("2x^3 - 3x^2 - 3x + 2"), Campo.Q);
        List<String> sobreLaEntrada = r.descartados().stream()
                .filter(d -> d.antes().equals(p("2x^3 - 3x^2 - 3x + 2"))).map(Paso::codigoTecnica).toList();
        assertEquals(List.of("F01", "F10", "F04"), sobreLaEntrada);

        // x⁴ + x + 1: trinomio de grado 4 → sin F10, F04 ni F11; sí los que tienen sentido.
        List<String> cuarta = motor.resolver(Parser.parse("x^4 + x + 1"), Campo.Q).descartados().stream()
                .map(Paso::codigoTecnica).toList();
        assertEquals(List.of("F01", "F07", "F14", "F15", "F18", "F16", "F19", "F20", "A4"), cuarta);
    }
}
