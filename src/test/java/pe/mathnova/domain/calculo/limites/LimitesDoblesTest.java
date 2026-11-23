package pe.mathnova.domain.calculo.limites;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Camino;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Estado;
import pe.mathnova.domain.calculo.limites.ResolucionLimiteDoble.Trayectoria;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.ImpresoraPizarra;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.LimiteDoble;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

/** Sprint 8, Parte A (HU29, RF54): límites dobles con evaluación directa, trayectorias (V05) y polares (V06). */
class LimitesDoblesTest {

    private static final MotorLimitesDobles MOTOR = new MotorLimitesDobles();

    private static ResolucionLimiteDoble resolver(String texto) {
        return MOTOR.resolver((LimiteDoble) Parser.parse(texto));
    }

    private static Trayectoria trayectoria(ResolucionLimiteDoble r, String nombre) {
        return r.trayectorias().stream().filter(t -> t.nombre().equals(nombre)).findFirst()
                .orElseThrow(() -> new AssertionError("no se probó " + nombre + ": " + r.trayectorias()));
    }

    private static String lineas(PasoLimite p) {
        return String.join("\n", ((TrabajoIntermedio.Agrupacion) p.trabajo()).grupos());
    }

    // ------------------------------------------------------------------ gramática

    @Test
    void seLeeElLimiteDoble() {
        LimiteDoble l = assertInstanceOf(LimiteDoble.class, Parser.parse("lim((x,y)→(0,0), xy/(x^2 + y^2))"));
        assertAll(
                () -> assertEquals('x', l.x()),
                () -> assertEquals('y', l.y()),
                () -> assertEquals("lim((x, y)→(0, 0), xy/(x^2 + y^2))", ImpresoraTexto.imprimir(l)),
                () -> assertEquals(l, Parser.parse(ImpresoraTexto.imprimir(l))),
                () -> assertEquals(l, Parser.parse("lim((x, y) -> (0, 0), x y/(x^2 + y^2))")));
    }

    @Test
    void erroresDeLaGramatica() {
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("lim((x,x)→(0,0), x)"));
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("lim((x,y)→(∞,0), x)"));
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("lim((x,y)→(a,0), x)"));
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("lim((x,y)→0, x)"));
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("2 + lim((x,y)→(0,0), x)"));
    }

    @Test
    void elEditorLoReconoce() {
        String latex = "\\lim_{(x,y)\\to(0,0)}\\frac{xy}{x^2+y^2}";
        assertEquals("lim((x, y)→(0, 0), xy/(x^2 + y^2))", TraductorLatex.traducir(latex));
        LimiteDoble l = (LimiteDoble) Parser.parse("lim((x,y)→(1,2), x^2 + x y)");
        // lo que dibuja ImpresoraLatex vuelve a leerse igual
        assertEquals(l, Parser.parse(TraductorLatex.traducir(ImpresoraLatex.imprimir(l))));
    }

    // ------------------------------------------------------------------ los ejemplos exactos

    @Test
    void xyEntreSumaDeCuadradosNoExiste() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), xy/(x^2 + y^2))");
        Trayectoria recta = trayectoria(r, "y = mx");
        assertAll(r.texto(),
                () -> assertEquals(Estado.NO_EXISTE, r.estado()),
                () -> assertEquals("V05", r.metodo()),
                () -> assertTrue(recta.dependeDeM()),
                () -> assertEquals("m/(1 + m²)", ImpresoraPizarra.imprimir(recta.valor())),
                () -> assertTrue(r.motivo().contains("depende de m"), r.motivo()),
                () -> assertEquals("No existe", r.texto()),
                () -> assertEquals(List.of("L01", "V05"), r.codigos()));
    }

    @Test
    void conLaParabolaDaOtroValor() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), x^2 y/(x^4 + y^2))");
        Trayectoria recta = trayectoria(r, "y = mx");
        Trayectoria parabola = trayectoria(r, "y = x²");
        assertAll(r.texto(),
                () -> assertEquals(Estado.NO_EXISTE, r.estado()),
                () -> assertEquals("0", recta.texto()),
                () -> assertTrue(!recta.dependeDeM()),
                () -> assertEquals("1/2", parabola.texto()),
                () -> assertEquals(Camino.POTENCIA_EN_X, parabola.camino()),
                () -> assertEquals(2, parabola.n()),
                () -> assertTrue(r.motivo().contains("y = x²"), r.motivo()));
    }

    @Test
    void x2yEntreSumaDeCuadradosEsCeroPorPolares() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), x^2 y/(x^2 + y^2))");
        assertAll(r.texto(),
                () -> assertEquals(Estado.EXISTE, r.estado()),
                () -> assertEquals("0", r.texto()),
                () -> assertEquals("V06", r.metodo()),
                () -> assertNotNull(r.polar()),
                () -> assertEquals(1, r.polar().k()),
                () -> assertEquals("r·cos²θ·sen θ", ImpresoraPizarra.imprimir(r.polar().forma())),
                () -> assertTrue(r.polar().cota().contains("≤ r"), r.polar().cota()),
                () -> assertTrue(lineas(r.pasos().getLast()).contains("sándwich")),
                () -> assertEquals(List.of("L01", "V05", "V06"), r.codigos()));
        // las trayectorias coinciden, y aun así no se declara con ellas (RN38)
        assertTrue(lineas(r.pasos().get(1)).contains("RN38"));
    }

    @Test
    void x2y2EntreSumaDeCuadradosEsCeroPorPolares() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), x^2 y^2/(x^2 + y^2))");
        assertAll(r.texto(),
                () -> assertEquals(Estado.EXISTE, r.estado()),
                () -> assertEquals("0", r.texto()),
                () -> assertEquals("V06", r.metodo()),
                () -> assertEquals(2, r.polar().k()),
                () -> assertTrue(ImpresoraPizarra.imprimir(r.polar().forma()).startsWith("r²"),
                        ImpresoraPizarra.imprimir(r.polar().forma())));
    }

    @Test
    void evaluacionDirectaEnElOrigen() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), (x^2 + y^2)/(x + y + 1))");
        assertAll(
                () -> assertEquals(Estado.EXISTE, r.estado()),
                () -> assertEquals("0", r.texto()),
                () -> assertEquals("L01", r.metodo()),
                () -> assertTrue(r.trayectorias().isEmpty()),
                () -> assertNull(r.polar()),
                () -> assertEquals(1, r.pasos().size()));
    }

    @Test
    void evaluacionDirectaEnOtroPunto() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(1,2), x^2 + xy)");
        assertAll(
                () -> assertEquals(Estado.EXISTE, r.estado()),
                () -> assertEquals("3", r.texto()),
                () -> assertEquals("L01", r.metodo()),
                () -> assertTrue(r.latex().endsWith("= 3"), r.latex()));
    }

    // ------------------------------------------------------------------ más casos

    @Test
    void lasTrayectoriasNoBastanParaDecirQueExiste() {
        // x y²/(x² + y⁴): todas las rectas dan 0, pero x = y² da 1/2
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), x y^2/(x^2 + y^4))");
        assertEquals(Estado.NO_EXISTE, r.estado());
        assertEquals("1/2", trayectoria(r, "x = y²").texto());
    }

    @Test
    void sinMetodoQueDecidaSeDiceAsi() {
        // x³/(x² + y⁴): las trayectorias dan 0 y el denominador no es homogéneo
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), x^3/(x^2 + y^4))");
        assertAll(
                () -> assertEquals(Estado.NO_DECIDIDO, r.estado()),
                () -> assertEquals(MotorLimitesDobles.NO_DECIDIDO, r.texto()),
                () -> assertNull(r.valor()));
    }

    @Test
    void enUnPuntoQueNoEsElOrigen() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(1,2), (x - 1)(y - 2)/((x - 1)^2 + (y - 2)^2))");
        assertEquals(Estado.NO_EXISTE, r.estado());
        assertTrue(trayectoria(r, "y − 2 = m(x − 1)").dependeDeM());
    }

    @Test
    void unValorSobreCeroNoExiste() {
        ResolucionLimiteDoble r = resolver("lim((x,y)→(0,0), 1/(x^2 + y^2))");
        assertEquals(Estado.NO_EXISTE, r.estado());
        assertEquals("L01", r.metodo());
    }
}
