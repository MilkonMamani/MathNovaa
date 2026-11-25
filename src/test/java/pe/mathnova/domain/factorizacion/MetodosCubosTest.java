package pe.mathnova.domain.factorizacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.aplica;
import static pe.mathnova.domain.factorizacion.MetodosBasicosTest.noAplica;

import org.junit.jupiter.api.Test;

class MetodosCubosTest {

    @Test
    void f08SumaDeCubos() {
        var r = aplica(new SumaODiferenciaDeCubos(true), "27x^3 + 125", "3x + 5", "9x^2 - 15x + 25");
        assertEquals("27x³ = (3x)³ y 125 = 5³", r.paso().justificacion());
        assertEquals("a³ + b³ = (a + b)(a² − ab + b²) con a = 3x y b = 5",
                assertInstanceOf(TrabajoIntermedio.Comparacion.class, r.paso().trabajo()).detalle());
        aplica(new SumaODiferenciaDeCubos(true), "x^3 + 1", "x + 1", "x^2 - x + 1");
    }

    @Test
    void f09DiferenciaDeCubos() {
        var r = aplica(new SumaODiferenciaDeCubos(false), "64x^3 - 27", "4x - 3", "16x^2 + 12x + 9");
        assertEquals("64x³ = (4x)³ y 27 = 3³", r.paso().justificacion());
        aplica(new SumaODiferenciaDeCubos(false), "x^6 - 8", "x^2 - 2", "x^4 + 2x^2 + 4");
    }

    @Test
    void cubosNoAplican() {
        noAplica(new SumaODiferenciaDeCubos(true), "x^3 - 8", "Es una diferencia: corresponde F09");
        noAplica(new SumaODiferenciaDeCubos(false), "x^3 + 8", "Es una suma: corresponde F08");
        noAplica(new SumaODiferenciaDeCubos(true), "x^2 + 4", "x² no es un cubo perfecto");
        noAplica(new SumaODiferenciaDeCubos(false), "x^3 - 2", "2 no es un cubo perfecto");
    }

    @Test
    void f10CuboPerfecto() {
        var r = aplica(new CuboPerfecto(), "27x^3 + 108x^2 + 144x + 64", "3x + 4");
        assertEquals("27x³ = (3x)³, 64 = 4³, 108x² = 3·(3x)²·4 y 144x = 3·(3x)·4²", r.paso().justificacion());
        assertEquals(3, r.factores().getFirst().multiplicidad());
        var r2 = aplica(new CuboPerfecto(), "x^3 - 6x^2 + 12x - 8", "x - 2");
        assertEquals("x³ = (x)³, −8 = (−2)³, −6x² = 3·(x)²·(−2) y 12x = 3·(x)·(−2)²", r2.paso().justificacion());
        assertEquals("a³ − 3a²b + 3ab² − b³ = (a − b)³ con a = x y b = 2",
                assertInstanceOf(TrabajoIntermedio.Comparacion.class, r2.paso().trabajo()).detalle());
    }

    @Test
    void f10NoAplica() {
        noAplica(new CuboPerfecto(), "x^3 - 2x^2 - x + 2", "2 no es un cubo perfecto");
        noAplica(new CuboPerfecto(), "x^3 + 4x^2 + 7x + 8",
                "x³ = (x)³ y 8 = 2³, pero (x + 2)³ = x³ + 6x² + 12x + 8, que no coincide");
    }

    @Test
    void f11PotenciasEnesimas() {
        var r = aplica(new PotenciasEnesimas(), "x^5 - 32", "x - 2", "x^4 + 2x^3 + 4x^2 + 8x + 16");
        assertEquals("x⁵ = (x)⁵ y 32 = 2⁵, con n = 5 impar", r.paso().justificacion());
        aplica(new PotenciasEnesimas(), "x^7 + 1", "x + 1", "x^6 - x^5 + x^4 - x^3 + x^2 - x + 1");
        aplica(new PotenciasEnesimas(), "32x^5 - 1", "2x - 1", "16x^4 + 8x^3 + 4x^2 + 2x + 1");
        noAplica(new PotenciasEnesimas(), "x^5 - 2",
                "No hay un exponente impar n ≥ 5 con x⁵ y 2 potencias n-ésimas exactas");
    }
}
