package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.application.service.FactorizacionService;
import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.CubicaCardano;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.parser.Parser;

/** Cardano con la raíz real como r, la comparación Q / R / C y la expansión del resultado a la entrada. */
class FormaLegibleTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();
    private static final FactorizacionService SERVICIO = new FactorizacionService(MOTOR, null);
    private static final String CUBICA = "3*x^3 + 8*x^2 - x + 10";
    private static final String OCTAVO = "2*x^8 - 12*x^6 + 18*x^4 - 8*x^2";

    private static Resolucion resolver(String t, Campo c) {
        return MOTOR.resolver(Parser.parse(t), c);
    }

    @Test
    void enRLaRaizSeLlamaRYElResultadoEsExacto() {
        FormaLegible f = FormaLegible.de(resolver(CUBICA, Campo.R)).orElseThrow();
        assertAll(
                () -> assertEquals("\\left(x - r\\right)\\left[3x^{2} + \\left(8 + 3r\\right)x"
                        + " + \\left(3r^{2} + 8r - 1\\right)\\right]", f.latex()),
                () -> assertFalse(f.latex().contains("\\sqrt"), "sin radicales anidados en el resultado"),
                () -> assertTrue(f.algebraExacta(), "(x − r)·Q = P(x) − P(r) con polinomios en x y r"),
                () -> assertTrue(f.raizComprobada(), "el valor de Cardano anula P"),
                () -> assertTrue(f.aproximacion().startsWith("-3.11")));
    }

    @Test
    void cardanoEnOrdenYUnaOperacionPorRenglon() {
        Desarrollo d = FormaLegible.de(resolver(CUBICA, Campo.R)).orElseThrow().desarrollo();
        List<String> formulas = d.renglones().stream().filter(r -> r instanceof Desarrollo.Formula)
                .map(r -> ((Desarrollo.Formula) r).latex()).toList();
        int sustitucion = indice(formulas, "x = t - \\frac{8}{9}");
        int reducida = indice(formulas, "t^{3} - \\frac{73}{27}t + \\frac{3670}{729} = 0");
        int delta = indice(formulas, "\\Delta = ");
        int factorizacion = indice(formulas, "P(x) = \\left(x - r\\right)");
        assertTrue(sustitucion < reducida && reducida < delta && delta < factorizacion, formulas.toString());
        Desarrollo.Definicion r = d.renglones().stream().filter(x -> x instanceof Desarrollo.Definicion)
                .map(x -> (Desarrollo.Definicion) x).findFirst().orElseThrow();
        assertAll(
                () -> assertEquals(FormaLegible.VER_R, r.accion()),
                () -> assertTrue(r.latex().contains("\\approx"), "la aproximación con ≈"),
                () -> assertTrue(r.detalle().getLast().contains("\\sqrt[3]"), "el valor exacto de Cardano"),
                () -> assertTrue(formulas.stream().noneMatch(l -> l.contains("\\sqrt[3]")),
                        "los radicales anidados solo en el detalle desplegable"));
    }

    @Test
    void comprobacionDelResultadoALaEntrada() {
        FormaLegible f = FormaLegible.de(resolver(CUBICA, Campo.R)).orElseThrow();
        List<CadenaIgualdades.Eslabon> c = f.cadena();
        assertAll(
                () -> assertEquals(f.latex(), c.getFirst().latex(), "el primer renglón es el resultado celeste"),
                () -> assertEquals("3x^{3} + 8x^{2} - x + 10", c.getLast().latex(), "el último es la entrada"),
                () -> assertEquals("3r^{3} + 8r^{2} - r = -10", c.getLast().notaLatex()));
    }

    @Test
    void enCTresFactoresLinealesConZ() {
        FormaLegible f = FormaLegible.de(resolver(CUBICA, Campo.C)).orElseThrow();
        assertAll(
                () -> assertEquals("3\\left(x - r\\right)\\left(x - z\\right)\\left(x - \\bar{z}\\right)", f.latex()),
                () -> assertEquals(f.latex(), f.cadena().getFirst().latex()),
                () -> assertEquals("3x^{3} + 8x^{2} - x + 10", f.cadena().getLast().latex()));
    }

    @Test
    void laComparacionDiceLaEstructuraDeCadaCampo() {
        List<ComparacionCampos.Fila> filas = ComparacionCampos.filas(SERVICIO.comparar(CUBICA));
        assertAll(
                () -> assertEquals(List.of("Q", "R", "C"), filas.stream().map(ComparacionCampos.Fila::rotulo).toList()),
                () -> assertEquals("Irreducible sobre los racionales", filas.get(0).descripcion()),
                () -> assertEquals("Un factor lineal y un factor cuadrático irreducible", filas.get(1).descripcion()),
                () -> assertEquals("Tres factores lineales: una raíz real y dos complejas conjugadas",
                        filas.get(2).descripcion()),
                () -> assertFalse(filas.get(1).latex().contains("\\sqrt"), "primero la forma legible"),
                () -> assertTrue(filas.get(1).exacto().contains("\\sqrt[3]"), "el detalle exacto, plegado"));
    }

    @Test
    void expansionDelGradoOcho() {
        Resolucion r = resolver(OCTAVO, Campo.Q);
        String inicial = pe.mathnova.domain.expresion.ImpresoraLatex.imprimir(r.resultado().aExpresion());
        CadenaExpansion.Resultado c = CadenaExpansion.de(r.resultado(), Polinomio.desde(r.entrada()), inicial);
        assertEquals(List.of(
                "2x^{2}\\left(x + 2\\right)\\left(x + 1\\right)^{2}\\left(x - 1\\right)^{2}\\left(x - 2\\right)",
                "2x^{2}\\left(x + 2\\right)\\left(x - 2\\right)\\left[\\left(x + 1\\right)\\left(x - 1\\right)\\right]^{2}",
                "2x^{2}\\left(x^{2} - 4\\right)\\left(x^{2} - 1\\right)^{2}",
                "2x^{2}\\left(x^{2} - 4\\right)\\left(x^{4} - 2x^{2} + 1\\right)",
                "2x^{2}\\left(x^{6} - 6x^{4} + 9x^{2} - 4\\right)",
                "2x^{8} - 12x^{6} + 18x^{4} - 8x^{2}"), c.expresiones());
        assertTrue(c.coincide());
        // Cada renglón es equivalente al anterior: todos se expanden a la entrada.
        for (String l : c.expresiones()) {
            assertEquals(Polinomio.desde(r.entrada()), Polinomio.desde(Parser.parse(sinLatex(l))), l);
        }
    }

    @Test
    void sinCardanoNoHayFormaLegible() {
        assertTrue(FormaLegible.de(resolver("x^2 - 2", Campo.R)).isEmpty());
        assertTrue(FormaLegible.de(resolver(CUBICA, Campo.Q)).isEmpty());
        assertTrue(CubicaCardano.de(Polinomio.desde(Parser.parse("x^3 - 3x + 1"))).isEmpty(), "Δ < 0");
    }

    private static int indice(List<String> l, String prefijo) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).startsWith(prefijo)) {
                return i;
            }
        }
        throw new AssertionError("no está " + prefijo + " en " + l);
    }

    /** Lo justo para volver a leer los renglones de la cadena con el Parser. */
    private static String sinLatex(String l) {
        return l.replace("\\left[", "(").replace("\\right]", ")").replace("\\left(", "(").replace("\\right)", ")")
                .replace("^{", "^(").replace("}", ")").replace(")(", ")*(").replaceAll("(\\d)x", "$1*x")
                .replaceAll("\\)x", ")*x");
    }
}
