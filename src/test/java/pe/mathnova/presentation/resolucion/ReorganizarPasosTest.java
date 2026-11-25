package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.expresion.Cociente;
import pe.mathnova.domain.expresion.ImpresoraLatex;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.parser.Parser;

/**
 * La factorización que necesita un límite va en su propio paso ("Factorizar el numerador con Ruffini…") y después
 * el límite completo con la expresión factorizada ("Sustituir en el límite"); el desarrollo se pliega desde 4 filas.
 */
class ReorganizarPasosTest {

    private static final MotorLimites LIMITES = new MotorLimites(new OrquestadorFactorizacion());

    private static List<VistaPaso> pasos(String limite) {
        var r = LIMITES.resolver((Limite) Parser.parse(limite));
        String entrada = ImpresoraLatex.imprimir(r.entrada());
        var marco = RenglonesPaso.Marco.de("CALCULA EL LÍMITE", entrada);
        return ReorganizarPasos.de(VistaResolucion.de(r, false).pasos(), marco);
    }

    private static VistaPaso auxiliar(List<VistaPaso> pasos) {
        return pasos.stream().filter(p -> p.codigo().equals(ReorganizarPasos.AUXILIAR)).findFirst().orElseThrow();
    }

    private static int filas(VistaPaso aux) {
        return aux.hijos().stream().mapToInt(DesarrolloAuxiliar::filas).sum();
    }

    @Test
    void ruffiniEnElNumeradorLuegoSustituirYEvaluar() {
        List<VistaPaso> p = pasos("lim(x->2, (x^3 - 3x - 2)/(x - 2))");
        assertEquals(List.of("Evaluación directa", "Factorizar el numerador con Ruffini y trinomio cuadrado perfecto",
                "Sustituir en el límite", "Evaluación directa"), p.stream().map(VistaPaso::titulo).toList());
        assertEquals(List.of(1, 2, 3, 4), p.stream().map(VistaPaso::orden).toList());
        VistaPaso aux = p.get(1);
        assertEquals("x^{3} - 3x - 2", ImpresoraLatex.imprimir(aux.antes()));
        assertEquals("\\left(x - 2\\right)\\left(x + 1\\right)^{2}", ImpresoraLatex.imprimir(aux.despues()));
        VistaPaso sust = p.get(2);
        assertTrue(sust.antes() instanceof Cociente c && c.numerador().equals(aux.despues()),
                "el límite vuelve a escribirse con el numerador factorizado");
        assertTrue(filas(aux) >= 4, "Ruffini con su tabla es un desarrollo largo: " + filas(aux));
    }

    @Test
    void laSustitucionMantieneElLimiteYDiceXDistintoDeA() {
        List<VistaPaso> p = pasos("lim(x->2, (x^3 - 3x - 2)/(x - 2))");
        var marco = RenglonesPaso.Marco.de("CALCULA EL LÍMITE", "\\lim_{x \\to 2} \\frac{x^{3} - 3x - 2}{x - 2}");
        List<Igualdades.Renglon> r = RenglonesPaso.renglones(p.get(2), marco, true);
        assertEquals(2, r.size());
        assertTrue(r.get(0).latex().startsWith("\\lim_{x \\to 2} \\frac{"), r.get(0).latex());
        assertTrue(r.get(1).latex().startsWith("= \\lim_{x \\to 2}"), r.get(1).latex());
        assertTrue(r.get(1).latex().endsWith("(x \\neq 2)"), r.get(1).latex());
    }

    @Test
    void umbralDeTresYCuatroFilas() {
        // diferencia de cuadrados: la identidad y la factorización
        assertEquals(2, filas(auxiliar(pasos("lim(x->3, (x^2 - 9)/(x - 3))"))));
        // factor común (1) + diferencia de cuadrados (2): a la vista
        assertEquals(3, filas(auxiliar(pasos("lim(x->1, (x^3 - x)/(x - 1))"))));
        // dos diferencias de cuadrados: plegado
        assertEquals(4, filas(auxiliar(pasos("lim(x->1, (x^4 - 1)/(x - 1))"))));
        assertFalse(3 > DesarrolloAuxiliar.MAXIMO_A_LA_VISTA);
        assertTrue(4 > DesarrolloAuxiliar.MAXIMO_A_LA_VISTA);
    }

    @Test
    void numeradorYDenominadorEnPasosSeparados() {
        List<VistaPaso> p = pasos("lim(x->2, (x^2 - 5x + 6)/(x^2 - 4))");
        assertEquals(List.of("Evaluación directa", "Factorizar el numerador por trinomio simple",
                "Factorizar el denominador por diferencia de cuadrados", "Sustituir en el límite",
                "Evaluación directa"), p.stream().map(VistaPaso::titulo).toList());
    }

    @Test
    void componerJuntaLosFactoresIguales() {
        VistaPaso a = new VistaPaso(1, "F04", "Agrupación · numerador", "", "", Parser.parse("x^3 + x^2 - x - 1"),
                null, Parser.parse("(x + 1)(x^2 - 1)"));
        VistaPaso b = new VistaPaso(2, "F05", "Diferencia de cuadrados · numerador", "", "", Parser.parse("x^2 - 1"),
                null, Parser.parse("(x + 1)(x - 1)"));
        assertEquals("\\left(x + 1\\right)^{2}\\left(x - 1\\right)",
                ImpresoraLatex.imprimir(ReorganizarPasos.componer(List.of(a, b))));
    }

    @Test
    void laExpansionDistribuyeUnaTransformacionPorRenglon() {
        var r = new pe.mathnova.domain.factorizacion.OrquestadorFactorizacion()
                .resolver(Parser.parse("x^2 - 5x + 6"), pe.mathnova.domain.factorizacion.Campo.Q);
        String inicial = ImpresoraLatex.imprimir(r.resultado().aExpresion());
        var c = CadenaExpansion.de(r.resultado(), pe.mathnova.domain.algebra.Polinomio.desde(r.entrada()), inicial);
        assertEquals(List.of(inicial, "x^{2} - 3x - 2x + 6", "x^{2} - 5x + 6"), c.expresiones());
        assertTrue(c.coincide());
    }
}
