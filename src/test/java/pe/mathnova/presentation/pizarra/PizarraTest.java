package pe.mathnova.presentation.pizarra;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Paso;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;

/**
 * HU06, definición de terminado 3: los modelos de los dibujos de pizarra tienen los números
 * correctos para el TrabajoIntermedio de los ejemplos. No abren la ventana.
 */
class PizarraTest {

    private static final OrquestadorFactorizacion MOTOR = new OrquestadorFactorizacion();

    private static <T extends TrabajoIntermedio> T trabajo(String entrada, String codigo, Class<T> tipo) {
        Paso p = MOTOR.resolver(Parser.parse(entrada), Campo.Q).pasos().stream()
                .filter(x -> x.codigoTecnica().equals(codigo)).findFirst().orElseThrow();
        return assertInstanceOf(tipo, p.trabajo());
    }

    private static List<String> textos(List<ModeloRuffini.Celda> celdas) {
        return celdas.stream().map(ModeloRuffini.Celda::texto).toList();
    }

    /**
     * <pre>
     *       |  2   -3   -3    2
     *   x=2 |       4    2   -2
     *       |-------------------
     *       |  2    1   -1  | 0
     * </pre>
     */
    @Test
    void tablaDeRuffini() {
        ModeloRuffini m = ModeloRuffini.de(trabajo("2x^3 - 3x^2 - 3x + 2", "F16", TrabajoIntermedio.TablaRuffini.class));
        ModeloRuffini.Tabla t = m.tablas().getFirst();
        assertAll(
                () -> assertEquals(1, m.tablas().size()),
                () -> assertEquals("x = 2", t.raiz()),
                () -> assertEquals(List.of("2", "−3", "−3", "2"), textos(t.arriba())),
                () -> assertEquals(List.of("", "4", "2", "−2"), t.medio()),
                () -> assertEquals(List.of("2", "1", "−1"), t.abajo()),
                () -> assertEquals("0", t.residuo()),
                () -> assertTrue(t.residuoCero()),
                // De dónde sale cada número de la fila del medio: resultado anterior × raíz.
                () -> assertEquals(List.of("", "2·2", "1·2", "−1·2"), t.origenes()),
                () -> assertEquals(List.of("x = 2 ✓"), m.raices()),
                () -> assertEquals(List.of("x = 1 ✗ residuo −2"), m.fallidos()));
    }

    /** Raíz repetida: las tablas se apilan y el 0 del término x² que faltaba va marcado. */
    @Test
    void tablasApiladasConRaizRepetida() {
        ModeloRuffini m = ModeloRuffini.de(trabajo("x^3 - 3x + 2", "F17", TrabajoIntermedio.TablaRuffini.class));
        ModeloRuffini.Tabla primera = m.tablas().get(0);
        ModeloRuffini.Tabla segunda = m.tablas().get(1);
        assertAll(
                () -> assertEquals(List.of("1", "0", "−3", "2"), textos(primera.arriba())),
                () -> assertEquals(List.of(false, true, false, false),
                        primera.arriba().stream().map(ModeloRuffini.Celda::relleno).toList()),
                () -> assertEquals(List.of("1", "1", "−2"), primera.abajo()),
                // el cociente de una es la fila de arriba de la siguiente
                () -> assertEquals(primera.abajo(), textos(segunda.arriba())),
                () -> assertEquals(List.of("", "1", "2"), segunda.medio()),
                () -> assertEquals(List.of("1", "2"), segunda.abajo()),
                () -> assertEquals("0", segunda.residuo()));
    }

    /** 6x² + x − 2: 2x ╲╱ −1 / 3x ╱╲ 2. */
    @Test
    void aspaSimple() {
        ModeloAspa m = ModeloAspa.de(trabajo("6x^2 + x - 2", "F13", TrabajoIntermedio.AspaSimple.class));
        assertAll(
                () -> assertEquals(List.of("2x", "3x"), m.izquierda()),
                () -> assertEquals(List.of("−1", "2"), m.derecha()),
                () -> assertEquals(List.of("2x·2 = 4x", "3x·(−1) = −3x"), m.productos()),
                () -> assertEquals("4x − 3x = x ✓", m.suma()),
                () -> assertEquals("(2x − 1)(3x + 2)", m.resultado()));
    }

    @Test
    void aspaSimpleDeF12() {
        ModeloAspa m = ModeloAspa.de(trabajo("x^2 - 5x + 6", "F12", TrabajoIntermedio.AspaSimple.class));
        assertAll(
                () -> assertEquals(List.of("x", "x"), m.izquierda()),
                () -> assertEquals(List.of("−2", "−3"), m.derecha()),
                () -> assertEquals(List.of("x·(−3) = −3x", "x·(−2) = −2x"), m.productos()),
                () -> assertEquals("−3x − 2x = −5x ✓", m.suma()),
                () -> assertEquals("(x − 2)(x − 3)", m.resultado()));
    }

    /** x⁴ + 5x³ + 11x² + 14x + 4 = (x² + 3x + 1)(x² + 2x + 4). */
    @Test
    void aspaDobleEspecial() {
        ModeloAspaDoble m = ModeloAspaDoble.de(trabajo("x^4 + 5x^3 + 11x^2 + 14x + 4", "F19",
                TrabajoIntermedio.AspaDobleEspecial.class));
        assertAll(
                () -> assertEquals(List.of("x²", "3x", "1"), m.fila1()),
                () -> assertEquals(List.of("x²", "2x", "4"), m.fila2()),
                () -> assertEquals(List.of("x⁴ = x²·x²", "4 = 1·4"), m.extremos()),
                () -> assertEquals("se tiene 1·x² + 4·x², faltan 6x²", m.seTiene()),
                () -> assertEquals("6x² = 3x·2x", m.central()),
                () -> assertEquals(List.of("x²·2x + x²·3x = 5x³ ✓", "3x·4 + 2x·1 = 14x ✓"), m.laterales()),
                () -> assertEquals("(x² + 3x + 1)(x² + 2x + 4)", m.resultado()));
    }

    @Test
    void sistemaYSustitucionTraenSusRenglones() {
        var s = trabajo("x^5 + x^4 + 2x^3 + 2x^2 + 2x + 1", "F20", TrabajoIntermedio.SistemaCoeficientes.class);
        assertEquals(5, s.ecuaciones().size(), "x⁴, x³, x², x y el independiente");
        var u = trabajo("x^6 - 7x^3 - 8", "F14", TrabajoIntermedio.Sustitucion.class);
        assertAll(
                () -> assertEquals("u", u.variableNueva()),
                () -> assertEquals("u = x³", u.definicion()),
                () -> assertFalse(u.regresar().contains("u =")));
    }
}
