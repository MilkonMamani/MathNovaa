package pe.mathnova.domain.calculo.parciales;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Direccional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Gradiente;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.ErrorSintaxis;
import pe.mathnova.domain.parser.Parser;

class ParcialesTest {

    private static final MotorParciales MOTOR = new MotorParciales();

    private static ResolucionParcial resolver(String texto) {
        return MOTOR.resolver(Parser.parse(texto));
    }

    private static void iguales(Expresion esperado, Expresion obtenido) {
        for (double[] p : List.of(new double[] {1.3, -0.7, 2.1}, new double[] {-2.2, 0.4, 1.7}, new double[] {0.9, 3.1, -1.2})) {
            Map<Character, pe.mathnova.domain.numeros.ComplejoDecimal> v = Map.of(
                    'x', real(p[0]), 'y', real(p[1]), 'z', real(p[2]), 't', real(p[0] + p[1]));
            assertTrue(EvaluacionNumerica.evaluar(esperado, v, EvaluacionNumerica.MC)
                            .cercano(EvaluacionNumerica.evaluar(obtenido, v, EvaluacionNumerica.MC), 30, EvaluacionNumerica.MC),
                    () -> "esperado " + esperado + ", obtenido " + obtenido);
        }
    }

    private static pe.mathnova.domain.numeros.ComplejoDecimal real(double d) {
        return pe.mathnova.domain.numeros.ComplejoDecimal.real(new java.math.BigDecimal(String.valueOf(d)));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = ';', value = {
            "∂/∂x(x^2 y + 3x y^2)  ; 2xy + 3y^2",
            "∂/∂y(x^2 y + 3x y^2)  ; x^2 + 6xy",
            "∂2/∂x∂y(x^2 y^3)      ; 6x y^2",
    })
    void parciales(String entrada, String esperado) {
        ResolucionParcial r = resolver(entrada);
        assertAll(entrada + " → " + r.texto(),
                () -> assertEquals(ResolucionParcial.Tipo.PARCIAL, r.tipo()),
                () -> iguales(Parser.parse(esperado), r.resultado()),
                () -> assertTrue(r.verificada()));
    }

    @Test
    void laOtraLetraSeTrataComoConstante() {
        ResolucionParcial r = resolver("∂/∂x(x^2 y + 3x y^2)");
        PasoLimite p = r.pasos().getFirst();
        assertEquals("V01", p.codigo());
        assertTrue(p.justificacion().endsWith("y se trata como constante"), p.justificacion());
        assertEquals("2x·y + 3y²", r.texto());
    }

    @Test
    void gradiente() {
        ResolucionParcial r = resolver("grad(x y z + x^2)");
        assertAll(
                () -> assertEquals(ResolucionParcial.Tipo.GRADIENTE, r.tipo()),
                () -> assertEquals(List.of('x', 'y', 'z'), r.variables()),
                () -> assertEquals(3, r.vector().size()),
                () -> iguales(Parser.parse("2x + y z"), r.vector().get(0)),
                () -> iguales(Parser.parse("x z"), r.vector().get(1)),
                () -> iguales(Parser.parse("x y"), r.vector().get(2)),
                () -> assertEquals("(y·z + 2x, x·z, x·y)", r.texto()),
                () -> assertTrue(r.verificada()));
    }

    @Test
    void gradienteEnUnPunto() {
        ResolucionParcial r = resolver("grad(x^2 + y^2, (1, 2))");
        assertEquals("(2, 4)", r.texto());
        assertEquals("Evaluar en el punto", r.pasos().getLast().titulo());
    }

    @Test
    void direccionalNormalizaPrimero() {
        ResolucionParcial r = resolver("Du(x^2 + y^2, (3, 4), (1, 2))");
        assertEquals("22/5", r.texto());
        PasoLimite normalizar = r.pasos().stream().filter(p -> p.titulo().equals("Normalizar el vector")).findFirst()
                .orElseThrow();
        List<String> l = ((TrabajoIntermedio.Agrupacion) normalizar.trabajo()).grupos();
        assertEquals("|v| = √(3² + 4²) = 5", l.get(0));
        assertEquals("u = v/|v| = (3/5, 4/5)", l.get(1));
        assertEquals("V03", r.pasos().getLast().codigo());
        assertTrue(r.verificada());
    }

    @Test
    void conUnVectorUnitarioNoSeNormaliza() {
        ResolucionParcial r = resolver("Du(x^2 + y^2, (1, 0), (1, 2))");
        assertEquals("2", r.texto());
        assertTrue(r.pasos().stream().noneMatch(p -> p.titulo().equals("Normalizar el vector")));
    }

    @Test
    void reglaDeLaCadenaConSuArbol() {
        ResolucionParcial r = MOTOR.cadena(MotorParciales.leerCadena("d/dt(x^2 y); x = t^2, y = sen t").orElseThrow());
        assertAll(
                () -> assertEquals(ResolucionParcial.Tipo.CADENA, r.tipo()),
                () -> iguales(Parser.parse("t^3(t cos t + 4sen t)"), r.resultado()),
                () -> assertTrue(r.verificada()));
        TrabajoIntermedio.ArbolCadena a = assertInstanceOf(TrabajoIntermedio.ArbolCadena.class,
                r.pasos().getFirst().trabajo());
        assertAll(
                () -> assertEquals("f", a.funcion()),
                () -> assertEquals(List.of("x", "y"), a.intermedias()),
                () -> assertEquals("t", a.parametro()),
                () -> assertEquals(List.of("∂f/∂x = 2x·y", "∂f/∂y = x²"), a.ramasArriba()),
                () -> assertEquals(List.of("dx/dt = 2t", "dy/dt = cos t"), a.ramasAbajo()),
                () -> assertTrue(a.suma().startsWith("df/dt = fₓ·x' + fᵧ·y' = "), a.suma()));
    }

    @Test
    void dominio() {
        assertEquals(List.of("x + y > 0"), MotorParciales.dominio(Parser.parse("ln(x + y)")));
        assertEquals(List.of("x − y ≥ 0", "x² + y ≠ 0"), MotorParciales.dominio(Parser.parse("√(x - y)/(x^2 + y)")));
        assertEquals("Todos los (x, y) del plano", MotorParciales.textoDominio(List.of(), List.of('x', 'y')));
        assertEquals(List.of("x + y > 0"), resolver("∂/∂x(ln(x + y))").dominio());
    }

    @Test
    void tablaYHessiana() {
        MotorParciales.TablaParciales t = MotorParciales.tabla(Parser.parse("x^3 y + y^2"));
        assertEquals(List.of("fx", "fy"), List.copyOf(t.primeras().keySet()));
        assertEquals(List.of("fxx", "fxy", "fyx", "fyy"), List.copyOf(t.segundas().keySet()));
        iguales(Parser.parse("6x y"), t.hessiana().get(0).get(0));
        iguales(Parser.parse("3x^2"), t.hessiana().get(0).get(1));
        iguales(Parser.parse("3x^2"), t.hessiana().get(1).get(0));
        iguales(Parser.parse("2"), t.hessiana().get(1).get(1));
    }

    @Test
    void gramatica() {
        Gradiente g = assertInstanceOf(Gradiente.class, Parser.parse("grad(x y, (1, 2))"));
        assertEquals(2, g.punto().size());
        Direccional d = assertInstanceOf(Direccional.class, Parser.parse("Du(x y, (3, 4), (1, 2))"));
        assertEquals(2, d.vector().size());
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("Du(x y, (3, 4))"));
        assertThrows(ErrorSintaxis.class, () -> Parser.parse("Du(x y, (3, 4, 5), (1, 2))"));
    }

    @Test
    void elTraductorConvierteNablaYDu() {
        assertEquals(Parser.parse("grad(x^2 + y^2)"),
                Parser.parse(TraductorLatex.traducir("\\nabla\\left(x^2+y^2\\right)")));
        assertEquals(Parser.parse("Du(x^2 + y^2, (3, 4), (1, 2))"), Parser.parse(TraductorLatex.traducir(
                "D_{\\vec u}\\left(x^2+y^2,\\left(3,4\\right),\\left(1,2\\right)\\right)")));
    }
}
