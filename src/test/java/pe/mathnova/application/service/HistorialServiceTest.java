package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.service.HistorialService.Filtro;
import pe.mathnova.application.service.HistorialService.Periodo;
import pe.mathnova.domain.factorizacion.Campo;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class HistorialServiceTest {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final ZonedDateTime AHORA = ZonedDateTime.of(2026, 11, 26, 12, 0, 0, 0, LIMA);

    @Autowired
    private HistorialService historial;
    @Autowired
    private ResolverService resolver;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager em;

    @BeforeEach
    void vaciar() {
        jdbc.update("DELETE FROM resolucion");
        jdbc.update("DELETE FROM ejercicio");
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private Map<String, Object> resolucion(int id) {
        return jdbc.queryForMap("SELECT * FROM resolucion WHERE id = ?", id);
    }

    private int guardarEn(String texto, ZonedDateTime cuando) {
        Object r = switch (resolver.decidir(texto)) {
            case ResolverService.Plan.Factorizar f -> new FactorizacionService(new pe.mathnova.domain.factorizacion
                    .OrquestadorFactorizacion()).factorizar(texto);
            case ResolverService.Plan.Derivar d -> resolver.resolverDerivada(texto, null, f -> { });
            case ResolverService.Plan.Integrar i -> resolver.resolverIntegral(texto, null, f -> { });
            case ResolverService.Plan.CalcularLimite l -> resolver.resolverLimite(texto, f -> { });
            default -> throw new IllegalArgumentException(texto);
        };
        return historial.guardar(new HistorialService.Guardado(texto, r, null, List.of()), cuando.toInstant(),
                historial.versionMotor()).orElseThrow();
    }

    @Test
    void alResolverSeGuardanElEjercicioSusTemasLaResolucionYTodosLosPasos() {
        int id = resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null).orElseThrow();

        Map<String, Object> ej = jdbc.queryForMap("SELECT e.* FROM ejercicio e JOIN resolucion r "
                + "ON r.ejercicio_id = e.id WHERE r.id = ?", id);
        Map<String, Object> r = resolucion(id);
        assertAll(
                () -> assertEquals("x^3 - 2x^2 - x + 2", ej.get("expresion_entrada")),
                () -> assertEquals("x^3 - 2x^2 - x + 2", ej.get("expresion_normalizada")),
                () -> assertEquals("x", ej.get("variables")),
                () -> assertEquals("POLINOMIO", ej.get("tipo")),
                () -> assertEquals("FACTORIZAR", r.get("operacion_codigo")),
                () -> assertEquals("Q", r.get("campo")),
                () -> assertEquals("campo=Q", r.get("clave_parametros")),
                () -> assertEquals("(x + 1)(x - 1)(x - 2)", r.get("resultado_texto")),
                () -> assertFalse(r.get("resultado_latex").toString().isBlank()),
                () -> assertEquals(historial.versionMotor(), r.get("version_motor")),
                () -> assertEquals("x^3 - 2x^2 - x + 2", r.get("entrada_texto")),
                () -> assertEquals("Factorización en Q", jdbc.queryForObject(
                        "SELECT t.nombre FROM tema t WHERE t.id = ?", String.class, r.get("tema_id"))),
                () -> assertEquals(List.of("Factorización en Q"), jdbc.queryForList("SELECT t.nombre FROM "
                        + "ejercicio_tema et JOIN tema t ON t.id = et.tema_id WHERE et.ejercicio_id = ?",
                        String.class, ej.get("id"))));
        List<Map<String, Object>> pasos = jdbc.queryForList("SELECT * FROM paso WHERE resolucion_id = ? ORDER BY orden", id);
        assertTrue(pasos.stream().anyMatch(p -> ((Number) p.get("es_descartado")).intValue() == 0));
        assertTrue(pasos.stream().anyMatch(p -> ((Number) p.get("es_descartado")).intValue() == 1
                && p.get("motivo_descarte") != null));
    }

    @Test
    void seGuardanTambienLasDemasOperacionesConSusPasosRestriccionesYTemas() {
        int fraccion = resolver.resolverYGuardar("(x^2 - 4)/(x - 2)", Campo.Q, null).orElseThrow();
        int derivada = resolver.resolverYGuardar("d/dx(x^3 + 2x)", Campo.Q, null).orElseThrow();
        int integral = resolver.resolverYGuardar("∫(x^2, x)", Campo.Q, null).orElseThrow();
        int ecuacion = resolver.resolverYGuardar("x^2 - 5x + 6 = 0", Campo.Q, null).orElseThrow();
        int limite = resolver.resolverYGuardar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q, null).orElseThrow();

        assertEquals("SIMPLIFICAR", resolucion(fraccion).get("operacion_codigo"));
        Map<String, Object> restriccion = jdbc.queryForMap(
                "SELECT * FROM restriccion_dominio WHERE resolucion_id = ?", fraccion);
        assertEquals("x ≠ 2", restriccion.get("texto"));
        assertEquals("DENOMINADOR", restriccion.get("origen"));
        assertTrue(contar("sugerencia") > 0, "las sugerencias de la fracción");

        Map<String, Object> d = resolucion(derivada);
        assertEquals("DERIVAR", d.get("operacion_codigo"));
        assertEquals("variable=x;orden=1", d.get("clave_parametros"));
        assertEquals("d/dx(x^3 + 2x)", d.get("entrada_texto"));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ? AND tecnica_codigo LIKE 'D%'",
                Integer.class, derivada) > 0);
        assertEquals("1", jdbc.queryForObject("SELECT valor FROM parametro_resolucion WHERE resolucion_id = ? "
                + "AND nombre = 'orden'", String.class, derivada));

        assertEquals("INTEGRAR", resolucion(integral).get("operacion_codigo"));
        assertEquals("RESOLVER_ECUACION", resolucion(ecuacion).get("operacion_codigo"));
        List<String> codigos = jdbc.queryForList("SELECT tecnica_codigo FROM paso WHERE resolucion_id = ? "
                + "AND paso_padre_id IS NULL ORDER BY orden", String.class, ecuacion);
        assertTrue(codigos.containsAll(List.of("EQ2", "EQ3")), codigos.toString());
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ? AND paso_padre_id IS NOT NULL",
                Integer.class, ecuacion) > 0, "los pasos de la factorización cuelgan de «Factorizar»");

        assertEquals("lim(x→2, (x^2 - 4)/(x - 2))", resolucion(limite).get("entrada_texto"));
        assertEquals(resolucion(fraccion).get("ejercicio_id"), resolucion(limite).get("ejercicio_id"));
        assertEquals(List.of("Fracciones algebraicas", "Límites"), jdbc.queryForList("SELECT t.nombre FROM "
                + "ejercicio_tema et JOIN tema t ON t.id = et.tema_id WHERE et.ejercicio_id = ? ORDER BY t.id",
                String.class, resolucion(limite).get("ejercicio_id")));
    }

    @Test
    void repetirElMismoEjercicioNoDuplicaSoloActualizaLaFecha() {
        int primero = guardarEn("x^2 - 4", AHORA.minusHours(3));
        int pasos = contar("paso");
        int segundo = guardarEn("x^2 - 4", AHORA);

        assertEquals(primero, segundo);
        assertEquals(1, contar("resolucion"));
        assertEquals(1, contar("ejercicio"));
        assertEquals(pasos, contar("paso"));
        Map<String, Object> r = resolucion(primero);
        assertEquals(AHORA.minusHours(3).toInstant().toString(), r.get("creado_en"));
        assertEquals(AHORA.toInstant().toString(), r.get("actualizado_en"));

        resolver.resolverYGuardar("x^2 - 4", Campo.R, null);
        resolver.resolverYGuardar("d/dx(x^2 - 4)", Campo.Q, null);
        assertEquals(3, contar("resolucion"));
        assertEquals(1, contar("ejercicio"));
    }

    @Test
    void siTerminaEnErrorNoSeGuardaNada() {
        assertThrows(RuntimeException.class, () -> resolver.resolverYGuardar("x^2 +", Campo.Q, null));
        assertThrows(RuntimeException.class, () -> resolver.resolverYGuardar("x^2 + y = 3", Campo.Q, null));
        assertEquals(0, contar("resolucion"));
        assertEquals(0, contar("ejercicio"));
        assertEquals(0, contar("paso"));
    }

    private void cuatroEjercicios() {
        guardarEn("x^2 - 4", AHORA.minusHours(2));
        guardarEn("d/dx(x^3)", AHORA.minusDays(2));
        guardarEn("∫(x^2, x)", AHORA.withDayOfMonth(3));
        guardarEn("lim(x→1, (x^2 - 1)/(x - 1))", AHORA.minusMonths(1));
    }

    private List<String> operaciones(Filtro f) {
        return historial.listar(f, 0, AHORA).filas().stream().map(HistorialService.Fila::operacion).toList();
    }

    @Test
    void laTablaVaDelMasRecienteAlMasAntiguo() {
        cuatroEjercicios();
        HistorialService.Pagina p = historial.listar(Filtro.todo(), 0, AHORA);
        assertEquals(4, p.total());
        assertEquals(List.of("Factorizar", "Derivar", "Integrar", "Calcular límite"),
                p.filas().stream().map(HistorialService.Fila::operacion).toList());
        HistorialService.Fila f = p.filas().getFirst();
        assertEquals("Factorización en Q", f.tema());
        assertEquals("Q", f.campo());
        assertEquals("—", p.filas().get(1).campo());
        assertEquals("Hoy, 10:00", HistorialService.fecha(f.fecha(), AHORA));
    }

    @Test
    void filtraPorTextoEnExpresionOperacionYTema() {
        cuatroEjercicios();
        assertEquals(List.of("Factorizar"), operaciones(new Filtro("x^2-4", null, Periodo.CUALQUIERA, false)));
        assertEquals(List.of("Derivar"), operaciones(new Filtro("deriv", null, Periodo.CUALQUIERA, false)));
        assertEquals(List.of("Calcular límite"), operaciones(new Filtro("limite", null, Periodo.CUALQUIERA, false)));
        assertEquals(List.of("Integrar"), operaciones(new Filtro("Integrales", null, Periodo.CUALQUIERA, false)));
        assertEquals(List.of(), operaciones(new Filtro("seno hiperbólico", null, Periodo.CUALQUIERA, false)));
    }

    @Test
    void filtraPorCurso() {
        cuatroEjercicios();
        assertEquals(List.of("Factorizar"),
                operaciones(new Filtro("", "Fundamentos de Matemática", Periodo.CUALQUIERA, false)));
        assertEquals(List.of("Derivar", "Calcular límite"), operaciones(new Filtro("", "Cálculo 1", Periodo.CUALQUIERA, false)));
        assertEquals(List.of("Integrar"), operaciones(new Filtro("", "Cálculo 2", Periodo.CUALQUIERA, false)));
        assertEquals(List.of(), operaciones(new Filtro("", "Cálculo 3", Periodo.CUALQUIERA, false)));
        assertEquals(4, operaciones(new Filtro("", HistorialService.TODOS_LOS_CURSOS, Periodo.CUALQUIERA, false)).size());
        assertEquals(List.of("Todos", "Fundamentos de Matemática", "Cálculo 1", "Cálculo 2", "Cálculo 3"), historial.cursos());
    }

    @Test
    void filtraPorFecha() {
        cuatroEjercicios();
        assertEquals(List.of("Factorizar"), operaciones(new Filtro("", null, Periodo.HOY, false)));
        assertEquals(List.of("Factorizar", "Derivar"), operaciones(new Filtro("", null, Periodo.SEMANA, false)));
        assertEquals(List.of("Factorizar", "Derivar", "Integrar"), operaciones(new Filtro("", null, Periodo.MES, false)));
    }

    @Test
    void filtraPorFavoritosYLaEstrellaSeDesmarca() {
        cuatroEjercicios();
        int derivada = historial.listar(Filtro.todo(), 0, AHORA).filas().get(1).id();
        assertTrue(historial.alternarFavorito(derivada));
        assertEquals(List.of("Derivar"), operaciones(new Filtro("", null, Periodo.CUALQUIERA, true)));
        assertTrue(historial.listar(Filtro.todo(), 0, AHORA).filas().get(1).favorito());
        assertFalse(historial.alternarFavorito(derivada));
        assertEquals(List.of(), operaciones(new Filtro("", null, Periodo.CUALQUIERA, true)));
    }

    @Test
    void lasConsultasVanDeCincuentaEnCincuenta() {
        for (int k = 1; k <= 55; k++) {
            guardarEn("x + " + k, AHORA.minusMinutes(k));
        }
        HistorialService.Pagina primera = historial.listar(Filtro.todo(), 0, AHORA);
        HistorialService.Pagina segunda = historial.listar(Filtro.todo(), 1, AHORA);
        assertEquals(50, primera.filas().size());
        assertTrue(primera.hayMas());
        assertEquals(55, primera.total());
        assertEquals(5, segunda.filas().size());
        assertFalse(segunda.hayMas());
        assertEquals("x + 1", primera.filas().getFirst().texto());
        assertEquals("x + 55", segunda.filas().getLast().texto());
    }

    @Test
    void borrarSeLlevaSusPasosSinDejarHuerfanos() {
        int limite = resolver.resolverYGuardar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q, null).orElseThrow();
        int fraccion = resolver.resolverYGuardar("(x^2 - 4)/(x - 2)", Campo.Q, null).orElseThrow();
        Object ejercicio = resolucion(limite).get("ejercicio_id");
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ? AND paso_padre_id IS NOT NULL",
                Integer.class, limite) > 0, "el límite tiene pasos anidados");

        historial.eliminar(limite);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ?", Integer.class, limite));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM parametro_resolucion WHERE resolucion_id = ?",
                Integer.class, limite));
        assertEquals(1, contar("ejercicio"), "la fracción sigue usando el ejercicio");

        historial.eliminar(fraccion);
        assertAll(
                () -> assertEquals(0, contar("resolucion")),
                () -> assertEquals(0, contar("ejercicio")),
                () -> assertEquals(0, contar("paso")),
                () -> assertEquals(0, contar("sugerencia")),
                () -> assertEquals(0, contar("restriccion_dominio")),
                () -> assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ejercicio_tema WHERE ejercicio_id = ?",
                        Integer.class, ejercicio)),
                () -> assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id NOT IN "
                        + "(SELECT id FROM resolucion)", Integer.class)));
    }

    @Test
    void laHojaSeArmaConLoGuardadoSinRecalcular() {
        int id = resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null).orElseThrow();
        jdbc.update("UPDATE paso SET justificacion = 'nota guardada' WHERE resolucion_id = ? AND orden = 1", id);
        em.clear();

        HistorialService.Hoja h = historial.hoja(id);
        assertFalse(h.recalculada());
        assertEquals("Factorizar", h.verbo());
        assertEquals("1", h.pasos().getFirst().numero());
        assertEquals("nota guardada", h.pasos().getFirst().nota(), "se leyó paso, no se volvió a resolver");
        assertEquals("Agrupación de términos", h.pasos().getFirst().titulo());
        assertFalse(h.descartados().isEmpty());
        assertTrue(h.descartados().stream().allMatch(d -> d.motivo() != null));
        assertEquals("x^3 - 2x^2 - x + 2", h.entradaTexto());
        assertEquals("Q", h.campo());
        assertFalse(h.resultadoLatex().isBlank());
    }

    @Test
    void conOtraVersionDelMotorLaHojaSeRecalcula() {
        int id = resolver.resolverYGuardar("x^3 - 2x^2 - x + 2", Campo.Q, null).orElseThrow();
        jdbc.update("UPDATE resolucion SET version_motor = '0.0.1' WHERE id = ?", id);
        jdbc.update("UPDATE paso SET justificacion = 'nota vieja' WHERE resolucion_id = ? AND orden = 1", id);
        em.clear();

        HistorialService.Hoja h = historial.hoja(id);
        assertTrue(h.recalculada());
        assertEquals(historial.versionMotor(), h.versionMotor());
        assertNotEquals("nota vieja", h.pasos().getFirst().nota());
        assertEquals(1, contar("resolucion"));
    }

    @Test
    void laHojaNumeraLosPasosAnidados() {
        int id = resolver.resolverYGuardar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q, null).orElseThrow();
        HistorialService.Hoja h = historial.hoja(id);
        assertEquals("Calcular límite", h.verbo());
        assertTrue(h.pasos().stream().anyMatch(p -> p.nivel() == 1 && p.numero().contains(".")),
                h.pasos().toString());
    }

    @Test
    void lasFechasDicenHoyAyerOElDia() {
        ZonedDateTime ahora = ZonedDateTime.of(2026, 11, 23, 21, 0, 0, 0, LIMA);
        assertEquals("Hoy, 20:14", HistorialService.fecha(ahora.withHour(20).withMinute(14).toInstant(), ahora));
        assertEquals("Ayer, 18:40", HistorialService.fecha(ahora.minusDays(1).withHour(18).withMinute(40).toInstant(), ahora));
        assertEquals("21 nov, 16:30", HistorialService.fecha(ahora.minusDays(2).withHour(16).withMinute(30).toInstant(), ahora));
        assertEquals("3 dic 2025, 09:05", HistorialService.fecha(
                ZonedDateTime.of(2025, 12, 3, 9, 5, 0, 0, LIMA).toInstant(), ahora));
        assertEquals("Hoy, 00:00", HistorialService.fecha(Instant.parse("2026-11-23T05:00:00Z"), ahora));
    }
}
