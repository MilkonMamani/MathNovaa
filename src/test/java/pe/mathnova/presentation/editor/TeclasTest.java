package pe.mathnova.presentation.editor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import pe.mathnova.application.editor.TraductorLatex;
import pe.mathnova.domain.parser.Parser;

/** RF72: distribución de las pestañas y que cada plantilla se traduzca. */
class TeclasTest {

    private static List<String> etiquetas(List<Teclas.Tecla> fila) {
        return fila.stream().map(Teclas.Tecla::etiqueta).toList();
    }

    private static String fila(Teclas.Bloque b, int i) {
        return String.join(" ", etiquetas(b.filas().get(i)));
    }

    @Test
    void numeros() {
        Teclas.Bloque izq = Teclas.NUMEROS.izquierda();
        assertEquals("x y π e", fila(izq, 0));
        assertEquals("□² □ⁿ √□ □/□", fila(izq, 1));
        assertEquals("< > ( )", fila(izq, 2));
        assertEquals("ans , ≤ ≥", fila(izq, 3));
        Teclas.Bloque der = Teclas.NUMEROS.derecha();
        assertEquals("7 8 9 × ÷", fila(der, 0));
        assertEquals("4 5 6 + −", fila(der, 1));
        assertEquals("1 2 3 = ⌫", fila(der, 2));
        assertEquals("0 . ← → Enter", fila(der, 3));
    }

    @Test
    void funciones() {
        Teclas.Bloque izq = Teclas.FUNCIONES.izquierda();
        assertEquals("sen cos tan", fila(izq, 0));
        assertEquals("sen⁻¹ cos⁻¹ tan⁻¹", fila(izq, 1));
        assertEquals("ln log₁₀ log□", fila(izq, 2));
        assertEquals("e^□ ⁿ√□ |□|", fila(izq, 3));
        Teclas.Tecla abs = izq.filas().get(3).get(2);
        assertEquals(Teclas.Tipo.DESACTIVADA, abs.tipo());
        assertEquals("fuera del alcance", abs.ayuda());
        Teclas.Bloque der = Teclas.FUNCIONES.derecha();
        assertEquals("lím d/dx ∂/∂x ∫ ∬", fila(der, 0));
        // Sprint 8: el límite doble y los extremos en el lugar de ! y °
        assertEquals("∭ ∞ i lím(x,y) extr", fila(der, 1));
        // Sprint 7: ∇f y Dᵤf en el lugar de las llaves, que la gramática no usa
        assertEquals("□₍□₎ ∇f Dᵤf ∂ ⌫", fila(der, 2));
        assertEquals("→ , ← → Enter", fila(der, 3));
    }

    @Test
    void letras() {
        Teclas.Bloque b = Teclas.LETRAS.izquierda();
        assertEquals("q w e r t y u i o p", fila(b, 0));
        assertEquals("a s d f g h j k l ñ", fila(b, 1));
        assertEquals("⇧ z x c v b n m ⌫", fila(b, 2));
        assertEquals("αβγ , ( ) espacio ← → Enter", fila(b, 3));
        for (Teclas.Tecla t : b.filas().get(0)) {
            if (t.etiqueta().equals("e") || t.etiqueta().equals("i")) {
                assertEquals(Teclas.Tipo.CONSTANTE, t.tipo());
                assertEquals("constante", t.ayuda());
            }
        }
        assertEquals(Teclas.Tipo.DESACTIVADA, b.filas().get(1).get(9).tipo());
    }

    @Test
    void griegas() {
        Teclas.Bloque b = Teclas.GRIEGAS.izquierda();
        Teclas.Tecla pi = b.filas().stream().flatMap(List::stream).filter(t -> t.etiqueta().equals("π")).findFirst().orElseThrow();
        Teclas.Tecla sigmaFinal = b.filas().stream().flatMap(List::stream).filter(t -> t.etiqueta().equals("ς")).findFirst().orElseThrow();
        assertEquals(Teclas.Tipo.CONSTANTE, pi.tipo());
        assertEquals(Teclas.Tipo.DESACTIVADA, sigmaFinal.tipo());
        assertEquals("ABC", b.filas().get(3).getFirst().etiqueta());
    }

    @Test
    void simbolos() {
        Teclas.Bloque b = Teclas.SIMBOLOS.izquierda();
        assertEquals("∞ ≠ ≈ ≤ ≥ → ∂ ∈ [ ]", fila(b, 0));
        assertEquals("± · ∘ ° ′ ″ | ∠ { }", fila(b, 1));
        assertEquals("( ) ⟨ ⟩ ; : ' \" % ⌫", fila(b, 2));
        assertEquals("ans , . = < > ! ← → Enter", fila(b, 3));
    }

    @Test
    void pestanasEnOrden() {
        assertEquals(List.of("123", "f(x)", "ABC", "#&¬"),
                Teclas.PESTANAS.stream().map(Teclas.Pestana::titulo).toList());
    }

    static Stream<Teclas.Tecla> teclasQueInsertan() {
        return Stream.concat(Teclas.PESTANAS.stream(), Stream.of(Teclas.GRIEGAS))
                .flatMap(p -> Stream.of(p.izquierda(), p.derecha()))
                .filter(java.util.Objects::nonNull)
                .flatMap(b -> b.filas().stream())
                .flatMap(List::stream)
                .filter(t -> t.accion() == Teclas.Accion.INSERTAR);
    }

    /** Toda plantilla, con sus casillas llenas, se traduce sin error. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("teclasQueInsertan")
    void cadaPlantillaSeTraduce(Teclas.Tecla t) {
        String lleno = t.valor().replace("#@", "x").replace("#?", "2");
        assertDoesNotThrow(() -> TraductorLatex.traducir(lleno), lleno);
    }

    /** Las plantillas de estructura, llenas, dan algo que el parser acepta. */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "#@^{2}            | x^2",
            "#@^{#?}           | x^2",
            "\\sqrt{#?}        | √2",
            "\\sqrt[#?]{#?}    | √2",
            "\\frac{#?}{#?}    | 2/2",
            "e^{#?}            | e^2",
            "\\operatorname{sen}\\left(#?\\right) | sen(2)",
            "\\operatorname{sen}^{-1}\\left(#?\\right) | arcsen(2)",
            "\\cos^{-1}\\left(#?\\right) | arccos(2)",
            "\\log_{10}\\left(#?\\right) | log(2)",
            "\\log_{#?}\\left(#?\\right) | log_2(2)",
            "\\lim_{x\\to#?}#? | lim(x→2, 2)",
            "\\frac{d}{dx}\\left(#?\\right) | d/dx(2)",
            "\\frac{\\partial}{\\partial x}\\left(#?\\right) | ∂/∂x(2)",
            "\\int_{#?}^{#?}#?\\,dx | ∫(2, x, 2, 2)",
            "\\iint_{[#?,#?]\\times[#?,#?]}#?\\,dx\\,dy | ∬(2, x, 2, 2, y, 2, 2)",
            "\\iiint_{[#?,#?]\\times[#?,#?]\\times[#?,#?]}#?\\,dx\\,dy\\,dz | ∭(2, x, 2, 2, y, 2, 2, z, 2, 2)"
    })
    void plantillasDeEstructura(String plantilla, String texto) {
        String lleno = plantilla.replace("#@", "x").replace("#?", "2");
        String traducido = TraductorLatex.traducir(lleno);
        assertEquals(texto, traducido);
        assertDoesNotThrow(() -> Parser.parse(traducido));
    }

    /** RF69: la integral recién insertada, con solo el cuerpo lleno, es indefinida y está completa. */
    @Test
    void integralSinLimitesEsIndefinida() {
        String latex = "\\int_{\\placeholder{}}^{\\placeholder{}}x\\,dx";
        assertTrue(TraductorLatex.completo(latex));
        assertEquals("∫(x, x)", TraductorLatex.traducir(latex));
    }

    /** RF70: cada palabra de la lista tiene al menos una sugerencia. */
    @Test
    void sugerenciasCubrenLasDiezPalabras() {
        for (String palabra : List.of("lim", "der", "int", "sen", "cos", "tan", "ln", "log", "raiz", "frac")) {
            assertTrue(Plantillas.SUGERENCIAS.stream().anyMatch(s -> s.palabra().equals(palabra)), palabra);
        }
    }
}
