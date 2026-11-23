package pe.mathnova;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Comprueba que Flyway crea el esquema de la sección 12. Usa una base aparte en target/. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/prueba-mathnova.db")
class BaseDeDatosTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayCreaLas15Tablas() {
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' "
                        + "AND name NOT LIKE 'flyway%' AND name NOT LIKE 'sqlite%'", Integer.class);
        assertEquals(15, tablas);
    }

    @Test
    void losCatalogosVienenLlenos() {
        assertEquals(84, jdbc.queryForObject("SELECT COUNT(*) FROM tecnica", Integer.class));
        assertEquals(15, jdbc.queryForObject("SELECT COUNT(*) FROM operacion", Integer.class));
        assertEquals(13, jdbc.queryForObject("SELECT COUNT(*) FROM tema", Integer.class));
    }

    @Test
    void lasClavesForaneasEstanActivas() {
        assertEquals(1, jdbc.queryForObject("PRAGMA foreign_keys", Integer.class));
    }
}
