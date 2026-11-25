package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.presentation.pizarra.AspaSimple;

class RenglonesPasoTest {

    private static final RenglonesPaso.Marco LIMITE = RenglonesPaso.Marco.de("CALCULA EL LÍMITE",
            "\\lim_{x \\to 3} \\frac{x^{2} - 9}{x - 3}");

    @Test
    void elMarcoDelLimiteTieneVariableYPunto() {
        assertEquals(RenglonesPaso.Operacion.LIMITE, LIMITE.operacion());
        assertEquals('x', LIMITE.variable());
        assertEquals("3", LIMITE.punto());
        assertEquals("\\lim_{x \\to 3}", LIMITE.limite());
    }

    @Test
    void elLimiteSeEscribeSoloDondeHayX() {
        assertEquals("\\lim_{x \\to 3} \\left(x + 3\\right)", RenglonesPaso.conLimite("x + 3", LIMITE));
        assertEquals("3 + 3", RenglonesPaso.conLimite("3 + 3", LIMITE));
        assertEquals("\\lim_{x \\to 3} \\frac{x - 1}{x}", RenglonesPaso.conLimite("\\frac{x - 1}{x}", LIMITE));
        assertFalse(RenglonesPaso.depende("\\frac{3}{\\text{max}}", LIMITE));
    }

    @Test
    void cancelarAnotaXDistintoDeA() {
        VistaPaso p = new VistaPaso(2, "L02", "Factorizar y cancelar", "L02", "porque",
                pe.mathnova.domain.parser.Parser.parse("(x^2 - 9)/(x - 3)"),
                new TrabajoIntermedio.Agrupacion(List.of("(x² − 9)/(x − 3) = ((x + 3)(x − 3))/(x − 3)", "= x + 3")),
                pe.mathnova.domain.parser.Parser.parse("x + 3"));
        List<Igualdades.Renglon> r = RenglonesPaso.renglones(p, LIMITE, true);
        assertEquals(2, r.size());
        assertTrue(r.get(0).latex().startsWith("\\lim_{x \\to 3} \\frac{x^{2} - 9}{x - 3} = \\lim_{x \\to 3}"),
                r.get(0).latex());
        assertTrue(r.get(1).latex().contains("\\lim_{x \\to 3} \\left(x + 3\\right)"), r.get(1).latex());
        assertTrue(r.get(1).latex().endsWith("(x \\neq 3)"), r.get(1).latex());
        assertNull(RenglonesPaso.conclusion(p, LIMITE, true, true, r.getLast()));
    }

    @Test
    void laEvaluacionEmpiezaEnElLimite() {
        VistaPaso p = new VistaPaso(3, "L01", "Evaluación directa", "L01", "porque",
                pe.mathnova.domain.parser.Parser.parse("x + 3"),
                new TrabajoIntermedio.Agrupacion(List.of("3 + 3 = 6")), pe.mathnova.domain.parser.Parser.parse("6"));
        List<Igualdades.Renglon> r = RenglonesPaso.renglones(p, LIMITE, true);
        assertEquals("\\lim_{x \\to 3} \\left(x + 3\\right) = 3 + 3 = 6", r.getFirst().latex());
    }

    @Test
    void laFactorizacionDentroDelLimiteNoLlevaLim() {
        VistaPaso p = new VistaPaso(1, "F05", "Diferencia de cuadrados", "F05", "porque", null,
                new TrabajoIntermedio.Agrupacion(List.of("x² − 9 = (x + 3)(x − 3)")), null);
        assertFalse(RenglonesPaso.renglones(p, LIMITE, false).getFirst().latex().contains("\\lim"));
    }

    @Test
    void reglaDeLaCadenaConValores() {
        RenglonesPaso.Marco d = RenglonesPaso.Marco.de("DERIVA", "\\frac{d}{dx}\\left((3x^{2} + 1)^{4}\\right)");
        VistaPaso p = new VistaPaso(1, "D06", "Regla de la cadena", "D06", "porque",
                pe.mathnova.domain.parser.Parser.parse("(3x^2 + 1)^4"),
                new TrabajoIntermedio.Agrupacion(List.of("(uⁿ)' = n·uⁿ⁻¹·u'", "u = 3x² + 1", "u' = 6x",
                        "((3x² + 1)⁴)' = 4(3x² + 1)³·6x")),
                pe.mathnova.domain.parser.Parser.parse("24(3x^2 + 1)^3 x"));
        List<Igualdades.Renglon> r = RenglonesPaso.renglones(p, d, true);
        assertTrue(r.getFirst().latex().startsWith("(u^{n})'"), r.getFirst().latex());
        assertEquals("y' = 4(3x^{2} + 1)^{3}\\cdot 6x", r.getLast().latex());
        assertTrue(RenglonesPaso.conclusion(p, d, true, true, r.getLast()).startsWith("= "));
    }

    @Test
    void aspaSimpleDescomponeAgrupaYFactoriza() {
        var a = new TrabajoIntermedio.AspaSimple("", "", "", 'x', Racional.UNO, Racional.de(-2), Racional.UNO,
                Racional.de(-3));
        assertEquals(List.of("x^{2} - 5x + 6 = x^{2} - 2x - 3x + 6", "= x(x - 2) - 3(x - 2)", "= (x - 2)(x - 3)"),
                AspaSimple.cadena(a));
    }
}
