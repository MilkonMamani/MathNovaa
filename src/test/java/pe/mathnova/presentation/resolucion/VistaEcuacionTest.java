package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.ecuacion.ResolucionEcuacion;
import pe.mathnova.domain.ecuacion.ResolvedorEcuaciones;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;

class VistaEcuacionTest {

    private static final ResolvedorEcuaciones RESOLVEDOR = new ResolvedorEcuaciones(new OrquestadorFactorizacion());

    private static VistaResolucion vista(String texto) {
        ResolucionEcuacion r = RESOLVEDOR.resolver((Ecuacion) Parser.parse(texto));
        return VistaResolucion.de(r, false);
    }

    @Test
    void pasosEnOrden() {
        VistaResolucion v = vista("2x^2 = 3x + 2");
        List<String> titulos = v.pasos().stream().map(VistaPaso::titulo).toList();
        assertEquals("Pasar todo a un lado", titulos.getFirst());
        assertEquals(List.of("Igualar cada factor a cero", "Despejar"), titulos.subList(titulos.size() - 2, titulos.size()));
        assertTrue(titulos.size() >= 4, titulos.toString());
        for (int i = 0; i < v.pasos().size(); i++) {
            assertEquals(i + 1, v.pasos().get(i).orden());
        }
        assertNull(v.formula());
        assertEquals("x_{1} = -\\frac{1}{2},\\quad x_{2} = 2", v.latexFormula());
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta());
        assertEquals("Ecuación de grado 2 · 2 soluciones en Q", v.subtitulo());
    }

    @Test
    void despejarMuestraLasSolucionesConSignoDePizarra() {
        VistaPaso despejar = vista("2x^2 = 3x + 2").pasos().getLast();
        List<String> lineas = ((TrabajoIntermedio.Agrupacion) despejar.trabajo()).grupos();
        assertTrue(lineas.contains("x₁ = −1/2"), lineas.toString());
        assertTrue(lineas.contains("x₂ = 2"), lineas.toString());
    }

    @Test
    void verificaReemplazandoCadaSolucion() {
        VistaResolucion v = vista("x^3 = x");
        assertEquals(3, v.verificacion().size());
        assertTrue(v.verificacion().stream().allMatch(l -> l.endsWith("✓")), v.verificacion().toString());
        assertTrue(v.coincide());
    }

    @Test
    void raizDoble() {
        assertEquals("x = 1\\ \\text{(doble)}", vista("(x - 1)^2 = 0").latexFormula());
    }

    @Test
    void sinSolucion() {
        VistaResolucion v = vista("x^2 + 1 = 0");
        assertEquals(VistaResolucion.Etiqueta.SIN_SOLUCION, v.etiqueta());
        assertEquals(List.of("x² + 1 = 0 no tiene solución en Q."), v.notas());
    }
}
