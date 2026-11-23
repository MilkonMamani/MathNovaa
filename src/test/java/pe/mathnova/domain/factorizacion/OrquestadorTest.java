package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.parser.Parser;

/** Ejemplos exactos del Sprint 2 (HU04, HU05, HU07, HU18). */
class OrquestadorTest {

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    private Resolucion resolver(String entrada) {
        return motor.resolver(Parser.parse(entrada), Campo.Q);
    }

    private static List<String> codigos(List<Paso> pasos) {
        return pasos.stream().map(Paso::codigoTecnica).toList();
    }

    /** Pasos, resultado (texto exacto y estructura) y verificación expandiendo (HU18). */
    private Resolucion comprobar(String entrada, List<String> pasos, String resultado) {
        Resolucion r = resolver(entrada);
        assertAll(entrada,
                () -> assertEquals(pasos, codigos(r.pasos()), "pasos"),
                () -> assertEquals(resultado, r.resultado().aTexto(), "texto RN12"),
                () -> assertEquals(ComparadorFactorizacion.leer(resultado),
                        ComparadorFactorizacion.leer(r.resultado().aTexto()), "estructura"),
                () -> assertEquals(Polinomio.desde(Parser.parse(entrada)), r.resultado().expandir(), "HU18"));
        for (int k = 0; k < r.pasos().size(); k++) {
            Paso p = r.pasos().get(k);
            assertEquals(k + 1, p.orden());
            assertFalse(p.descartado());
            assertFalse(p.justificacion().isBlank());
        }
        return r;
    }

    @Test
    void agrupacionYDiferenciaDeCuadrados() {
        Resolucion r = comprobar("x^3 - 2x^2 - x + 2", List.of("F04", "F05"), "(x + 1)(x - 1)(x - 2)");
        Paso f01 = r.descartados().stream().filter(p -> p.codigoTecnica().equals("F01")).findFirst().orElseThrow();
        assertAll(
                () -> assertTrue(f01.descartado()),
                () -> assertEquals("MCD(1, 2, 1, 2) = 1 y no todos los términos tienen x", f01.motivoDescarte()),
                () -> assertEquals("Los grupos (x³ − 2x²) y (−x + 2) comparten (x − 2)",
                        r.pasos().getFirst().justificacion()),
                () -> assertEquals("(x - 2)(x^2 - 1)", r.pasos().getFirst().despuesComoForma().aTexto()));
    }

    @Test
    void factorComunYTrinomioSimple() {
        Resolucion r = comprobar("2x^2 - 10x + 12", List.of("F01", "F12"), "2(x - 2)(x - 3)");
        assertEquals("MCD(2, 10, 12) = 2 y no todos los términos tienen x", r.pasos().get(0).justificacion());
        assertEquals("−2 + (−3) = −5 y (−2)·(−3) = 6", r.pasos().get(1).justificacion());
    }

    @Test
    void diferenciaDeCuadradosRepetida() {
        Resolucion r = comprobar("x^4 - 16", List.of("F05", "F05"), "(x + 2)(x - 2)(x^2 + 4)");
        assertEquals(Polinomio.desde(Parser.parse("x^2 - 4")), r.pasos().get(1).antes());
        Irreducibilidad x2mas4 = r.irreducibles().stream()
                .filter(i -> i.factor().grado() == 2).findFirst().orElseThrow();
        assertEquals("Δ = −16 < 0", x2mas4.justificacion());
    }

    @Test
    void trinomioCuadradoPerfecto() {
        Resolucion r = comprobar("9x^2 - 12x + 4", List.of("F07"), "(3x - 2)^2");
        assertEquals("9x² = (3x)², 4 = 2² y 12x = 2·3x·2", r.pasos().getFirst().justificacion());
    }

    /**
     * El enunciado escribe (2x − 1)(3x + 2); RN12 ordena por raíz de menor a mayor (−2/3 antes que 1/2),
     * igual que el banco, así que el texto es (3x + 2)(2x − 1). Por estructura son la misma forma.
     */
    @Test
    void aspaSimple() {
        Resolucion r = comprobar("6x^2 + x - 2", List.of("F13"), "(3x + 2)(2x - 1)");
        assertEquals(ComparadorFactorizacion.leer("(2x - 1)(3x + 2)"), ComparadorFactorizacion.leer(r.resultado().aTexto()));
        var aspa = assertInstanceOf(TrabajoIntermedio.AspaSimple.class, r.pasos().getFirst().trabajo());
        assertEquals("2x·2 + 3x·(−1) = 4x − 3x = x", aspa.verificacion());
    }

    @Test
    void factorComunPolinomio() {
        Resolucion r = comprobar("x^2(x - 3) + 5(x - 3)", List.of("F03"), "(x - 3)(x^2 + 5)");
        assertEquals("Los términos x²(x − 3) y 5(x − 3) comparten (x − 3)", r.pasos().getFirst().justificacion());
    }

    @Test
    void irreducibleEnQ() {
        Resolucion r = comprobar("x^2 + x + 1", List.of(), "x^2 + x + 1");
        assertEquals(1, r.irreducibles().size());
        Irreducibilidad i = r.irreducibles().getFirst();
        assertAll(
                () -> assertEquals("A1", i.codigoTecnica()),
                () -> assertEquals("Δ = −3 < 0", i.justificacion()),
                () -> assertTrue(i.certificada()),
                () -> assertEquals(List.of("F01", "F07", "F12"), codigos(r.descartados())));
    }

    @Test
    void signoNegativoConDiferenciaDeCuadrados() {
        Resolucion r = comprobar("-x^2 + 36", List.of("F01", "F05"), "-(x + 6)(x - 6)");
        assertEquals("-(x^2 - 36)", r.pasos().getFirst().despuesComoForma().aTexto());
    }

    @Test
    void descartadosNumeradosConMotivo() {
        Resolucion r = resolver("x^2 + 4");
        assertEquals(List.of("F01", "F05", "F08"), codigos(r.descartados()));
        for (int k = 0; k < r.descartados().size(); k++) {
            Paso d = r.descartados().get(k);
            assertEquals(k + 1, d.orden());
            assertTrue(d.descartado());
            assertTrue(d.despues().isEmpty());
        }
        assertEquals("x² + 4 es una suma, no una diferencia", r.descartados().get(1).motivoDescarte());
    }

    @Test
    void irreducibilidadPorGrado() {
        var cubica = resolver("x^3 + x + 1").irreducibles().getFirst();
        assertEquals("Ninguno de los candidatos ±1 es raíz: no tiene raíces racionales", cubica.justificacion());
        assertTrue(cubica.certificada());

        // Sprint 3: con Ruffini, x³ − 2x² − 5x + 6 ya se factoriza por completo.
        Resolucion conRaiz = resolver("x^3 - 2x^2 - 5x + 6");
        assertEquals("F16", conRaiz.pasos().getFirst().codigoTecnica());
        assertTrue(conRaiz.irreducibles().stream().allMatch(i -> i.factor().grado() == 1));

        // Sprint 3 (A.3): A4 certifica con Kronecker, sin mencionar sprints ni códigos.
        var cuarta = resolver("x^4 + 1").irreducibles().getFirst();
        assertEquals("No se puede factorizar en Q: no tiene raíces racionales y no se parte en factores "
                + "de grado 2 o más.", cuarta.justificacion());
        assertTrue(cuarta.certificada());

        var x2menos2 = resolver("x^2 - 2").irreducibles().getFirst();
        assertEquals("Δ = 8 no es un cuadrado perfecto", x2menos2.justificacion());
    }

    /** RN12: el texto sale igual que en el banco. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "2x^4 - 4x^2           | 2x^2(x^2 - 2)",
            "-(3/5)x + 1           | (-1/5)(3x - 5)",
            "2x^2 - (7/2)x         | (1/2)x(4x - 7)",
            "-(1/7)x^3 + (3/7)x^2  | (-1/7)x^2(x - 3)",
            "16x^2 + 72x + 81      | (4x + 9)^2",
            "x^3 + 4x^2 - 16x - 64 | (x + 4)^2(x - 4)",
            "x^8 - 1               | (x + 1)(x - 1)(x^2 + 1)(x^4 + 1)",
            "4x(x - 4) + 6(x - 4)  | 2(2x + 3)(x - 4)",
            "x^3(x + 1) + 8(x + 1) | (x + 2)(x + 1)(x^2 - 2x + 4)",
            "-x^2 + 9x - 8         | -(x - 1)(x - 8)",
            "5x^2 + 27x + 10       | (x + 5)(5x + 2)",
            "a^2 - b^2             | (a + b)(a - b)",
            "7                     | 7",
            "0                     | 0"
    })
    void formatoDelBanco(String entrada, String esperado) {
        assertEquals(esperado, motor.factorizar(entrada, Campo.Q));
    }

    // ================================================================== Sprint 4: R y C

    /**
     * Ejemplos exactos de la Parte B. Se compara por estructura (los mismos factores mónicos y la misma
     * constante), se exige primero los pasos de Q y luego "Pasar a R/C" con los métodos esperados, y la
     * verificación de la Parte E.
     */
    @ParameterizedTest(name = "{0} en {1}")
    @CsvSource(delimiter = '|', value = {
            "x^2 - 2         | R | -               | F21           | (x − √2)(x + √2)",
            "x^2 - 2x - 1    | R | -               | F22 F21       | (x − 1 − √2)(x − 1 + √2)",
            "x^4 - 5x^2 + 6  | R | F14             | F21 F21       | (x − √2)(x + √2)(x − √3)(x + √3)",
            "x^3 - 2         | R | -               | F26           | (x − ∛2)(x² + ∛2·x + ∛4)",
            "x^4 + 1         | R | -               | F29           | (x² − √2·x + 1)(x² + √2·x + 1)",
            "x^2 + 4         | C | -               | F24           | (x − 2i)(x + 2i)",
            "x^2 + 2x + 5    | C | -               | F25           | (x + 1 − 2i)(x + 1 + 2i)",
            "x^3 + 8         | C | F08             | F30           | (x + 2)(x − 1 − √3·i)(x − 1 + √3·i)",
            "x^4 - 16        | C | F05 F05         | F30           | (x − 2)(x + 2)(x − 2i)(x + 2i)",
            "x^2 + x + 1     | C | -               | F25           | (x + 1/2 − (√3/2)i)(x + 1/2 + (√3/2)i)",
    })
    void factorizaEnRyC(String entrada, Campo campo, String enQ, String enCampo, String esperado) {
        Resolucion r = motor.resolver(Parser.parse(entrada), campo);
        String texto = r.texto();
        List<String> pasosQ = enQ.equals("-") ? List.of() : List.of(enQ.split(" "));
        List<String> extension = new java.util.ArrayList<>();
        extension.add(campo == Campo.R ? "PR" : "PC");
        extension.addAll(List.of(enCampo.split(" ")));
        assertAll(entrada + " → " + texto,
                () -> assertEquals(pasosQ, codigos(r.pasos()), "pasos en Q"),
                () -> assertEquals(extension, r.extension().pasos().stream().map(Extension.Paso::codigoTecnica).toList(),
                        "pasos en el campo"),
                () -> assertTrue(ComparadorFactorizacion.igualesEnCampo(
                        ComparadorFactorizacion.leerEnCampo(esperado.replace('−', '-').replace("²", "^2")),
                        ComparadorFactorizacion.leerEnCampo(texto)), "estructura"),
                () -> assertTrue(VerificacionCampo.de(r).coincide(), "verificación"),
                () -> assertFalse(VerificacionCampo.de(r).numerica(), "exacta"));
        for (int k = 0; k < r.extension().pasos().size(); k++) {
            assertEquals(r.pasos().size() + k + 1, r.extension().pasos().get(k).orden());
        }
    }

    @Test
    void textoEnRyC() {
        assertEquals("(x - √2)(x + √2)", motor.factorizar("x^2 - 2", Campo.R));
        assertEquals("(x - 1 - √2)(x - 1 + √2)", motor.factorizar("x^2 - 2x - 1", Campo.R));
        assertEquals("(x - ∛2)(x^2 + ∛2·x + ∛4)", motor.factorizar("x^3 - 2", Campo.R));
        assertEquals("(x^2 - √2·x + 1)(x^2 + √2·x + 1)", motor.factorizar("x^4 + 1", Campo.R));
        assertEquals("(x - 2·i)(x + 2·i)", motor.factorizar("x^2 + 4", Campo.C));
        assertEquals("(x + 1 - 2·i)(x + 1 + 2·i)", motor.factorizar("x^2 + 2x + 5", Campo.C));
        assertEquals("(x + 1/2 - √3·i/2)(x + 1/2 + √3·i/2)", motor.factorizar("x^2 + x + 1", Campo.C));
        assertEquals("2(x - 1 - √6/2)(x - 1 + √6/2)", motor.factorizar("2x^2 - 4x - 1", Campo.R));
    }

    @Test
    void abelRuffini() {
        Resolucion r = motor.resolver(Parser.parse("x^5 - x - 1"), Campo.R);
        assertAll(
                () -> assertEquals("Irreducible en Q; raíces reales ≈ 1.1673039783", r.texto()),
                () -> assertTrue(r.extension().aproximada()),
                () -> assertEquals(List.of("PR", "A5"),
                        r.extension().pasos().stream().map(Extension.Paso::codigoTecnica).toList()),
                () -> assertTrue(r.extension().pasos().getLast().trabajo().toString().contains("Abel–Ruffini")));
    }

    @Test
    void enRLosCuadraticosConDeltaNegativoQuedan() {
        Resolucion r = motor.resolver(Parser.parse("x^2 + 1"), Campo.R);
        assertAll(
                () -> assertEquals("x^2 + 1", r.texto()),
                () -> assertTrue(r.extension().pasos().isEmpty()),
                () -> assertEquals(List.of("x^2 + 1 no se factoriza en R porque Δ = -4 < 0."), r.extension().notas()));
    }

    @Test
    void casusIrreducibilis() {
        Resolucion r = motor.resolver(Parser.parse("x^3 - 3x + 1"), Campo.R);
        VerificacionCampo v = VerificacionCampo.de(r);
        Extension.Paso f27 = r.extension().pasos().getLast();
        assertAll(
                () -> assertEquals("F27", f27.codigoTecnica()),
                () -> assertTrue(f27.trabajo().toString().contains("casus irreducibilis")),
                () -> assertEquals(3, r.extension().resultado().factores().size()),
                () -> assertFalse(r.texto().contains("i")), // sin complejos en el resultado final
                () -> assertTrue(r.texto().contains("cos")),
                () -> assertTrue(v.coincide()),
                () -> assertTrue(v.numerica()));
    }

    @Test
    void cardanoConRadicalesExactos() {
        Resolucion r = motor.resolver(Parser.parse("x^3 - 6x - 6"), Campo.R);
        assertTrue(ComparadorFactorizacion.igualesEnCampo(
                ComparadorFactorizacion.leerEnCampo("(x - ∛4 - ∛2)(x^2 + (∛2 + ∛4)·x - 2 + ∛4 + 2·∛2)"),
                ComparadorFactorizacion.leerEnCampo(r.texto())));
        assertFalse(VerificacionCampo.de(r).numerica());
        assertTrue(VerificacionCampo.de(r).coincide());
    }

    @Test
    void ferrariConRadicalAnidado() {
        Resolucion r = motor.resolver(Parser.parse("x^4 + 2x^3 + x^2 - 2"), Campo.R);
        VerificacionCampo v = VerificacionCampo.de(r);
        assertAll(
                () -> assertTrue(r.texto().contains("√(1 + 4·√2)"), r::texto),
                () -> assertTrue(v.coincide()),
                () -> assertTrue(v.numerica()));
    }

    // ================================================================== Sprint 3

    /** Pasos y resultado por estructura (el orden del texto lo fija RN12) y verificación HU18. */
    private Resolucion comprobarEstructura(String entrada, List<String> pasos, String resultado) {
        Resolucion r = resolver(entrada);
        assertAll(entrada,
                () -> assertEquals(pasos, codigos(r.pasos()), "pasos"),
                () -> assertEquals(ComparadorFactorizacion.leer(resultado),
                        ComparadorFactorizacion.leer(r.resultado().aTexto()), "estructura"),
                () -> assertEquals(Polinomio.desde(Parser.parse(entrada)), r.resultado().expandir(), "HU18"));
        return r;
    }

    private static List<String> raices(Paso p) {
        var t = assertInstanceOf(TrabajoIntermedio.TablaRuffini.class, p.trabajo());
        return t.divisiones().stream().map(d -> d.raiz().toString()).toList();
    }

    @Test
    void agrupacionConTodasLasParticiones() {
        Resolucion r = comprobar("2x^4 - 5x^3 + 5x - 2", List.of("F04", "F05", "F13"), "(x + 1)(2x - 1)(x - 1)(x - 2)");
        Paso f04 = r.pasos().getFirst();
        assertAll(
                () -> assertEquals("Los grupos (2x⁴ − 2) y (−5x³ + 5x) comparten (x² − 1)", f04.justificacion()),
                () -> assertEquals("(x^2 - 1)(2x^2 - 5x + 2)", f04.despuesComoForma().aTexto()),
                () -> assertEquals(List.of("2x⁴ − 2 = 2(x⁴ − 1) = 2(x² + 1)(x² − 1)", "−5x³ + 5x = −5x(x² − 1)"),
                        assertInstanceOf(TrabajoIntermedio.Agrupacion.class, f04.trabajo()).grupos()),
                // Parte D: al inicio también aplica Ruffini.
                () -> assertEquals(List.of("F16"), r.alternativas()));
    }

    /**
     * HU08. El enunciado dice "x = 1, luego x = −1"; con el orden de RN09 (positivos antes que
     * negativos) el cociente 2x³ − 3x² − 3x + 2 tiene antes la raíz x = 2, como en el ejemplo
     * siguiente. Queda (2x − 1)(x + 1) para F13.
     */
    @Test
    void forzarRuffini() {
        Resolucion r = motor.resolver(Parser.parse("2x^4 - 5x^3 + 5x - 2"), Campo.Q, "F16");
        assertAll(
                () -> assertEquals("F16", r.metodoForzado()),
                () -> assertEquals(List.of("F16", "F16", "F13"), codigos(r.pasos())),
                () -> assertEquals(List.of("1"), raices(r.pasos().get(0))),
                () -> assertEquals(List.of("2"), raices(r.pasos().get(1))),
                () -> assertEquals(ComparadorFactorizacion.leer("(x + 1)(2x - 1)(x - 1)(x - 2)"),
                        ComparadorFactorizacion.leer(r.resultado().aTexto())));
    }

    @Test
    void forzarUnMetodoQueNoAplicaDaElMotivo() {
        MetodoNoAplicable e = assertThrows(MetodoNoAplicable.class,
                () -> motor.resolver(Parser.parse("x^2 + 4"), Campo.Q, "F05"));
        assertEquals("x² + 4 es una suma, no una diferencia", e.motivo());
    }

    @Test
    void ruffiniConRaizDos() {
        Resolucion r = comprobar("2x^3 - 3x^2 - 3x + 2", List.of("F16", "F13"), "(x + 1)(2x - 1)(x - 2)");
        var tabla = assertInstanceOf(TrabajoIntermedio.TablaRuffini.class, r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals(List.of("2"), raices(r.pasos().getFirst())),
                () -> assertEquals("1", tabla.candidatos().getFirst().valor().toString()),
                () -> assertEquals("-2", tabla.candidatos().getFirst().residuo().toString()),
                () -> assertTrue(tabla.candidatos().getLast().esRaiz()));
    }

    @Test
    void ruffiniConRaizRepetida() {
        Resolucion r = comprobarEstructura("x^3 - 3x + 2", List.of("F16", "F17"), "(x - 1)^2(x + 2)");
        assertEquals("(x + 2)(x - 1)^2", r.resultado().aTexto());
        var f17 = assertInstanceOf(TrabajoIntermedio.TablaRuffini.class, r.pasos().get(1).trabajo());
        assertEquals(2, f17.divisiones().size(), "las dos tablas apiladas");
        assertTrue(r.pasos().getFirst().justificacion().contains("Se completa con 0 el término de x² que falta"));
    }

    @Test
    void cambioDeVariable() {
        Resolucion r = comprobar("x^4 - 5x^2 + 4", List.of("F14", "F05", "F05"), "(x + 2)(x + 1)(x - 1)(x - 2)");
        var s = assertInstanceOf(TrabajoIntermedio.Sustitucion.class, r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("u = x²", s.definicion()),
                () -> assertEquals("x⁴ − 5x² + 4 = u² − 5u + 4", s.sustituir()),
                () -> assertEquals("u² − 5u + 4 = (u − 1)(u − 4)", s.factorizar()),
                () -> assertEquals("(u − 1)(u − 4) = (x² − 1)(x² − 4)", s.regresar()));
    }

    @Test
    void quitaYPon() {
        Resolucion r = comprobarEstructura("x^4 + 4", List.of("F15"), "(x^2 - 2x + 2)(x^2 + 2x + 2)");
        assertEquals(List.of("x⁴ + 4 = x⁴ + 4x² + 4 − 4x²", "= (x² + 2)² − (2x)²", "= (x² + 2x + 2)(x² − 2x + 2)"),
                assertInstanceOf(TrabajoIntermedio.Agrupacion.class, r.pasos().getFirst().trabajo()).grupos());
    }

    @Test
    void reciproco() {
        Resolucion r = comprobar("x^4 + 3x^3 + 4x^2 + 3x + 1", List.of("F18", "F07"), "(x + 1)^2(x^2 + x + 1)");
        var s = assertInstanceOf(TrabajoIntermedio.Sustitucion.class, r.pasos().getFirst().trabajo());
        assertEquals("z² + 3z + 2 = (z + 1)(z + 2)", s.factorizar());
    }

    @Test
    void aspaDobleEspecial() {
        Resolucion r = comprobarEstructura("x^4 + 5x^3 + 11x^2 + 14x + 4", List.of("F19"), "(x^2 + 3x + 1)(x^2 + 2x + 4)");
        var a = assertInstanceOf(TrabajoIntermedio.AspaDobleEspecial.class, r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("3", a.p().toString()),
                () -> assertEquals("1", a.e1().toString()),
                () -> assertEquals("2", a.q().toString()),
                () -> assertEquals("4", a.e2().toString()));
    }

    @Test
    void coeficientesIndeterminados() {
        Resolucion r = comprobarEstructura("x^5 + x^4 + 2x^3 + 2x^2 + 2x + 1", List.of("F20"), "(x^2 + x + 1)(x^3 + x + 1)");
        var s = assertInstanceOf(TrabajoIntermedio.SistemaCoeficientes.class, r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("(x² + ax + b)(x³ + cx² + dx + e)", s.planteo()),
                () -> assertEquals("c + a = 1", s.ecuaciones().getFirst().ecuacion()),
                () -> assertEquals("be = 1", s.ecuaciones().getLast().ecuacion()),
                () -> assertEquals(List.of("a = 1", "b = 1", "c = 0", "d = 1", "e = 1"), s.solucion()));
    }

    @Test
    void irreducibleCertificadoPorA4() {
        Resolucion r = comprobar("x^4 + x + 1", List.of(), "x^4 + x + 1");
        Irreducibilidad i = r.irreducibles().getFirst();
        assertAll(
                () -> assertEquals("A4", i.codigoTecnica()),
                () -> assertTrue(i.certificada()),
                () -> assertEquals("No se puede factorizar en Q: no tiene raíces racionales y no se parte en "
                        + "factores de grado 2 o más.", i.justificacion()),
                () -> assertTrue(codigos(r.descartados()).containsAll(List.of("F16", "F19", "F20", "A4"))));
    }

    @Test
    void gradoDemasiadoAltoNoSeCertifica() {
        Irreducibilidad i = resolver("x^10 + x + 1").irreducibles().getFirst();
        assertFalse(i.certificada());
        assertEquals("Grado demasiado alto para certificar que no se puede factorizar.", i.justificacion());
    }

    @Test
    void simplificarFraccion() {
        assertEquals("(x + 2)/(x + 3); x ≠ -3, x ≠ 2", motor.factorizar("(x^2 - 4)/(x^2 + x - 6)", Campo.Q));
        Simplificacion s = new SimplificadorFracciones(motor).simplificar(Parser.parse("(x^3 - 1)/(x^2 - 1)"));
        assertAll(
                () -> assertEquals("(x^2 + x + 1)/(x + 1)", s.fraccion()),
                () -> assertEquals("x + 1/(x + 1)", s.division()),
                () -> assertTrue(s.verifica()));
    }
}
