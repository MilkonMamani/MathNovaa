package pe.mathnova.domain.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.Constante;
import pe.mathnova.domain.expresion.ConstanteEspecial;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.DerivadaParcial;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Funcion;
import pe.mathnova.domain.expresion.ImpresoraTexto;
import pe.mathnova.domain.expresion.Integral;
import pe.mathnova.domain.expresion.IntegralMultiple;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.expresion.Negacion;
import pe.mathnova.domain.expresion.Potencia;
import pe.mathnova.domain.expresion.Producto;
import pe.mathnova.domain.expresion.Raiz;
import pe.mathnova.domain.expresion.Rango;
import pe.mathnova.domain.expresion.Suma;
import pe.mathnova.domain.expresion.TipoFuncion;
import pe.mathnova.domain.expresion.Variable;

class ParserOperacionesTest {

    private static final Variable X = new Variable('x');
    private static final Variable Y = new Variable('y');
    private static final Variable A = new Variable('a');
    private static final Variable B = new Variable('b');
    private static final Variable T = new Variable('t');
    private static final Variable M = new Variable('m');
    private static final Variable L = new Variable('l');
    private static final Variable N = new Variable('n');
    private static final Variable THETA = new Variable('θ');
    private static final ConstanteEspecial E = new ConstanteEspecial(ConstanteEspecial.Tipo.E);
    private static final ConstanteEspecial PI = new ConstanteEspecial(ConstanteEspecial.Tipo.PI);

    private static Constante n(long v) {
        return Constante.de(v);
    }

    private static Potencia pot(Expresion base, long exponente) {
        return new Potencia(base, n(exponente));
    }

    private static Negacion menos(Expresion e) {
        return new Negacion(e);
    }

    private static Limite.Finito en(Expresion valor) {
        return new Limite.Finito(valor);
    }

    private static void lee(String texto, Expresion esperado) {
        assertEquals(esperado, Parser.parse(texto), texto);
    }

    @Test
    void limiteBilateral() {
        lee("lim(x→2, (x^2 - 4)/(x - 2))", new Limite('x', en(n(2)), Limite.Lado.AMBOS,
                new Cociente(new Suma(pot(X, 2), menos(n(4))), new Suma(X, menos(n(2))))));
    }

    @Test
    void limiteLateralConFlechaAscii() {
        lee("lim(x->2+, 1/(x - 2))", new Limite('x', en(n(2)), Limite.Lado.DERECHA,
                new Cociente(n(1), new Suma(X, menos(n(2))))));
        lee("lim(x→2⁻, 1/(x - 2))", new Limite('x', en(n(2)), Limite.Lado.IZQUIERDA,
                new Cociente(n(1), new Suma(X, menos(n(2))))));
    }

    @Test
    void limiteAlInfinito() {
        Expresion cuerpo = new Cociente(new Suma(new Producto(n(2), pot(X, 2)), n(1)), new Suma(pot(X, 2), menos(n(3))));
        lee("lim(x→∞, (2x^2 + 1)/(x^2 - 3))", new Limite('x', new Limite.Infinito(true), Limite.Lado.AMBOS, cuerpo));
        lee("lim(x→-inf, (2x^2 + 1)/(x^2 - 3))", new Limite('x', new Limite.Infinito(false), Limite.Lado.AMBOS, cuerpo));
    }

    @Test
    void limiteEnUnMultiploDePi() {
        lee("lim(x→-π/2, tan x)", new Limite('x', en(menos(new Cociente(PI, n(2)))), Limite.Lado.AMBOS,
                new Funcion(TipoFuncion.TAN, X)));
        lee("lim(x→3π/4, x)", new Limite('x', en(new Cociente(new Producto(n(3), PI), n(4))), Limite.Lado.AMBOS, X));
        lee("lim(x→1/2, x)", new Limite('x', en(new Cociente(n(1), n(2))), Limite.Lado.AMBOS, X));
    }

    @Test
    void derivadas() {
        lee("d/dx(x^2 sen x)", new Derivada(1, 'x', new Producto(pot(X, 2), new Funcion(TipoFuncion.SEN, X))));
        lee("d2/dx2(x^3)", new Derivada(2, 'x', pot(X, 3)));
        lee("d²/dx²(x^3)", new Derivada(2, 'x', pot(X, 3)));
        lee("d/dt(a t^2)", new Derivada(1, 't', new Producto(A, pot(T, 2))));
    }

    @Test
    void derivadasParciales() {
        lee("∂/∂y(x^2 y + 3x y^2)", new DerivadaParcial(List.of('y'),
                new Suma(new Producto(pot(X, 2), Y), new Producto(n(3), X, pot(Y, 2)))));
        lee("∂2/∂x∂y(x^2 y^3)", new DerivadaParcial(List.of('x', 'y'), new Producto(pot(X, 2), pot(Y, 3))));
        lee("∂2/∂x2(x^2 y^3)", new DerivadaParcial(List.of('x', 'x'), new Producto(pot(X, 2), pot(Y, 3))));
    }

    @Test
    void integrales() {
        lee("∫(x e^x, x)", new Integral(new Producto(X, new Potencia(E, X)), 'x'));
        lee("∫(x^2, x, 0, 1)", new Integral(pot(X, 2), 'x', n(0), n(1)));
        lee("int(1/x, x, 1, e)", new Integral(new Cociente(n(1), X), 'x', n(1), E));
    }

    @Test
    void integralDoble() {
        lee("∬(x^2 + y^2, x, 0, 1, y, 0, 2)", new IntegralMultiple(new Suma(pot(X, 2), pot(Y, 2)),
                List.of(new Rango('x', n(0), n(1)), new Rango('y', n(0), n(2)))));
    }

    @Test
    void cualquierLetra() {
        lee("a^2 - b^2", new Suma(pot(A, 2), menos(pot(B, 2))));
        lee("3ab + 2a", new Suma(new Producto(n(3), A, B), new Producto(n(2), A)));
        lee("m x + b", new Suma(new Producto(M, X), B));
    }

    @Test
    void letrasGriegas() {
        lee("sen^2 θ + cos^2 θ", new Suma(pot(new Funcion(TipoFuncion.SEN, THETA), 2),
                pot(new Funcion(TipoFuncion.COS, THETA), 2)));
        lee("2α + β", new Suma(new Producto(n(2), new Variable('α')), new Variable('β')));
    }

    @Test
    void raizDeIndiceN() {
        lee("√[4](x)", new Raiz(4, X));
        lee("√[5]x + 1", new Suma(new Raiz(5, X), n(1)));
    }

    @Test
    void losNombresDeFuncionGananALasLetras() {
        lee("ln x", new Funcion(TipoFuncion.LN, X));
        lee("l n", new Producto(L, N));
        lee("l·n", new Producto(L, N));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "lim(x→2, (x^2 - 4)/(x - 2))", "lim(x->2+, 1/(x - 2))", "lim(x→-∞, 1/x)", "lim(x→-π/2, tan x)",
            "d/dx(x^2 sen x)", "d2/dx2(x^3)", "∂/∂y(x^2 y + 3x y^2)", "∂2/∂x∂y(x^2 y^3)",
            "∫(x e^x, x)", "int(1/x, x, 1, e)", "∬(x^2 + y^2, x, 0, 1, y, 0, 2)",
            "∭(x y z, x, 0, 1, y, 0, 2, z, 0, 3)", "√[4](x)", "2α + β"})
    void imprimirYVolverALeerDaElMismoArbol(String texto) {
        Expresion e = Parser.parse(texto);
        assertEquals(e, Parser.parse(ImpresoraTexto.imprimir(e)), ImpresoraTexto.imprimir(e));
    }
}
