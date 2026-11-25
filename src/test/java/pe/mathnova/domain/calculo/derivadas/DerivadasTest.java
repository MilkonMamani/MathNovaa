package pe.mathnova.domain.calculo.derivadas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.calculo.limites.PasoLimite;
import pe.mathnova.domain.expresion.Derivada;
import pe.mathnova.domain.expresion.Ecuacion;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Expresiones;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DerivadasTest {

    private static final MotorDerivadas MOTOR = new MotorDerivadas(new OrquestadorFactorizacion());
    private static final MathContext MC = new MathContext(90);
    private static final BigDecimal H = BigDecimal.ONE.movePointLeft(20);
    private static final List<Racional> PUNTOS = List.of(Racional.de(1, 3), Racional.de(13, 11), Racional.de(17, 9));
    private static final Racional OTRA_LETRA = Racional.de(5, 7);

    private static ResolucionDerivada resolver(String texto) {
        return MOTOR.resolver((Derivada) Parser.parse(texto));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "d/dx(x^2 sen x)             | 2x·sen x + x²·cos x",
            "d/dx((x^2 + 1)/(x - 1))     | (x² − 2x − 1)/(x − 1)²",
            "d/dx(sen(3x^2))             | 6x·cos(3x²)",
            "d/dx(e^x cos x)             | eˣ(cos x − sen x)",
            "d/dx(√(x^2 + 1))            | x/√(x² + 1)",
            "d/dx(ln(x^2 + 1))           | 2x/(x² + 1)",
            "d/dx(x^x)                   | xˣ(ln x + 1)",
            "d/dt(a t^2)                 | 2a·t",
            "d2/dx2(x^3 - 3x)            | 6x",
    })
    void ejemplosExactos(String entrada, String esperado) {
        ResolucionDerivada r = resolver(entrada);
        assertEquals(esperado, r.texto());
        comprobarConCocienteIncremental(r);
    }

    private static void comprobarConCocienteIncremental(ResolucionDerivada r) {
        char x = r.variable();
        Expresion f = r.funcion();
        for (int k = 1; k < r.orden(); k++) {
            f = Derivador.derivar(f, x).derivada();
        }
        for (Racional p : PUNTOS) {
            BigDecimal x0 = new BigDecimal(p.num()).divide(new BigDecimal(p.den()), MC);
            BigDecimal mas = valor(f, x, x0.add(H));
            BigDecimal menos = valor(f, x, x0.subtract(H));
            BigDecimal incremental = mas.subtract(menos).divide(H.multiply(BigDecimal.TWO), MC);
            BigDecimal derivada = valor(r.resultado(), x, x0);
            BigDecimal error = incremental.subtract(derivada).abs();
            assertTrue(error.compareTo(BigDecimal.ONE.movePointLeft(25)) < 0,
                    () -> r.texto() + " en " + p + ": incremental " + incremental + " y derivada " + derivada);
        }
    }

    private static BigDecimal valor(Expresion e, char x, BigDecimal v) {
        Map<Character, ComplejoDecimal> valores = new HashMap<>();
        for (char letra : Expresiones.variables(e)) {
            valores.put(letra, ComplejoDecimal.de(OTRA_LETRA, MC));
        }
        valores.put(x, ComplejoDecimal.real(v));
        return EvaluacionNumerica.evaluar(e, valores, MC).re();
    }

    @Test
    void cadaReglaEsUnPasoConSuFormulaYAlFinalSimplificar() {
        ResolucionDerivada r = resolver("d/dx(x^2 sen x)");
        PasoLimite producto = r.pasos().getFirst();
        assertEquals("D04", producto.codigo());
        assertEquals("Regla del producto", producto.titulo());
        List<String> trabajo = ((TrabajoIntermedio.Agrupacion) producto.trabajo()).grupos();
        assertEquals("(u·v)' = u'·v + u·v'", trabajo.getFirst());
        assertEquals("u = x², v = sen x", trabajo.get(1));
        assertEquals("u' = 2x, v' = cos x", trabajo.get(2));
        assertEquals(List.of("D02", "D07"), producto.hijos().stream().map(PasoLimite::codigo).toList());
        assertEquals(MotorDerivadas.SIMPLIFICAR, r.pasos().get(1).codigo());
    }

    @Test
    void laCadenaDentroDelProductoQuedaAnidada() {
        ResolucionDerivada r = resolver("d/dx(x sen(x^2))");
        PasoLimite producto = r.pasos().getFirst();
        assertEquals("D04", producto.codigo());
        PasoLimite cadena = producto.hijos().stream().filter(h -> h.codigo().equals("D06")).findFirst().orElseThrow();
        assertEquals("Regla de la cadena", cadena.titulo());
        assertTrue(((TrabajoIntermedio.Agrupacion) cadena.trabajo()).grupos().contains("u = x²"));
        assertEquals("D02", cadena.hijos().getFirst().codigo());
    }

    @Test
    void ordenSuperiorEsUnPasoPorDerivada() {
        ResolucionDerivada r = resolver("d2/dx2(x^3 - 3x)");
        assertEquals(List.of("Primera derivada", "Segunda derivada"),
                r.pasos().stream().map(PasoLimite::titulo).toList());
        assertEquals("3x² − 3", pe.mathnova.domain.expresion.ImpresoraPizarra.imprimir(r.pasos().getFirst().despues()));
        assertEquals(3, resolver("d3/dx3(x^4 / 2)").pasos().size());
        assertEquals("12x", resolver("d3/dx3(x^4 / 2)").texto());
    }

    @Test
    void derivacionImplicita() {
        ResolucionDerivada r = resolver("d/dx(x^2 + y^2 = 25)");
        assertEquals("dy/dx = −x/y", r.texto());
        assertEquals(List.of("D08", "D08"), r.pasos().stream().map(PasoLimite::codigo).toList());
        List<String> derivar = ((TrabajoIntermedio.Agrupacion) r.pasos().getFirst().trabajo()).grupos();
        assertEquals("2x + 2y·y' = 0", derivar.get(1));
        Ecuacion ec = (Ecuacion) r.funcion();
        Expresion f = new pe.mathnova.domain.expresion.Suma(List.of(ec.izquierda(),
                new pe.mathnova.domain.expresion.Negacion(ec.derecha())));
        BigDecimal fx = parcial(f, 'x', 3, 4);
        BigDecimal fy = parcial(f, 'y', 3, 4);
        BigDecimal esperado = fx.negate().divide(fy, MC);
        Map<Character, ComplejoDecimal> en = Map.of('x', ComplejoDecimal.real(BigDecimal.valueOf(3)),
                'y', ComplejoDecimal.real(BigDecimal.valueOf(4)));
        BigDecimal obtenido = EvaluacionNumerica.evaluar(r.resultado(), en, MC).re();
        assertTrue(esperado.subtract(obtenido).abs().compareTo(BigDecimal.ONE.movePointLeft(25)) < 0);
    }

    private static BigDecimal parcial(Expresion f, char letra, long x, long y) {
        BigDecimal vx = BigDecimal.valueOf(x);
        BigDecimal vy = BigDecimal.valueOf(y);
        BigDecimal mas = EvaluacionNumerica.evaluar(f, Map.of('x', ComplejoDecimal.real(letra == 'x' ? vx.add(H) : vx),
                'y', ComplejoDecimal.real(letra == 'y' ? vy.add(H) : vy)), MC).re();
        BigDecimal menos = EvaluacionNumerica.evaluar(f, Map.of('x', ComplejoDecimal.real(letra == 'x' ? vx.subtract(H) : vx),
                'y', ComplejoDecimal.real(letra == 'y' ? vy.subtract(H) : vy)), MC).re();
        return mas.subtract(menos).divide(H.multiply(BigDecimal.TWO), MC);
    }

    @Test
    void derivacionLogaritmica() {
        ResolucionDerivada r = resolver("d/dx(x^x)");
        PasoLimite d09 = r.pasos().getFirst();
        assertEquals("D09", d09.codigo());
        List<String> t = ((TrabajoIntermedio.Agrupacion) d09.trabajo()).grupos();
        assertTrue(t.contains("Tomar ln: ln y = x·ln x"), t.toString());
        assertTrue(t.contains("Derivar: y'/y = ln x + 1"), t.toString());
    }

    @Test
    void puntosCriticos() {
        ResolucionDerivada r = resolver("d/dx(x^3 - 3x^2 - 9x + 5)");
        AnalisisDerivada a = r.analisis();
        assertEquals(2, a.criticos().size());
        assertEquals(Racional.de(-1), a.criticos().get(0).x());
        assertEquals(AnalisisDerivada.Tipo.MAXIMO, a.criticos().get(0).tipo());
        assertEquals(Racional.de(3), a.criticos().get(1).x());
        assertEquals(AnalisisDerivada.Tipo.MINIMO, a.criticos().get(1).tipo());
        assertEquals(Racional.de(10), a.criticos().get(0).y());
        assertEquals(List.of(1, -1, 1), a.crecimiento().signos());
        assertEquals(List.of(-1, 1), a.concavidad().signos());
        assertEquals(Racional.UNO, a.inflexiones().getFirst().x());
        PasoLimite pc = r.pasos().getLast();
        assertEquals(MotorDerivadas.PUNTOS_CRITICOS, pc.codigo());
        assertTrue(pc.hijos().stream().anyMatch(h -> h.titulo().equals("Factorizar")));
        assertTrue(((TrabajoIntermedio.Agrupacion) pc.trabajo()).grupos().stream()
                .anyMatch(l -> l.startsWith("x = −1") && l.contains("máximo")));
    }

    @Test
    void exploracionOfreceExpandir() {
        ResolucionDerivada r = resolver("d/dx((x + 1)(x - 2))");
        var caminos = MOTOR.caminos(r);
        assertEquals("D04", caminos.getFirst().codigo());
        assertTrue(caminos.stream().anyMatch(c -> c.codigo().equals(MotorDerivadas.EXPANDIR)
                && c.estado() == pe.mathnova.domain.calculo.CaminoCalculo.Estado.SIRVE), caminos.toString());
        ResolucionDerivada expandida = MOTOR.resolver((Derivada) Parser.parse("d/dx((x + 1)(x - 2))"),
                MotorDerivadas.EXPANDIR);
        assertEquals("2x − 1", expandida.texto());
        assertEquals(MotorDerivadas.EXPANDIR, expandida.pasos().getFirst().codigo());
    }
}
