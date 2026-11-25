package pe.mathnova.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import pe.mathnova.application.service.PracticaService.Opcion;
import pe.mathnova.application.service.PracticaService.Practica;
import pe.mathnova.application.service.PracticaService.Pregunta;
import pe.mathnova.application.service.PracticaService.Respuesta;
import pe.mathnova.domain.factorizacion.Campo;

/**
 * Sprint 9, parte D: el modo Práctica con los tres ejercicios del enunciado. En cada paso la opción correcta
 * está entre las opciones, un distractor devuelve su motivo, el conteo de aciertos a la primera es correcto y la
 * sesión queda guardada. Base de prueba de target/; cada prueba se deshace al terminar.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
@Transactional
class PracticaServiceTest {

    @Autowired
    private PracticaService practica;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void ordenFijo() {
        practica.setAzar(new Random(7));
    }

    /**
     * Recorre la práctica: en el primer paso elige antes un distractor (falla una vez); en los demás acierta a la
     * primera. Comprueba en cada paso que la correcta está entre las opciones y que el distractor da su motivo.
     */
    private Practica recorrer(String texto, List<String> esperados) {
        Practica p = practica.iniciar(texto, Campo.Q);
        assertEquals(esperados.size(), p.totalPasos(), texto);
        for (int k = 1; k <= esperados.size(); k++) {
            Pregunta q = practica.opciones(p.sesionId(), k);
            String correcta = esperados.get(k - 1);
            assertEquals(correcta, practica.correcta(p.sesionId(), k), texto + ", paso " + k);
            List<String> codigos = q.opciones().stream().map(Opcion::codigo).toList();
            assertTrue(codigos.contains(correcta), texto + ", paso " + k + ": falta la correcta en " + codigos);
            assertEquals(codigos.size(), codigos.stream().distinct().count(), "opciones repetidas: " + codigos);
            int esperadas = PracticaService.TERMINASTE.equals(q.pregunta()) ? 2 : 3;
            assertTrue(codigos.size() >= esperadas && codigos.size() <= 4,
                    texto + ", paso " + k + ": la correcta y 2 o 3 distractores, no " + codigos);
            // Cada distractor tiene su motivo
            Map<String, String> motivos = practica.motivos(p.sesionId(), k);
            for (String c : codigos) {
                if (!c.equals(correcta)) {
                    assertFalse(motivos.getOrDefault(c, "").isBlank(), "sin motivo: " + c);
                }
            }
            assertFalse(practica.pista(p.sesionId(), k).isBlank());

            if (k == 1) {
                String distractor = codigos.stream().filter(c -> !c.equals(correcta)).findFirst().orElseThrow();
                Respuesta mal = practica.responder(p.sesionId(), k, distractor);
                assertFalse(mal.correcta());
                assertEquals(motivos.get(distractor), mal.motivo());
                assertTrue(mal.pasos().isEmpty());
                assertEquals(1, mal.estado().fallidos());
                assertEquals(1, mal.estado().paso(), "fallar no avanza");
            }
            Respuesta bien = practica.responder(p.sesionId(), k, correcta);
            assertTrue(bien.correcta());
            assertNull(bien.motivo());
            if (!PracticaService.TERMINASTE.equals(q.pregunta())) {
                assertFalse(bien.pasos().isEmpty(), "al acertar se muestra el paso");
            }
        }
        PracticaService.Estado e = practica.estado(p.sesionId());
        assertTrue(e.terminada());
        assertEquals(esperados.size() - 1, e.aciertos(), "el primer paso se falló una vez: no cuenta");
        assertEquals(1, e.fallidos());

        // La sesión y los intentos quedaron guardados
        Map<String, Object> fila = jdbc.queryForMap("SELECT * FROM sesion_practica WHERE id = ?", p.sesionId());
        assertEquals(p.resolucionId(), ((Number) fila.get("resolucion_id")).intValue());
        assertEquals(esperados.size(), ((Number) fila.get("total_pasos")).intValue());
        assertEquals(esperados.size() - 1, ((Number) fila.get("aciertos")).intValue());
        assertNotNull(fila.get("terminada_en"));
        assertEquals(esperados.size() + 1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM intento_paso WHERE sesion_id = ?", Integer.class, p.sesionId()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM intento_paso WHERE sesion_id = ? AND es_correcto = 0", Integer.class,
                p.sesionId()));
        return p;
    }

    @Test
    void agrupacionDiferenciaDeCuadradosYTerminado() {
        Practica p = recorrer("x^3 - 2x^2 - x + 2", List.of("F04", "F05", PracticaService.CODIGO_SI));
        assertEquals("Factorizar", p.titulo());
        // La técnica elegida se guarda con su código; "Sí, todos los factores…", sin código y con su texto
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM intento_paso WHERE sesion_id = ? AND orden_paso = 3 "
                + "AND tecnica_propuesta IS NULL AND expresion_propuesta = ?", Integer.class, p.sesionId(),
                PracticaService.SI_TERMINE));
    }

    @Test
    void ruffiniAspaSimpleYTerminado() {
        recorrer("2x^3 - 3x^2 - 3x + 2", List.of("F16", "F13", PracticaService.CODIGO_SI));
    }

    @Test
    void limiteEvaluarFactorizarYEvaluar() {
        Practica p = recorrer("lim(x→2, (x^2 - 4)/(x - 2))", List.of("L01", "L02", "L01"));
        assertEquals("Calcular límite", p.titulo());
    }

    @Test
    void enElLimiteNingunDistractorEsUnMetodoQueTambienFunciona() {
        Practica p = practica.iniciar("lim(x→2, (x^2 - 4)/(x - 2))", Campo.Q);
        // Paso 2 (0/0 en una fracción de polinomios): L'Hôpital también funciona, así que no es un error
        assertFalse(practica.motivos(p.sesionId(), 2).containsKey("L07"));
        // Paso 1: todavía no se evaluó; cualquier transformación es prematura
        assertTrue(practica.motivos(p.sesionId(), 1).values().stream().allMatch(m -> m.contains("primero")));
        // Paso 3: ya no hay indeterminación
        assertTrue(practica.motivos(p.sesionId(), 3).values().stream()
                .allMatch(m -> m.contains("no queda ninguna indeterminación")), practica.motivos(p.sesionId(), 3)
                .toString());
    }

    @Test
    void losDistractoresDeLaFactorizacionSalenPrimeroDeLosDescartados() {
        Practica p = practica.iniciar("x^3 - 2x^2 - x + 2", Campo.Q);
        // F01 se probó en la entrada y se descartó (HU05): es uno de los motivos del paso 1
        Map<String, String> m = practica.motivos(p.sesionId(), 1);
        assertTrue(m.containsKey("F01"), m.toString());
    }

    @Test
    void laPistaNoDiceElNombreDelMetodo() {
        Practica p = practica.iniciar("x^3 - 2x^2 - x + 2", Campo.Q);
        assertEquals("Mira si al juntar los términos de dos en dos aparece algo que se repite.",
                practica.pista(p.sesionId(), 1));
        String pista = practica.pista(p.sesionId(), 2);
        assertFalse(pista.toLowerCase().contains("diferencia de cuadrados"), pista);
    }

    @Test
    void soloSeRespondeElPasoQueToca() {
        Practica p = practica.iniciar("x^3 - 2x^2 - x + 2", Campo.Q);
        assertThrows(IllegalStateException.class, () -> practica.responder(p.sesionId(), 2, "F05"));
    }

    @Test
    void queSePuedePracticar() {
        assertTrue(practica.sePuedePracticar("x^3 - 2x^2 - x + 2", null));
        assertTrue(practica.sePuedePracticar("lim(x→2, 1/(x - 2))", null));
        assertFalse(practica.sePuedePracticar("x^2 - 2", Campo.R));
        assertFalse(practica.sePuedePracticar("d/dx(x^2 sen x)", null));
        assertFalse(practica.sePuedePracticar("", null));
        assertThrows(IllegalArgumentException.class, () -> practica.iniciar("d/dx(x^2)", null));
    }

    @Test
    void cadaEjercicioPropuestoTieneDosOTresDistractoresEnCadaPaso() {
        StringBuilder faltan = new StringBuilder();
        for (PracticaService.Propuesta x : practica.propuestas()) {
            Practica p = practica.iniciar(x.texto(), x.campo());
            for (int k = 1; k <= p.totalPasos(); k++) {
                Pregunta q = practica.opciones(p.sesionId(), k);
                int distractores = q.opciones().size() - 1;
                int minimo = PracticaService.TERMINASTE.equals(q.pregunta()) ? 1 : 2;
                if (distractores < minimo || distractores > 3) {
                    faltan.append(x.texto()).append(" paso ").append(k).append(": ").append(q.opciones()).append(System.lineSeparator());
                }
            }
        }
        assertTrue(faltan.isEmpty(), faltan.toString());
    }

    @Test
    void elegirUnEjercicioPropone() {
        List<PracticaService.Propuesta> p = practica.propuestas();
        assertTrue(p.stream().anyMatch(x -> x.texto().equals("2x^3 - 3x^2 - 3x + 2") && x.pasos() == 3), p.toString());
        assertTrue(p.stream().anyMatch(x -> x.tema().equals("Límites")));
        assertTrue(p.stream().allMatch(x -> x.pasos() > 0));
    }
}
