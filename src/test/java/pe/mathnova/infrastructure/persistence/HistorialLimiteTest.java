package pe.mathnova.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.service.HistorialFactorizacionService;
import pe.mathnova.domain.calculo.limites.MotorLimites;
import pe.mathnova.domain.calculo.limites.ResolucionLimite;
import pe.mathnova.domain.expresion.Limite;
import pe.mathnova.domain.factorizacion.OrquestadorFactorizacion;
import pe.mathnova.domain.factorizacion.Simplificacion;
import pe.mathnova.domain.factorizacion.SimplificadorFracciones;
import pe.mathnova.domain.parser.Parser;
import pe.mathnova.domain.sugerencias.EjercicioResuelto;
import pe.mathnova.domain.sugerencias.MotorSugerencias;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class HistorialLimiteTest {

    private static final OrquestadorFactorizacion ORQUESTADOR = new OrquestadorFactorizacion();
    private static final Instant AHORA = Instant.parse("2026-11-22T10:00:00Z");

    @Autowired
    private HistorialFactorizacionService historial;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void elLimiteSeGuardaConSusPasosAnidadosYParametros() {
        String texto = "lim(x→2, (x^2 - 4)/(x - 2))";
        ResolucionLimite r = new MotorLimites(ORQUESTADOR).resolver((Limite) Parser.parse(texto));
        Integer id = historial.guardarLimite(texto, r, AHORA, "0.2.0").orElseThrow().getId();

        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM resolucion WHERE id = ?", id);
        assertEquals("LIMITE", fila.get("operacion_codigo"));
        assertEquals("4", fila.get("resultado_texto"));
        assertEquals("variable=x;punto=2;lado=AMBOS", fila.get("clave_parametros"));
        assertEquals("(x^2 - 4)/(x - 2)", jdbc.queryForObject("SELECT e.expresion_normalizada FROM ejercicio e "
                + "JOIN resolucion r ON r.ejercicio_id = e.id WHERE r.id = ?", String.class, id));

        List<Map<String, Object>> pasos = jdbc.queryForList("SELECT * FROM paso WHERE resolucion_id = ? ORDER BY orden", id);
        assertEquals(List.of("L01", "L02", "F05", "L01"), pasos.stream().map(p -> p.get("tecnica_codigo")).toList());
        Object padre = pasos.get(2).get("paso_padre_id");
        assertNotNull(padre);
        assertEquals(pasos.get(1).get("id"), padre);
        assertEquals("0/0", pasos.getFirst().get("despues_texto").toString().replaceAll(".* = ", ""));

        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM parametro_resolucion WHERE resolucion_id = ?",
                Integer.class, id));
        historial.guardarLimite(texto, r, AHORA.plusSeconds(60), "0.3.0");
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM paso WHERE resolucion_id = ?", Integer.class, id));
    }

    @Test
    void laFraccionSeGuardaConSusSugerencias() {
        String texto = "(x^2 - 4)/(x - 2)";
        Simplificacion s = new SimplificadorFracciones(ORQUESTADOR).simplificar(Parser.parse(texto));
        var sugerencias = new MotorSugerencias().sugerir(EjercicioResuelto.de(s).orElseThrow());
        Integer id = historial.guardarSimplificacion(texto, s, sugerencias, AHORA, "0.2.0").orElseThrow().getId();
        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT * FROM sugerencia WHERE resolucion_id = ? ORDER BY orden", id);
        assertEquals(sugerencias.size(), filas.size());
        assertEquals("RN15", filas.getFirst().get("regla_codigo"));
        assertEquals("LIMITE", filas.getFirst().get("operacion_codigo"));
        assertTrue(filas.getFirst().get("justificacion").toString().contains("0/0"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM sugerencia WHERE resolucion_id = ? "
                + "AND json_valid(parametros_json) = 0", Integer.class, id));
        assertEquals("SIMPLIFICAR", jdbc.queryForObject("SELECT operacion_codigo FROM resolucion WHERE id = ?",
                String.class, id));
    }
}
