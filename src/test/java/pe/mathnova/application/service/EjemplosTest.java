package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import pe.mathnova.application.service.EjemplosService.Ejemplo;
import pe.mathnova.domain.factorizacion.Campo;

/**
 * Sprint 9, parte B: los ejemplos de la migración V4. RN20: un ejemplo que el motor no resuelve no puede estar
 * en la app, así que aquí se resuelven todos y cada resultado se compara con el esperado de su fila.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class EjemplosTest {

    /** Lo que "Resolver" decide hacer con cada operación de la tabla. */
    private static final Map<String, Set<Class<?>>> PLANES = Map.ofEntries(
            Map.entry("FACTORIZAR", Set.of(ResolverService.Plan.Factorizar.class,
                    ResolverService.Plan.FactorizarFunciones.class)),
            Map.entry("SIMPLIFICAR", Set.of(ResolverService.Plan.Simplificar.class)),
            Map.entry("LIMITE", Set.of(ResolverService.Plan.CalcularLimite.class)),
            Map.entry("DERIVAR", Set.of(ResolverService.Plan.Derivar.class)),
            Map.entry("PUNTOS_CRITICOS", Set.of(ResolverService.Plan.Derivar.class)),
            Map.entry("INTEGRAR", Set.of(ResolverService.Plan.Integrar.class)),
            Map.entry("INTEGRAL_DEFINIDA", Set.of(ResolverService.Plan.Integrar.class)),
            Map.entry("LIMITE_DOBLE", Set.of(ResolverService.Plan.LimiteDoble.class)),
            Map.entry("DERIVADA_PARCIAL", Set.of(ResolverService.Plan.Parciales.class)),
            Map.entry("GRADIENTE", Set.of(ResolverService.Plan.Parciales.class)),
            Map.entry("EXTREMOS", Set.of(ResolverService.Plan.Extremos.class)),
            Map.entry("LAGRANGE", Set.of(ResolverService.Plan.Extremos.class)),
            Map.entry("INTEGRAL_MULTIPLE", Set.of(ResolverService.Plan.IntegralMultiple.class)));

    @Autowired
    private EjemplosService ejemplos;
    @Autowired
    private ResolverService resolver;

    @Test
    void elMotorResuelveTodosLosEjemplos() {
        List<Ejemplo> todos = ejemplos.todos();
        assertEquals(33, todos.size());
        assertAll(todos.stream().map(e -> (Executable) () -> assertEquals(Optional.of(e.esperado()),
                ejemplos.resultado(e), e.tema() + " · " + e.texto() + " en " + e.campo())));
    }

    @Test
    void cadaOperacionCoincideConLoQueHaceResolver() {
        assertAll(ejemplos.todos().stream().map(e -> (Executable) () -> {
            Set<Class<?>> esperados = PLANES.get(e.operacion());
            assertTrue(esperados != null && esperados.contains(resolver.decidir(e.texto()).getClass()),
                    e.texto() + " no es " + e.operacion());
        }));
    }

    @Test
    void cadaTemaTieneAlMenosDosYFactorizacionEnQCuatro() {
        List<EjemplosService.Curso> cursos = ejemplos.cursos();
        assertEquals(List.of("Fundamentos de Matemática", "Cálculo 1", "Cálculo 2", "Cálculo 3"),
                cursos.stream().map(EjemplosService.Curso::nombre).toList());
        assertAll(cursos.stream().flatMap(c -> c.temas().stream()).map(t -> (Executable) () -> {
            assertTrue(t.cantidad() >= 2, t.nombre() + " tiene " + t.cantidad());
            assertEquals(t.cantidad(), ejemplos.ejemplos(t.id()).size(), t.nombre());
        }));
        EjemplosService.Tema q = cursos.getFirst().temas().getFirst();
        assertEquals("Factorización en Q", q.nombre());
        assertTrue(q.cantidad() >= 4);
    }

    @Test
    void laTarjetaTraeSuFormulaSuNotaYSuCampo() {
        Ejemplo ruffini = ejemplos.todos().stream().filter(e -> e.texto().equals("2x^3 - 3x^2 - 3x + 2"))
                .findFirst().orElseThrow();
        assertEquals("Factoriza", ruffini.enunciado());
        assertEquals("Ruffini y aspa simple", ruffini.nota());
        assertNull(ruffini.campo());
        assertTrue(ruffini.latex().startsWith("2x^{3}"), ruffini.latex());

        Ejemplo enC = ejemplos.todos().stream().filter(e -> e.texto().equals("x^2 + 4")).findFirst().orElseThrow();
        assertEquals(Campo.C, enC.campo());

        Ejemplo criticos = ejemplos.todos().stream().filter(e -> e.operacion().equals("PUNTOS_CRITICOS"))
                .findFirst().orElseThrow();
        assertTrue(criticos.latex().startsWith("f(x) = "), criticos.latex());
        // Ninguna fórmula cae al texto plano: todas se leen
        assertAll(ejemplos.todos().stream().map(e -> (Executable) () -> assertFalse(
                e.latex().startsWith("\text{"), e.texto())));
    }

    @Test
    void elCampoSeLeeDeLosParametros() {
        assertEquals(Campo.R, EjemplosService.campo("{\"campo\":\"R\"}"));
        assertEquals(Campo.C, EjemplosService.campo("{\"campo\": \"C\"}"));
        assertNull(EjemplosService.campo(null));
        assertNull(EjemplosService.campo("{}"));
    }
}
