package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.application.service.LimiteService;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.TablaValores;
import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.parser.Parser;

/** Sprint 5, corrección 2: qué muestran Gráfica, Análisis y Exploración según el tipo de ejercicio. */
class ContenidoPestanasTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final LimiteService LIMITES = new LimiteService(MOTOR);
    private static final FactorizacionService FACTORIZACION = new FactorizacionService(MOTOR);

    private static VistaExploracion.Tarjeta tarjeta(VistaExploracion x, String codigo) {
        return x.tarjetas().stream().filter(t -> t.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new AssertionError("no hay tarjeta " + codigo + " en " + x.tarjetas()));
    }

    @Test
    void limite() {
        ResolucionLimite r = LIMITES.calcular("lim(x→2, (x^2 - 4)/(x - 2))");
        ContenidoPestanas c = ContenidoLimite.de(r, LIMITES, false);

        // Gráfica
        assertEquals("\\lim_{x \\to 2} f(x) = 4", c.latexSolucion().replace("\\to2", "\\to 2"));
        assertTrue(c.grafica().isPresent());
        assertNotNull(c.grafica().get().extra().limite());

        // Análisis: 6 filas, por la izquierda 1.9 → 1.999 y por la derecha 2.1 → 2.001, y "→ 4"
        TablaLimite t = c.tabla().orElseThrow();
        List<TablaValores.Fila> filas = t.filas();
        assertEquals(6, filas.size());
        assertEquals(List.of("1.9", "1.99", "1.999"), t.izquierda().stream().map(TablaValores.Fila::x).toList());
        assertEquals(List.of("2.1", "2.01", "2.001"), t.derecha().stream().map(TablaValores.Fila::x).toList());
        for (TablaValores.Fila f : filas) {
            assertEquals(6, f.valor().length() - f.valor().indexOf('.') - 1, f.valor()); // 6 decimales
        }
        assertTrue(Math.abs(Double.parseDouble(filas.getLast().valor()) - 4) < 0.01, filas.getLast().valor());
        assertTrue(Math.abs(Double.parseDouble(t.izquierda().getLast().valor()) - 4) < 0.01);
        assertEquals("→ 4", t.tiende());

        // Exploración: L02 usado; L'Hôpital y la tabla también sirven; sin comparación Q/R/C
        VistaExploracion x = c.exploracion();
        assertEquals(VistaExploracion.Estado.USADO, tarjeta(x, "L02").estado());
        assertEquals("Factorizar y cancelar", tarjeta(x, "L02").nombre());
        VistaExploracion.Tarjeta lhopital = tarjeta(x, "L07");
        assertEquals(VistaExploracion.Estado.PROBAR, lhopital.estado());
        assertEquals("(2x)/1 en x = 2 → 4", lhopital.vistaPrevia());
        VistaExploracion.Tarjeta tabla = tarjeta(x, "TABLA");
        assertEquals(VistaExploracion.Estado.PROBAR, tabla.estado());
        assertEquals("se acerca a 4 por ambos lados", tabla.vistaPrevia());
        assertEquals(VistaExploracion.Accion.ABRIR_ANALISIS, tabla.accion());
        VistaExploracion.Tarjeta notable = tarjeta(x, "L06");
        assertEquals(VistaExploracion.Estado.NO_APLICA, notable.estado());
        assertEquals("no hay sen, cos, e ni ln", notable.motivo());
        assertTrue(x.probables());
        assertEquals(Optional.empty(), c.comparacion());
    }

    /** "Probar" L'Hôpital vuelve a resolver con L'Hôpital y llega al mismo 4. */
    @Test
    void probarLHopital() {
        ResolucionLimite r = LIMITES.resolver("lim(x→2, (x^2 - 4)/(x - 2))", "L07", fase -> { });
        assertEquals("4", r.resultado().texto());
        assertEquals("L07", r.metodoFinal());
        VistaExploracion x = ContenidoLimite.de(r, LIMITES, false).exploracion();
        assertEquals(VistaExploracion.Estado.USADO, tarjeta(x, "L07").estado());
        assertEquals(VistaExploracion.Estado.PROBAR, tarjeta(x, "L02").estado());
    }

    @Test
    void fraccion() {
        String texto = "(x^2 - 2)/(x - 1)";
        Simplificacion s = FACTORIZACION.simplificar(texto, fase -> { });
        VistaResolucion v = VistaResolucion.de(s, FACTORIZACION.verificar(s), false);
        ContenidoPestanas c = ContenidoFraccion.de(s, v, FACTORIZACION.asintotas(s), texto, false);
        assertEquals(Optional.of(texto), c.comparacion());
        assertTrue(c.tabla().isEmpty());

        FactorizacionService.Comparacion comp = FACTORIZACION.comparar(c.comparacion().get());
        assertEquals("\\frac{\\left(x - \\sqrt{2}\\right)\\left(x + \\sqrt{2}\\right)}{x - 1}",
                comp.latex().get(Campo.R));
        assertTrue(comp.latex().get(Campo.Q).contains("x^{2} - 2"), comp.latex().get(Campo.Q));
        assertFalse(comp.iguales());

        // Si el resultado es el mismo en los tres campos, una sola vez con la nota.
        FactorizacionService.Comparacion igual = FACTORIZACION.comparar("(x^2 - 1)/(x + 1)");
        assertTrue(igual.iguales());
        assertEquals(FactorizacionService.MISMA_FRACCION, igual.nota());
        assertEquals(3, igual.latex().size());
        assertTrue(igual.avisos().values().stream().noneMatch(a -> a.contains("se simplifican en Q")));
    }

    @Test
    void factorizacion() {
        String texto = "x^2 - 5x + 6";
        Resolucion r = FACTORIZACION.factorizar(texto);
        VistaResolucion v = VistaResolucion.de(r, FACTORIZACION.verificar(r), false);
        ContenidoPestanas c = ContenidoFactorizacion.de(r, v, texto, false);
        assertEquals(v.latexFormula(), c.latexSolucion());
        assertTrue(c.propiedades().isPresent());
        assertTrue(c.tabla().isEmpty());
        assertFalse(c.exploracion().tarjetas().isEmpty());
        assertEquals(Optional.of(texto), c.comparacion());
        assertEquals(3, FACTORIZACION.comparar(texto).latex().size());
    }

    @Test
    void ecuacion() {
        ResolucionEcuacion r = new ResolvedorEcuaciones(MOTOR).resolver((Ecuacion) Parser.parse("x^2 - 2 = 0"));
        VistaResolucion v = VistaResolucion.de(r, false);
        ContenidoPestanas c = ContenidoEcuacion.de(r, v, false);
        assertTrue(c.tabla().isEmpty());
        assertTrue(c.comparacion().isPresent());
        // Se compara el polinomio, no la ecuación: ningún campo se queda sin resultado.
        FactorizacionService.Comparacion comp = FACTORIZACION.comparar(c.comparacion().get());
        assertEquals(3, comp.latex().size());
        assertTrue(comp.latex().get(Campo.R).contains("\\sqrt{2}"));
    }

    /** Corrección 4: nada que se resolvió sale como "No se pudo calcular". */
    @Test
    void nuncaNoSePudoCalcular() {
        for (String texto : List.of("x^2 - 2", "(x^2 - 2)/(x - 1)", "(x^2 - 4)/(x - 2)", "e^(2x) - 1", "x^5 - x - 1")) {
            FactorizacionService.Comparacion comp = FACTORIZACION.comparar(texto);
            assertTrue(comp.avisos().values().stream().noneMatch(a -> a.contains("No se pudo calcular")), texto);
            assertTrue(comp.latex().containsKey(Campo.Q), texto);
        }
    }
}
