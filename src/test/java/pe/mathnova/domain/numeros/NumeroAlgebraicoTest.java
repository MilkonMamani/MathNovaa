package pe.mathnova.domain.numeros;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.parser.Parser;

class NumeroAlgebraicoTest {

    private static final NumeroAlgebraico UNO = NumeroAlgebraico.UNO;
    private static final NumeroAlgebraico I = NumeroAlgebraico.I;

    private static NumeroAlgebraico r(int n, long q) {
        return NumeroAlgebraico.raiz(n, q);
    }

    private static NumeroAlgebraico q(long n) {
        return NumeroAlgebraico.de(n);
    }

    private static NumeroAlgebraico q(long a, long b) {
        return NumeroAlgebraico.de(Racional.de(a, b));
    }

    private static NumeroAlgebraico leer(String texto) {
        return PolinomioAlgebraico.desde(Parser.parse(texto), 'x').coeficiente(0);
    }

    @ParameterizedTest(name = "{0}√{1} = {2}")
    @CsvSource({
            "2, 8, 2√2",
            "3, 16, 2∛2",
            "2, 12, 2√3",
            "2, 72, 6√2",
            "3, 54, 3∛2",
            "4, 4, √2",
            "6, 8, √2",
            "2, 49, 7",
            "3, 27, 3",
            "3, -8, -2",
            "3, -16, -2∛2",
            "2, 1, 1",
    })
    void simplificaRadicales(int indice, long radicando, String esperado) {
        assertEquals(esperado, Radical.raiz(indice, radicando).toString());
    }

    @Test
    void radicalDeUnaFraccion() {
        assertEquals("√6/2", Radical.raiz(2, Racional.de(3, 2)).toString());
        assertEquals("∛6/2", Radical.raiz(3, Racional.de(3, 4)).toString());
    }

    @Test
    void elRadicandoQuedaSinPotenciasDelIndice() {
        Radical r = Radical.raiz(3, 250);
        assertEquals(Racional.de(5), r.coeficiente());
        assertEquals(3, r.indice());
        assertEquals(BigInteger.TWO, r.radicando());
    }

    @Test
    void laRaizParDeUnNegativoNoEsReal() {
        assertThrows(ArithmeticException.class, () -> Radical.raiz(2, -4));
    }

    @Test
    void multiplicaRadicales() {
        assertEquals("2", Radical.raiz(3, 2).multiplicar(Radical.raiz(3, 4)).toString());
        assertEquals("√6", Radical.raiz(2, 2).multiplicar(Radical.raiz(2, 3)).toString());
        assertEquals(Radical.raiz(6, 32), Radical.raiz(2, 2).multiplicar(Radical.raiz(3, 2)));
    }

    @Test
    void productoDeConjugados() {
        assertEquals(q(-1), UNO.sumar(r(2, 2)).multiplicar(UNO.restar(r(2, 2))));
    }

    @Test
    void cuadradoDeRaiz() {
        assertEquals(q(2), r(2, 2).potencia(2));
        assertEquals(q(3), r(2, 3).multiplicar(r(2, 3)));
    }

    @Test
    void cuboDeRaizCubica() {
        assertEquals(q(2), r(3, 2).potencia(3));
        assertEquals(r(3, 4), r(3, 2).potencia(2));
        assertEquals(q(2).multiplicar(r(3, 2)), r(3, 4).potencia(2));
    }

    @Test
    void unidadImaginaria() {
        assertEquals(q(-1), I.multiplicar(I));
        assertEquals(I.negar(), I.potencia(3));
        assertEquals(UNO, I.potencia(4));
        assertEquals(I.negar(), I.inverso());
    }

    @Test
    void productoDeComplejosConjugados() {
        NumeroAlgebraico z = NumeroAlgebraico.complejo(Racional.UNO, Racional.de(2));
        assertEquals(q(5), z.multiplicar(z.conjugar()));
    }

    @Test
    void raizDeNegativos() {
        assertEquals(I.multiplicar(q(2)), r(2, -4));
        assertEquals(I.multiplicar(r(2, 3)), r(2, -3));
        assertEquals(r(3, 2).negar(), r(3, -2));
    }

    @Test
    void racionalizaDenominadores() {
        assertEquals(r(2, 2).multiplicar(Racional.de(1, 2)), r(2, 2).inverso());
        assertEquals(r(2, 3).multiplicar(Racional.de(1, 3)), r(2, 3).inverso());
        assertEquals("√3/3", r(2, 3).inverso().toString());
        assertEquals(r(3, 4).multiplicar(Racional.de(1, 2)), r(3, 2).inverso());
        assertEquals(q(-1).sumar(r(2, 2)), UNO.sumar(r(2, 2)).inverso());
    }

    @Test
    void invierteSumasDeVariosRadicales() {
        NumeroAlgebraico z = UNO.sumar(r(2, 2)).sumar(r(2, 3));
        assertEquals(UNO, z.multiplicar(z.inverso()));
        NumeroAlgebraico w = r(3, 2).sumar(r(3, 4));
        assertEquals(UNO, w.multiplicar(w.inverso()));
    }

    @Test
    void invierteComplejos() {
        NumeroAlgebraico z = NumeroAlgebraico.complejo(Racional.UNO, Racional.de(2));
        assertEquals(NumeroAlgebraico.complejo(Racional.de(1, 5), Racional.de(-2, 5)), z.inverso());
        NumeroAlgebraico w = UNO.sumar(r(2, 3).multiplicar(I));
        assertEquals(UNO, w.multiplicar(w.inverso()));
    }

    @Test
    void sumaYResta() {
        assertEquals(r(2, 2).multiplicar(Racional.de(3)), r(2, 2).sumar(r(2, 8)));
        assertEquals(NumeroAlgebraico.CERO, r(2, 12).restar(r(2, 3).multiplicar(Racional.de(2))));
        assertTrue(r(2, 2).sumar(r(2, 2).negar()).esCero());
    }

    @Test
    void igualdadExacta() {
        assertNotEquals(UNO.sumar(r(2, 2)), r(2, 3));
        assertEquals(r(2, 12), r(2, 3).multiplicar(Racional.de(2)));
        assertEquals(r(2, 8).multiplicar(r(2, 2)), q(4));
    }

    @Test
    void potenciasNegativas() {
        assertEquals(q(1, 2), r(2, 2).potencia(-2));
        assertEquals(q(1, 8).multiplicar(r(2, 2)), r(2, 2).potencia(-5));
    }

    @Test
    void cardano() {
        NumeroAlgebraico x = r(3, 4).sumar(r(3, 2));
        assertTrue(x.potencia(3).restar(x.multiplicar(Racional.de(6))).restar(q(6)).esCero());
        assertEquals(leer("-2 + ∛4 + 2·∛2"), x.potencia(2).restar(q(6)));
    }

    @Test
    void conjugadoYPartes() {
        NumeroAlgebraico z = leer("1/2 + √3·i/2");
        assertEquals(q(1, 2), z.parteReal());
        assertEquals(r(2, 3).multiplicar(Racional.de(1, 2)), z.parteImaginaria());
        assertEquals(leer("1/2 - √3·i/2"), z.conjugar());
        assertEquals(UNO, z.multiplicar(z.conjugar()));
        assertEquals(q(-1), z.potencia(3));
    }

    @Test
    void signoExacto() {
        assertEquals(1, UNO.sumar(r(2, 2)).signo());
        assertEquals(-1, UNO.restar(r(2, 2)).signo());
        assertEquals(-1, q(1).restar(r(2, 2).multiplicar(Racional.de(4))).signo());
        assertEquals(-1, r(2, 3).sumar(r(2, 2)).restar(r(2, 10)).signo());
        assertEquals(-1, r(3, 2).restar(r(2, 2)).signo());
        assertEquals(1, r(3, 3).comparar(r(2, 2)));
    }

    @Test
    void raizCuadradaDesanida() {
        assertEquals(UNO.sumar(r(2, 2)), leer("3 + 2√2").raizCuadrada());
        assertEquals(r(2, 3).restar(r(2, 2)), leer("5 - 2√6").raizCuadrada());
        assertEquals(I.multiplicar(q(3)), q(-9).raizCuadrada());
        assertEquals(NumeroAlgebraico.complejo(Racional.de(2), Racional.UNO), leer("3 + 4i").raizCuadrada());
    }

    @Test
    void raizAnidadaQuedaComoAtomo() {
        NumeroAlgebraico z = leer("1 + 4√2").raizCuadrada();
        assertFalse(z.exacto());
        assertEquals("√(1 + 4·√2)", z.toString());
        assertEquals(leer("1 + 4√2"), z.potencia(2));
        assertEquals(1, z.restar(q(2)).signo());
    }

    @Test
    void texto() {
        assertEquals("1 + √2", UNO.sumar(r(2, 2)).toString());
        assertEquals("-1 - √2", UNO.sumar(r(2, 2)).negar().toString());
        assertEquals("2·i", I.multiplicar(q(2)).toString());
        assertEquals("√6/2", r(2, 6).multiplicar(Racional.de(1, 2)).toString());
        assertEquals("-1/2 + √3·i/2", leer("-1/2 + √3·i/2").toString());
        assertEquals("5·i/2", leer("5i/2").toString());
        assertEquals("∛4", r(3, 4).toString());
        assertEquals("2·√3", r(2, 12).toString());
    }

    @Test
    void polinomioMinimo() {
        assertEquals(List.of(Racional.de(-2), Racional.CERO, Racional.UNO), r(2, 2).polinomioMinimo());
        assertEquals(List.of(Racional.de(-1), Racional.de(-2), Racional.UNO), UNO.sumar(r(2, 2)).polinomioMinimo());
    }

    @Test
    void polinomioConRadicales() {
        PolinomioAlgebraico a = PolinomioAlgebraico.desde(Parser.parse("x - √2"), 'x');
        PolinomioAlgebraico b = PolinomioAlgebraico.desde(Parser.parse("x + √2"), 'x');
        assertEquals(PolinomioAlgebraico.desde(Parser.parse("x^2 - 2"), 'x'), a.multiplicar(b));
        PolinomioAlgebraico c = PolinomioAlgebraico.desde(Parser.parse("(x^2 - √2·x + 1)(x^2 + √2·x + 1)"), 'x');
        assertEquals(PolinomioAlgebraico.desde(Parser.parse("x^4 + 1"), 'x'), c);
        assertEquals("x^2 - √2·x + 1", PolinomioAlgebraico.desde(Parser.parse("x^2 - √2·x + 1"), 'x').toString());
    }
}
