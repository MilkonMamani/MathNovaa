package pe.mathnova.domain.algebra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RacionalTest {

    @Test
    void seSimplificaAlCrearse() {
        Racional r = Racional.de(6, 8);
        assertEquals(BigInteger.valueOf(3), r.num());
        assertEquals(BigInteger.valueOf(4), r.den());
        assertEquals(Racional.de(3, 4), r);
        assertEquals(Racional.CERO, Racional.de(0, -5));
    }

    @Test
    void elDenominadorSiempreEsPositivo() {
        assertEquals("-1/2", Racional.de(1, -2).toString());
        assertEquals("1/2", Racional.de(-1, -2).toString());
        assertEquals(BigInteger.valueOf(2), Racional.de(3, -2).den());
        assertEquals(-1, Racional.de(3, -2).signo());
    }

    @Test
    void toStringDeEnterosYFracciones() {
        assertEquals("3", Racional.de(3).toString());
        assertEquals("3", Racional.de(6, 2).toString());
        assertEquals("-1/2", Racional.de(-1, 2).toString());
        assertTrue(Racional.de(6, 2).esEntero());
        assertFalse(Racional.de(1, 2).esEntero());
    }

    @Test
    void operaciones() {
        Racional a = Racional.de(1, 2);
        Racional b = Racional.de(1, 3);
        assertEquals(Racional.de(5, 6), a.sumar(b));
        assertEquals(Racional.de(1, 6), a.restar(b));
        assertEquals(Racional.de(1, 6), a.multiplicar(b));
        assertEquals(Racional.de(3, 2), a.dividir(b));
        assertEquals(Racional.de(-1, 2), a.negar());
        assertEquals(Racional.de(1, 8), a.potencia(3));
        assertEquals(Racional.de(8), a.potencia(-3));
        assertEquals(Racional.UNO, a.potencia(0));
    }

    @Test
    void divisionEntreCeroLanzaArithmeticException() {
        assertThrows(ArithmeticException.class, () -> Racional.de(1, 2).dividir(Racional.CERO));
        assertThrows(ArithmeticException.class, () -> Racional.de(1, 0));
        assertThrows(ArithmeticException.class, () -> Racional.CERO.potencia(-1));
    }

    @Test
    void compareTo() {
        assertTrue(Racional.de(1, 3).compareTo(Racional.de(1, 2)) < 0);
        assertTrue(Racional.de(-1, 2).compareTo(Racional.de(-2, 3)) > 0);
        assertEquals(0, Racional.de(2, 4).compareTo(Racional.de(1, 2)));
    }

    @ParameterizedTest
    @CsvSource({
            "0.25, 1, 4",
            "0.5, 1, 2",
            "-1.5, -3, 2",
            "3, 3, 1",
            "-7, -7, 1",
            "2.50, 5, 2",
            "0.125, 1, 8",
            "2/4, 1, 2",
            "10.0, 10, 1"
    })
    void parseDeDecimales(String texto, long num, long den) {
        assertEquals(Racional.de(num, den), Racional.parse(texto));
    }

    @ParameterizedTest
    @CsvSource({"abc", "1.", ".5", "1.2.3", "''", "-", "1e3"})
    void parseRechazaTextoInvalido(String texto) {
        assertThrows(NumberFormatException.class, () -> Racional.parse(texto));
    }

    @Test
    void propiedadesConDoscientosCasosAleatorios() {
        Random azar = new Random(20260101L);
        for (int k = 0; k < 200; k++) {
            Racional a = aleatorio(azar);
            Racional b = aleatorio(azar);
            assertEquals(a, a.sumar(b).restar(b), () -> "(a + b) - b con a=" + a + ", b=" + b);
            if (!b.esCero()) {
                assertEquals(a, a.multiplicar(b).dividir(b), () -> "(a * b) / b con a=" + a + ", b=" + b);
            }
        }
    }

    private static Racional aleatorio(Random azar) {
        long num = azar.nextInt(2_000_001) - 1_000_000L;
        long den = azar.nextInt(2_000_001) - 1_000_000L;
        return den == 0 ? Racional.de(num) : Racional.de(num, den);
    }
}
