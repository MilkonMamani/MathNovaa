package pe.mathnova.domain.expresion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.integrales.MotorIntegrales;
import pe.mathnova.domain.calculo.integrales.ResolucionIntegral;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;

/**
 * Una suma escribe un solo operador por término (x − 3, no x + −3), en la pizarra, en LaTeX y en el texto de
 * los pasos. Los árboles se arman a mano con la forma que dejan los motores (Constante(−3), (−2)·x), que el
 * parser no produce al leer "x - 3".
 */
class SignosTest {

    private static final Variable X = new Variable('x');

    private static Constante k(long n) {
        return Constante.de(n);
    }

    private static Expresion por(long n, Expresion e) {
        return new Producto(k(n), e);
    }

    private static Expresion abs(Expresion e) {
        return new Funcion(TipoFuncion.ABS, e);
    }

    private static Expresion ln(Expresion e) {
        return new Funcion(TipoFuncion.LN, e);
    }

    private static void tres(Expresion e, String pizarra, String latex, String texto) {
        assertEquals(pizarra, ImpresoraPizarra.imprimir(e));
        assertEquals(latex, ImpresoraLatex.imprimir(e));
        assertEquals(texto, ImpresoraTexto.imprimir(e));
    }

    @Test
    void sumarUnNegativoEsRestar() {
        tres(new Suma(X, k(-3)), "x − 3", "x - 3", "x - 3");
        tres(new Suma(por(5, X), k(-3)), "5x − 3", "5x - 3", "5x - 3");
    }

    @Test
    void sumarUnTerminoConCoeficienteNegativo() {
        Expresion e = new Suma(new Potencia(X, k(2)), por(-2, X), k(-3));
        tres(e, "x² − 2x − 3", "x^{2} - 2x - 3", "x^2 - 2x - 3");
        tres(new Suma(new Potencia(X, k(2)), por(-1, X)), "x² − x", "x^{2} - x", "x^2 - x");
    }

    @Test
    void restarUnNegativoEsSumar() {
        // x − (−3): los dos menos se cancelan
        tres(new Suma(X, new Negacion(k(-3))), "x + 3", "x + 3", "x + 3");
        tres(new Suma(X, new Negacion(por(-2, X))), "x + 2x", "x + 2x", "x + 2x");
    }

    @Test
    void coeficienteInicialNegativoSinParentesis() {
        tres(new Suma(por(-2, X), k(1)), "−2x + 1", "-2x + 1", "-2x + 1");
        assertEquals("−2(x + 1)", ImpresoraPizarra.imprimir(por(-2, new Suma(X, k(1)))));
        assertEquals("-x", ImpresoraLatex.imprimir(por(-1, X)));
    }

    @Test
    void argumentoDeUnValorAbsoluto() {
        Expresion e = new Suma(new Producto(k(3), ln(abs(new Suma(X, k(-3))))),
                new Producto(k(2), ln(abs(new Suma(X, k(1))))));
        assertEquals("3ln|x − 3| + 2ln|x + 1|", ImpresoraPizarra.imprimir(e));
        assertEquals("3\\ln\\left|x - 3\\right| + 2\\ln\\left|x + 1\\right|", ImpresoraLatex.imprimir(e));
    }

    @Test
    void terminoNegativoDentroDeFraccionesYFactores() {
        Expresion frac = new Cociente(new Suma(por(5, X), k(-3)),
                new Suma(new Potencia(X, k(2)), por(-2, X), k(-3)));
        assertEquals("(5x − 3)/(x² − 2x − 3)", ImpresoraPizarra.imprimir(frac));
        assertEquals("\\frac{5x - 3}{x^{2} - 2x - 3}", ImpresoraLatex.imprimir(frac));
        // x + (−3)/(x + 1): la fracción negativa se resta
        Expresion resta = new Suma(X, new Cociente(k(-3), new Suma(X, k(1))));
        assertEquals("x − 3/(x + 1)", ImpresoraPizarra.imprimir(resta));
        assertEquals("x - \\frac{3}{x + 1}", ImpresoraLatex.imprimir(resta));
        // x·(−2x): el factor negativo conserva su paréntesis
        assertEquals("x\\left(-2x\\right)", ImpresoraLatex.imprimir(new Producto(X, por(-2, X))));
        // −(−2x): nunca dos signos seguidos
        assertEquals("−(−2x)", ImpresoraPizarra.imprimir(new Negacion(por(-2, X))));
    }

    @Test
    void laRestaDeUnaSumaConservaSuParentesis() {
        Expresion e = new Suma(X, new Negacion(new Suma(new Variable('a'), new Variable('b'))));
        tres(e, "x − (a + b)", "x - \\left(a + b\\right)", "x - (a + b)");
    }

    @Test
    void elCuadradoDeUnNegativoConservaSuParentesis() {
        Expresion menosXalCuadrado = new Potencia(new Negacion(X), k(2));
        Expresion menosDeXalCuadrado = new Negacion(new Potencia(X, k(2)));
        assertEquals("(−x)²", ImpresoraPizarra.imprimir(menosXalCuadrado));
        assertEquals("−x²", ImpresoraPizarra.imprimir(menosDeXalCuadrado));
        assertEquals("\\left(-x\\right)^{2}", ImpresoraLatex.imprimir(menosXalCuadrado));
        assertEquals("-x^{2}", ImpresoraLatex.imprimir(menosDeXalCuadrado));
        assertEquals("(−3)²", ImpresoraPizarra.imprimir(new Potencia(k(-3), k(2))));
    }

    @Test
    void elExponenteNegativoNoSeToca() {
        assertEquals("x⁻²", ImpresoraPizarra.imprimir(new Potencia(X, k(-2))));
        assertEquals("x^{-2}", ImpresoraLatex.imprimir(new Potencia(X, k(-2))));
        assertEquals("x² + x⁻²", ImpresoraPizarra.imprimir(new Suma(new Potencia(X, k(2)), new Potencia(X, k(-2)))));
    }

    @Test
    void elMasMenosConservaSuSignificado() {
        // La fórmula cuadrática de los pasos (texto de pizarra) conserva el ± y el menos de −b
        String formula = pe.mathnova.presentation.latex.TextoLatex.convertir("x = (−b ± √(b² − 4ac))/(2a)");
        assertEquals("x = \\frac{-b \\pm \\sqrt{b^{2} - 4ac}}{2a}", formula);
        Expresion menosB = new Suma(new Negacion(new Variable('b')), new Raiz(2, new Variable('d')));
        assertEquals("−b + √d", ImpresoraPizarra.imprimir(menosB));
    }

    @Test
    void unaFraccionConCoeficienteNegativoSeResta() {
        // (8/3)ln|x − 1| − (1/3)ln|x + 2| con el segundo coeficiente negativo en el numerador
        Expresion e = new Suma(new Cociente(new Producto(k(8), ln(abs(new Suma(X, k(-1))))), k(3)),
                new Cociente(new Producto(k(-1), ln(abs(new Suma(X, k(2))))), k(3)));
        assertEquals("(8/3)ln|x − 1| − (1/3)ln|x + 2|", ImpresoraPizarra.imprimir(e));
        assertFalse(ImpresoraLatex.imprimir(e).contains("+ -"));
    }

    @Test
    void elEjercicioDeFraccionesParcialesSeLeeSinSignosDobles() {
        ResolucionIntegral r = new MotorIntegrales(new OrquestadorFactorizacion())
                .resolver(Parser.parse("∫((5*x-3)/(x^2-2*x-3), x)"));
        assertEquals("3ln|x − 3| + 2ln|x + 1| + C", r.texto());
        assertEquals("\\int \\frac{5x - 3}{x^{2} - 2x - 3} \\, dx", ImpresoraLatex.imprimir(r.entrada()));
        assertEquals("3\\ln\\left|x - 3\\right| + 2\\ln\\left|x + 1\\right|", ImpresoraLatex.imprimir(r.antiderivada()));
        r.pasos().forEach(p -> {
            for (Expresion e : new Expresion[] {p.antes(), p.despues()}) {
                if (e != null) {
                    assertFalse(ImpresoraLatex.imprimir(e).matches(".*[+-] -.*"), () -> ImpresoraLatex.imprimir(e));
                    assertFalse(ImpresoraPizarra.imprimir(e).matches(".*[+−] −.*"), () -> ImpresoraPizarra.imprimir(e));
                }
            }
        });
        assertEquals(Racional.UNO, r.coeficiente());
    }
}
