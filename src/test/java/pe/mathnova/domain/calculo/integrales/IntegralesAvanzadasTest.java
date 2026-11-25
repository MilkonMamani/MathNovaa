package pe.mathnova.domain.calculo.integrales;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.calculo.CaminoCalculo;
import pe.mathnova.domain.calculo.derivadas.Derivador;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;

class IntegralesAvanzadasTest {

    private static final MotorIntegrales MOTOR = new MotorIntegrales(new OrquestadorFactorizacion());

    private static ResolucionIntegral resolver(String texto) {
        return MOTOR.resolver(Parser.parse(texto));
    }

    private static void derivadaEsElIntegrando(ResolucionIntegral r, String puntos) {
        assertTrue(r.resuelta(), () -> "no se resolvió: " + r.texto());
        char x = r.variable();
        Expresion derivada = Derivador.derivar(r.antiderivada(), x).derivada();
        Expresion f = r.integral().cuerpo();
        for (String punto : puntos.split(" ")) {
            ComplejoDecimal v = ComplejoDecimal.real(new BigDecimal(punto));
            ComplejoDecimal y1 = EvaluacionNumerica.evaluar(derivada, Map.of(x, v), EvaluacionNumerica.MC);
            ComplejoDecimal y2 = EvaluacionNumerica.evaluar(f, Map.of(x, v), EvaluacionNumerica.MC);
            assertTrue(y1.cercano(y2, 30, EvaluacionNumerica.MC),
                    () -> r.texto() + ": en x = " + punto + " F′ = " + y1 + " y f = " + y2);
        }
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "∫(x e^x, x)                 ; (x − 1)eˣ + C                              ; I03 ; 0.5 1.3 -2.1 3",
            "∫(x sen x, x)               ; −x·cos x + sen x + C                       ; I03 ; 0.5 1.3 -2.1 3",
            "∫(ln x, x)                  ; x(ln x − 1) + C                            ; I03 ; 0.5 1.3 2.7 9",
            "∫(x^2 e^x, x)               ; (x² − 2x + 2)eˣ + C                        ; I03 ; 0.5 1.3 -2.1 3",
            "∫(e^x sen x, x)             ; (eˣ/2)(sen x − cos x) + C                  ; I03 ; 0.5 1.3 -2.1 3",
            "∫(arctan x, x)              ; x·arctan x − (1/2)ln(x² + 1) + C           ; I03 ; 0.5 1.3 -2.1 3",
            "∫(x ln x, x)                ; (x²/4)(2ln x − 1) + C                      ; I03 ; 0.5 1.3 2.7 9",
            "∫(sen^2 x, x)               ; x/2 − sen(2x)/4 + C                        ; I04 ; 0.5 1.3 -2.1 3",
            "∫(sen^3 x, x)               ; −cos x + cos³x/3 + C                       ; I04 ; 0.5 1.3 -2.1 3",
            "∫(sen^2 x cos^3 x, x)       ; sen³x/3 − sen⁵x/5 + C                      ; I04 ; 0.5 1.3 -2.1 3",
            "∫(tan^2 x, x)               ; tan x − x + C                              ; I04 ; 0.5 1.3 -2.1 3",
            "∫(sec^3 x, x)               ; (1/2)(sec x·tan x + ln|sec x + tan x|) + C ; I04 ; 0.5 1.3 -2.1 3",
            "∫(√(1 - x^2), x)            ; (1/2)(arcsen x + x√(1 − x²)) + C           ; I05 ; 0.5 -0.3 0.9 0.1",
            "∫(1/√(4 + x^2), x)          ; ln|x + √(x² + 4)| + C                      ; I05 ; 0.5 1.3 -2.1 3",
            "∫(√(x^2 - 9)/x, x)          ; √(x² − 9) − 3·arcsec(x/3) + C              ; I05 ; 3.5 4 7.25 10",
            "∫(1/(x^2 √(9 - x^2)), x)    ; −√(9 − x²)/(9x) + C                        ; I05 ; 0.5 1.3 -2.1 2.9",
    })
    void ejemplosExactos(String entrada, String delEnunciado, String metodo, String puntos) {
        ResolucionIntegral r = resolver(entrada);
        assertAll(entrada + " → " + r.texto() + " (enunciado: " + delEnunciado + ")",
                () -> assertEquals(metodo, r.metodo()),
                () -> derivadaEsElIntegrando(r, puntos),
                () -> assertTrue(r.verificada(), () -> "la verificación del motor no pasó: " + r.verificacion()));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "∫(x e^x, x, 0, 1)     ; 1   ; I03",
            "∫(sen^2 x, x, 0, π)   ; π/2 ; I04",
    })
    void definidas(String entrada, String valor, String metodo) {
        ResolucionIntegral r = resolver(entrada);
        assertEquals(valor, r.texto());
        assertEquals(metodo, r.metodo());
        assertEquals(MotorIntegrales.BARROW, r.pasos().getLast().codigo());
    }

    @Test
    void laTablaDePartesMuestraUduDvYv() {
        ResolucionIntegral r = resolver("∫(x ln x, x)");
        TrabajoIntermedio.TablaPartes t = assertInstanceOf(TrabajoIntermedio.TablaPartes.class,
                r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("ln x", t.u()),
                () -> assertEquals("x dx", t.dv()),
                () -> assertEquals("x²/2", t.v()),
                () -> assertTrue(t.du().contains("1/x"), t.du()),
                () -> assertTrue(t.ilate().contains("logarítmica (L)") && t.ilate().contains("algebraica (A)"),
                        t.ilate()));
    }

    @Test
    void unSoloFactorVaConDvIgualADx() {
        TrabajoIntermedio.TablaPartes t = (TrabajoIntermedio.TablaPartes) resolver("∫(arctan x, x)").pasos().getFirst()
                .trabajo();
        assertEquals("arctan x", t.u());
        assertEquals("dx", t.dv());
        assertEquals("x", t.v());
    }

    @Test
    void lasPartesRepetidasVanAnidadas() {
        ResolucionIntegral r = resolver("∫(x^2 e^x, x)");
        PasoLimite primero = r.pasos().getFirst();
        assertEquals("I03", primero.codigo());
        assertTrue(primero.hijos().stream().anyMatch(h -> h.codigo().equals("I03")),
                "la segunda vez va dentro de la primera");
        assertEquals(2, r.codigos().stream().filter("I03"::equals).count());
    }

    @Test
    void laCiclicaSeDespejaComoEcuacion() {
        ResolucionIntegral r = resolver("∫(e^x sen x, x)");
        PasoLimite despeje = r.pasos().getLast();
        List<String> lineas = ((TrabajoIntermedio.Agrupacion) despeje.trabajo()).grupos();
        assertTrue(lineas.getFirst().startsWith("Reaparece la integral original: I = "), lineas.getFirst());
        assertTrue(lineas.getFirst().contains(" − I, entonces 2I = "), lineas.getFirst());
    }

    @Test
    void ilateEligeLaTrigonometricaAntesQueLaExponencial() {
        TrabajoIntermedio.TablaPartes t = (TrabajoIntermedio.TablaPartes) resolver("∫(e^x sen x, x)").pasos()
                .getFirst().trabajo();
        assertEquals("sen x", t.u());
    }

    @Test
    void sustitucionTrigonometricaDibujaElTriangulo() {
        ResolucionIntegral r = resolver("∫(1/(x^2 √(9 - x^2)), x)");
        TrabajoIntermedio.TrianguloSustitucion t = assertInstanceOf(TrabajoIntermedio.TrianguloSustitucion.class,
                r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("x = 3·sen θ", t.sustitucion()),
                () -> assertEquals("x", t.opuesto()),
                () -> assertEquals("3", t.hipotenusa()),
                () -> assertTrue(t.adyacente().startsWith("√("), t.adyacente()),
                () -> assertEquals("θ = arcsen(x/3)", t.angulo()),
                () -> assertTrue(t.razones().stream().anyMatch(z -> z.startsWith("cot θ")), t.razones().toString()),
                () -> assertFalse(r.pasos().getFirst().hijos().isEmpty(), "la integral en θ va anidada"));
    }

    @Test
    void laSecanteAlCuboUsaPartesYDespeja() {
        ResolucionIntegral r = resolver("∫(sec^3 x, x)");
        List<String> l = ((TrabajoIntermedio.Agrupacion) r.pasos().getFirst().trabajo()).grupos();
        assertTrue(l.stream().anyMatch(s -> s.startsWith("Por partes con u = sec x")), l.toString());
        assertTrue(l.stream().anyMatch(s -> s.startsWith("Reaparece la integral original")), l.toString());
    }

    @Test
    void conUnExponenteImparSeSeparaUnFactor() {
        List<String> l = ((TrabajoIntermedio.Agrupacion) resolver("∫(sen^3 x, x)").pasos().getFirst().trabajo()).grupos();
        assertTrue(l.getFirst().contains("(1 − cos x²)") || l.getFirst().contains("1 − "), l.toString());
        assertTrue(l.stream().anyMatch(s -> s.startsWith("u = cos x")), l.toString());
    }

    @Test
    void exploracionMuestraLaEleccionIlate() {
        ResolucionIntegral r = resolver("∫(x ln x, x)");
        List<CaminoCalculo> c = MOTOR.caminos(r);
        CaminoCalculo usado = c.getFirst();
        assertEquals("I03", usado.codigo());
        assertEquals(CaminoCalculo.Estado.USADO, usado.estado());
        assertNotNull(usado.texto());
        assertTrue(usado.texto().startsWith("ILATE: u = ln x"), usado.texto());
        assertTrue(c.stream().filter(x -> x.estado() == CaminoCalculo.Estado.NO_APLICA)
                .allMatch(x -> x.texto() != null && !x.texto().contains("próxima versión")), c.toString());
    }

    @Test
    void yaNoDiceQueLlegaEnLaProximaVersion() {
        for (String e : Arrays.asList("∫(x e^x, x)", "∫(x sen x, x)", "∫(e^(x^2), x)")) {
            ResolucionIntegral r = resolver(e);
            assertFalse(r.texto().contains("próxima versión"), r.texto());
            MOTOR.caminos(r).forEach(k -> assertFalse(k.texto() != null && k.texto().contains("próxima versión"),
                    k.toString()));
        }
    }
}
