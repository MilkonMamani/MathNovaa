package pe.mathnova.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.service.HistorialFactorizacionService;
import pe.mathnova.domain.factorizacion.Campo;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Resolucion;
import pe.mathnova.domain.parser.Parser;

/** RN24 y RN25 sobre la base de prueba de target/. Cada prueba se deshace al terminar. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class ResolucionRepositoryTest {

    private static final String ENTRADA = "x^3 - 2x^2 - x + 2";
    private static final String CONSULTA = "SELECT r.* FROM resolucion r JOIN ejercicio e ON e.id = r.ejercicio_id "
            + "WHERE e.expresion_normalizada = ? AND r.operacion_codigo = 'FACTORIZAR' AND r.clave_parametros = 'campo=Q'";

    @Autowired
    private HistorialFactorizacionService historial;
    @Autowired
    private JdbcTemplate jdbc;

    private final Resolucion resolucion =
            new OrquestadorFactorizacion().resolver(Parser.parse(ENTRADA), Campo.Q);

    private List<Map<String, Object>> filas() {
        return jdbc.queryForList(CONSULTA, "x^3 - 2x^2 - x + 2");
    }

    @Test
    void guardarDosVecesDejaUnaSolaFilaConLaFechaActualizada() {
        historial.guardar(ENTRADA, resolucion, Instant.parse("2026-11-15T10:00:00Z"), "0.2.0");
        historial.guardar(ENTRADA, resolucion, Instant.parse("2026-11-15T11:30:00Z"), "0.2.0");

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        Map<String, Object> fila = filas.getFirst();
        assertAll(
                () -> assertEquals("2026-11-15T11:30:00Z", fila.get("actualizado_en")),
                () -> assertEquals("2026-11-15T10:00:00Z", fila.get("creado_en")),
                () -> assertEquals("(x + 1)(x - 1)(x - 2)", fila.get("resultado_texto")),
                () -> assertEquals("0.2.0", fila.get("version_motor")),
                () -> assertEquals("Q", fila.get("campo")));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM ejercicio WHERE expresion_normalizada = ?", Integer.class, "x^3 - 2x^2 - x + 2"));
    }

    @Test
    void guardaLosPasosYLosDescartados() {
        historial.guardar(ENTRADA, resolucion, Instant.parse("2026-11-15T10:00:00Z"), "0.2.0");
        Object id = filas().getFirst().get("id");
        List<Map<String, Object>> pasos = jdbc.queryForList(
                "SELECT * FROM paso WHERE resolucion_id = ? ORDER BY orden", id);
        int aplicados = resolucion.pasos().size();
        assertEquals(aplicados + resolucion.descartados().size(), pasos.size());
        assertEquals("F04", pasos.get(0).get("tecnica_codigo"));
        assertEquals("OTRO", pasos.get(0).get("trabajo_tipo"));
        assertEquals("(x - 2)(x^2 - 1)", pasos.get(0).get("despues_texto"));
        Map<String, Object> f01 = pasos.get(aplicados);
        assertAll(
                () -> assertEquals("F01", f01.get("tecnica_codigo")),
                () -> assertEquals(1, ((Number) f01.get("es_descartado")).intValue()),
                () -> assertEquals("MCD(1, 2, 1, 2) = 1 y no todos los términos tienen x", f01.get("motivo_descarte")),
                () -> assertEquals(null, f01.get("despues_texto")));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ? "
                + "AND trabajo_json IS NOT NULL AND json_valid(trabajo_json) = 0", Integer.class, id));
    }

    @Test
    void otraVersionDelMotorReemplazaLosPasos() {
        historial.guardar(ENTRADA, resolucion, Instant.parse("2026-11-15T10:00:00Z"), "0.1.0");
        Resolucion otra = new OrquestadorFactorizacion().resolver(Parser.parse("x^2(x - 2) - (x - 2)"), Campo.Q);
        historial.guardar(ENTRADA, otra, Instant.parse("2026-11-15T12:00:00Z"), "0.2.0");

        List<Map<String, Object>> filas = filas();
        assertEquals(1, filas.size());
        Object id = filas.getFirst().get("id");
        assertEquals("0.2.0", filas.getFirst().get("version_motor"));
        List<String> tecnicas = jdbc.queryForList(
                "SELECT tecnica_codigo FROM paso WHERE resolucion_id = ? AND es_descartado = 0 ORDER BY orden",
                String.class, id);
        assertEquals(List.of("F03", "F05"), tecnicas);
        assertTrue(otra.pasos().size() + otra.descartados().size() > 0);
    }

    @Test
    void unaConstanteNoSeGuarda() {
        Resolucion siete = new OrquestadorFactorizacion().resolver(Parser.parse("7"), Campo.Q);
        assertTrue(historial.guardar("7", siete, Instant.parse("2026-11-15T10:00:00Z"), "0.2.0").isEmpty());
    }
}
