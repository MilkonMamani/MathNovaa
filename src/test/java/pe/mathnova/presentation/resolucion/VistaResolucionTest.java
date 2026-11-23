package pe.mathnova.presentation.resolucion;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import pe.mathnova.domain.algebra.Polinomio;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.factorizacion.TrabajoIntermedio;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.presentation.pizarra.ModeloRuffini;

/** Lo que ve el estudiante: etiquetas, nombres sin códigos, Ruffini seguido, descartados y notas. */
class VistaResolucionTest {

    private static final Pattern CODIGO = Pattern.compile("\\b(F[0-9][0-9]|A[0-9]|P[0-9][0-9])\\b");

    private final OrquestadorFactorizacion motor = new OrquestadorFactorizacion();

    private VistaResolucion vista(String texto) {
        return vista(texto, null, false);
    }

    private VistaResolucion vista(String texto, String forzado, boolean codigos) {
        Resolucion r = motor.resolver(Parser.parse(texto), Campo.Q, forzado);
        return VistaResolucion.de(r, new VerificacionDePrueba().de(r), codigos);
    }

    /** Verificacion.de es del paquete de aplicación: se llega por el servicio, sin historial. */
    private static final class VerificacionDePrueba {
        private final pe.mathnova.application.service.FactorizacionService servicio =
                new pe.mathnova.application.service.FactorizacionService(new OrquestadorFactorizacion(), null);

        pe.mathnova.application.service.Verificacion de(Resolucion r) {
            return servicio.verificar(r);
        }

        pe.mathnova.application.service.Verificacion de(Simplificacion s) {
            return servicio.verificar(s);
        }
    }

    private static String todoElTexto(VistaResolucion v) {
        StringBuilder sb = new StringBuilder(v.subtitulo()).append(' ').append(v.notas()).append(' ').append(v.sinPasos());
        v.pasos().forEach(p -> sb.append(' ').append(p.titulo()).append(' ').append(p.justificacion()));
        v.descartados().forEach(g -> g.filas().forEach(d -> sb.append(' ').append(d.nombre()).append(' ').append(d.motivo())));
        v.alternativas().forEach(a -> sb.append(' ').append(a.nombre()));
        return sb.toString();
    }

    @Test
    void verificadoSoloConPasos() {
        assertEquals(VistaResolucion.Etiqueta.VERIFICADO, vista("x^2 - 4").etiqueta());
        assertEquals(VistaResolucion.Etiqueta.IRREDUCIBLE, vista("x^2 + x + 1").etiqueta());
        assertEquals(VistaResolucion.Etiqueta.IRREDUCIBLE, vista("x^4 + x + 1").etiqueta());
        assertEquals(VistaResolucion.Etiqueta.SIN_FACTORIZACION, vista("x^10 + x + 1").etiqueta());
    }

    /** A.1: ningún código interno a la vista (ni en títulos, ni en motivos, ni en alternativas). */
    @Test
    void sinCodigosPorDefecto() {
        for (String texto : List.of("2x^4 - 5x^3 + 5x - 2", "6x^2 + x - 2", "x^4 + 1", "x^10 + x + 1", "x^3 + x + 1",
                "x^2 + y^2", "-x^3 + 8", "2x^2 - 10x + 12", "x^5 - 5x^4 + 7x^3 - 7x^2 + 16x - 12")) {
            String todo = todoElTexto(vista(texto));
            assertFalse(CODIGO.matcher(todo).find(), texto + ": " + todo);
            assertFalse(todo.toLowerCase().contains("sprint") || todo.contains("certificación"), texto + ": " + todo);
        }
    }

    @Test
    void nombresCortosYCodigosSoloSiSePiden() {
        VistaResolucion sin = vista("2x^4 - 5x^3 + 5x - 2");
        assertAll(
                () -> assertEquals(List.of("Agrupación", "Diferencia de cuadrados", "Aspa simple"),
                        sin.pasos().stream().map(VistaPaso::titulo).toList()),
                () -> assertEquals("Agrupación de términos", sin.pasos().getFirst().ayuda(), "nombre largo en el tooltip"),
                () -> assertEquals(List.of(new VistaResolucion.Alternativa("F16", "Ruffini", "Ruffini (divisores binómicos)")),
                        sin.alternativas()));
        VistaResolucion con = vista("2x^4 - 5x^3 + 5x - 2", null, true);
        assertEquals(List.of("F04 · Agrupación", "F05 · Diferencia de cuadrados", "F13 · Aspa simple"),
                con.pasos().stream().map(VistaPaso::titulo).toList());
    }

    /** A.4: tres pasos de Ruffini seguidos se presentan como uno con tres tablas apiladas. */
    @Test
    void ruffiniSeguidoEsUnSoloPaso() {
        String entrada = "x^5 - 5x^4 + 7x^3 - 7x^2 + 16x - 12";
        Resolucion r = motor.resolver(Parser.parse(entrada), Campo.Q);
        assertEquals(List.of("F16", "F16", "F16"), r.pasos().stream().map(p -> p.codigoTecnica()).toList(),
                "el motor (y la base) los tiene como tres pasos");

        VistaResolucion v = vista(entrada);
        assertEquals(1, v.pasos().size());
        VistaPaso paso = v.pasos().getFirst();
        ModeloRuffini m = ModeloRuffini.de(assertInstanceOf(TrabajoIntermedio.TablaRuffini.class, paso.trabajo()));
        assertAll(
                () -> assertEquals("Ruffini (3 raíces)", paso.titulo()),
                () -> assertEquals(1, paso.orden()),
                () -> assertEquals(3, m.tablas().size()),
                () -> assertEquals(List.of("x = 1", "x = 2", "x = 3"), m.tablas().stream().map(ModeloRuffini.Tabla::raiz).toList()),
                // el cociente de una es la fila de arriba de la siguiente
                () -> assertEquals(m.tablas().get(0).abajo(),
                        m.tablas().get(1).arriba().stream().map(ModeloRuffini.Celda::texto).toList()),
                () -> assertEquals(m.tablas().get(1).abajo(),
                        m.tablas().get(2).arriba().stream().map(ModeloRuffini.Celda::texto).toList()),
                () -> assertEquals(List.of("1", "1", "2"), m.tablas().get(2).abajo()),
                () -> assertEquals(Polinomio.desde(Parser.parse(entrada)), Polinomio.desde(paso.despues()),
                        "(x − 1)(x − 2)(x − 3)(x² + x + 2)"),
                () -> assertTrue(paso.justificacion().contains("x = 1, x = 2 y x = 3")));
    }

    @Test
    void ruffiniConRaizRepetidaTambienSeFusiona() {
        VistaResolucion v = vista("x^3 - 3x + 2");
        VistaPaso paso = v.pasos().getFirst();
        assertAll(
                () -> assertEquals(1, v.pasos().size(), "F16 y F17 en una sola fila"),
                () -> assertEquals("Ruffini (2 raíces)", paso.titulo()),
                () -> assertEquals(2, assertInstanceOf(TrabajoIntermedio.TablaRuffini.class, paso.trabajo()).divisiones().size()),
                () -> assertTrue(paso.justificacion().contains("Se completa con 0 el término de x² que falta")));
    }

    /** A.5: agrupados por la expresión sobre la que se probaron. */
    @Test
    void descartadosAgrupadosPorExpresion() {
        VistaResolucion v = vista("2x^4 - 5x^3 + 5x - 2");
        VistaResolucion.GrupoDescartados primero = v.descartados().getFirst();
        assertAll(
                () -> assertEquals(Polinomio.desde(Parser.parse("2x^4 - 5x^3 + 5x - 2")), Polinomio.desde(primero.sobre())),
                () -> assertEquals("Factor común", primero.filas().getFirst().nombre()),
                () -> assertEquals("Factor común monomio", primero.filas().getFirst().ayuda()),
                () -> assertTrue(v.descartados().stream().anyMatch(g ->
                        Polinomio.desde(g.sobre()).equals(Polinomio.desde(Parser.parse("2x^2 - 5x + 2"))))),
                () -> assertEquals(v.descartados().stream().mapToInt(g -> g.filas().size()).sum(), v.totalDescartados()));
        // Los motivos del dominio que nombran un código pasan a nombrar el método.
        VistaResolucion aspa = vista("6x^2 + x - 2", "F13", false);
        assertTrue(aspa.descartados().isEmpty());
        assertEquals("corresponde Aspa simple", pe.mathnova.presentation.FormateadorTexto.sinCodigos("corresponde F13"));
    }

    /** A.6 y A.7: una sola frase sobre los factores y el subtítulo con el número de factores. */
    @Test
    void notaDeFactoresYSubtitulo() {
        VistaResolucion lineales = vista("2x^4 - 5x^3 + 5x - 2");
        VistaResolucion conCuadratico = vista("x^4 - 16");
        VistaResolucion cuarta = vista("x^4 + x + 1");
        assertAll(
                () -> assertEquals("Polinomio de grado 4 · 4 factores en Q", lineales.subtitulo()),
                () -> assertEquals(List.of("Los 4 factores son de grado 1: ya no se pueden factorizar más."), lineales.notas()),
                () -> assertEquals("Polinomio de grado 4 · 3 factores en Q", conCuadratico.subtitulo()),
                () -> assertEquals(List.of("x² + 4 no se factoriza en Q porque Δ = −16 < 0."), conCuadratico.notas()),
                () -> assertEquals("Polinomio de grado 4 · irreducible en Q", cuarta.subtitulo()),
                () -> assertEquals(List.of("x⁴ + x + 1 no se puede factorizar en Q: no tiene raíces racionales y no se "
                        + "parte en factores de grado 2 o más."), cuarta.notas()));
    }

    @Test
    void fraccionConRestricciones() {
        Simplificacion s = new SimplificadorFracciones(motor).simplificar(Parser.parse("(x^2 - 4)/(x^2 + x - 6)"));
        VistaResolucion v = VistaResolucion.de(s, new VerificacionDePrueba().de(s), false);
        assertAll(
                () -> assertEquals(VistaResolucion.Etiqueta.VERIFICADO, v.etiqueta()),
                () -> assertEquals(List.of("Restricciones del denominador original: x ≠ −3, x ≠ 2"), v.notas()),
                // Mismo grado arriba y abajo: es impropia, así que después de cancelar (P01) se divide (P02).
                () -> assertEquals(List.of("F05", "F12", "P01", "P02"), v.pasos().stream().map(VistaPaso::codigo).toList()),
                () -> assertEquals(List.of("Diferencia de cuadrados · numerador", "Trinomio simple · denominador",
                        "Simplificar fracción", "División de polinomios"), v.pasos().stream().map(VistaPaso::titulo).toList()));
    }
}
