package pe.mathnova.domain.calculo.limites;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import pe.mathnova.domain.algebra.Racional;
import pe.mathnova.domain.expresion.Expresion;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.numeros.ComplejoDecimal;
import pe.mathnova.domain.numeros.EvaluacionNumerica;
import pe.mathnova.domain.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parte C del Sprint 5: los 16 ejemplos exactos, con sus métodos y la comprobación numérica. */
class LimitesTest {

    private static final MotorLimites MOTOR = new MotorLimites(new OrquestadorFactorizacion());
    private static final MathContext MC = new MathContext(30);
    private static final BigDecimal TOLERANCIA = new BigDecimal("1e-4");

    private static ResolucionLimite resolver(String texto) {
        return MOTOR.resolver((Limite) Parser.parse(texto));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource(delimiter = '|', value = {
            "lim(x→2, (x^2 - 4)/(x - 2))     | 4         | L01 L02 F05",
            "lim(x→1, (x^3 - 1)/(x^2 - 1))   | 3/2       | L02 F09 F05",
            "lim(x→∞, (2x^2 + 1)/(x^2 - 3))  | 2         | L04",
            "lim(x→∞, (x^3 + 1)/x^2)         | +∞        | L04",
            "lim(x→∞, (3x + 1)/(x^2 + 5))    | 0         | L04",
            "lim(x→0, sen(x)/x)              | 1         | L06",
            "lim(x→0, sen(3x)/x)             | 3         | L06",
            "lim(x→0, (1 - cos x)/x^2)       | 1/2       | L06",
            "lim(x→0, ln(1 + x)/x)           | 1         | L06",
            "lim(x→0, (e^x - 1 - x)/x^2)     | 1/2       | L07 L07",
            "lim(x→2+, 1/(x - 2))            | +∞        | L05",
            "lim(x→2-, 1/(x - 2))            | −∞        | L05",
            "lim(x→2, 1/(x - 2))             | no existe | L05",
            "lim(x→4, (√x - 2)/(x - 4))      | 1/4       | L03",
            "lim(x→∞, √(x^2 + x) - x)        | 1/2       | L03 L04",
            "lim(x→∞, (1 + 1/x)^x)           | e         | L06",
    })
    void ejemplosExactos(String entrada, String esperado, String metodos) {
        ResolucionLimite r = resolver(entrada);
        assertEquals(esperado, r.resultado().texto(), () -> entrada + ": " + r.resultado() + " " + r.codigos());
        List<String> codigos = r.codigos();
        for (String m : metodos.split(" ")) {
            assertTrue(codigos.contains(m), () -> entrada + " debería usar " + m + "; usó " + codigos);
        }
        if (metodos.equals("L07 L07")) {
            assertEquals(2, codigos.stream().filter("L07"::equals).count(), codigos::toString);
            assertTrue(!codigos.contains("L06"), "L'Hôpital, no un límite notable: " + codigos);
        }
        if (r.resultado() instanceof ResultadoLimite.Finito f) {
            comprobarNumericamente(r.entrada(), f);
        }
    }

    /** Se evalúa la función cerca del punto con 30 cifras y se ve que se acerca al resultado. */
    private static void comprobarNumericamente(Limite l, ResultadoLimite.Finito resultado) {
        BigDecimal esperado = EvaluacionNumerica.evaluar(resultado.valor().aExpresion(), Map.of(), MC).re();
        List<Racional> puntos;
        if (l.punto() instanceof Limite.Infinito inf) {
            Racional grande = Racional.de(BigInteger.TEN.pow(12));
            puntos = List.of(inf.positivo() ? grande : grande.negar());
        } else {
            Expresion a = ((Limite.Finito) l.punto()).valor();
            Racional centro = Racional.parse(pe.mathnova.domain.expresion.ImpresoraTexto.imprimir(a));
            Racional h = new Racional(BigInteger.ONE, BigInteger.TEN.pow(6));
            puntos = switch (l.lado()) {
                case DERECHA -> List.of(centro.sumar(h));
                case IZQUIERDA -> List.of(centro.restar(h));
                case AMBOS -> List.of(centro.sumar(h), centro.restar(h));
            };
        }
        for (Racional p : puntos) {
            BigDecimal valor = EvaluacionNumerica.evaluar(l.cuerpo(),
                    Map.of(l.variable(), ComplejoDecimal.de(p, MC)), MC).re();
            assertTrue(valor.subtract(esperado).abs().compareTo(TOLERANCIA) < 0,
                    () -> "f(" + p + ") = " + valor + " no se acerca a " + esperado);
        }
    }

    @Test
    void laEvaluacionDirectaDiceLaIndeterminacion() {
        ResolucionLimite r = resolver("lim(x→2, (x^2 - 4)/(x - 2))");
        assertEquals(Indeterminacion.CERO_SOBRE_CERO, r.indeterminacion());
        assertEquals("Evaluación directa: 0/0, indeterminación", r.pasos().getFirst().justificacion());
        assertEquals("L02", r.metodoFinal());
        PasoLimite l02 = r.pasos().get(1);
        assertEquals("L02", l02.codigo());
        assertEquals("Factorizar numerador y denominador", l02.grupo());
        assertEquals(List.of("F05"), l02.hijos().stream().map(PasoLimite::codigo).toList());
        assertEquals(Parser.parse("x + 2"), l02.despues());
    }

    @Test
    void lateralesDistintos() {
        ResolucionLimite r = resolver("lim(x→2, 1/(x - 2))");
        ResultadoLimite.NoExiste no = assertInstanceOf(ResultadoLimite.NoExiste.class, r.resultado());
        assertEquals("lím por la izquierda = −∞ y por la derecha = +∞", no.motivo());
        assertEquals("L05", r.metodoFinal());
    }

    @Test
    void lHopitalSeVerificaYSeDerivaPorSeparado() {
        ResolucionLimite r = resolver("lim(x→0, (e^x - 1 - x)/x^2)");
        List<PasoLimite> lhopital = r.pasos().stream().filter(p -> p.codigo().equals("L07")).toList();
        assertEquals(Simplificar.de("(e^x - 1)/(2x)"), lhopital.get(0).despues());
        assertEquals(Simplificar.de("e^x/2"), lhopital.get(1).despues());
        assertTrue(lhopital.getFirst().hijos().stream().allMatch(h -> h.codigo().startsWith("D")));
        assertTrue(lhopital.getFirst().justificacion().startsWith("Hay 0/0 (comprobado)"));
    }

    @Test
    void otrosCasos() {
        assertEquals("10", resolver("lim(x→3, x^2 + 1)").resultado().texto());
        assertNull(resolver("lim(x→3, x^2 + 1)").indeterminacion());
        assertEquals("2", resolver("lim(x→-∞, (2x^2 + 1)/(x^2 - 3))").resultado().texto());
        assertEquals("0", resolver("lim(x→0+, x*ln(x))").resultado().texto());
        assertEquals("+∞", resolver("lim(x→∞, x^2 - x)").resultado().texto());
        assertEquals("3/2", resolver("lim(x→0, sen(3x)/(2x))").resultado().texto());
        assertEquals("1", resolver("lim(x→0, x/sen(x))").resultado().texto());
    }

    @Test
    void sinValorInventado() {
        ResolucionLimite r = resolver("lim(x→0, sen(1/x))");
        ResultadoLimite.NoCalculado no = assertInstanceOf(ResultadoLimite.NoCalculado.class, r.resultado());
        assertEquals("No se pudo calcular con los métodos disponibles", no.motivo());
        assertNull(r.metodoFinal());
        assertInstanceOf(ResultadoLimite.NoExiste.class, resolver("lim(x→∞, sen(x))").resultado());
    }

    /** Ayuda para comparar con la forma simplificada. */
    private static final class Simplificar {
        static Expresion de(String texto) {
            return pe.mathnova.domain.calculo.derivadas.Simplificador.simplificar(Parser.parse(texto));
        }
    }
}
