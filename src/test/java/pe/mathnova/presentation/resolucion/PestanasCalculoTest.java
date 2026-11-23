package pe.mathnova.presentation.resolucion;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.ResolverService;
import pe.mathnova.domain.calculo.derivadas.ResolucionDerivada;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.sugerencias.Sugerencia;
import pe.mathnova.presentation.grafica.DatosGrafica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sprint 6, Partes C y D: lo que muestran las pestañas de una derivada y de una integral, y las sugerencias. */
class PestanasCalculoTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR, null);
    private static final ResolverService SERVICIO = new ResolverService(FACTORIZACION, MOTOR,
            new ResolvedorEcuaciones(MOTOR));

    private static ResolucionDerivada derivada(String t) {
        return SERVICIO.resolverDerivada(t, null, f -> { });
    }

    private static ResolucionIntegral integral(String t) {
        return SERVICIO.resolverIntegral(t, null, f -> { });
    }

    @Test
    void derivadaDeUnPolinomio() {
        ResolucionDerivada r = derivada("d/dx(x^3 - 3x^2 - 9x + 5)");
        ContenidoDerivada c = ContenidoDerivada.de(r, SERVICIO.caminos(r), false);

        // Gráfica: f en azul, f' en naranja con leyenda, los críticos marcados y la tangente
        DatosGrafica.Calculo g = c.grafica().orElseThrow().extra().calculo();
        assertEquals("f(x)", g.nombre());
        assertEquals("f'(x)", g.nombreSegunda());
        assertNotNull(g.segunda());
        assertNotNull(g.pendiente());
        assertEquals(List.of("máx (−1, 10)", "mín (3, −22)"),
                g.puntos().stream().map(DatosGrafica.PuntoMarcado::etiqueta).toList());
        assertEquals(-12, g.pendiente().valor(1), 1e-9); // f'(1) = 3 − 6 − 9

        // Análisis: críticos, crecimiento y concavidad
        List<SeccionAnalisis> s = c.secciones();
        assertEquals(3, s.size());
        assertTrue(s.get(0).filas().get(0).valor().startsWith("máximo"), s.get(0).filas().toString());
        assertEquals("(−∞, −1) ∪ (3, ∞)", s.get(1).filas().get(0).valor());
        assertEquals("(−1, 3)", s.get(1).filas().get(1).valor());
        assertEquals(List.of("−1", "3"), s.get(1).marcas());
        assertEquals(List.of(1, -1, 1), s.get(1).signos());
        assertEquals("(1, −6)", s.get(2).filas().get(2).valor()); // inflexión

        // Exploración: lo usado y "Expandir" no aplica (ya es una suma)
        assertEquals(VistaExploracion.Estado.USADO, c.exploracion().tarjetas().getFirst().estado());
        assertTrue(c.exploracion().probables());
    }

    @Test
    void derivadaConOtroCamino() {
        ResolucionDerivada r = derivada("d/dx((x + 1)(x - 2))");
        ContenidoDerivada c = ContenidoDerivada.de(r, SERVICIO.caminos(r), false);
        VistaExploracion.Tarjeta expandir = c.exploracion().tarjetas().stream()
                .filter(t -> t.codigo().equals("EXP")).findFirst().orElseThrow();
        assertEquals(VistaExploracion.Estado.PROBAR, expandir.estado());
        assertEquals("Expandir primero y derivar término a término", expandir.nombre());
        assertTrue(expandir.vistaPrevia().contains("2x − 1"), expandir.vistaPrevia());
    }

    @Test
    void laVistaDeUnaDerivadaTienePasosYVerificado() {
        ResolucionDerivada r = derivada("d/dx(x^2 sen x)");
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertEquals("Regla del producto", v.pasos().getFirst().titulo());
        assertEquals(2, v.pasos().getFirst().hijos().size());
        assertEquals("Simplificar", v.pasos().getLast().titulo());
        assertTrue(v.latexFormula().contains("\\frac{d}{dx}"), v.latexFormula());
    }

    @Test
    void integralDefinidaConAreaYSimpson() {
        ResolucionIntegral r = integral("∫(x^2, x, 0, 1)");
        ContenidoIntegral c = ContenidoIntegral.de(r, SERVICIO.caminos(r), false);
        DatosGrafica.Area area = c.grafica().orElseThrow().extra().calculo().area();
        assertEquals(0, area.a());
        assertEquals(1, area.b());
        assertEquals("∫ = 1/3", area.valor());
        assertEquals("sí, exacto", c.secciones().get(0).filas().get(3).valor());
        SeccionAnalisis simpson = c.secciones().get(1);
        assertTrue(simpson.filas().get(1).nombre().contains("n = 100"));
        assertTrue(simpson.filas().get(1).valor().startsWith("0.333333333"), simpson.filas().get(1).valor());

        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertEquals("Regla de Barrow", v.pasos().getLast().titulo());
    }

    @Test
    void integralIndefinidaConAntiderivada() {
        ResolucionIntegral r = integral("∫(x e^(x^2), x)");
        ContenidoIntegral c = ContenidoIntegral.de(r, SERVICIO.caminos(r), false);
        DatosGrafica.Calculo g = c.grafica().orElseThrow().extra().calculo();
        assertEquals("F(x) con C = 0", g.nombreSegunda());
        assertNull(g.area());
        assertEquals("sí, exacto", c.secciones().getFirst().filas().get(3).valor());
        assertEquals(1, c.secciones().size()); // sin Simpson en una indefinida
    }

    @Test
    void integralSinMetodoNoVerifica() {
        // ∫x·sen x ya se resuelve por partes (Sprint 7); e^(x²) no tiene antiderivada elemental
        ResolucionIntegral r = integral("∫(e^(x^2), x)");
        VistaResolucion v = VistaResolucion.de(r, false);
        assertEquals(VistaResolucion.Etiqueta.SIN_CALCULAR, v.etiqueta());
        assertEquals(pe.mathnova.domain.calculo.integrales.MotorIntegrales.SIN_METODO, v.notas().getFirst());
        ContenidoIntegral c = ContenidoIntegral.de(r, SERVICIO.caminos(r), false);
        assertTrue(c.exploracion().tarjetas().stream().anyMatch(t -> t.codigo().equals("I03")
                && t.estado() == VistaExploracion.Estado.NO_APLICA));
    }

    @Test
    void exploracionDeFraccionesParciales() {
        ResolucionIntegral r = integral("∫((x + 1)/(x^2 + 1), x)");
        ContenidoIntegral c = ContenidoIntegral.de(r, SERVICIO.caminos(r), false);
        VistaExploracion.Tarjeta sustitucion = c.exploracion().tarjetas().stream()
                .filter(t -> t.codigo().equals("I02")).findFirst().orElseThrow();
        assertEquals(VistaExploracion.Estado.NO_APLICA, sustitucion.estado());
        assertNotNull(sustitucion.motivo());
    }

    /** RN16, RN17 y RN18 quedan activas y su plantilla se resuelve de verdad. */
    @Test
    void lasSugerenciasDeDerivadaEIntegralSeResuelven() {
        List<Sugerencia> s = FACTORIZACION.sugerencias(FACTORIZACION.factorizar("x^3 - 3x", null,
                pe.mathnova.domain.factorizacion.Campo.Q, f -> { }));
        s = new java.util.ArrayList<>(s);
        s.addAll(FACTORIZACION.sugerencias(FACTORIZACION.simplificar("(3x + 5)/((x - 1)(x + 2))", f -> { })));
        s.addAll(FACTORIZACION.sugerencias(FACTORIZACION.simplificar("(x^3 + 1)/(x - 1)", f -> { })));
        List<String> reglas = s.stream().filter(Sugerencia::disponible).map(Sugerencia::regla).toList();
        assertTrue(reglas.containsAll(List.of("RN16", "RN17", "RN18")), reglas.toString());
        for (Sugerencia x : s) {
            if (x.operacion().equals("INTEGRAR")) {
                assertInstanceOf(ResolverService.Plan.Integrar.class, SERVICIO.decidir(x.plantilla()));
                assertTrue(integral(x.plantilla()).resuelta(), x.plantilla());
            } else if (x.operacion().equals("PUNTOS_CRITICOS")) {
                assertInstanceOf(ResolverService.Plan.Derivar.class, SERVICIO.decidir(x.plantilla()));
                assertNotNull(derivada(x.plantilla()).analisis(), x.plantilla());
            }
        }
    }

    /** Lo que escribe el editor (MathLive) llega como derivada o integral y se resuelve. */
    @Test
    void desdeElEditor() {
        assertEquals("= 2x·sen x + x²·cos x", SERVICIO.enVivo(TraductorLatex.traducir(
                "\\frac{d}{dx}\\left(x^2\\sin x\\right)")).orElseThrow());
        assertEquals("= 6x", SERVICIO.enVivo(TraductorLatex.traducir("\\frac{d^2}{dx^2}\\left(x^3-3x\\right)"))
                .orElseThrow());
        assertEquals("= 243/2 ≈ 121.5", SERVICIO.enVivo(TraductorLatex.traducir("3\\int_{3}^{6}3x\\,dx")).orElseThrow());
        assertFalse(SERVICIO.enVivo(TraductorLatex.traducir("\\int x^2\\,dx")).isEmpty());
        // D08 desde el editor
        assertEquals("d/dx(x^2 + y^2 = 25)", TraductorLatex.traducir("\\frac{d}{dx}\\left(x^2+y^2=25\\right)"));
        assertEquals("dy/dx = −x/y", SERVICIO.enVivo(TraductorLatex.traducir(
                "\\frac{d}{dx}\\left(x^2+y^2=25\\right)")).orElseThrow());
    }
}
