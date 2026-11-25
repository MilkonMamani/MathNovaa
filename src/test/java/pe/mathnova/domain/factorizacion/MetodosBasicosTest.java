package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.factorizacion.MetodoFactorizacion.ResultadoMetodo;
import pe.mathnova.domain.parser.Parser;

class MetodosBasicosTest {

    static Polinomio p(String texto) {
        return Polinomio.desde(Parser.parse(texto));
    }

    static ResultadoMetodo aplica(MetodoFactorizacion m, String entrada, String... factores) {
        Polinomio antes = p(entrada);
        assertTrue(m.puedeAplicarse(antes, Campo.Q).aplica(), () -> m.puedeAplicarse(antes, Campo.Q).motivo());
        ResultadoMetodo r = m.aplicar(antes, Campo.Q);
        Polinomio producto = Polinomio.UNO;
        for (Factor f : r.factores()) {
            producto = producto.multiplicar(f.expandir());
        }
        Polinomio expandido = producto;
        List<Polinomio> esperados = java.util.Arrays.stream(factores).map(MetodosBasicosTest::p).toList();
        assertAll(
                () -> assertEquals(antes, expandido, "los factores multiplicados dan la entrada"),
                () -> assertEquals(esperados, r.factores().stream().map(Factor::polinomio).toList()),
                () -> assertEquals(m.codigo(), r.paso().codigoTecnica()),
                () -> assertEquals(antes, r.paso().antes()),
                () -> assertFalse(r.paso().descartado()));
        return r;
    }

    static void noAplica(MetodoFactorizacion m, String entrada, String motivo) {
        Polinomio antes = p(entrada);
        var a = m.puedeAplicarse(antes, Campo.Q);
        assertFalse(a.aplica());
        assertEquals(motivo, a.motivo());
        assertThrows(IllegalArgumentException.class, () -> m.aplicar(antes, Campo.Q));
    }

    @Test
    void f01SacaMcdYMenorPotencia() {
        var r = aplica(FactorComun.monomio(), "6x^3 + 9x^2", "3", "x^2", "2x + 3");
        assertEquals("MCD(6, 9) = 3 y la menor potencia de x es x²", r.paso().justificacion());
        assertEquals("3x^2(2x + 3)", r.paso().despuesComoForma().aTexto());
    }

    @Test
    void f01ConSignoNegativo() {
        var r = aplica(FactorComun.monomio(), "-x^2 + 36", "-1", "x^2 - 36");
        assertEquals("MCD(1, 36) = 1, no todos los términos tienen x y el coeficiente principal es negativo: "
                + "se extrae −1", r.paso().justificacion());
        assertEquals("-(x^2 - 36)", r.paso().despuesComoForma().aTexto());
    }

    @Test
    void f01NoAplica() {
        noAplica(FactorComun.monomio(), "x^3 - 2x^2 - x + 2", "MCD(1, 2, 1, 2) = 1 y no todos los términos tienen x");
        noAplica(FactorComun.monomio(), "x/2 + 1", "Hay coeficientes fraccionarios: corresponde F02");
    }

    @Test
    void f02ContenidoYPartePrimitiva() {
        var r = aplica(FactorComun.fraccionario(), "-(3/5)x + 1", "-1/5", "3x - 5");
        assertEquals("Contenido = MCD(3, 1)/MCM(5, 1) = 1/5, no todos los términos tienen x y el coeficiente "
                + "principal es negativo: se extrae −1/5", r.paso().justificacion());
        aplica(FactorComun.fraccionario(), "2x^2 - (7/2)x", "1/2", "x", "4x - 7");
        noAplica(FactorComun.fraccionario(), "2x + 4", "Todos los coeficientes son enteros: corresponde F01");
    }

    @Test
    void f05DiferenciaDeCuadrados() {
        var r = aplica(new DiferenciaDeCuadrados(), "4x^2 - 25", "2x + 5", "2x - 5");
        assertEquals("4x² = (2x)² y 25 = 5²", r.paso().justificacion());
        var trabajo = assertInstanceOf(TrabajoIntermedio.Comparacion.class, r.paso().trabajo());
        assertEquals("a² − b² = (a + b)(a − b) con a = 2x y b = 5", trabajo.detalle());
        aplica(new DiferenciaDeCuadrados(), "x^4 - 16", "x^2 + 4", "x^2 - 4");
    }

    @Test
    void f05NoAplica() {
        noAplica(new DiferenciaDeCuadrados(), "x^2 + 4", "x² + 4 es una suma, no una diferencia");
        noAplica(new DiferenciaDeCuadrados(), "x^2 - 3", "3 no es un cuadrado perfecto");
        noAplica(new DiferenciaDeCuadrados(), "x^3 - 1", "x³ no es un cuadrado perfecto");
        noAplica(new DiferenciaDeCuadrados(), "x^2 + x + 1", "Tiene 3 términos; hacen falta 2");
    }

    @Test
    void f07TrinomioCuadradoPerfecto() {
        var r = aplica(new TrinomioCuadradoPerfecto(), "9x^2 - 12x + 4", "3x - 2");
        assertEquals("9x² = (3x)², 4 = 2² y 12x = 2·3x·2", r.paso().justificacion());
        assertEquals(2, r.factores().getFirst().multiplicidad());
        assertEquals("(3x - 2)^2", r.paso().despuesComoForma().aTexto());
        aplica(new TrinomioCuadradoPerfecto(), "16x^2 + 72x + 81", "4x + 9");
    }

    @Test
    void f07NoAplica() {
        noAplica(new TrinomioCuadradoPerfecto(), "x^2 + x + 1",
                "x² y 1 son cuadrados, pero x no es 2·x·1 = 2x");
        noAplica(new TrinomioCuadradoPerfecto(), "6x^2 + x + 2", "6x² no es un cuadrado perfecto");
        noAplica(new TrinomioCuadradoPerfecto(), "x^2 + 2x - 1", "−1 es negativo: no es un cuadrado");
    }

    @Test
    void f12TrinomioSimple() {
        var r = aplica(new TrinomioSimple(), "x^2 - 5x + 6", "x - 2", "x - 3");
        assertEquals("−2 + (−3) = −5 y (−2)·(−3) = 6", r.paso().justificacion());
        var aspa = assertInstanceOf(TrabajoIntermedio.AspaSimple.class, r.paso().trabajo());
        assertAll(
                () -> assertEquals(pe.mathnova.domain.algebra.Racional.de(-2), aspa.c1()),
                () -> assertEquals(pe.mathnova.domain.algebra.Racional.de(-3), aspa.c2()),
                () -> assertEquals("x·(−3) + x·(−2) = −3x − 2x = −5x", aspa.verificacion()));
        var r2 = aplica(new TrinomioSimple(), "x^2 + x - 20", "x + 5", "x - 4");
        assertEquals("5 + (−4) = 1 y 5·(−4) = −20", r2.paso().justificacion());
    }

    @Test
    void f12NoAplica() {
        noAplica(new TrinomioSimple(), "x^2 + x + 1", "No hay enteros m y n con m + n = 1 y m·n = 1");
        noAplica(new TrinomioSimple(), "6x^2 + x - 2", "El coeficiente de x² es 6, no 1: corresponde F13");
    }

    @Test
    void f13AspaSimple() {
        var r = aplica(new TrinomioAspa(), "6x^2 + x - 2", "2x - 1", "3x + 2");
        assertEquals("Δ = 1² − 4·6·(−2) = 49 = 7², cuadrado perfecto", r.paso().justificacion());
        var aspa = assertInstanceOf(TrabajoIntermedio.AspaSimple.class, r.paso().trabajo());
        assertAll(
                () -> assertEquals("2x    −1", aspa.fila1()),
                () -> assertEquals("3x    +2", aspa.fila2()),
                () -> assertEquals("2x·2 + 3x·(−1) = 4x − 3x = x", aspa.verificacion()));
        aplica(new TrinomioAspa(), "18x^2 - 9x - 2", "3x - 2", "6x + 1");
    }

    @Test
    void f13NoAplica() {
        noAplica(new TrinomioAspa(), "2x^2 + x + 1", "Δ = 1² − 4·2·1 = −7 < 0");
        noAplica(new TrinomioAspa(), "2x^2 + x - 2", "Δ = 1² − 4·2·(−2) = 17 no es un cuadrado perfecto");
        noAplica(new TrinomioAspa(), "x^3 + x - 2", "Es de grado 3, no 2");
    }
}
