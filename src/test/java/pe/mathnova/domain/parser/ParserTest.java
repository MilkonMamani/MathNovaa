package pe.mathnova.domain.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

class ParserTest {

    private static final Variable X = new Variable('x');
    private static final Variable Y = new Variable('y');
    private static final ConstanteEspecial E = new ConstanteEspecial(ConstanteEspecial.Tipo.E);
    private static final ConstanteEspecial PI = new ConstanteEspecial(ConstanteEspecial.Tipo.PI);
    private static final ConstanteEspecial I = new ConstanteEspecial(ConstanteEspecial.Tipo.I);

    private static Constante n(long v) {
        return Constante.de(v);
    }

    private static Funcion sen(Expresion arg) {
        return new Funcion(TipoFuncion.SEN, arg);
    }

    private static void lee(String texto, Expresion esperado) {
        assertEquals(esperado, Parser.parse(texto), texto);
    }

    @Test
    void potenciaPorParentesis() {
        lee("x^2(x - 3)", new Producto(new Potencia(X, n(2)), new Suma(X, new Negacion(n(3)))));
    }

    @Test
    void eAlCuadradoPorX() {
        lee("e^2x", new Producto(new Potencia(E, n(2)), X));
    }

    @Test
    void senoConArgumentoSinParentesis() {
        lee("sen 2x", sen(new Producto(n(2), X)));
        lee("sen x^2", sen(new Potencia(X, n(2))));
    }

    @Test
    void senoPorCoseno() {
        lee("sen x cos x", new Producto(sen(X), new Funcion(TipoFuncion.COS, X)));
    }

    @Test
    void senoAlCuadrado() {
        Expresion esperado = new Potencia(sen(X), n(2));
        lee("sen^2(x)", esperado);
        lee("sen(x)^2", esperado);
    }

    @Test
    void raizDeDosPorX() {
        lee("√2x", new Producto(new Raiz(2, n(2)), X));
        lee("∛(x + 1)", new Raiz(3, new Suma(X, n(1))));
    }

    @Test
    void logaritmos() {
        lee("log x", new Funcion(TipoFuncion.LOG, X, 10));
        lee("log_2(x)", new Funcion(TipoFuncion.LOG, X, 2));
        lee("ln x", new Funcion(TipoFuncion.LN, X));
    }

    @Test
    void potenciaAsociaALaDerecha() {
        lee("2^3^2", new Potencia(n(2), new Potencia(n(3), n(2))));
    }

    @Test
    void menosXAlCuadradoEsMenosDeLaPotencia() {
        lee("-x^2", new Negacion(new Potencia(X, n(2))));
    }

    @Test
    void exponenteNegativo() {
        lee("x^-1", new Potencia(X, new Negacion(n(1))));
    }

    @Test
    void multiplicacionImplicitaDeVariables() {
        lee("2xy", new Producto(n(2), X, Y));
        lee("2x^2y", new Producto(n(2), new Potencia(X, n(2)), Y));
    }

    @Test
    void signoAlInicioDeUnTermino() {
        lee("-2x", new Negacion(new Producto(n(2), X)));
        lee("x - 2x", new Suma(X, new Negacion(new Producto(n(2), X))));
    }

    @Test
    void divisionAsociaALaIzquierda() {
        lee("x/2/3", new Cociente(new Cociente(X, n(2)), n(3)));
        lee("1/2x", new Producto(new Cociente(n(1), n(2)), X));
    }

    @Test
    void normalizaSimbolosUnicode() {
        lee("x²+3x", new Suma(new Potencia(X, n(2)), new Producto(n(3), X)));
        lee("3(x+1)(x−2)", new Producto(n(3), new Suma(X, n(1)), new Suma(X, new Negacion(n(2)))));
        lee("2×x³", new Producto(n(2), new Potencia(X, n(3))));
        lee("2·x", new Producto(n(2), X));
    }

    @Test
    void constantesYDecimales() {
        lee("πx", new Producto(PI, X));
        lee("pi x", new Producto(PI, X));
        lee("2·i", new Producto(n(2), I));
        lee("0.5x", new Producto(new Constante(Racional.de(1, 2)), X));
    }

    @Test
    void nombresMasLargosPrimero() {
        lee("arcsen(x) + cosec x", new Suma(new Funcion(TipoFuncion.ARCSEN, X), new Funcion(TipoFuncion.CSC, X)));
        lee("arctg x", new Funcion(TipoFuncion.ARCTAN, X));
        lee("exp(x)", new Funcion(TipoFuncion.EXP, X));
        lee("sqrt(x)", new Funcion(TipoFuncion.SQRT, X));
        lee("ex", new Producto(E, X));
    }
}
