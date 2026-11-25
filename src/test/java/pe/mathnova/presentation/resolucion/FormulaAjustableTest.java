package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FormulaAjustableTest {

    @Test
    void seReduceDe28Hasta18AntesDePartir() {
        FormulaAjustable.Disposicion d = FormulaAjustable.elegir(560, 300, 100, 566, 28, 18);
        assertEquals(new FormulaAjustable.Disposicion(false, 22, 1, true), d);
    }

    @Test
    void laEtiquetaBajaCuandoNoHayLugarAlCostado() {
        FormulaAjustable.Disposicion d = FormulaAjustable.elegir(560, 300, 100, 400, 28, 18);
        assertFalse(d.lateralAlCostado());
        assertFalse(d.dosLineas());
        assertEquals(1, d.escala());
    }

    @Test
    void siA18TampocoCabePasaADosLineas() {
        FormulaAjustable.Disposicion d = FormulaAjustable.elegir(900, 460, 100, 300, 28, 18);
        assertTrue(d.dosLineas());
        assertEquals(18, d.tamano());
        assertEquals(1, d.escala(), 1e-9);
        assertFalse(d.lateralAlCostado());
    }

    @Test
    void comoUltimoRecursoSeEncogeYNuncaSeCorta() {
        FormulaAjustable.Disposicion d = FormulaAjustable.elegir(2000, 1400, 100, 300, 28, 18);
        assertTrue(d.dosLineas());
        assertEquals(300, 1400 * 18.0 / 28 * d.escala(), 1e-6);
    }

    @Test
    void partePreguntaYResultado() {
        assertArrayEquals(new String[] {"\\int x\\,dx", "= \\frac{x^{2}}{2}+C"},
                FormulaAjustable.partir("\\int x\\,dx = \\frac{x^{2}}{2}+C"));
        assertArrayEquals(new String[] {"\\lim_{x \\to 2}\\frac{x^2-4}{x-2}", "= 4"},
                FormulaAjustable.partir("\\lim_{x \\to 2}\\frac{x^2-4}{x-2} = 4"));
        assertArrayEquals(new String[] {"\\frac{a=b}{2}", "= 1"}, FormulaAjustable.partir("\\frac{a=b}{2} = 1"));
    }

    @Test
    void lasSolucionesSePartenEnLaComa() {
        assertArrayEquals(new String[] {"x = 2,", "x = 3"}, FormulaAjustable.partir("x = 2, \\quad x = 3"));
    }

    @Test
    void sinIgualSeParteEnUnMasOEntreFactores() {
        String[] suma = FormulaAjustable.partir("x^{2}+3x+2x^{3}+5");
        assertEquals(2, suma.length);
        assertTrue(suma[1].startsWith("+"), suma[1]);
        String[] factores = FormulaAjustable.partir("\\left(x+1\\right)\\left(x-1\\right)");
        assertArrayEquals(new String[] {"\\left(x+1\\right)", "\\left(x-1\\right)"}, factores);
        assertNull(FormulaAjustable.partir("x"));
    }
}
