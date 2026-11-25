package pe.mathnova.presentation.resolucion;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.LimiteService;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.calculo.limites.TablaValores;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.presentation.grafica.DatosGrafica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parte F del Sprint 5, sin JavaFX: lo que muestra la pantalla para un límite. */
class VistaLimiteTest {

    private static final LimiteService SERVICIO = new LimiteService(new OrquestadorFactorizacion());

    @Test
    void tarjetaYPasosAnidados() {
        ResolucionLimite r = SERVICIO.calcular("lim(x→2, (x^2 - 4)/(x - 2))");
        VistaResolucion v = VistaResolucion.de(r, false);
        assertTrue(v.latexFormula().startsWith("\\lim"), v.latexFormula());
        assertTrue(v.latexFormula().endsWith("= 4"), v.latexFormula());
        assertEquals("Límite cuando x → 2 · 0/0 · Factorizar y cancelar", v.subtitulo());
        assertEquals(VistaResolucion.Etiqueta.CALCULADO, v.etiqueta());
        assertTrue(v.notas().getFirst().contains("Indeterminación encontrada: 0/0"));
        assertTrue(v.notas().getFirst().contains("Método final: Factorizar y cancelar"));
        VistaPaso l02 = v.pasos().get(1);
        assertEquals("Factorizar y cancelar", l02.titulo());
        assertEquals("Factorizar numerador y denominador", l02.grupo());
        assertEquals(List.of("F05"), l02.hijos().stream().map(VistaPaso::codigo).toList());
        assertNull(v.formula());
    }

    @Test
    void noExisteYSinCalcular() {
        VistaResolucion v = VistaResolucion.de(SERVICIO.calcular("lim(x→2, 1/(x - 2))"), false);
        assertEquals(VistaResolucion.Etiqueta.NO_EXISTE, v.etiqueta());
        assertTrue(v.latexFormula().endsWith("\\text{no existe}"));
        assertTrue(v.notas().getFirst().contains("lím por la izquierda = −∞ y por la derecha = +∞"));
        VistaResolucion s = VistaResolucion.de(SERVICIO.calcular("lim(x→0, sen(1/x))"), false);
        assertEquals(VistaResolucion.Etiqueta.SIN_CALCULAR, s.etiqueta());
        assertTrue(s.notas().getFirst().contains("No se pudo calcular con los métodos disponibles"));
    }

    @Test
    void tablaDeValoresSeisPuntos() {
        TablaValores t = SERVICIO.tabla(SERVICIO.calcular("lim(x→2, (x^2 - 4)/(x - 2))"));
        assertEquals(List.of("1.9", "1.99", "1.999"), t.izquierda().stream().map(TablaValores.Fila::x).toList());
        assertEquals(List.of("2.001", "2.01", "2.1"), t.derecha().stream().map(TablaValores.Fila::x).toList());
        assertEquals("3.999000", t.izquierda().get(2).valor());
        assertEquals("4.100000", t.derecha().get(2).valor());
        TablaValores inf = SERVICIO.tabla(SERVICIO.calcular("lim(x→∞, (2x^2 + 1)/(x^2 - 3))"));
        assertTrue(inf.izquierda().isEmpty());
        assertEquals(6, inf.derecha().size());
        assertEquals("1000000", inf.derecha().getLast().x());
    }

    @Test
    void graficaDelLimite() {
        ResolucionLimite r = SERVICIO.calcular("lim(x→2, (x^2 - 4)/(x - 2))");
        DatosGrafica d = DatosGrafica.de(r, SERVICIO.fraccion(r)).orElseThrow();
        DatosGrafica.MarcaLimite m = d.extra().limite();
        assertEquals(2.0, m.a());
        assertEquals(4.0, m.valor());
        assertTrue(m.izquierda() && m.derecha());
        // El hueco (2, 4) lo marca la simplificación: el círculo no se repite.
        assertEquals(1, d.huecos().size());
        assertFalse(m.hueco());

        ResolucionLimite inf = SERVICIO.calcular("lim(x→∞, (2x^2 + 1)/(x^2 - 3))");
        DatosGrafica di = DatosGrafica.de(inf, SERVICIO.fraccion(inf)).orElseThrow();
        assertNull(di.extra().limite().a());
        assertEquals(2.0, di.extra().limite().valor());
        assertEquals(2.0, di.extra().horizontal());
        assertEquals(2, di.extra().verticales().size()); // ±√3

        ResolucionLimite sen = SERVICIO.calcular("lim(x→0, sen(x)/x)");
        DatosGrafica ds = DatosGrafica.de(sen, Optional.empty()).orElseThrow();
        assertTrue(ds.extra().limite().hueco(), "sen x/x no está definida en 0");
    }

    @Test
    void asintotaOblicuaEnLaGrafica() {
        ResolucionLimite r = SERVICIO.calcular("lim(x→∞, (x^2 + 1)/(x - 1))");
        DatosGrafica d = DatosGrafica.de(r, SERVICIO.fraccion(r)).orElseThrow();
        assertEquals(new DatosGrafica.Recta(1.0, 1.0), d.extra().oblicua());
        assertEquals(1, d.asintotas().size());
    }
}
