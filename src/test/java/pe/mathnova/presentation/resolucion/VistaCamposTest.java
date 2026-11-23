package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.Verificacion;
import pe.mathnova.domain.BancoCsv;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.ResolucionFunciones;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.grafica.DatosGrafica;
import pe.mathnova.presentation.latex.RenderizadorLatex;

/** Parte D: lo que se ve en R, en C y con funciones (sin abrir la ventana). */
class VistaCamposTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService SERVICIO = new FactorizacionService(MOTOR, null);
    private static final RenderizadorLatex LATEX = new RenderizadorLatex(24f);

    @BeforeAll
    static void sinPantalla() {
        System.setProperty("java.awt.headless", "true");
    }

    private static Resolucion resolver(String texto, Campo c) {
        return MOTOR.resolver(Parser.parse(texto), c);
    }

    private static VistaResolucion vista(String texto, Campo c) {
        Resolucion r = resolver(texto, c);
        return VistaResolucion.de(r, SERVICIO.verificar(r), true);
    }

    private static VistaResolucion vistaFunciones(String texto) {
        ResolucionFunciones r = SERVICIO.factorizarFunciones(texto, f -> { });
        return VistaResolucion.de(r, SERVICIO.verificar(r), true);
    }

    @Test
    void primeroQYLuegoPasarAR() {
        VistaResolucion v = vista("x^4 - 5x^2 + 6", Campo.R);
        assertAll(
                () -> assertEquals(List.of("F14", "PR", "F21", "F21"), v.pasos().stream().map(VistaPaso::codigo).toList()),
                () -> assertEquals(List.of(1, 2, 3, 4), v.pasos().stream().map(VistaPaso::orden).toList()),
                () -> assertTrue(v.pasos().get(1).titulo().contains("Pasar a R")),
                () -> assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta()),
                () -> assertNull(v.ayudaEtiqueta(), "verificación exacta: sin tooltip numérico"),
                () -> assertEquals("Polinomio de grado 4 · 4 factores en R", v.subtitulo()));
    }

    @Test
    void radicalesEnLatex() {
        String r = vista("x^3 - 2", Campo.R).latexFormula();
        String c = vista("x^2 + 4", Campo.C).latexFormula();
        assertAll(
                () -> assertTrue(r.contains("\\sqrt[3]{2}") && r.contains("\\sqrt[3]{4}"), r),
                () -> assertTrue(vista("x^2 - 2", Campo.R).latexFormula().contains("\\sqrt{2}")),
                () -> assertTrue(c.contains("2i"), c));
    }

    @Test
    void verificadoNumericamenteConAnidados() {
        VistaResolucion v = vista("x^4 + 2x^3 + x^2 - 2", Campo.R);
        assertAll(
                () -> assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta()),
                () -> assertEquals("Verificado numéricamente con 40 cifras", v.ayudaEtiqueta()),
                () -> assertTrue(v.verificacion().getFirst().contains("40 cifras")));
    }

    @Test
    void aproximadasConAproximadamente() {
        Resolucion r = resolver("x^5 - x - 1", Campo.R);
        VistaResolucion v = VistaResolucion.de(r, SERVICIO.verificar(r), false);
        DatosGrafica g = DatosGrafica.de(r).orElseThrow();
        VistaPropiedades p = VistaPropiedades.de(r).orElseThrow();
        assertAll(
                () -> assertEquals(VistaResolucion.Etiqueta.APROXIMADO, v.etiqueta()),
                () -> assertTrue(v.notas().getFirst().contains("≈ 1.1673039783"), v.notas()::toString),
                () -> assertTrue(v.latexFormula().contains("\\approx"), v.latexFormula()),
                () -> assertEquals(1, g.extra().marcas().size()),
                () -> assertTrue(g.extra().marcas().getFirst().etiqueta().contains("≈")),
                () -> assertTrue(p.filas().stream().anyMatch(f -> f.valor().contains("≈ 1.1673039783"))));
    }

    @Test
    void irreducibleEnR() {
        VistaResolucion v = vista("x^2 + 1", Campo.R);
        assertAll(
                () -> assertEquals(VistaResolucion.Etiqueta.IRREDUCIBLE_R, v.etiqueta()),
                () -> assertTrue(v.notas().getFirst().contains("Δ")),
                () -> assertTrue(v.pasos().isEmpty()));
    }

    @Test
    void raicesConRadicalesEnLaGraficaYEnAnalisis() {
        Resolucion r = resolver("x^2 - 2", Campo.R);
        DatosGrafica g = DatosGrafica.de(r).orElseThrow();
        VistaPropiedades p = VistaPropiedades.de(r).orElseThrow();
        assertAll(
                () -> assertEquals(2, g.extra().marcas().size()),
                () -> assertEquals(Math.sqrt(2), Math.abs(g.extra().marcas().getFirst().x()), 1e-12),
                () -> assertTrue(p.filas().stream().anyMatch(f -> f.nombre().equals("Raíces reales")
                        && f.valor().contains("√2"))));
    }

    @Test
    void exploracionMarcaLosMetodosDelCampo() {
        Resolucion r = resolver("x^2 - 2x - 1", Campo.R);
        List<String> usados = VistaExploracion.de(r, true).tarjetas().stream()
                .filter(t -> t.estado() == VistaExploracion.Estado.USADO).map(VistaExploracion.Tarjeta::codigo).toList();
        assertEquals(List.of("F22", "F21"), usados);
    }

    @Test
    void comparacionQRC() {
        FactorizacionService.Comparacion c = SERVICIO.comparar("x^2 - 2");
        FactorizacionService.Comparacion f = SERVICIO.comparar("(x^2 - 1)/(x + 1)");
        FactorizacionService.Comparacion t = SERVICIO.comparar("e^(2x) - 1");
        assertAll(
                () -> assertEquals("x^{2} - 2", c.latex().get(Campo.Q)),
                () -> assertTrue(c.latex().get(Campo.R).contains("\\sqrt{2}")),
                () -> assertTrue(c.latex().get(Campo.C).contains("\\sqrt{2}")),
                () -> assertTrue(SERVICIO.comparar("x^2 + 1").latex().get(Campo.C).contains("i")),
                () -> assertTrue(f.latex().containsKey(Campo.Q)),
                // Sprint 5: la fracción también tiene resultado en R y C; aquí es el mismo en los tres.
                () -> assertTrue(f.latex().containsKey(Campo.R) && f.iguales()),
                () -> assertTrue(t.latex().containsKey(Campo.Q)),
                () -> assertTrue(t.avisos().containsKey(Campo.C)));
    }

    // ------------------------------------------------------------------ funciones

    @Test
    void sustitucionConTresRenglones() {
        VistaResolucion v = vistaFunciones("e^(2x) - 3e^x + 2");
        VistaPaso t = v.pasos().getFirst();
        var s = (pe.mathnova.domain.factorizacion.TrabajoIntermedio.Sustitucion) t.trabajo();
        assertAll(
                () -> assertEquals("T02", t.codigo()),
                () -> assertEquals("u = e^x", s.definicion()),
                () -> assertEquals("u^2 - 3u + 2", s.sustituir()),
                () -> assertEquals("(u - 1)(u - 2)", s.factorizar()),
                () -> assertEquals("(e^x - 1)·(e^x - 2)", s.regresar()),
                () -> assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta()));
    }

    @Test
    void restriccionesDeDominio() {
        assertTrue(vistaFunciones("ln(x)^2 - 4").notas().getFirst().contains("x > 0"));
        assertTrue(vistaFunciones("tan(x)^2 - 1").notas().getFirst().contains("x ≠ π/2 + kπ"));
    }

    @Test
    void noEsPolinomicaEnUnaFuncion() {
        VistaResolucion v = vistaFunciones("x + sen(x)");
        assertAll(
                () -> assertEquals(VistaResolucion.Etiqueta.NO_FACTORIZABLE, v.etiqueta()),
                () -> assertEquals("No se puede factorizar: no es polinómica en una sola función.", v.sinPasos()));
    }

    @Test
    void laGraficaDeTanSeCortaEnLasAsintotas() {
        ResolucionFunciones r = SERVICIO.factorizarFunciones("tan(x)^2 - 1", f -> { });
        DatosGrafica g = DatosGrafica.de(r).orElseThrow();
        assertAll(
                () -> assertTrue(g.raices().isEmpty(), "las raíces trascendentes no se marcan como exactas"),
                () -> assertEquals(1, g.extra().periodicas().size()),
                () -> assertEquals(Math.PI / 2, g.extra().periodicas().getFirst().fase(), 1e-12),
                () -> assertEquals(4, g.extra().periodicas().getFirst().entre(-5, 5).size()), // ±π/2, ±3π/2
                () -> assertEquals(0, g.funcion().valor(Math.PI / 4), 1e-12),
                () -> assertTrue(Double.isNaN(DatosGrafica.de(SERVICIO.factorizarFunciones("ln(x)^2 - 4", f -> { }))
                        .orElseThrow().funcion().valor(-1))));
    }

    // ------------------------------------------------------------------ LaTeX del banco

    static List<BancoCsv.Fila> filasNuevas() {
        return BancoCsv.filas().stream().filter(f -> !f.campo().equals("Q") || f.metodo().startsWith("T")).toList();
    }

    /** Los resultados en R, C y con funciones se dibujan con jlatexmath. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("filasNuevas")
    void dibujaLosResultados(BancoCsv.Fila f) {
        String latex;
        if (f.metodo().startsWith("T")) {
            latex = vistaFunciones(f.entrada()).latexFormula();
        } else {
            latex = FactorizacionService.latex(resolver(f.entrada(), Campo.valueOf(f.campo())));
        }
        BufferedImage imagen = LATEX.dibujar(latex);
        assertTrue(imagen.getWidth() > 10 && imagen.getHeight() > 10, latex);
        assertFalse(latex.contains("√"), latex);
    }

    /** Solo para que Verificacion quede importada: la etiqueta numérica es la de la Parte E. */
    @Test
    void textoDeLaAyuda() {
        assertEquals("Verificado numéricamente con 40 cifras", Verificacion.AYUDA_NUMERICA);
    }
}
