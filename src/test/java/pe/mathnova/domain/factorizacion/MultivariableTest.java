package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.pizarra.ModeloAspaDobleXY;

class MultivariableTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    private static Resolucion resolver(String entrada) {
        return MOTOR.resolver(Parser.parse(entrada), Campo.Q);
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "6x^2y - 9xy^2                       ; 3xy(2x - 3y)              ; M01",
            "x^2 + xy + 2x + 2y                  ; (x + 2)(x + y)            ; M02",
            "x^2 - 4y^2                          ; (x - 2y)(x + 2y)          ; M03",
            "2x^2 + 5xy - 3y^2                   ; (2x - y)(x + 3y)          ; M04",
            "x^2 + 3xy + 2y^2 + 4x + 5y + 3      ; (x + y + 1)(x + 2y + 3)   ; M05",
            "x^3 - 7xy^2 + 6y^3                  ; (x - y)(x - 2y)(x + 3y)   ; M06",
            "x^3 - x^2y - xy^2 + y^3             ; (x - y)^2(x + y)          ; M07",
            "x^4 - y^4                           ; (x - y)(x + y)(x^2 + y^2) ; M03",
    })
    void ejemplosExactos(String entrada, String esperado, String metodo) {
        Resolucion r = resolver(entrada);
        assertAll(entrada + " → " + r.texto(),
                () -> assertEquals(ComparadorFactorizacion.leer(esperado), ComparadorFactorizacion.leer(r.texto())),
                () -> assertEquals(Polinomio.desde(Parser.parse(entrada)), r.resultado().expandir()),
                () -> assertEquals(metodo, r.pasos().getFirst().codigoTecnica()),
                () -> assertTrue(r.irreducibles().stream().allMatch(Irreducibilidad::certificada), r.irreducibles()::toString));
    }

    @Test
    void factorComunTomaElMcdYLaMenorPotencia() {
        Paso p = resolver("12x^3y^2z - 18x^2y^3").pasos().getFirst();
        assertEquals("M01", p.codigoTecnica());
        assertEquals("6x^2y^2(2xz - 3y)", p.despuesComoForma().aTexto());
    }

    @Test
    void aspaDobleVerificaLasTresAspas() {
        Paso p = resolver("x^2 + 3xy + 2y^2 + 4x + 5y + 3").pasos().getFirst();
        TrabajoIntermedio.AspaDoble a = assertInstanceOf(TrabajoIntermedio.AspaDoble.class, p.trabajo());
        ModeloAspaDobleXY m = ModeloAspaDobleXY.de(a);
        assertAll(
                () -> assertEquals(List.of("x", "y", "indep."), m.titulos()),
                () -> assertEquals(3, m.aspas().size()),
                () -> assertTrue(m.aspas().get(0).contains("= 3xy ✓"), m.aspas().get(0)),
                () -> assertTrue(m.aspas().get(1).contains("= 5y ✓"), m.aspas().get(1)),
                () -> assertTrue(m.aspas().get(2).contains("= 4x ✓"), m.aspas().get(2)),
                () -> assertEquals(ComparadorFactorizacion.leer("(x + y + 1)(x + 2y + 3)"),
                        ComparadorFactorizacion.leer(m.resultado().replace('−', '-'))));
    }

    @Test
    void homogeneoUsaTIgualAXSobreY() {
        Paso p = resolver("x^3 - 7xy^2 + 6y^3").pasos().getFirst();
        TrabajoIntermedio.Sustitucion s = assertInstanceOf(TrabajoIntermedio.Sustitucion.class, p.trabajo());
        assertEquals("t = x/y", s.definicion());
        assertTrue(s.sustituir().contains("y³(t³ − 7t + 6)"), s.sustituir());
    }

    @Test
    void ruffiniConYComoParametro() {
        Paso p = resolver("x^3 - x^2y - xy^2 + y^3").pasos().getFirst();
        List<String> l = assertInstanceOf(TrabajoIntermedio.Agrupacion.class, p.trabajo()).grupos();
        assertTrue(l.getFirst().contains("x = y anula el polinomio"), l.getFirst());
        assertTrue(l.stream().anyMatch(s -> s.contains("vuelve a anular")), l.toString());
        assertTrue(r("x^3 - x^2y - xy^2 + y^3").descartados().stream().anyMatch(d -> d.codigoTecnica().equals("M06")),
                "M06 se prueba antes y se descarta por la raíz repetida");
    }

    private static Resolucion r(String e) {
        return resolver(e);
    }

    @Test
    void a6CertificaLosFactoresFinales() {
        Irreducibilidad i = resolver("x^2 + y^2").irreducibles().getFirst();
        assertEquals("A6", i.codigoTecnica());
        assertTrue(i.certificada());
        assertTrue(i.justificacion().startsWith("Reducción de Kronecker: con y = x³"), i.justificacion());
        Irreducibilidad tres = resolver("2xz - 3y").irreducibles().getFirst();
        assertTrue(tres.certificada(), tres.justificacion());
    }

    @Test
    void a6EncuentraUnFactorSiLoHay() {
        assertFalse(AnalisisIrreducibilidad.analizar(Polinomio.desde(Parser.parse("x^2 - y^2"))).certificada());
    }

    @Test
    void coeficientesIndeterminadosDeRespaldo() {
        Resolucion r = resolver("(x^2 + y + 1)(x + y^2 + 2)");
        assertEquals(ComparadorFactorizacion.leer("(x^2 + y + 1)(x + y^2 + 2)"), ComparadorFactorizacion.leer(r.texto()));
        assertTrue(r.pasos().stream().anyMatch(p -> p.codigoTecnica().equals("M08")),
                () -> r.pasos().stream().map(Paso::codigoTecnica).toList().toString());
    }

    @Test
    void lasAlternativasSonDeVariasVariables() {
        Resolucion r = resolver("x^2 - 4y^2");
        assertTrue(r.alternativas().stream().allMatch(c -> c.startsWith("M")), r.alternativas().toString());
    }
}
