package pe.mathnova.domain.calculo.limites;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsintotasTest {

    private static final SimplificadorFracciones FRACCIONES = new SimplificadorFracciones(new OrquestadorFactorizacion());

    private static Asintotas de(String texto) {
        return Asintotas.de(FRACCIONES.simplificar(Parser.parse(texto))).orElseThrow();
    }

    @Test
    void verticalYOblicua() {
        Asintotas a = de("(x^2 + 1)/(x - 1)");
        assertEquals(List.of("x = 1"), a.textoVerticales());
        assertEquals(Optional.of("y = x + 1"), a.textoOblicua());
        assertTrue(a.horizontal().isEmpty());
    }

    @Test
    void horizontalYVerticalesConRadicales() {
        Asintotas a = de("(2x^2 + 1)/(x^2 - 3)");
        assertEquals(List.of("x = -√3", "x = √3"), a.textoVerticales());
        assertEquals(Optional.of("y = 2"), a.textoHorizontal());
        assertTrue(a.oblicua().isEmpty());
    }

    @Test
    void unFactorCanceladoEsUnHuecoNoUnaAsintota() {
        Asintotas a = de("(x^2 - 4)/(x - 2)");
        assertTrue(a.ninguna(), a::toString);
        Asintotas b = de("(x - 2)/(x^2 - 4)");
        assertEquals(List.of("x = -2"), b.textoVerticales());
        assertEquals(Optional.of("y = 0"), b.textoHorizontal());
    }

    @Test
    void sinRaicesRealesNoHayVertical() {
        Asintotas a = de("1/(x^2 + 1)");
        assertTrue(a.verticales().isEmpty());
        assertEquals(Optional.of("y = 0"), a.textoHorizontal());
        assertTrue(de("x^3/(x - 1)").horizontal().isEmpty());
        assertTrue(de("x^3/(x - 1)").oblicua().isEmpty());
    }
}
